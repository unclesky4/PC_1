package org.unclesky4.project.entity;

import org.unclesky4.project.enums.JudgeResultType;

/**
 * 编译运行的程序输出信息
 * @author unclesky4 28/09/2017
 *
 */
public class JudgeResult {

	private JudgeResultType judgeResultType = null;
	
	private long compileTimeMS = 0L;
	
	private long ExecuteTimeMS = 0L;
	
	private int timeOutInSeconds = 0;
	
	private String programOuputInfo = "";
	
	public JudgeResult(JudgeResultType judgeResultType) {
		this.judgeResultType = judgeResultType;
	}
	
	public JudgeResult(JudgeResultType judgeResultType, long compileTimeMS, long ExecuteTimeMS, int timeOutInSeconds, 
			String programOuputInfo) {
		this.judgeResultType = judgeResultType;
		this.timeOutInSeconds = timeOutInSeconds;
		this.compileTimeMS = compileTimeMS;
		this.ExecuteTimeMS = ExecuteTimeMS;
	}
	
	
	public JudgeResultType getJudgeResultType() {
		return judgeResultType;
	}

	public void setJudgeResultType(JudgeResultType judgeResultType) {
		this.judgeResultType = judgeResultType;
	}

	public long getCompileTimeMS() {
		return compileTimeMS;
	}

	public void setCompileTimeMS(long compileTimeMS) {
		this.compileTimeMS = compileTimeMS;
	}

	public long getExecuteTimeMS() {
		return ExecuteTimeMS;
	}

	public void setExecuteTimeMS(long executeTimeMS) {
		ExecuteTimeMS = executeTimeMS;
	}

	public int getTimeOutInSeconds() {
		return timeOutInSeconds;
	}

	public void setTimeOutInSeconds(int timeOutInSeconds) {
		this.timeOutInSeconds = timeOutInSeconds;
	}

	public String getProgramOuputInfo() {
		return programOuputInfo;
	}

	public void setProgramOuputInfo(String programOuputInfo) {
		this.programOuputInfo = programOuputInfo;
	}

	@Override
	public String toString() {
		return "JudgeResult [judgeResultType=" + judgeResultType + ", compileTimeMS=" + compileTimeMS
				+ ", ExecuteTimeMS=" + ExecuteTimeMS + ", timeOutInSeconds=" + timeOutInSeconds + ", programOuputInfo="
				+ programOuputInfo + "]";
	}
}
