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
	
	public static boolean decodeResource(File apkFile, File outDir, boolean copyOrigFile) {
		ExtFile extFile = new ExtFile(apkFile);
		Config config = Config.getDefaultConfig();
		try {
			config.forceDelete = true;
			config.setDecodeSources(Config.DECODE_SOURCES_NONE);
			config.copyOriginalFiles = copyOrigFile;
		} catch (AndrolibException e) {
			e.printStackTrace();
			return false;
		}
		ApkDecoder apkDecoder = new ApkDecoder(config, extFile);
		try {
			apkDecoder.decode(outDir);
		} catch (AndrolibException | DirectoryException | IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public static boolean decodeResource(File apkFile, File outDir) {
		return decodeResource(apkFile, outDir, false);
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
