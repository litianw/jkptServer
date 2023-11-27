package com.example.demo;

import com.hankcs.hanlp.corpus.io.IOUtil;
import com.hankcs.hanlp.mining.word2vec.DocVectorModel;
import com.hankcs.hanlp.mining.word2vec.Word2VecTrainer;
import com.hankcs.hanlp.mining.word2vec.WordVectorModel;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@SpringBootApplication
@RestController
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Value("${minganci}")
    String minganci;

    @Value("${MODEL_FILE_NAME}")
    String MODEL_FILE_NAME;

    @Value("${TRAIN_FILE_NAME}")
    String TRAIN_FILE_NAME;

    @Value("${drdrc_limit}")
    int drdrc_limit;

    @Value("${drdrc_limit}")
    int ffts_limit;

    @GetMapping("/getTjsj")
    public Map getTjsj() {
        //总计单数
        List<Map<String, Object>> total = jdbcTemplate.queryForList("select count(1) as num from tsgd");

        //反复投诉
        List<Map<String, Object>> ffts = jdbcTemplate.queryForList("select count(1) as num from tsgd_ffts");

        //多人多次投诉
        List<Map<String, Object>> drdcts = jdbcTemplate.queryForList("select count(1) as num from tsgd_drdcts");

        //敏感词统计

        String[] arr = minganci.split("、");
        String params = "";
        for (String s : arr) {
            params += "context like '%" + s + "%' or";
        }
        List<Map<String, Object>> mgctj = jdbcTemplate.queryForList("select count(1) as num from tsgd where  \t" + params.substring(0, params.length() - 2));

        //违规违纪
        List<Map<String, Object>> wgwj = jdbcTemplate.queryForList("select count(1) as num from wjtj");

        Map<String, Object> result = new HashMap();
        result.put("gdzj", total.get(0).get("num"));
        result.put("ffts", ffts.get(0).get("num"));
        result.put("drdcts", drdcts.get(0).get("num"));
        result.put("mgcsc", mgctj.get(0).get("num"));
        result.put("wgwj", wgwj.get(0).get("num"));
        result.put("myd", 100);
        return result;
    }

    @GetMapping("/getCfts")
    public Map getCfts() {
        List<Map<String, Object>> ffts = jdbcTemplate.queryForList("select a.* from tsgd_ffts b,tsgd a where  b.gdid = a.id ORDER BY b.xh desc");
        return Collections.singletonMap("data", ffts);
    }

    @GetMapping("/getDrdc")
    public Map getDrdc() {
        List<Map<String, Object>> drdcts = jdbcTemplate.queryForList("select a.* from tsgd_drdcts b,tsgd a where  b.gdid = a.id ORDER BY b.xh desc");
        return Collections.singletonMap("data", drdcts);
    }

    public void exportDrdc(HttpServletResponse response) throws IOException {
        List<Map<String, Object>> drdcts = jdbcTemplate.queryForList("select a.* from tsgd_drdcts b,tsgd a where  b.gdid = a.id ORDER BY b.xh desc");
        Workbook workbook = new XSSFWorkbook();
        OutputStream outputStream = response.getOutputStream();
        workbook.createSheet("多人多次投诉");
        createHeader(workbook.getSheetAt(0));
        //返回下载文件
        try {
            response.setContentType("application/vnd.ms-excel");
            response.setHeader("Content-Disposition", "attachment;filename=" + new String(("多人多次投诉" + ".xlsx").getBytes("gb2312"), "iso-8859-1"));

            workbook.write(outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            outputStream.flush();
            outputStream.close();
            workbook.close();
        }
    }

    String[] header = {"序号", "标题","诉求内容","来电号码","交办时间"};
    String[] ziduan = {"xh", "title","context","ldhm","jbsj"};
    private void createHeader(Sheet sheet) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < header.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(header[i]);
        }
    }

    @GetMapping("/getMgcsc")
    public Map getMgcsc() {
        //敏感词统计

        String[] arr = minganci.split("、");
        String params = "";
        for (String s : arr) {
            params += "context like '%" + s + "%' or";
        }
        List<Map<String, Object>> mgctj = jdbcTemplate.queryForList("select *  from tsgd where  \t" + params.substring(0, params.length() - 2));
        return Collections.singletonMap("data", mgctj);
    }

    @GetMapping("/getWjtj")
    public Map getWjtj() {
        List<Map<String, Object>> wjtj = jdbcTemplate.queryForList("select * from wjtj");
        return Collections.singletonMap("data", wjtj);
    }

    @RequestMapping("/upload/{fileType}")
    public String upload(MultipartFile file, @PathVariable String fileType) throws IOException {


        Workbook workbook = null;
        try {
            //读取文件数据
            workbook = new XSSFWorkbook(file.getInputStream());
            Sheet sheet = workbook.getSheetAt(0); //get the first sheet
            if (fileType.equals("")) {

                insertTsgd(sheet);
            }

            workbook.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            file.getInputStream().close();
            workbook.close();
        }

        System.out.println("导入完成");
        return "success";
    }

    private void insertTsgd(Sheet sheet) throws IOException {
        List<Map<String, String>> list = new ArrayList<>();
        int lastRowNum = sheet.getLastRowNum();
        for (int i = 2; i <= lastRowNum; i++) {
            Row row = sheet.getRow(i);
            Map<String, String> map = new HashMap<>();
            map.put("title", getCellValue(row.getCell(2)));
            map.put("context", getCellValue(row.getCell(3)));
            map.put("ldhm", getCellValue(row.getCell(4)));
            map.put("jbsj", getCellValue(row.getCell(5)));
            list.add(map);
        }

        SqlParameterSource[] params = SqlParameterSourceUtils.createBatch(list.toArray());
        jdbcTemplate.update("truncate table tsgd");
        //数据入库
        namedParameterJdbcTemplate.batchUpdate("insert into tsgd (title,context,ldhm,jbsj) values (:title,:context,:ldhm,:jbsj)", params);
        //
        WordVectorModel wordVectorModel = trainOrLoadModel();
        // 文档向量
        DocVectorModel docVectorModel = new DocVectorModel(wordVectorModel);
        computdrdcts(docVectorModel);

    }


    public void computdrdcts(DocVectorModel docVectorModel) throws IOException {
        List<Map<String, Object>> savaList = jdbcTemplate.queryForList("select * from tsgd");


        Map<String, List<String>> flMap = new HashMap<>();

        List<String> temList = new ArrayList<>();
        for (Map<String, Object> map : savaList) {
            if (temList.contains(map.get("id").toString())) {
                continue;
            }
            temList.clear();
            Set<String> types = flMap.keySet();
            String title = map.get("title").toString();
            boolean flag = false;
            for (String type : types) {
                if (docVectorModel.similarity(type, title) >= 0.8) {
                    flag = true;
                    break;
                }
            }
            if (flag) {
                continue;
            }

            flMap.put(title, new ArrayList<String>());
            flMap.get(title).add(map.get("id").toString());
            for (Map<String, Object> map1 : savaList) {
                if (!flMap.get(title).contains(map1.get("id").toString())) {
                    if (docVectorModel.similarity(title, map1.get("title").toString()) >= 0.8) {
                        flMap.get(title).add(map1.get("id").toString());
                    }
                }
            }
            temList.addAll(flMap.get(title));
        }


        Set<String> fl = flMap.keySet();
        List<Map<String, Object>> list = new ArrayList<>();
        for (String s : fl) {
            if (flMap.get(s).size() >= drdrc_limit) {
                for (String s1 : flMap.get(s)) {
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("xh", flMap.get(s).size());
                    map.put("gdid", Integer.parseInt(s1));
                    list.add(map);
                }
            }

        }

        SqlParameterSource[] params = SqlParameterSourceUtils.createBatch(list.toArray());
        jdbcTemplate.update("truncate table tsgd_drdcts");
        //数据入库
        namedParameterJdbcTemplate.batchUpdate("insert into tsgd_drdcts (gdid,xh) values (:gdid,:xh)", params);

        computFfts();

    }

    private void computFfts() {
        List<Map<String, Object>> drdctsList = jdbcTemplate.queryForList("select a.* from tsgd_drdcts b,tsgd a where  b.gdid = a.id ORDER BY b.xh desc");
        jdbcTemplate.update("truncate table tsgd_ffts");

        HashMap<String, List<String>> umap = new HashMap<>();
        for (Map<String, Object> map1 : drdctsList) {
            if (umap.containsKey(map1.get("ldhm").toString())) {
                umap.get(map1.get("ldhm").toString()).add(map1.get("id").toString());
            } else {
                List<String> list1 = new ArrayList<>();
                list1.add(map1.get("id").toString());
                umap.put(map1.get("ldhm").toString(), list1);
            }
        }
        List<Map<String, Object>> list = new ArrayList<>();
        Set<String> fl = umap.keySet();
        for (String s : fl) {
            if (umap.get(s).size() >= ffts_limit) {
                for (String s1 : umap.get(s)) {
                    HashMap<String, Object> map1 = new HashMap<>();
                    map1.put("xh", umap.get(s1).size());
                    map1.put("gdid", Integer.parseInt(s1));
                    list.add(map1);
                }
            }

        }
        if (list.size() == 0) {
            return;
        }
        SqlParameterSource[] params = SqlParameterSourceUtils.createBatch(list.toArray());
        jdbcTemplate.update("truncate table tsgd_ffts");
        //数据入库
        namedParameterJdbcTemplate.batchUpdate("insert into tsgd_ffts (gdid,xh) values (:gdid,:xh)", params);
    }

    private String getCellValue(Cell cell) {
        // 获取单元格的值并返回

        String value = "";
        try {
            // 尝试获取单元格的字符串值
            value = cell.getStringCellValue();
        } catch (Exception e) {
            try {
                // 如果获取字符串值失败，则尝试获取单元格的数值并转换为字符串
                value = new BigDecimal(cell.getNumericCellValue()).setScale(0).toString();
            } catch (Exception e1) {
                // 如果获取数值值失败，则获取单元格的日期值并格式化为字符串
                value = format(cell.getDateCellValue());
            }
        }

        return value;
    }


    //格式化时间
    private String format(Date date) {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String format = "";
        try {
            format = simpleDateFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return format;
    }

    private WordVectorModel trainOrLoadModel() throws IOException {
        if (!IOUtil.isFileExisted(MODEL_FILE_NAME)) {
            if (!IOUtil.isFileExisted(TRAIN_FILE_NAME)) {
                System.err.println("语料不存在，请阅读文档了解语料获取与格式：https://github.com/hankcs/HanLP/wiki/word2vec");
                System.exit(1);
            }
            Word2VecTrainer trainerBuilder = new Word2VecTrainer();
            return trainerBuilder.train(TRAIN_FILE_NAME, MODEL_FILE_NAME);
        }

        return loadModel();
    }

    private WordVectorModel loadModel() throws IOException {
        return new WordVectorModel(MODEL_FILE_NAME);
    }
}
