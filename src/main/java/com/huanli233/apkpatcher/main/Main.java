package com.huanli233.apkpatcher.main;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.android.tools.smali.dexlib2.AccessFlags;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.DexFile;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.MethodParameter;
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction10x;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction11n;
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction11x;
import com.android.tools.smali.dexlib2.rewriter.DexRewriter;
import com.android.tools.smali.dexlib2.rewriter.Rewriter;
import com.android.tools.smali.dexlib2.rewriter.RewriterModule;
import com.android.tools.smali.dexlib2.rewriter.Rewriters;
import com.huanli233.apkpatcher.apktool.ApkTool;
import com.huanli233.apkpatcher.apktool.ApkTool.ConfigSetter;
import com.huanli233.apkpatcher.dexlib2.MethodWrapper;
import com.huanli233.apkpatcher.event.MessageEvent;
import com.huanli233.apkpatcher.listener.MessageEventListener;
import com.huanli233.apkpatcher.patcher.XmlPatcher;
import com.huanli233.apkpatcher.utils.RandomNameUtil;

import brut.androlib.ApkDecoder;
import brut.androlib.Config;
import brut.androlib.exceptions.AndrolibException;
import brut.directory.DirectoryException;
import lanchon.multidexlib2.BasicDexFileNamer;
import lanchon.multidexlib2.DexIO;
import lanchon.multidexlib2.MultiDexIO;

public class Main {
	
	public static final String VERSION = "1.2.7";
	
	public static Map<String, String> fileMapping = new HashMap<>();
	
	static MessageEventListener listener;
	
	/**
	 * @return listener
	 */
	public static MessageEventListener getListener() {
		return listener;
	}

	/**
	 * @param listener 要设置的 listener
	 */
	public static void setListener(MessageEventListener listener) {
		Main.listener = listener;
	}

	public static void main(String[] args) throws AndrolibException, DirectoryException, IOException {
		setupLogging();
		listener = new MessageEventListener() {
			public void onEvent(MessageEvent event) {
				if (event.getLevel() == Level.INFO || (event.getLevel() == Level.WARNING && !event.getMessage().contains("properties"))) {
					System.out.println("- Apktool: " + event.getMessage());
				}
			}
		};
		clean();
		System.out.println(String.format("ApkPatcher v%s", VERSION));
		System.out.println("https://github.com/huanli233/ApkPatcher");
		if (args.length != 2 && args.length != 3) {
			printHelp();
		} else {
			new Main().run(args);
		}
		clean();
	}

	public static void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
	
	private static void clean() {
		for (File file : new File(System.getProperty("user.dir")).listFiles()) {
			if (file.isDirectory() && file.getName().startsWith("apkpatcher_temp") && file.getName().length() == 25) {
				deleteDirectory(file);
			}
		}
	}

	private static void setupLogging() {
		Logger logger = Logger.getLogger("");
        for (Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
        }
        LogManager.getLogManager().reset();

        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (getFormatter() == null) {
                    setFormatter(new Formatter() {
                        @Override
                        public String format(LogRecord record) {
                            return record.getMessage();
                        }
                    });
                }

                try {
                    listener.onEvent(new MessageEvent(record.getLevel(), record.getMessage()));
                } catch (Exception exception) {
                    reportError(null, exception, ErrorManager.FORMAT_FAILURE);
                }
            }

            @Override
            public void close() throws SecurityException {}
            @Override
            public void flush(){}
        };

        logger.addHandler(handler);

        handler.setLevel(Level.ALL);
        logger.setLevel(Level.ALL);
	}

	public static void printHelp() {
		System.out.println("Usage: apkpatcher <Script> <APK> [Sign Option]");
		System.out.println("  Script - ApkPatcher Script File");
		System.out.println("  APK - Input APK File");
		System.out.println("  Signing Option:");
		System.out.println("    remove - (Default)Do not retain the original signature");
		System.out.println("    keep - Do not modify the original signature information");
	}
	
	public static Object getFieldValue(Object obj, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        // 获取对象的Class对象
        Class<?> clazz = obj.getClass();
        
        // 在Class对象中查找指定名称的字段
        Field field = clazz.getDeclaredField(fieldName);
        
        // 设置字段的访问权限为可访问，即使是私有字段也可以访问
        field.setAccessible(true);
        
        // 返回字段的值
        return field.get(obj);
    }
	
	public void run(String[] args) {
		String[] scriptFiles = args[0].split(";");
		File apkFile = new File(args[1]);
		String signSetting = "remove";
		if (args.length == 3) {
			switch (args[2]) {
			case "remove":
				break;
			case "keep":
				signSetting = "keep";
				break;
			default:
				System.out.println("! Unknown sign setting: " + args[2]);
				return;
			}
		}
		File tempDir = RandomNameUtil.randomTempDir();
		DexFile dex = null;
		boolean hasAnyResourcePatch = false;
		boolean hasAnyDexPatch = false;
		List<PatchInfo> patchInfos = new LinkedList<>();
		int patchedCount = 0;
		int skippedCount = 0;
		
		for (String string : scriptFiles) {
			File scriptFile = new File(string);
			System.out.println("- Start check script " + scriptFile.getAbsolutePath());
			if (!scriptFile.exists()) {
				System.out.println("! Script file not exists");
				return;
			}
			File scriptDir = scriptFile.getParentFile();
			if (!scriptFile.isFile()) {
				System.out.println("! Script file is not a file");
				return;
			}
			Document document = null;
			try {
	            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	            factory.setValidating(false);
	            factory.setNamespaceAware(true);
	            DocumentBuilder builder = factory.newDocumentBuilder();
	            document = builder.parse(scriptFile);
	        } catch (SAXException e) {
	            System.out.println("! Script file content if invalid: " + e.getMessage());
	        } catch (IOException e) {
	            System.out.println("! IO Error: " + e.getMessage());
	        } catch (Exception e) {
	            System.out.println("! Other Error: " + e.getMessage());
	        }
			if (document == null) {
				System.out.println("! Script file check error");
				return;
			}
			System.out.println("- Parsing script file");
			Element rootElement = document.getDocumentElement();
			boolean hasResourcePatch = hasChildNodeWithName(rootElement, "resource");
			boolean hasDexPatch = hasChildNodeWithName(rootElement, "dex");
			hasAnyResourcePatch = hasAnyResourcePatch || hasResourcePatch;
			hasAnyDexPatch = hasAnyDexPatch || hasDexPatch;
			if (!hasDexPatch && !hasResourcePatch) {
				System.out.println("* This script file doesn't have any patch, skipping...");
				skippedCount++;
			} else {
				patchInfos.add(new PatchInfo(hasResourcePatch, hasDexPatch, scriptDir, rootElement, scriptFile));
			}
		}
		
		String signString = signSetting;
		boolean resourceDecode = hasAnyResourcePatch;
		ApkDecoder apkDecoder = ApkTool.getDecoder(apkFile, new ConfigSetter() {
			@Override
			public void setConfig(Config config) {
				config.copyOriginalFiles = signString.equals("keep");
				config.forceDecodeManifest = Config.FORCE_DECODE_MANIFEST_FULL;
				if (!resourceDecode) {
					config.decodeResources = Config.DECODE_RESOURCES_NONE;
				}
			}
		});
		if (hasAnyDexPatch || hasAnyResourcePatch) {
			System.out.println("- Decoding APK file");
			boolean decodeResult = ApkTool.decode(apkDecoder, tempDir);
			if (!decodeResult) {
				System.out.println("! Unable to decode APK file");
				return;
			}
			if (hasAnyResourcePatch) {
				System.out.println("- Moving res");
				moveFiles(fileMapping, tempDir);
			}
			if (hasAnyDexPatch) {
				System.out.println("- Loading dex");
				try {
					dex = MultiDexIO.readDexFile(true, apkFile, new BasicDexFileNamer(), null, null);
				} catch (IOException e) {
					System.out.println(e.getMessage());
					System.out.println("! Unable to load dex");
					return;
				}
				if (dex == null) {
					System.out.println("! Unable to load dex");
					return;
				}
			}
		} else {
			System.out.println("! No patch tasks in these script files");
			return;
		}
		
		DexContainer dexContainer = new DexContainer(dex);
		for (PatchInfo patchInfo : patchInfos) {
			System.out.println("- Doing patch " + patchInfo.getScriptFile().getAbsolutePath());
			doPatch(patchInfo.isHasResourcePatch(), patchInfo.isHasDexPatch(), patchInfo.getRootElement(), tempDir, patchInfo.getScriptDir(), apkFile, dexContainer);
			patchedCount++;
		}
		dex = dexContainer.get();
		System.out.println("- All patch scripts have been completed");
		System.out.println(String.format("- %d patched, %d skipped", patchedCount, skippedCount));
		
		if (hasAnyDexPatch) {
			System.out.println("- Writing dex file");
			try {
				MultiDexIO.writeDexFile(true, tempDir, new BasicDexFileNamer(), dex, DexIO.DEFAULT_MAX_DEX_POOL_SIZE, null);
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("- Write dex file failed");
			}
		}
		
		System.out.println("- Building patched APK file");
		File outDir = new File(apkFile.getName().substring(0, (apkFile.getName().lastIndexOf(".") != -1 ? apkFile.getName().lastIndexOf(".") : (apkFile.getName().length() - 1))) + "_patched-" + tempDir.getName().substring(15) + ".apk");
		if (!ApkTool.build(tempDir, outDir, (signSetting.equals("keep") ? true : false))) {
			System.out.println("! Unable to build APK file");
			return;
		}
		System.out.println("- APK file saved to " + outDir.getAbsolutePath());
		
		System.out.println("- Cleaning");
		clean();
		System.out.println("- Done!");
	}
	
	private void doPatch(boolean hasResourcePatch, boolean hasDexPatch, Element rootElement, File tempDir, File scriptDir, File apkFile, DexContainer dexContainer) {
		DexFile dex = dexContainer.get();
		if (hasResourcePatch) {
			System.out.println("- Patching resource patchs");
			NodeList resourceElements = rootElement.getElementsByTagName("resource");
			for (int i = 0; i < resourceElements.getLength(); i++) {
				Node node = resourceElements.item(i);
				NodeList patchs = node.getChildNodes();
				for (int j = 0; j < patchs.getLength(); j++) {
					Node patch = patchs.item(j);
					if (!(patch instanceof Element)) continue;
					Element element = (Element) patch;
					if (!element.hasAttribute("path")) {
						System.out.println("! A patch doesn't have attribute 'path'");
						return;
					}
					if (!element.hasAttribute("type")) {
						System.out.println("! A patch doesn't have attribute 'type'");
						return;
					}
					String type = element.getAttribute("type");
					String path = element.getAttribute("path");
					switch (type) {
					case "xml":
						System.out.println("- Patching " + type + " patch " + path);
						if (!XmlPatcher.patch(tempDir, element, path)) {
							System.out.println("! A xml patch is failed");
							return;
						}
						break;
					case "replace":
						System.out.println("- Patching " + type + " patch " + path);
						if (element.getTextContent() != null) {
							File replaceFile = new File(scriptDir, element.getTextContent());
							if (!replaceFile.exists()) {
								System.out.println("! The file that replaced it does not exist");
								return;
							}
							File replacedFile = new File(tempDir, path);
							if (!replaceFile(replaceFile, replacedFile)) {
								System.out.println("! A replace patch is failed");
								return;
							}
						} else {
							System.out.println("! Missing content");
							return;
						}
						break;
					default:
						System.out.println("! A patch's type is unknown");
						return;
					}
				}
			}
		}
		if (hasDexPatch) {
			System.out.println("- Patching dex patchs");
			NodeList dexElements = rootElement.getElementsByTagName("dex");
			for (int i = 0; i < dexElements.getLength(); i++) {
				Node node = dexElements.item(i);
				NodeList targetClasses = node.getChildNodes();
				for (int j = 0; j < targetClasses.getLength(); j++) {
					if (!(targetClasses.item(j) instanceof Element)) continue;
					Element element = (Element) targetClasses.item(j);
					if (!element.hasAttribute("type")) {
						System.out.println("! A patch doesn't have attribute 'type'");
						return;
					}
					if (!element.hasAttribute("find")) {
						System.out.println("! A patch doesn't have attribute 'find'");
						return;
					}
					String type = element.getAttribute("type");
					String find = element.getAttribute("find");
					String definingClass = null;
					ClassDef classDef = null;
					Set<? extends ClassDef> classDefs = dex.getClasses();
					System.out.println("- Finding target class");
					switch (type) {
					case "precise":
						definingClass = javaClassToType(find);
						for (ClassDef classDef2 : classDefs) {
							if (classDef2.getType().equals(definingClass)) {
								classDef = classDef2;
							}
						}
						break;
					case "source":
						String[] findSplit = find.split(";");
						if (findSplit.length != 2) {
							System.out.println("! A class's 'find' attribute is invalid");
							return;
						}
						String source = findSplit[0];
						String suffix = findSplit[1];
						boolean disableSuffix = suffix.equals("0");
						for (ClassDef classDef2 : classDefs) {
							boolean suffixFlag = false;
							if (disableSuffix && classDef2.getSourceFile() != null) {
								int lastIndex = classDef2.getType().lastIndexOf("/");
								suffixFlag = !classDef2.getType().substring((lastIndex != -1 ? lastIndex : 0), (classDef2.getType().length() - 1)).contains("$");
							} else {
								suffixFlag = classDef2.getType().endsWith(suffix + ";");
							}
							if (classDef2.getSourceFile() != null && classDef2.getSourceFile().equals(source) && suffixFlag) {
								classDef = classDef2;
							}
						}
						break;
					default:
						System.out.println("! A class's type is unknown");
						break;
					}
					if (classDef == null) {
						System.out.println("! Can't find target class " + find);
						return;
					}
					NodeList patchs = targetClasses.item(j).getChildNodes();
					for (int k = 0; k < patchs.getLength(); k++) {
						Node patch = patchs.item(k);
						if (!(patch instanceof Element)) continue;
						Element patchElement = (Element) patch;
						if (!patchElement.hasAttribute("type")) {
							System.out.println("! A patch doesn't have attribute 'type'");
							return;
						}
						if (!patchElement.hasAttribute("find")) {
							System.out.println("! A dex patch doesn't have attribute 'find'");
							return;
						}
						if (!patchElement.hasAttribute("patchtype")) {
							System.out.println("! A dex patch doesn't have attribute 'patchtype'");
							return;
						}
						String patchType = patchElement.getAttribute("type");
						String patchFind = patchElement.getAttribute("find");
						String patchMode = patchElement.getAttribute("patchtype");
						String textContent = patchElement.getTextContent();
						String[] findSplit = patchFind.split(";");
						if (findSplit.length != 4) {
							System.out.println("! A patch's attribute 'find' is invalid");
							return;
						}
						boolean isStatic;
						try {
							isStatic = Boolean.valueOf(findSplit[3]);
						} catch (Exception e) {
							System.out.println("! A patch's attribute 'find[3]' must be a boolean value");
							return;
						}
						System.out.println("- Patching dex patch " + patchFind);
						Iterable<? extends Method> methods = classDef.getMethods();
						System.out.println("- Finding target method");
						Method targetMethod = null;
						switch (patchType) {
						case "precise":
							for (Method method : methods) {
								String paramType = "";
								if (findSplit[1].equals("V")) {
									findSplit[1] = "";
								}
								for (MethodParameter parameter : method.getParameters()) {
									paramType += "," + parameter.getType().replace(";", "");
								};
								paramType = paramType.replaceFirst(",", "");
								if (method.getName().equals(findSplit[0]) && method.getReturnType().equals(findSplit[2]) && paramType.equals(findSplit[1])) {
									targetMethod = method;
								}
							}
							break;
						default:
							System.out.println("! A patch's patchtype is unknown");
							return;
						}
						if (targetMethod == null) {
							System.out.println("! Can't find target method " + patchFind);
							return;
						}
						Method target = targetMethod;
						DexRewriter dexRewriter = new DexRewriter(new RewriterModule() {
							@Override
							public Rewriter<Method> getMethodRewriter(Rewriters rewriters) {
								return new Rewriter<Method>() {
									@Override
									public Method rewrite(Method value) {
										AccessFlags[] flags = AccessFlags.getAccessFlagsForMethod(value.getAccessFlags());
										boolean mStatic = false;
										for (int l = 0; l < flags.length; l++) {
											if (flags[l] == AccessFlags.STATIC) {
												mStatic = true;
											}
										}
										if (value.equals(target) && mStatic == isStatic) {
											switch (patchMode) {
											case "RETURN-BOOL":
												if (textContent == null) {
													System.out.println("! A dex patch needs content but doesn't have, ignoring..");
													return value;
												}
												return new MethodWrapper(value).apply(new ImmutableMethodImplementation(value.getParameters().size() + 2, Arrays.asList(
														new ImmutableInstruction11n(Opcode.CONST_4, 0, (textContent.equals("true") ? 1 : 0)), 
														new ImmutableInstruction11x(Opcode.RETURN, 0)
														), null, null)).build();
											case "RETURN-VOID":
												return new MethodWrapper(value).apply(new ImmutableMethodImplementation(value.getParameters().size() + 1, Arrays.asList(
														new ImmutableInstruction10x(Opcode.RETURN_VOID)
														), null, null)).build();
											default:
												System.out.println("! A dex patch has an unknown patchtype " + patchMode);
												break;
											}
										}
										return value;
									}
								};
							}
						});
						dexContainer.set(dexRewriter.getDexFileRewriter().rewrite(dex));
					}
				}
			}
		}
	}
	
	public static boolean replaceFile(File source, File destination) {
        try {
        	if (!destination.exists()) {
                Files.copy(source.toPath(), destination.toPath());
            } else {
                Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
		} catch (IOException e) {
			return false;
		}
        return true;
    }
	
	public static String javaClassToType(String clz) {
        return "L" + clz.replace(".", "/") + ";";
    }
	
	private static boolean hasChildNodeWithName(Element parentElement, String nodeName) {
        NodeList nodeList = parentElement.getChildNodes();

        for (int i = 0; i < nodeList.getLength(); i++) {
            if (nodeList.item(i) instanceof Element) {
                Element element = (Element) nodeList.item(i);
                
                if (element.getTagName().equals(nodeName)) {
                    return true;
                }
            }
        }
        return false;
    }
	
	class PatchInfo {
		boolean hasResourcePatch;
		boolean hasDexPatch;
		File scriptDir;
		Element rootElement;
		File scriptFile;

		public PatchInfo(boolean hasResourcePatch, boolean hasDexPatch, File scriptDir, Element rootElement,
				File scriptFile) {
			this.hasResourcePatch = hasResourcePatch;
			this.hasDexPatch = hasDexPatch;
			this.scriptDir = scriptDir;
			this.rootElement = rootElement;
			this.scriptFile = scriptFile;
		}

		/**
		 * @return hasResourcePatch
		 */
		public boolean isHasResourcePatch() {
			return hasResourcePatch;
		}

		/**
		 * @param hasResourcePatch 要设置的 hasResourcePatch
		 */
		public void setHasResourcePatch(boolean hasResourcePatch) {
			this.hasResourcePatch = hasResourcePatch;
		}

		/**
		 * @return hasDexPatch
		 */
		public boolean isHasDexPatch() {
			return hasDexPatch;
		}

		/**
		 * @param hasDexPatch 要设置的 hasDexPatch
		 */
		public void setHasDexPatch(boolean hasDexPatch) {
			this.hasDexPatch = hasDexPatch;
		}

		/**
		 * @return scriptDir
		 */
		public File getScriptDir() {
			return scriptDir;
		}

		/**
		 * @param scriptDir 要设置的 scriptDir
		 */
		public void setScriptDir(File scriptDir) {
			this.scriptDir = scriptDir;
		}

		/**
		 * @return rootElement
		 */
		public Element getRootElement() {
			return rootElement;
		}

		/**
		 * @param rootElement 要设置的 rootElement
		 */
		public void setRootElement(Element rootElement) {
			this.rootElement = rootElement;
		}

		/**
		 * @return scriptFile
		 */
		public File getScriptFile() {
			return scriptFile;
		}

		/**
		 * @param scriptFile 要设置的 scriptFile
		 */
		public void setScriptFile(File scriptFile) {
			this.scriptFile = scriptFile;
		}
		
	}
	
	public static void moveFiles(Map<String, String> pathMap, File workDir) {
        for (Map.Entry<String, String> entry : pathMap.entrySet()) {
            String sourcePath = entry.getKey();
            String destinationPath = entry.getValue();

            File sourceFile = new File(workDir, sourcePath);
            File destinationFile = new File(workDir, destinationPath);

            if (!sourceFile.exists()) {
            	// .9.png
            	int index = sourcePath.lastIndexOf(".");
            	sourceFile = new File(workDir, sourcePath.substring(0, index) + ".9" + sourcePath.substring(index));
            	if (!sourceFile.exists()) {
            		System.out.println("* Source file does not exist: " + sourceFile.getPath());
                    continue;
				}
            }

            if (!destinationFile.exists()) {
                // If destination path does not exist, create the directory
                destinationFile.getParentFile().mkdirs();
            }

            try {
                Path source = sourceFile.toPath();
                Path destination = destinationFile.toPath();
                
                if (destinationFile.isDirectory()) {
                    // If destination is a directory, resolve it with source file name
                    destination = destination.resolve(sourceFile.getName());
                }
                
                Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                System.out.println("! Error moving file from " + sourcePath + " to " + destinationPath + ": " + e.getMessage());
            }
        }
    }

	
	public class DexContainer {
		
		DexFile dex;
		
		public DexContainer(DexFile dex) {
			this.dex = dex;
		}
		
		public DexFile get() {
			return dex;
		}
		
		public void set(DexFile dex) {
			this.dex = dex;
		}
		
	}
	
}
