package com.huanli233.apkpatcher.patcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
		int mode = MODE_REPLACE_EXISTS;
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
			for (int i = 0; i < nodeList.getLength(); i++) {
				if (!(nodeList.item(i) instanceof Element)) {
					continue;
				}
				Element element = (Element) nodeList.item(i);
				writeElementToXMLFile(doc, element, mode);
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
        Node importedNode = doc.importNode(newElement, true);
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
			processElement(newElement, doc);
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
        // 检查子节点数量是否相同
        if (node1.getChildNodes().getLength() != node2.getChildNodes().getLength()) {
            return false;
        }
        // 递归检查子节点是否相同
        NodeList children1 = node1.getChildNodes();
        NodeList children2 = node2.getChildNodes();
        for (int i = 0; i < children1.getLength(); i++) {
            if (!nodeMatches(children1.item(i), children2.item(i))) {
                return false;
            }
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
    
    public static void processElement(Element element, Document document) {
        // 遍历传入的 Element 对象的所有分支的每一个末端节点
        List<Node> terminalNodes = findTerminalNodes(element);
        for (Node terminalNode : terminalNodes) {
            // 查找在 Document 对象的根节点下与每一个末端节点路径相同的节点
            Node correspondingNode = findCorrespondingNode(terminalNode, document.getDocumentElement());
            // 若存在对应节点，则替换
            if (correspondingNode != null) {
                Node newNode = document.importNode(terminalNode, true);
                correspondingNode.getParentNode().replaceChild(newNode, correspondingNode);
            } else {
                // 若不存在对应节点，则添加
                Node parentNode = findParentNode(terminalNode, document.getDocumentElement());
                if (parentNode != null) {
                    Node newNode = document.importNode(terminalNode, true);
                    parentNode.appendChild(newNode);
                }
            }
        }
    }

    // 找到节点的所有分支的每一个末端节点
    private static List<Node> findTerminalNodes(Node node) {
        List<Node> terminals = new ArrayList<>();
        findTerminalNodesRecursive(node, terminals);
        return terminals;
    }

    private static void findTerminalNodesRecursive(Node node, List<Node> terminals) {
        if (node.hasChildNodes()) {
            for (Node child : getChildNodes(node)) {
                findTerminalNodesRecursive(child, terminals);
            }
        } else {
            terminals.add(node);
        }
    }

    private static List<Node> getChildNodes(Node node) {
        List<Node> children = new ArrayList<>();
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            children.add(child);
        }
        return children;
    }

    // 在根节点下查找与给定节点路径相同的节点
    private static Node findCorrespondingNode(Node node, Node rootNode) {
        String path = getNodePath(node);
        return findNodeByPath(path, rootNode);
    }

    // 获取节点路径
    private static String getNodePath(Node node) {
        StringBuilder pathBuilder = new StringBuilder();
        Node currentNode = node;
        while (currentNode.getParentNode() != null) {
            pathBuilder.insert(0, "/" + currentNode.getNodeName());
            currentNode = currentNode.getParentNode();
        }
        return pathBuilder.toString();
    }

    // 根据节点路径在根节点下查找节点
    private static Node findNodeByPath(String path, Node rootNode) {
        String[] nodeNames = path.split("/");
        Node currentNode = rootNode;
        for (String nodeName : nodeNames) {
            if (nodeName.isEmpty()) continue;
            boolean found = false;
            for (Node child : getChildNodes(currentNode)) {
                if (child.getNodeName().equals(nodeName)) {
                    currentNode = child;
                    found = true;
                    break;
                }
            }
            if (!found) return null;
        }
        return currentNode;
    }

    // 在根节点下查找给定节点的父节点
    private static Node findParentNode(Node node, Node rootNode) {
        String path = getNodePath(node);
        String parentPath = path.substring(0, path.lastIndexOf("/" + node.getNodeName()));
        return findNodeByPath(parentPath, rootNode);
    }
    
}
