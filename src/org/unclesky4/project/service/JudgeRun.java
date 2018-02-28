package org.unclesky4.project.service;

import java.io.File;
import java.io.Serializable;

import org.unclesky4.project.entity.JudgeResult;
import org.unclesky4.project.enums.JudgeResultType;
import org.unclesky4.project.utils.FileUtil;
import org.unclesky4.project.utils.UUIDUtils;

import edu.csus.ecs.pc2.api.exceptions.NotLoggedInException;
import edu.csus.ecs.pc2.core.IInternalController;
import edu.csus.ecs.pc2.core.execute.Executable;
import edu.csus.ecs.pc2.core.execute.ExecutionData;
import edu.csus.ecs.pc2.core.model.ClientId;
import edu.csus.ecs.pc2.core.model.Filter;
import edu.csus.ecs.pc2.core.model.IInternalContest;
import edu.csus.ecs.pc2.core.model.Language;
import edu.csus.ecs.pc2.core.model.Problem;
import edu.csus.ecs.pc2.core.model.ProblemDataFiles;
import edu.csus.ecs.pc2.core.model.Run;
import edu.csus.ecs.pc2.core.model.RunFiles;
import edu.csus.ecs.pc2.core.model.SerializedFile;

/**
* 在线编译
* @author unclesky4
* @date Feb 28, 2018 7:23:45 PM
*
*/

public class JudgeRun implements Serializable {
	
	private static final long serialVersionUID = -6256897304969728503L;
	
	private IInternalContest contest;
	private IInternalController controller;
	private ClientId clientId;
	private String inputFilePath = null;
	private String programFilePath = null;
	
	private Executable executable;
	
	//创建用于存放用户提交的程序和数据的目录
	String tmpDir = FileUtil.tmpPath+UUIDUtils.getUUID()+"/"; 
	
	public JudgeRun(ServerConnection serverConnection) throws NotLoggedInException {
		this.contest = serverConnection.getIInternalContest();
		this.controller = serverConnection.getIInternalController();
		this.clientId = serverConnection.getIInternalContest().getClientId();
	}
	
	/**
	 * 获取PC^2中已存在的Language实例
	 * @param languageDisplayName
	 * @return
	 */
	Language getLanguage(String languageDisplayName) {
		Language[] languages = contest.getLanguages();
		Language language = null;
		for(int i=0; i<languages.length; i++) {
			if(languages[i].getDisplayName().equals(languageDisplayName)) {
				language = languages[i];
			}
		}
		return language;
	}
	
	/**
	 * 获取PC^2中已存在的Problem实例
	 * @param problemDisplayName
	 * @return
	 */
	Problem getProblem(String problemDisplayName) {
		Problem problem = null;
		Problem[] problems = contest.getProblems();
		for(int i=0; i<problems.length; i++) {
			if(problems[i].getShortName().equals(problemDisplayName)) {
				problem = problems[i];
			}
		}
		return problem;
	}
	
	/**
	 * 用户自定义输入数据
	 * @param inputContent
	 * @param outputContent
	 * @return
	 */
	public boolean setIOContent(String inputContent) {
		if(inputContent != "" || inputContent != null) {
			inputFilePath = tmpDir+System.getProperty("file.separator")+"data.input";
			File inputFile = FileUtil.createFile(inputFilePath);
			if(inputFile != null && inputFile.exists()) {
				if(!FileUtil.writeFile(inputFile, inputContent)) {
					FileUtil.deleteFile(inputFile);
					inputFilePath = null;
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * 设置主程序文件
	 * @param languageDisplayName
	 * @param programContent
	 * @return
	 */
	public boolean setProgramFile(String languageDisplayName, String programContent) {
		String programFilePath = null;
		switch (languageDisplayName) {
		case "Java":
			String className = getClassName(programContent);
			programFilePath = tmpDir+className+".java";
			break;
		case "GNU C++":
			programFilePath = tmpDir+"solve.cpp";
			break;
		case "GNU C":
			programFilePath = tmpDir+"solve.c";
			break;
		case "PHP":
			programFilePath = tmpDir+"solve.php";
			break;
		case "Python":
			programFilePath = tmpDir+"solve.py";
			break;
		default:
			System.err.println("未知的编程语言");
			return false;
		}
		if(programFilePath != null || programFilePath != "") {
			File programFile = FileUtil.createFile(programFilePath);
			if (programFile != null && programFile.exists()) {
				if(!FileUtil.writeFile(programFile, programContent)) {
					programFilePath = null;
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * 实例化一个Run对象
	 * @param language
	 * @param problem
	 * @return
	 */
	Run getRun(Language language, Problem problem) {
		return new Run(clientId, language, problem);
	}
	
	/**
	 * 编译运行
	 * @param problemName
	 * @param languageName
	 * @return
	 */
	public JudgeResult executeRun(String problemName, String languageName) {
		Problem problem = getProblem(problemName);
		if(problem == null) {
			System.out.println("问题不存在");
			return null;
		}
		Language language = getLanguage(languageName);
		if(language == null) {
			System.out.println("编程语言不存在");
			return null;
		}
		Run run = getRun(language, problem);
		if(run == null) {
			System.out.println("获取Run实例失败");
			return null;
		}
		
		if(inputFilePath != null) {
			ProblemDataFiles problemDataFiles = contest.getProblemDataFile(problem);
			problemDataFiles.setJudgesDataFile(new SerializedFile(inputFilePath));
			
			//设置自动评判
			contest.getClientSettings().setAutoJudging(true);
			Filter filter = new Filter();
			filter.addProblem(problem);
			contest.getClientSettings().setAutoJudgeFilter(filter);
			problem.setValidatedProblem(true);
			problem.setUsingPC2Validator(true);
			problem.setWhichPC2Validator(1);
			
			contest.updateProblem(problem, problemDataFiles);
		}else {
			ProblemDataFiles problemDataFiles = contest.getProblemDataFile(problem);
			problemDataFiles.setJudgesDataFile(new SerializedFile(problem.getColorName()));
			contest.updateProblem(problem, problemDataFiles);
		}
		
		SerializedFile[] serializedFiles = new SerializedFile[0];
		SerializedFile mainFile =  new SerializedFile(programFilePath);
		RunFiles runFiles = new RunFiles(run, mainFile, serializedFiles);
		
		executable = new Executable(contest, controller, run, runFiles);
		this.executable.setExecuteDirectoryNameSuffix(UUIDUtils.getUUID());
		executable.setUsingGUI(false);
		executable.execute();
		ExecutionData executionData = executable.getExecutionData();
		
		JudgeResult result = new JudgeResult(JudgeResultType.COMPILERFAILED);
		if (!executionData.isCompileSuccess()) {
			return result;
		}
		if(executionData.getExecuteExitValue() == 1) {
			result.setJudgeResultType(JudgeResultType.EXECUTEFAILED);
			return result;
		}
		if(executionData.isExecuteSucess() && executionData.getExecuteExitValue() == 0) {
			result.setJudgeResultType(JudgeResultType.SUCCESS);
			result.setCompileTimeMS(executionData.getCompileTimeMS());
			result.setExecuteTimeMS(executionData.getExecuteTimeMS());
			System.out.println("评判："+executionData.isValidationSuccess());
			return result;
		}
		return null;
	}
	

	public IInternalContest getContest() {
		return contest;
	}

	public void setContest(IInternalContest contest) {
		this.contest = contest;
	}

	public IInternalController getController() {
		return controller;
	}

	public void setController(IInternalController controller) {
		this.controller = controller;
	}

	public ClientId getClientId() {
		return clientId;
	}

	public void setClientId(ClientId clientId) {
		this.clientId = clientId;
	}

	public String getInputFilePath() {
		return inputFilePath;
	}

	public void setInputFilePath(String inputFilePath) {
		this.inputFilePath = inputFilePath;
	}

	public String getProgramFilePath() {
		return programFilePath;
	}

	public void setProgramFilePath(String programFilePath) {
		this.programFilePath = programFilePath;
	}

	public Executable getExecutable() {
		return executable;
	}

	public void setExecutable(Executable executable) {
		this.executable = executable;
	}
	
	/**
	 * 截取代码中的类名--针对java程序
	 * @param data - 程序代码
	 * @return
	 */
	String getClassName(String data) {
		int start = data.indexOf("class")+5;
		int end = data.indexOf("{");
		return data.substring(start, end).trim();
	}

}
