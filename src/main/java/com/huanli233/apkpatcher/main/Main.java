package com.huanli233.apkpatcher.main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
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
import com.huanli233.apkpatcher.dexlib2.MethodWrapper;
import com.huanli233.apkpatcher.event.MessageEvent;
import com.huanli233.apkpatcher.listener.MessageEventListener;
import com.huanli233.apkpatcher.patcher.XmlPatcher;
import com.huanli233.apkpatcher.utils.RandomNameUtil;

import brut.androlib.exceptions.AndrolibException;
import brut.directory.DirectoryException;
import lanchon.multidexlib2.BasicDexFileNamer;
import lanchon.multidexlib2.DexIO;
import lanchon.multidexlib2.MultiDexIO;

public class Main {
	
	public static final String VERSION = "1.1.0";
	
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
					System.out.println(event.getMessage());
				}
			}
		};
		clean();
		System.out.println(String.format("ApkPatcher v%s", VERSION));
		System.out.println("https://github.com/huanli233/ApkPatcher");
		if (args.length != 2) {
			printHelp();
		} else {
			new Main().run(args);
		}
		clean();
	}
	
	@SuppressWarnings("unused")
	private static void test() throws IOException {
		// 读取DEX文件
        File dexFile = new File("E:\\Desktop\\手表_11.6.15.apk"); // 替换为你的DEX文件路径
        DexFile dex = MultiDexIO.readDexFile(true, dexFile, new BasicDexFileNamer(), null, null);

        // 遍历DEX文件中的类
        for (ClassDef classDef : dex.getClasses()) {
            // 输出类名
        	if (classDef.getSourceFile() != null && classDef.getSourceFile().contains("SpanManager")) {
                System.out.println("Class: " + classDef.getType());
        		System.out.println(classDef.getSourceFile());
			}
        }
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
		System.out.println("Usage: apkpatcher <Script> <APK>");
		System.out.println("  Script - ApkPatcher Script File");
		System.out.println("  APK - Input APK File");
	}
	
	public void run(String[] args) {
		File scriptFile = new File(args[0]);
		File apkFile = new File(args[1]);
		System.out.println("- Start check script");
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
		if (!hasResourcePatch && !hasDexPatch) {
			System.out.println("! Don't have any patch");
			return;
		}
		System.out.println("- Decoding APK file");
		File tempDir = RandomNameUtil.randomTempDir();
		if (!ApkTool.decodeResource(apkFile, tempDir)) {
			System.out.println("! Unable to decode APK file");
			return;
		}
		if (hasResourcePatch) {
			System.out.println("- Patching resource patchs");
			NodeList resourceElements = rootElement.getElementsByTagName("resource");
			for (int i = 0; i < resourceElements.getLength(); i++) {
				Node node = resourceElements.item(i);
				NodeList patchs = node.getChildNodes();
				for (int j = 0; j < patchs.getLength(); j++) {
					Node patch = patchs.item(j);
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
			System.out.println("- Loading dex");
			DexFile dex = null;
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
						for (ClassDef classDef2 : classDefs) {
							if (classDef2.getSourceFile().equals(source) && classDef2.getType().endsWith((suffix + ";"))) {
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
												return new MethodWrapper(value).apply(new ImmutableMethodImplementation(value.getParameters().size() + 1, Arrays.asList(
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
						dex = dexRewriter.getDexFileRewriter().rewrite(dex);
					}
				}
			}
			System.out.println("- Writing dex file");
			try {
				MultiDexIO.writeDexFile(true, tempDir, new BasicDexFileNamer(), dex, DexIO.DEFAULT_MAX_DEX_POOL_SIZE, null);
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("- Write dex file failed");
			}
		}
		System.out.println("- Building patched APK file");
		File outDir = new File(apkFile.getName().substring(0, (apkFile.getName().lastIndexOf("."))) + "_patched-" + tempDir.getName().substring(15) + ".apk");
		if (!ApkTool.build(tempDir, outDir)) {
			System.out.println("! Unable to build APK file");
			return;
		}
		System.out.println("- APK file saved to " + outDir.getAbsolutePath());
		System.out.println("- Cleaning");
		clean();
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
	
}
