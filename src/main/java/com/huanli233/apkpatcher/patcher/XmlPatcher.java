package com.huanli233.apkpatcher.patcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XmlPatcher {
	
	public static final int MODE_REPLACE_EXISTS = 0;
	public static final int MODE_FULL_REPLACE = 1;
	
	public static boolean patch(File dir, Element patchElement, String path) {
		int mode = MODE_FULL_REPLACE;
		if (patchElement.hasAttribute("mode")) {
			switch (patchElement.getAttribute("mode")) {
			case "0":
			case "REPLACE_EXISTS":
				mode = MODE_REPLACE_EXISTS;
				break;
			case "1":
			case "FULL_REPLACE":
				mode = MODE_FULL_REPLACE;
				break;
			default:
				System.out.println(String.format("! Unknown mode %s", patchElement.getAttribute("mode")));
				return false;
			}
		}
		File patchFile = new File(dir, path);
		if (!patchFile.exists()) {
			System.out.println("! The patch target file does not exist");
			return false;
		}
		try {
			Document doc = readXMLFile(patchFile);
			NodeList nodeList = patchElement.getChildNodes();
			if (mode == MODE_REPLACE_EXISTS) {
				writeElementToXMLFile(doc, patchElement, mode);
			} else {
				for (int i = 0; i < nodeList.getLength(); i++) {
					if (!(nodeList.item(i) instanceof Element)) {
						continue;
					}
					Element element = (Element) nodeList.item(i);
					writeElementToXMLFile(doc, element, mode);
				}
			}
			writeDocumentToFile(doc, patchFile);
		} catch (SAXException e) {
			System.out.println("! The patch target xml file content is invalid");
		}
		return true;
	}
	
	private static Document readXMLFile(File file) throws SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            return db.parse(file);
        } catch (ParserConfigurationException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void writeElementToXMLFile(Document doc, Element newElement, int mode) {
    	// 导入新Element对象到当前文档中
        Element importedNode = (Element) doc.importNode(newElement, true);
        NodeList nodeList = doc.getElementsByTagName(newElement.getTagName());
        if (mode == MODE_FULL_REPLACE) {
        	// 遍历相同节点名称的所有节点
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node existingNode = nodeList.item(i);
                // 检查属性和子节点是否相同
                if (nodeMatches(existingNode, newElement)) {
                    // 替换相同节点
                    existingNode.getParentNode().replaceChild(importedNode, existingNode);
                    return;
                }
            }
		} else if (mode == MODE_REPLACE_EXISTS) {
			replaceOrAddMatchingNodes(newElement, doc);
			return;
		}
        // 如果没有相同节点，则添加新节点到根节点下
        doc.getDocumentElement().appendChild(importedNode);
    }
    
    @SuppressWarnings("unused")
	private static boolean hasElementChild(Node node) {
		boolean result = false;
		NodeList list = node.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			if (list.item(i) instanceof Element) {
				result = true;
			}
		}
		return result;
	}

    private static boolean nodeMatches(Node node1, Node node2) {
        // 检查节点名称是否相同
        if (!node1.getNodeName().equals(node2.getNodeName())) {
            return false;
        }
        // 检查节点属性是否相同
        if (!attributesMatch(node1, node2)) {
            return false;
        }
        return true;
    }

    private static boolean attributesMatch(Node node1, Node node2) {
        if (node1.getAttributes() == null && node2.getAttributes() == null) {
            return true;
        }
        if (node1.getAttributes() == null || node2.getAttributes() == null) {
            return false;
        }
        if (node1.getAttributes().getLength() != node2.getAttributes().getLength()) {
            return false;
        }
        for (int i = 0; i < node1.getAttributes().getLength(); i++) {
            Node attr1 = node1.getAttributes().item(i);
            Node attr2 = node2.getAttributes().getNamedItem(attr1.getNodeName());
            if (attr2 == null || !attr1.getNodeValue().equals(attr2.getNodeValue())) {
                return false;
            }
        }
        return true;
    }

    private static void writeDocumentToFile(Document doc, File file) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            FileOutputStream fos = new FileOutputStream(file);
            StreamResult result = new StreamResult(fos);
            transformer.transform(source, result);
            fos.close();
        } catch (TransformerException | IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void replaceOrAddMatchingNodes(Element element, Document document) {
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (childNode instanceof Element) {
                Element childElement = (Element) childNode;
                if (hasElementChild(childElement)) {
                    // 递归调用处理子节点
                    replaceOrAddMatchingNodes(childElement, document);
                } else {
                    // 处理末端节点
                    replaceOrAddNode(childElement, document);
                }
            }
        }
    }

    private static void replaceOrAddNode(Element childElement, Document document) {
        String path = getNodePath(childElement);
        Element existingNode = findMatchingNode(path, document.getDocumentElement());
        if (existingNode != null) {
            // 替换已存在的节点
            document.getDocumentElement().replaceChild(childElement, existingNode);
        } else {
            // 添加新节点
            Element parentNode = findOrCreateParentNode(path, document.getDocumentElement(), document);
            parentNode.appendChild(document.importNode(childElement, true));
        }
    }
    
    private static String getName() {
    	return UUID.randomUUID().toString().replace("-", "");
	}

    static Map<String, Element> eleMap = new HashMap<>();
    private static String getNodePath(Element element) {
        StringBuilder pathBuilder = new StringBuilder();
        Node currentNode = element;
        while (currentNode.getParentNode() instanceof Element) {
            currentNode = currentNode.getParentNode();
            String string = getName();
            eleMap.put(string, (Element) currentNode);
            pathBuilder.insert(0, "/" + string);
        }
        return pathBuilder.toString();
    }

    private static Element findMatchingNode(String path, Element rootNode) {
        String[] segments = path.split("/");
        Element currentNode = rootNode;
        for (String segment : segments) {
            NodeList nodeList = currentNode.getElementsByTagName(segment);
            if (nodeList.getLength() > 0) {
                currentNode = (Element) nodeList.item(0);
            } else {
                return null; // 没有找到匹配的节点
            }
        }
        return currentNode;
    }

    private static Element findOrCreateParentNode(String path, Element rootNode, Document document) {
        String[] segments = path.split("/");
        Element currentNode = rootNode;
        boolean flag = false;
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            if (segment.equals("")) {
				continue;
			}
            Element element = eleMap.get(segment);
            if (element.getTagName().equals("patch")) {
				flag = true;
				continue;
			}
            if (!flag) {
				continue;
			}
            NodeList nodeList = currentNode.getChildNodes();
            boolean flag1 = true;
            for (int j = 0; j < nodeList.getLength(); j++) {
				Node node = nodeList.item(j);
				if (node instanceof Element) {
					if (nodeMatches(element, node)) {
						flag1 = false;
						currentNode = (Element) node;
					}
				}
            }
            if (flag1) {
            	Element importElement = (Element) document.importNode(element, true);
				currentNode.appendChild(importElement);
				currentNode = element;
			}
        }
        return currentNode;
    }
    
}
