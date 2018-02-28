package org.unclesky4.project.utils;

import java.util.UUID;

public class UUIDUtils {
	/**
	 * 获得随机的字符串
	 */
	public static String getUUID(){
		return UUID.randomUUID().toString().replaceAll("-", "");
}
}
