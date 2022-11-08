package io.metersphere.api.jmeter.utils;

import io.metersphere.utils.LoggerUtil;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

public class XMLUtil {
    public static String formatXmlString(String xmlString) throws Exception {
        xmlString = StringUtils.replace(xmlString, StringUtils.LF, StringUtils.EMPTY);
        Document document = getDom4jDocumentByXmlString(xmlString);
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setEncoding(document.getXMLEncoding());
        StringWriter stringWriter = new StringWriter();
        XMLWriter writer = new XMLWriter(stringWriter, format);
        writer.write(document);
        writer.close();
        return stringWriter.toString();
    }

    public static Document getDom4jDocumentByXmlString(String xmlString) throws Exception {
        SAXReader reader = new SAXReader();
        try {
            reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
            reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception e) {
            LoggerUtil.error(e);
        }
        return reader.read(new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8.name())));
    }
}
