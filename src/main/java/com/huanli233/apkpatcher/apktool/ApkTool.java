package com.huanli233.apkpatcher.apktool;

import java.io.File;
import java.io.IOException;

import brut.androlib.ApkBuilder;
import brut.androlib.ApkDecoder;
import brut.androlib.Config;
import brut.androlib.exceptions.AndrolibException;
import brut.common.BrutException;
import brut.directory.DirectoryException;
import brut.directory.ExtFile;

public class ApkTool {
	
	public interface ConfigSetter {
		public void setConfig(Config config);
	}
	
	public static ApkDecoder getDecoder(File apkFile, ConfigSetter setter) {
		ExtFile extFile = new ExtFile(apkFile);
		Config config = Config.getDefaultConfig();
		config.forceDelete = true;
		try {
			config.setDecodeSources(Config.DECODE_SOURCES_NONE);
		} catch (AndrolibException e) {
			e.printStackTrace();
		}
		setter.setConfig(config);
		ApkDecoder apkDecoder = new ApkDecoder(config, extFile);
		return apkDecoder;
	}
	
	public static boolean decode(File apkFile, File outDir, ConfigSetter setter) {
		ApkDecoder apkDecoder = getDecoder(apkFile, setter);
		return decode(apkDecoder, outDir);
	}
	
	public static boolean decode(File apkFile, File outDir) {
		return decode(apkFile, outDir, (config) -> {});
	}
	
	public static boolean decode(ApkDecoder apkDecoder, File outDir) {
		try {
			apkDecoder.decode(outDir);
		} catch (AndrolibException | DirectoryException | IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public static boolean build(File dir, File out, boolean copyOrigFile) {
		Config config = Config.getDefaultConfig();
		config.copyOriginalFiles = copyOrigFile;
		ApkBuilder apkBuilder = new ApkBuilder(config, new ExtFile(dir));
		try {
			apkBuilder.build(out);
		} catch (BrutException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public static boolean build(File dir, File out) {
		return build(dir, out, false);
	}
	
}
