package cn.com.wufan.tool;

import javax.swing.table.DefaultTableModel;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cn.com.wufan.Enum.BookSetting.*;


public class CreteMobi {

    //新保存目录
    private String savePrefix;
    //文件所在目录
    private String oldPrefix;
    //文件名
    private String fileName;

    private String filePath ;

    //存储各种配置
    private HashMap<String,Object> setting ;
    private DefaultTableModel tList;

    final String defCSS = "h2,h3,h4 { text-align: center; }\n\n@font-face { font-family: \"hei\"; src: local(\"Zfull-GB\"); }\n.content { font-family: \"hei\"; }\n";
    String css = defCSS;

    //存储失败的文件
    private List<String> errFile;



    public CreteMobi(String oldPrefix,String savePrefix,  String fileName) {
        this.savePrefix = savePrefix;
        this.oldPrefix = oldPrefix;
        this.fileName = fileName;
        this.filePath = oldPrefix + File.separator + fileName;
        this.setting = new HashMap<>();
        this.setting.put(ENCODING.getContext(), "GB18030");

        this.tList = new DefaultTableModel(new Object [][] {

        },
                new String [] {
                        "标题", "行号"
                });


    }

    /**
     * 将txt文本转换成mobi格式
     *
     * true 为成功，false为失败
     */
    public Boolean createEBook(){

        if(!fileName.endsWith(".txt")){
            System.out.println(filePath + "----文件不存在");
            return false;
        }


        File file = new File(savePrefix + File.separator + fileName.replace(".txt",".mobi"));
        if(file.exists()){
            System.out.println(fileName +"已存在");
            return false;
        }


        //定义支持的编码格式
        ArrayList<Object> encodings = new ArrayList<>();
        encodings.add("GB18030");
        encodings.add("UTF-8");
        encodings.add("UTF-16");



        //获取TOC: 标题: 行号
        //预处理标题
        Vector lists = null;
        int listslen = 0;
        for (int i = 0; i < encodings.size(); i++) {
            setting.put(ENCODING.getContext(),encodings.get(i));
            preProcessTxt();
            lists = tList.getDataVector();
            listslen = lists.size();
            if (listslen != 0) {
                break;
            }
        }

        //判断是否存在可解析编码
        if(listslen == 0){
            System.out.println(fileName + "编码格式不支持");
            return false;
        }



        //读取文本，切割成字符串数组,自动检测编码格式
        String s = ToolJava.readText(filePath, (String) setting.get(ENCODING.getContext()));
        String[] split = s.split("[\r]?\n");
        s = "";
        //txt文本行数
        int lCount = split.length;

        //创建保存路径
        File saveDir = new File(savePrefix);
        if(!saveDir.exists() || !saveDir.isDirectory()){
            saveDir.mkdirs();
        }

        long sTime = System.currentTimeMillis();



        //预处理元数据
        getMetaData();

        String bookname = (String) setting.get(BOOK_NAME.getContext());
        String author = (String) setting.get(AUTHOR.getContext());

        //格式化初始工作
        FoxEpubWriter oEpub = null;
        BufferedWriter bw = null;
        oEpub = new FoxEpubWriter(new File(saveDir + File.separator + fileName.replace(".txt",".mobi")),fileName.replace(",txt",""),savePrefix);
        oEpub.setBookCreator(author);
        oEpub.setCSS(css);

        int chaTitleNum = 0;
        int nextChaTitleNum = 0;
        String chaTitle = "";
        StringBuffer chaContent;
        String line = "";
        for (int i = 0; i < listslen; i++) {
            chaTitle = (String)((Vector)lists.get(i)).get(0);  // 标题行
            chaTitleNum = Integer.valueOf((String)((Vector)lists.get(i)).get(1)); // 标题行号
            chaTitle = chaTitle.replaceAll("^[ \t　]*", ""); // 删除头部空白
            if (  ( 1 + i ) == listslen ) { // 最后一个记录
                nextChaTitleNum = lCount + 1; // 下一个标题行号
            } else { //
                nextChaTitleNum = Integer.valueOf((String)((Vector)lists.get(i+1)).get(1)); // 下一个标题行号
            }

            chaContent = new StringBuffer(40960);
            for (int j = chaTitleNum + 1; j < nextChaTitleNum; j++) { // 两个标题行之间的区间即为正文内容
                line = split[j-1];
                // line = line.replaceAll("^[ \t　]*", "　　"); // 替换头部空白为两个空格
                if ( ( line.isEmpty() || line.equalsIgnoreCase("　　") )) {
                    continue;
                }
                chaContent.append(line).append("<br />\n"); // 行
            }
            //添加正文
            oEpub.addChapter(chaTitle, chaContent.toString());

        }


        //存储文件
        oEpub.saveAll();

        System.out.println("★"+fileName+"　生成完毕，耗时(ms): " + (System.currentTimeMillis() - sTime));

        return true;


    }

    /**
     * 预获取文件标题
     */
    void preProcessTxt(){
        File file = new File(filePath);
        String regex = ".*第[0-9零○一二两三四五六七八九十百千廿卅卌壹贰叁肆伍陆柒捌玖拾佰仟万１２３４５６７８９０]{1,5}[章节節]{1}.*";

        //标题长度 默认4096
        int titleMax = 4096;

        //清空目录记录
        tList.setRowCount(0);

        //开始生成mobi
        long sTime = System.currentTimeMillis();

        try {
           BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), (String) setting.get(ENCODING.getContext())));
           String line = "";
           int nLine = 0;
           while ((line = br.readLine()) != null){
               ++nLine;
               if(line.length() < titleMax){
                   if(line.matches(regex)){
                       tList.addRow(new Object[]{line,String.valueOf(nLine)});
                   }
               }
           }
           br.close();
        } catch (Exception e){
            System.out.println(e.toString());
        }


    }


    //获取书名，作者等元信息
    public void getMetaData(){
        //从书名中获取书名和作者
        //正则获取书名 《》中为书名，正则匹配不到则直接将文件名作为书名
        String bookRegex = "《(.*?)》";
        Pattern compile = Pattern.compile(bookRegex);
        Matcher matcher = compile.matcher(fileName);
        if(matcher.find()){
            setting.put(BOOK_NAME.getContext(),matcher.group(1));
        }else {
            setting.put(BOOK_NAME.getContext(),fileName.replace(".txt",""));
        }
        //获取作者
        String authorRegex = "作者：(.*?)\\.";
        compile = Pattern.compile(authorRegex);
        Matcher authorMatch = compile.matcher(fileName);
        if(authorMatch.find()){
            setting.put(AUTHOR.getContext(),authorMatch.group(1));
        }else {
            setting.put(AUTHOR.getContext(),"佚名");
        }

    }




    public String getSavePrefix() {
        return savePrefix;
    }

    public void setSavePrefix(String savePrefix) {
        this.savePrefix = savePrefix;
    }

    public String getOldPrefix() {
        return oldPrefix;
    }

    public void setOldPrefix(String oldPrefix) {
        this.oldPrefix = oldPrefix;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
