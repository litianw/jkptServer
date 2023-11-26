package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
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

    @Value("minganci")
    String minganci;


    @GetMapping("/getTjsj")
    public Map getTjsj() {
        //总计单数
        List<Map<String, Object>> total = jdbcTemplate.queryForList("select count(1) as num from tsgd");

        //反复投诉
        List<Map<String, Object>> ffts = jdbcTemplate.queryForList("select count(1) as num from tsgd_ffts");

        //多人多次投诉
        List<Map<String, Object>> drdcts = jdbcTemplate.queryForList("select count(1) as num from tsgd_drdcts");

        //敏感词统计

        String[] arr = minganci.split("，");
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
        List<Map<String, Object>> ffts = jdbcTemplate.queryForList("select *  from tsgd_ffts");
        return Collections.singletonMap("data", ffts);
    }

    @GetMapping("/getDrdc")
    public Map getDrdc() {
        List<Map<String, Object>> drdcts = jdbcTemplate.queryForList("select * from tsgd_drdcts");
        return Collections.singletonMap("data", drdcts);
    }

    @GetMapping("/getMgcsc")
    public Map getMgcsc() {
        //敏感词统计

        String[] arr = minganci.split("，");
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

         // 获取文件名
        String fileName = file.getOriginalFilename();
        // 在file文件夹中创建名为fileName的文件
        OutputStreamWriter op = new OutputStreamWriter(new FileOutputStream("./file/" + fileName), "UTF-8");
        // 获取文件输入流
        InputStreamReader inputStreamReader = new InputStreamReader(file.getInputStream());
        try {
            char[] bytes = new char[12];
            // 如果这里的bytes不是数组，则每次只会读取一个字节，例如test会变成 t   e     s    t
            while (inputStreamReader.read(bytes) != -1){
                op.write(bytes);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            // 关闭输出流
            op.close();
            // 关闭输入流
            inputStreamReader.close();
        }

        return "success";
    }
}
