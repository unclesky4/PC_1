package org.unclesky4.project.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;

import edu.csus.ecs.pc2.api.IClient;
import edu.csus.ecs.pc2.api.IContest;
import edu.csus.ecs.pc2.api.ILanguage;
import edu.csus.ecs.pc2.api.IProblem;
import edu.csus.ecs.pc2.api.exceptions.LoginFailureException;
import edu.csus.ecs.pc2.api.exceptions.NotLoggedInException;
import edu.csus.ecs.pc2.api.implementation.Contest;
import edu.csus.ecs.pc2.api.implementation.LanguageImplementation;
import edu.csus.ecs.pc2.api.implementation.ProblemImplementation;
import edu.csus.ecs.pc2.api.listener.IConnectionEventListener;
import edu.csus.ecs.pc2.core.IInternalController;
import edu.csus.ecs.pc2.core.InternalController;
import edu.csus.ecs.pc2.core.ParseArguments;
import edu.csus.ecs.pc2.core.PermissionGroup;
import edu.csus.ecs.pc2.core.exception.IllegalContestState;
import edu.csus.ecs.pc2.core.model.Account;
import edu.csus.ecs.pc2.core.model.ClientId;
import edu.csus.ecs.pc2.core.model.ClientType;
import edu.csus.ecs.pc2.core.model.ContestTime;
import edu.csus.ecs.pc2.core.model.IInternalContest;
import edu.csus.ecs.pc2.core.model.InternalContest;
import edu.csus.ecs.pc2.core.model.Language;
import edu.csus.ecs.pc2.core.model.LanguageAutoFill;
import edu.csus.ecs.pc2.core.model.Problem;
import edu.csus.ecs.pc2.core.model.ProblemDataFiles;
import edu.csus.ecs.pc2.core.model.SerializedFile;
import edu.csus.ecs.pc2.core.security.Permission;

/**
 * 封装PC^2方法 - 不使用PC^2包中的ServerConnection类
 * @author unclesky4 28/09/2017
 *
 */

public class ServerConnection {
	private String[] problemPropertyNames = { "JUDGING_TYPE", "VALIDATOR_PROGRAM", "VALIDATOR_COMMAND_LINE" };
	protected IInternalController controller;
	protected IInternalContest internalContest;
	private Contest contest = null;

	/**
	 * PC^2角色用户登陆
	 * @param account - 帐号
	 * @param password - 密码
	 * @return
	 * @throws LoginFailureException
	 */
	public IContest login(String account, String password) throws LoginFailureException {
		boolean overrideContestUsed = false;
		if (this.contest != null) {
			throw new LoginFailureException("Already logged in as: " + this.contest.getMyClient().getLoginName());
		}
		if (this.internalContest == null) {
			this.internalContest = new InternalContest();
		} else {
			overrideContestUsed = true;
		}
		if (this.controller == null) {
			this.controller = new InternalController(this.internalContest);
		}
		this.controller.setUsingGUI(false);
		if ((this.controller instanceof InternalController)) {
			((InternalController) this.controller).setUsingMainUI(false);
			((InternalController) this.controller).setHaltOnFatalError(false);
		}
		this.controller.setClientAutoShutdown(false);
		try {
			this.controller.start(new String[0]);
			if (!overrideContestUsed) {
				this.internalContest = this.controller.clientLogin(this.internalContest, account, password);
			}
			this.contest = new Contest(this.internalContest, this.controller, this.controller.getLog());
			this.contest.addConnectionListener(new ConnectionEventListener());
			this.controller.register(this.contest);

			return this.contest;
		} catch (Exception e) {
			throw new LoginFailureException(e.getMessage());
		}
	}

	/**
	 * 判断是否是有效的客户端类型
	 * @param name
	 * @return
	 */
	public boolean isValidAccountTypeName(String name) {
		try {
			ClientType.Type clientType = ClientType.Type.valueOf(name);
			return clientType != null;
		} catch (IllegalArgumentException localIllegalArgumentException) {
		}
		return false;
	}

	/**
	 * 添加帐号 - ADMINISTRATOR角色权限
	 * @param accountTypeName - 帐号类型（team， judge， administrator）
	 * @param displayName - 帐号名
	 * @param password - 密码
	 * @throws Exception
	 */
	public void addAccount(String accountTypeName, String displayName, String password) throws Exception {
		accountTypeName = accountTypeName.toUpperCase();
		if (!isValidAccountTypeName(accountTypeName)) {
			throw new IllegalArgumentException("Invalid account type name '" + accountTypeName + "'");
		}
		checkIsAllowed(Permission.Type.ADD_ACCOUNT, "This login/account is not allowed to add an account");

		ClientType.Type clientType = ClientType.Type.valueOf(accountTypeName);

		ClientId clientId = new ClientId(this.internalContest.getSiteNumber(), clientType, 0);
		if ((password == null) || (password.trim().length() == 0)) {
			throw new IllegalArgumentException("Invalid password (null or missing) '" + password + "'");
		}
		if (displayName == null) {
			displayName = clientId.getName();
		}
		Account account = new Account(clientId, password, this.internalContest.getSiteNumber());
		account.setDisplayName(displayName);

		account.clearListAndLoadPermissions(new PermissionGroup().getPermissionList(clientType));

		this.controller.addNewAccount(account);
	}

	/**
	 * 参赛团队提交疑问
	 * @param problem - 题目
	 * @param question - 疑问
	 * @throws Exception
	 */
	public void submitClarification(IProblem problem, String question) throws Exception {
		checkWhetherLoggedIn();

		checkIsAllowed(Permission.Type.SUBMIT_CLARIFICATION, "User not allowed to submit clarification");

		ProblemImplementation problemImplementation = (ProblemImplementation) problem;
		Problem submittedProblem = this.internalContest.getProblem(problemImplementation.getElementId());
		if (submittedProblem == null) {
			throw new Exception("Could not find any problem matching: '" + problem.getName());
		}
		if (!this.contest.isContestClockRunning()) {
			throw new Exception("Contest is STOPPED - no clarifications accepted.");
		}
		try {
			this.controller.submitClarification(submittedProblem, question);
		} catch (Exception e) {
			throw new Exception("Unable to submit clarifications " + e.getLocalizedMessage());
		}
	}

	private void checkWhetherLoggedIn() throws NotLoggedInException {
		if ((this.contest == null) || (this.internalContest == null)) {
			throw new NotLoggedInException("Not logged in");
		}
	}

	/**
	 * TEAM角色提交一个Run (Contest的时钟必须是开始状态)
	 * @param problem - 问题
	 * @param language - 编程语言
	 * @param mainFileName - 主程序文件路径(String)
	 * @param additionalFileNames - 附加的文件路径(String)
	 * @param overrideSubmissionTimeMS
	 * @param overrideRunId
	 * @throws Exception
	 */
/*	public void submitRun(IProblem problem, ILanguage language, String mainFileName, String[] additionalFileNames,
			long overrideSubmissionTimeMS, long overrideRunId) throws Exception {
		checkWhetherLoggedIn();

		checkIsAllowed(Permission.Type.SUBMIT_RUN, "User not allowed to submit run");
		if (!new File(mainFileName).isFile()) {
			throw new Exception("File '" + mainFileName + "' no such file (not found)");
		}
		SerializedFile[] list = new SerializedFile[additionalFileNames.length];
		for (int i = 0; i < additionalFileNames.length; i++) {
			if (new File(additionalFileNames[i]).isFile()) {
				list[i] = new SerializedFile(additionalFileNames[i]);
			} else {
				throw new Exception("File '" + additionalFileNames[i] + "' no such file (not found)");
			}
		}
		ProblemImplementation problemImplementation = (ProblemImplementation) problem;
		Problem submittedProblem = this.internalContest.getProblem(problemImplementation.getElementId());

		LanguageImplementation languageImplementation = (LanguageImplementation) language;
		Language submittedLanguage = this.internalContest.getLanguage(languageImplementation.getElementId());
		if (submittedProblem == null) {
			throw new Exception("Could not find any problem matching: '" + problem.getName());
		}
		if (submittedLanguage == null) {
			throw new Exception("Could not find any language matching: '" + language.getName());
		}
		if (!this.contest.isContestClockRunning()) {
			throw new Exception("Contest is STOPPED - no runs accepted.");
		}
		try {
			this.controller.submitRun(submittedProblem, submittedLanguage, mainFileName, list, overrideSubmissionTimeMS,
					overrideRunId);
		} catch (Exception e) {
			throw new Exception("Unable to submit run " + e.getLocalizedMessage());
		}
	}*/
	
	/**
	 * TEAM角色提交一个Run (Contest的时钟必须是开始状态)
	 * @param problem - 问题
	 * @param language - 编程语言
	 * @param mainFileName - 主程序文件路径(String)
	 * @param additionalFileNames - 附加的文件路径(String)
	 * @throws Exception
	 */
	public void submitRun(IProblem problem, ILanguage language, String mainFileName, String[] additionalFileNames) throws Exception {
		checkWhetherLoggedIn();

		checkIsAllowed(Permission.Type.SUBMIT_RUN, "User not allowed to submit run");
		if (!new File(mainFileName).isFile()) {
			throw new Exception("File '" + mainFileName + "' no such file (not found)");
		}
		SerializedFile[] list = new SerializedFile[additionalFileNames.length];
		for (int i = 0; i < additionalFileNames.length; i++) {
			if (new File(additionalFileNames[i]).isFile()) {
				list[i] = new SerializedFile(additionalFileNames[i]);
			} else {
				throw new Exception("File '" + additionalFileNames[i] + "' no such file (not found)");
			}
		}
		ProblemImplementation problemImplementation = (ProblemImplementation) problem;
		Problem submittedProblem = this.internalContest.getProblem(problemImplementation.getElementId());

		LanguageImplementation languageImplementation = (LanguageImplementation) language;
		Language submittedLanguage = this.internalContest.getLanguage(languageImplementation.getElementId());
		if (submittedProblem == null) {
			throw new Exception("Could not find any problem matching: '" + problem.getName());
		}
		if (submittedLanguage == null) {
			throw new Exception("Could not find any language matching: '" + language.getName());
		}
		if (!this.contest.isContestClockRunning()) {
			throw new Exception("Contest is STOPPED - no runs accepted.");
		}
		
		try {
			this.controller.submitRun(submittedProblem, submittedLanguage, mainFileName, list);
		} catch (Exception e) {
			throw new Exception("Unable to submit run " + e.getLocalizedMessage());
		}
	}

	/**
	 * 关闭PC^2连接
	 * @return
	 * @throws NotLoggedInException
	 */
	public boolean logoff() throws NotLoggedInException {
		if (this.contest == null) {
			throw new NotLoggedInException("Can not log off, not logged in");
		}
		try {
			this.controller.logoffUser(this.internalContest.getClientId());
			this.contest.setLoggedIn(false);
			this.contest = null;
			return true;
		} catch (Exception e) {
			throw new NotLoggedInException(e);
		}
	}

	/**
	 * 获取Contest（比赛）
	 * @return
	 * @throws NotLoggedInException
	 */
	public Contest getContest() throws NotLoggedInException {
		if (this.contest != null) {
			return this.contest;
		}
		throw new NotLoggedInException("Can not get IContest, not logged in");
	}

	/**
	 * 判断是否登陆
	 * @return
	 */
	public boolean isLoggedIn() {
		return (this.contest != null) && (this.contest.isLoggedIn());
	}

	public IClient getMyClient() throws NotLoggedInException {
		if (this.contest != null) {
			return this.contest.getMyClient();
		}
		throw new NotLoggedInException("Not logged in");
	}

	protected class ConnectionEventListener implements IConnectionEventListener {
		protected ConnectionEventListener() {
		}

		public void connectionDropped() {
			ServerConnection.this.contest = null;
		}
	}

	/**
	 * 开启比赛时钟
	 * @throws Exception
	 */
	public void startContestClock() throws Exception {
		checkWhetherLoggedIn();

		checkIsAllowed(Permission.Type.START_CONTEST_CLOCK, "User not allowed to start contest clock");
		try {
			this.controller.startContest(this.internalContest.getSiteNumber());
		} catch (Exception e) {
			throw new Exception("Unable to start Contest " + e.getLocalizedMessage());
		}
	}

	/**
	 * 停止比赛
	 * @throws Exception
	 */
	public void stopContestClock() throws Exception {
		checkWhetherLoggedIn();

		checkIsAllowed(Permission.Type.STOP_CONTEST_CLOCK, "User not allowed to start contest clock");
		try {
			this.controller.stopContest(this.internalContest.getSiteNumber());
		} catch (Exception e) {
			throw new Exception("Unable to stop Contest " + e.getLocalizedMessage());
		}
	}

	/**
	 * 设置验证器（评判类型）
	 * @param problem
	 */
	protected void setPC2Validator(Problem problem) {
		problem.setValidatedProblem(true);
		problem.setValidatorCommandLine("{:validator} {:infile} {:outfile} {:ansfile} {:resfile} ");

		problem.setUsingPC2Validator(true);
		problem.setWhichPC2Validator(1);
		problem.setIgnoreSpacesOnValidation(true);
		problem.setValidatorCommandLine("{:validator} {:infile} {:outfile} {:ansfile} {:resfile}  -pc2 "
				+ problem.getWhichPC2Validator() + " " + problem.isIgnoreSpacesOnValidation());
		problem.setValidatorProgramName("pc2.jar edu.csus.ecs.pc2.validator.Validator");
	}

	/**
	 * 添加题目
	 * @param title - 题目
	 * @param shortName - 题目短描述
	 * @param judgesDataFile - File类型的输入数据文件
	 * @param judgesAnswerFile - File类型的参考答案文件
	 * @param validated - 是否使用验证器（boolean）
	 * @param problemProperties - 一些可选属性（JUDGING_TYPE，VALIDATOR_PROGRAM， VALIDATOR_COMMAND_LINE）
	 * @param timeOutInSeconds - 程序超时时间（int 秒）
	 */
	public void addProblem(String title, String shortName, File judgesDataFile, File judgesAnswerFile,
			boolean validated, Properties problemProperties) {
		checkNotEmpty("Problem title", title);
		checkNotEmpty("Problem short name", shortName);
		checkFile("Judges data file", judgesDataFile);

		checkIsAllowed(Permission.Type.ADD_PROBLEM);

		Problem problem = new Problem(title);
		problem.setReadInputDataFromSTDIN(true);    //设置输入方式为stdin
		problem.setShortName(shortName);
//		problem.setTimeOutInSeconds(timeOutInSeconds);   //设置超时时间
		problem.setDataFileName(judgesDataFile.getName());
		problem.setAnswerFileName(judgesAnswerFile.getName());

		String[] invalids = validateProperties(problemProperties);
		if (invalids.length > 0) {
			throw new IllegalArgumentException("Unknown/Invalid property names: " + Arrays.toString(invalids));
		}
		String judgingType = getProperty(problemProperties, "JUDGING_TYPE", null);
		/*if (judgingType != null)
	    {
	      if (("MANUAL_JUDGING_ONLY".equals(judgingType)) && (validated)) {
	        throw new IllegalArgumentException("Problem cannot be validated and not judging type computer judged");
	      }
	    }
	    else {
	      judgingType = "MANUAL_JUDGING_ONLY";
	    }*/
		//------重写代码----------------strat
		if (judgingType != null) {
			if (judgingType.equals("COMPUTER_JUDGING_ONLY")) {   //自动评判
				problem.setComputerJudged(true);
				problem.setManualReview(false);
			} else if(judgingType.equals("MANUAL_JUDGING_ONLY")){   //手动评判
				problem.setComputerJudged(false);
				problem.setManualReview(true);
			}else {
				problem.setComputerJudged(true);
				problem.setManualReview(true);
			}
		}
		//------------------------end

		boolean usingPc2Validator = false;
		String validatorProgram = getProperty(problemProperties, "VALIDATOR_PROGRAM",
				"pc2.jar edu.csus.ecs.pc2.validator.Validator");
		usingPc2Validator = "pc2.jar edu.csus.ecs.pc2.validator.Validator".equals(validatorProgram);
		if (validated) {
			problem.setValidatedProblem(validated);

			String validatorCommandLine = getProperty(problemProperties, "VALIDATOR_COMMAND_LINE",
					"{:validator} {:infile} {:outfile} {:ansfile} {:resfile} ");
			problem.setValidatorCommandLine(validatorCommandLine);
			if (usingPc2Validator) {
				setPC2Validator(problem);
			}
		}
		problem.setShowValidationToJudges(false);
		problem.setHideOutputWindow(true);
		//用Problem的colorName属性保存输入文件路径作为备用
		problem.setColorName(judgesDataFile.getAbsolutePath());

		ProblemDataFiles problemDataFiles = new ProblemDataFiles(problem);
		problemDataFiles.setJudgesDataFile(new SerializedFile(judgesDataFile.getAbsolutePath()));
		problemDataFiles.setJudgesAnswerFile(new SerializedFile(judgesAnswerFile.getAbsolutePath()));
		if ((validated) && (!usingPc2Validator)) {
			if (new File(validatorProgram).isFile()) {
				SerializedFile validatorFile = new SerializedFile(validatorProgram);
				problemDataFiles.setValidatorFile(validatorFile);
			}
		}
		this.controller.addNewProblem(problem, problemDataFiles);
	}

	protected String getProperty(Properties problemProperties, String key, String defaultValue) {
		String value = null;
		if (problemProperties != null) {
			value = problemProperties.getProperty(key);
			if (value == null) {
				value = defaultValue;
			}
		}
		return value;
	}

	protected String[] validateProperties(Properties properties) {
		if (properties == null) {
			return new String[0];
		}
		ArrayList<String> unknownKeys = new ArrayList<String>();

		String[] names = getProblemPropertyNames();

		Set<Object> keys = properties.keySet();
		for (Object object : keys) {
			String key = (String) object;
			boolean found = false;
			String[] arrayOfString1;
			int j = (arrayOfString1 = names).length;
			for (int i = 0; i < j; i++) {
				String name = arrayOfString1[i];
				if (name.equals(key)) {
					found = true;
				}
			}
			if (!found) {
				unknownKeys.add(key);
			}
		}
		return (String[]) unknownKeys.toArray(new String[unknownKeys.size()]);
	}

	/**
	 * 判断文件是否存在
	 * @param name - 文件名
	 * @param file - 文件对象
	 */
	private void checkFile(String name, File file) {
		if (file == null) {
			throw new IllegalArgumentException(name + " is null");
		}
		if (!file.isFile()) {
			throw new IllegalArgumentException(name + " does not exist");
		}
		if (file.length() == 0L) {
			throw new IllegalArgumentException(name + " must be a non-zero byte (in length) file");
		}
	}

	private void checkNotEmpty(String name, String value) {
		if (value == null) {
			throw new IllegalArgumentException(name + " is null");
		}
		if (value.trim().length() == 0) {
			throw new IllegalArgumentException(name + " cannot be an empty string (or all spaces) '" + value + "'");
		}
	}

	public String[] getProblemPropertyNames() {
		return this.problemPropertyNames;
	}

	public boolean isValidAutoFillLangauageName(String name) {
		boolean valid = false;
		String[] arrayOfString;
		int j = (arrayOfString = LanguageAutoFill.getLanguageList()).length;
		for (int i = 0; i < j; i++) {
			String langName = arrayOfString[i];
			if (langName.equals(name)) {
				valid = true;
			}
		}
		return valid;
	}

	public String[] getAutoFillLanguageList() {
		return LanguageAutoFill.getLanguageList();
	}

	private void checkIsAllowed(Permission.Type type) {
		checkIsAllowed(type, null);
	}

	private void checkIsAllowed(Permission.Type type, String message) {
		if (!this.internalContest.isAllowed(type)) {
			if (message == null) {
				throw new SecurityException("Not allowed to " + getPermissionDescription(type));
			}
			throw new SecurityException(
					"Not allowed to " + message + "(requires " + getPermissionDescription(type) + " permission)");
		}
	}

	private void checkIsAnyAllowed(Permission.Type[] types, String message) {
		boolean allowed = false;
		Permission.Type[] arrayOfType;
		int j = (arrayOfType = types).length;
		for (int i = 0; i < j; i++) {
			Permission.Type type = arrayOfType[i];
			if (this.internalContest.isAllowed(type)) {
				allowed = true;
			}
		}
		if (!allowed) {
			if (message == null) {
				throw new SecurityException("Not allowed to " + getPermissionDescription(types[0]));
			}
			throw new SecurityException(
					"Not allowed to " + message + "(requires " + getPermissionDescription(types[0]) + " permission)");
		}
	}

	private String getPermissionDescription(Permission.Type type) {
		return new Permission().getDescription(type);
	}

	/**
	 * 添加编程语言
	 * @param title
	 * @param compilerCommandLine
	 * @param executionCommandLine
	 * @param interpreted
	 * @param executableMask
	 */
	public void addLanguage(String title, String compilerCommandLine, String executionCommandLine, boolean interpreted,
			String executableMask) {
		checkNotEmpty("Language Name/title", title);
		checkNotEmpty("Language compilation command", compilerCommandLine);
		checkNotEmpty("Language executable mask", executableMask);
		checkNotEmpty("Language execution comman lLine", executionCommandLine);

		Language language = new Language(title);

		language.setCompileCommandLine(compilerCommandLine);
		language.setInterpreted(interpreted);
		language.setExecutableIdentifierMask(executableMask);
		language.setProgramExecuteCommandLine(executionCommandLine);

		checkIsAllowed(Permission.Type.ADD_LANGUAGE);

		this.controller.addNewLanguage(language);
	}
	
	/**
	 * 添加编程语言
	 * @param autoFillTitleName
	 */
	public void addLanguage(String autoFillTitleName) {
		checkNotEmpty("Language Name/title", autoFillTitleName);
		if (!isValidAutoFillLangauageName(autoFillTitleName)) {
			throw new IllegalArgumentException("No such language name '" + autoFillTitleName + "'");
		}
		checkIsAllowed(Permission.Type.ADD_LANGUAGE);

		Language language = LanguageAutoFill.languageLookup(autoFillTitleName);
		this.controller.addNewLanguage(language);
	}

	public void shutdownServer() {
		checkIsAllowed(Permission.Type.SHUTDOWN_SERVER, "Shutdown local server");
		this.controller.sendShutdownSite(this.internalContest.getSiteNumber());
	}

	public void shutdownAllServers() {
		Permission.Type[] allowList = { Permission.Type.SHUTDOWN_ALL_SERVERS, Permission.Type.SHUTDOWN_SERVER };
		checkIsAnyAllowed(allowList, "Shutdown local server");
		this.controller.sendShutdownAllSites();
	}

	public void setContestTimes(long contestLengthSeconds, long contestElapsedSeconds, long contestRemainingSeconds)
			throws IllegalContestState {
		checkIsAllowed(Permission.Type.EDIT_CONTEST_CLOCK);
		if (this.internalContest.getContestTime().isContestRunning()) {
			throw new IllegalContestState("Cannot set contest times while contest clock is running/started");
		}
		if (contestLengthSeconds != contestElapsedSeconds + contestRemainingSeconds) {
			throw new IllegalArgumentException("Contest Length must equal elapsed plus remaining ( "
					+ contestLengthSeconds + " != " + contestElapsedSeconds + " + " + contestRemainingSeconds + " )");
		}
		ContestTime newContestTime = this.internalContest.getContestTime();
		newContestTime.setContestLengthSecs(contestLengthSeconds);
		newContestTime.setElapsedSecs(contestElapsedSeconds);
		newContestTime.setRemainingSecs(contestRemainingSeconds);

		this.controller.updateContestTime(newContestTime);
	}

	public void setContestLength(long contestLengthSeconds) throws IllegalContestState {
		ContestTime newContestTime = this.internalContest.getContestTime();
		long newRemain = contestLengthSeconds - newContestTime.getElapsedSecs();
		setContestTimes(contestLengthSeconds, newContestTime.getElapsedSecs(), newRemain);
	}
	
	public IInternalController getIInternalController() {
		return this.controller;
	}
	
	public IInternalContest getIInternalContest() {
		return this.internalContest;
	}

	public static void main(String[] args) {
		String[] requireArguementArgs = { "--login", "--password" };

		ParseArguments parseArguments = new ParseArguments(args, requireArguementArgs);
		if (parseArguments.isOptPresent("--help")) {
			System.out.println("Usage: ServerConnection [--help] --login LOGIN [--passowrd PASS] [--stop]");
			System.out.println("Purpose to start (default) pc2 server contest clock, or stop contest clock");
			System.exit(0);
		}
		boolean stopContest = parseArguments.isOptPresent("--stop");

		String login = parseArguments.getOptValue("--login");
		String password = parseArguments.getOptValue("--password");
		if (login == null) {
			fatalError("Missing login name, use --help for usage");
		}
		if (password == null) {
			password = "";
		}
		ServerConnection connection = new ServerConnection();
		try {
			IContest contest2 = connection.login(login, password);
			System.out.println("Logged in as " + contest2.getMyClient().getLoginName());
			System.out.println("Contest is running?  " + contest2.isContestClockRunning());
			if (stopContest) {
				System.out.println("Send Stop Contest");
				connection.stopContestClock();
			} else {
				System.out.println("Send Start Contest");
				connection.startContestClock();
			}
			Thread.sleep(1000L);
			System.out.println("Contest is running?  " + contest2.isContestClockRunning());
			connection.logoff();
			System.out.println("Logged off");
			System.exit(0);
		} catch (Exception e) {
			System.err.println("Login failure for login=" + login + " " + e.getMessage());
			e.printStackTrace();
		}
	}

	private static void fatalError(String string) {
		System.err.println(string);
		System.err.println("Program Halted");
		System.exit(43);
	}
}
