package cn.com.wufan;


import cn.com.wufan.tool.CreteMobi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;


public class test {
    private javax.swing.table.DefaultTableModel tList;

    public static void main(String[] args) {
        new CreteMobi("G:\\新建文件夹\\知轩藏书-藏尽网络中最好的精校小说", "G:\\test1\\知轩藏书-藏尽网络中最好的精校小说", "《剑之学者》作者：Le司马离.txt").createEBook();
    }

}
