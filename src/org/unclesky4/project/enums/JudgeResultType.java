package org.unclesky4.project.enums;


/**
 * 
 * @author unclesky4 28/09/2017
 *
 */
public enum JudgeResultType {

	NOTLOGIN("没有登陆"),
	NOPERMISSION("没有该权限"),
	COMPILERFAILED("程序编译失败"),
	EXECUTEFAILED("程序运行失败"),
	SUCCESS("运行成功");
	
	private String description;
	
	private JudgeResultType(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
