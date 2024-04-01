package com.huanli233.apkpatcher.utils;

import java.io.File;
import java.util.UUID;

public class RandomNameUtil {

	public static File randomTempDir() {
		String name = "apkpatcher_temp" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
		File dir = new File(name);
		return dir;
	}
	
}
