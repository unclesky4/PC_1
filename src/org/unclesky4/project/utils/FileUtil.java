package org.unclesky4.project.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * 文件工具类
 * @author unclesky4 28/09/2017
 *
 */
public class FileUtil {
	
	//存放添加到PC2的Problem所需要的数据文件和参考答案文件 (用户目录下的PC2Files目录)
	public static String path = "/home/uncle/PC2Files/data/".replace("/", System.getProperty("file.separator"));
	
	//临时保存程序文件和用户提交的程序输入产生的文件的父目录
	public static String tmpPath = "/home/uncle/PC2Files/programFile/";
	
	/**
	 * 创建目录
	 * @param destDirName
	 * @return 成功返回true，失败返回false
	 */
	public static boolean createDir(String destDirName) {  
        File dir = new File(destDirName);  
        if (dir.exists()) {  
            System.out.println("创建目录" + destDirName + "失败，目标目录已经存在");  
            return false;  
        }  
        if (!destDirName.endsWith(File.separator)) {  
            destDirName = destDirName + File.separator;  
        }  
        //创建目录  
        if (dir.mkdirs()) {  
            //System.out.println("创建目录" + destDirName + "成功！");  
            return true;  
        } else {  
            System.out.println("创建目录" + destDirName + "失败！");  
            return false;  
        }  
    }  
	
	/**
	 * 创建文件
	 * @param fileName - 文件绝对路径
	 * @return 返回null表示创建失败，成功返回创建的File对象
	 */
	public static File createFile(String fileName) {
		File file = new File(fileName);
		if(file.exists()) {
			return file;
		}
		if(fileName.endsWith(File.separator)) {
			System.out.println("创建'" + fileName + "'文件失败，目标文件不能为目录！");
			return null;
		}
		//判断目标文件所在的目录是否存在
        if(!file.getParentFile().exists()) {  
            if(!file.getParentFile().mkdirs()) {  
                System.out.println("创建目标文件所在目录失败！");  
                return null;  
            }  
        }  
      //创建目标文件
        if(file.exists()) {
        	return file;
        }
        try {  
            if (file.createNewFile()) {  
                //System.out.println("创建文件'" + fileName + "'成功！");  
                return file;  
            } else {  
                System.out.println("创建文件'" + fileName + "'失败！");  
                return null;  
            }  
        } catch (IOException e) {  
            e.printStackTrace();  
            System.out.println("创建文件'" + fileName + "'失败！" + e.getMessage());  
            return null;  
        }
	}
	
	/**
	 * 递归删除目录下的所有文件及子目录下所有文件
	 * @param directoryName
	 * @return
	 */
	public static boolean deleteDir(File dir) {
		if(!dir.exists()) {
			return true;
		}
        if (dir.isDirectory()) {
            String[] children = dir.list();
            //递归删除目录中的子目录下
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        // 目录此时为空，可以删除
        return dir.delete();
    }
	
	/**
	 * 删除一个文件
	 * @param file
	 * @return
	 */
	public static boolean deleteFile(File file) {
		if(file.isFile()) {
			return file.delete();
		}
		return false;
	}
	
	/**
	 * 写文件
	 * @param file
	 * @param data
	 * @return
	 */
	public static boolean writeFile(File file, String data) {
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter(file, false);
			if(fileWriter != null) {
				fileWriter.write(data);
			}
			fileWriter.flush();
			fileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("写入数据失败");
			return false;
		}
		//System.out.println("成功写入数据");
		return true;
	}
	
	/**
	 * 读文件
	 * @param file
	 * @return
	 */
	public static String readFile(File file) {
		BufferedReader reader = null;
		StringBuffer info = new StringBuffer();
		String tempString = "";
		try {
			reader = new BufferedReader(new FileReader(file));
			while ((tempString = reader.readLine()) != null) {
	            info.append(tempString+"\n");
            }
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
		return info.toString();
	}
	
	/**
	 * 测试
	 * @param args
	 */
/*	public static void main(String[] args) {
		boolean b = FIleUtil.deleteDir(new File("/home/uncle/workspace-neon/pc2/executesite1judge12b96063115ed4722a2bf753c6a6bdf0f"));
		System.out.println(b);
	}*/

}
