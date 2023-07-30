package cn.com.wufan;


import cn.com.wufan.tool.CreteMobi;
import cn.com.wufan.tool.ToolJava;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MobiMain {

    public static void main(String[] args) {
        //文件目录
        String oldDir = "G:\\新建文件夹\\知轩藏书-藏尽网络中最好的精校小说";

        //新存储目录
        String newDir = "G:\\书籍转换\\知轩藏书-藏尽网络中最好的精校小说";

        File file = new File(newDir);
        if(!file.isDirectory() || !file.exists()){
            file.mkdirs();
        }
        //获取目录下所有文件夹
        List<String> allFile = ToolJava.getAllFile(oldDir);
        List<String> list = Collections.synchronizedList(allFile);
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch countDownLatch = new CountDownLatch(list.size());
        for (String fileName : list) {
            //使用多线程
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    if(fileName.endsWith(".txt")){
                        File file1 = new File(newDir + File.separator + fileName.replace(".txt",".mobi"));
                        if(!file1.exists()){
                            new CreteMobi(oldDir, newDir,fileName).createEBook();

                        }else {
                            System.out.println(fileName + "已存在");
                        }
                    }
                    countDownLatch.countDown();
                }
            });

        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            executor.shutdown();
        }


    }
}
