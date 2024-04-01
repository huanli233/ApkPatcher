package com.huanli233.apkpatcher.patcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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
        // 如果没有相同节点，则添加新节点到根节点下
        doc.getDocumentElement().appendChild(importedNode);
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
    
}
