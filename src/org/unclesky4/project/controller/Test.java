package org.unclesky4.project.controller;

import java.io.File;

import org.unclesky4.project.entity.JudgeResult;
import org.unclesky4.project.service.JudgeRun;
import org.unclesky4.project.service.ServerConnection;
import org.unclesky4.project.utils.FileUtil;

import edu.csus.ecs.pc2.api.IContest;
import edu.csus.ecs.pc2.api.exceptions.LoginFailureException;
import edu.csus.ecs.pc2.api.exceptions.NotLoggedInException;
import edu.csus.ecs.pc2.core.execute.Executable;
import edu.csus.ecs.pc2.core.model.SerializedFile;

/**
*
* @author unclesky4
* @date Feb 28, 2018 9:09:44 PM
*
*/

public class Test {
	
	/**
	 * 获取程序的编译错误输出文件
	 * @param executable
	 * @return
	 */
	static File getErrorFile(Executable executable) {
		File file = null;
		if(executable.getExecutionData() != null && executable.getExecutionData().getCompileStderr() != null) {
			file = new File(executable.getExecutionData().getCompileStderr().getAbsolutePath());
		}
		return file;
	}
	
	/**
	 * 获取程序的输出文件
	 * @return
	 */
	static File getOutputFile(Executable executable) {
		String name = null;
		
		for(String tmp : executable.getTeamsOutputFilenames()) {
			String[] aStrings = tmp.split(File.separator);
			name = aStrings[aStrings.length-1];
		}
		File runOutputFile = new File(getExecuteDirectoryName(executable)+File.separator+name);
		return runOutputFile;
	}
	
	/**
	 * 获得编译目录的名称
	 * @param executable
	 * @return
	 */
	static String getExecuteDirectoryName(Executable executable) {
		SerializedFile tmp = null;
		tmp = executable.getExecutionData().getCompileStdout();
		if(tmp == null) {
			return "executesite1administrator1"+executable.getExecuteDirectoryNameSuffix();
		}
		return executable.getExecuteDirectoryName();
	}
	
	/**
	 * 获取编译目录的绝对路径
	 * @return
	 */
	static String getExecuteDirectoryPath(Executable executable) {
		String path = executable.getExecutionData().getCompileStdout().getAbsolutePath();
		String[] tmp = path.trim().split(System.getProperty("file.separator"));
		path = "";
		int length = tmp.length;
		for(int i = 1; i < length-1; i++)
			path = path + System.getProperty("file.separator") + tmp[i];
		return path;
	}

	public static void main(String[] args) throws NotLoggedInException {
		//连接服务器
		ServerConnection serverConnection = new ServerConnection();
		IContest iContest = null;
		
		//登陆--judge1
		try {
			iContest = serverConnection.login("judge1", "judge1");
			iContest = serverConnection.getContest();
		} catch (LoginFailureException e) {
			e.printStackTrace();
		} catch (NotLoggedInException e) {
			e.printStackTrace();
		}
		
		JudgeRun judgeRun = new JudgeRun(serverConnection);
		judgeRun.setInputFilePath("/home/uncle/Desktop/pc2_data/JudgeDataFile.input");
		judgeRun.setProgramFilePath("/home/uncle/Desktop/pc2_data/solve.java");
		JudgeResult result = judgeRun.executeRun("aa", "Java");
		System.out.println(result.getJudgeResultType().getDescription());
		
//		System.out.println(judgeRun.getExecutable().getExecuteDirectoryName());
//		System.out.println(judgeRun.getExecutable().getExecutionData().getExecuteProgramOutput().getAbsolutePath());
//		System.out.println(getExecuteDirectoryPath(judgeRun.getExecutable()));
		
		System.out.println(FileUtil.readFile(getOutputFile(judgeRun.getExecutable())));
		
		//断开连接
		try {
			serverConnection.logoff();
		} catch (NotLoggedInException e) {
			System.out.println("Unable to execute API method");
			e.printStackTrace();
		}
	}

}
