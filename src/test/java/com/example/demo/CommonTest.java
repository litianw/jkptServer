package com.example.demo;

import com.hankcs.hanlp.corpus.io.IOUtil;
import com.hankcs.hanlp.mining.word2vec.DocVectorModel;
import com.hankcs.hanlp.mining.word2vec.Word2VecTrainer;
import com.hankcs.hanlp.mining.word2vec.WordVectorModel;

import java.io.IOException;
import java.util.Map;

public class CommonTest {

    private static final String TRAIN_FILE_NAME = "";
    private static final String MODEL_FILE_NAME = "F:\\IdeaProjects\\test\\hanlp-wiki-vec-zh\\hanlp-wiki-vec-zh.txt";

//    public static void main(String[] args) throws IOException {
//        String a = "关于拖欠工资相关问题";
//        String b = "关于拖欠工资的问题";
//
//        WordVectorModel wordVectorModel = trainOrLoadModel();
//        // 文档向量
//        DocVectorModel docVectorModel = new DocVectorModel(wordVectorModel);
//
//        System.out.println(docVectorModel.similarity(a, b));
//
//    }

    static void printNearest(String word, WordVectorModel model) {
        System.out.printf("\n                                                Word     Cosine\n------------------------------------------------------------------------\n");
        for (Map.Entry<String, Float> entry : model.nearest(word)) {
            System.out.printf("%50s\t\t%f\n", entry.getKey(), entry.getValue());
        }
    }

    static void printNearestDocument(String document, String[] documents, DocVectorModel model) {
        printHeader(document);
        for (Map.Entry<Integer, Float> entry : model.nearest(document)) {
            System.out.printf("%50s\t\t%f\n", documents[entry.getKey()], entry.getValue());
        }
    }

    private static void printHeader(String query) {
        System.out.printf("\n%50s          Cosine\n------------------------------------------------------------------------\n", query);
    }

    static WordVectorModel trainOrLoadModel() throws IOException {
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

    static WordVectorModel loadModel() throws IOException {
        return new WordVectorModel(MODEL_FILE_NAME);
    }

}
