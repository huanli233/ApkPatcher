package com.huanli233.apkpatcher.main;

import java.io.File;
import java.io.IOException;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.huanli233.apkpatcher.apktool.ApkTool;
import com.huanli233.apkpatcher.event.MessageEvent;
import com.huanli233.apkpatcher.listener.MessageEventListener;
import com.huanli233.apkpatcher.patcher.XmlPatcher;
import com.huanli233.apkpatcher.utils.RandomNameUtil;

import brut.androlib.exceptions.AndrolibException;
import brut.directory.DirectoryException;
import lanchon.multidexlib2.BasicDexFileNamer;
import lanchon.multidexlib2.MultiDexIO;

public class Main {
	
	public static final String VERSION = "1.0.0";
	
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
		System.out.println(String.format("ApkPatcher v%s", VERSION));
		System.out.println("https://github.com/huanli233/ApkPatcher");
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
							System.out.println("! A patch is failed");
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
