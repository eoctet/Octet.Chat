package chat.octet.utils;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.StringReader;
import java.util.Iterator;
import java.util.LinkedHashMap;


@Slf4j
public class XmlParser {

    private XmlParser() {
    }

    public static LinkedHashMap<String, Object> parseXmlToMap(String xmlContent) {
        LinkedHashMap<String, Object> maps = Maps.newLinkedHashMap();
        try (StringReader stringReader = new StringReader(xmlContent)) {
            SAXReader reader = new SAXReader();
            Document document = reader.read(stringReader);
            Element node = document.getRootElement();
            parseXmlToMap(maps, node);
        } catch (Exception e) {
            log.error("Parse xml content error", e);
        }
        return maps;
    }

    private static void parseXmlToMap(LinkedHashMap<String, Object> result, Element node) {
        if (node.isTextOnly()) {
            result.put(node.getName(), node.getText().trim());
            return;
        }
        Iterator<Element> iterator = node.elementIterator();
        while (iterator.hasNext()) {
            Element element = iterator.next();
            if (!CommonUtils.isEmpty(element.elements())) {
                LinkedHashMap<String, Object> elementMaps = Maps.newLinkedHashMap();
                result.put(element.getName(), elementMaps);
                parseXmlToMap(elementMaps, element);
            } else {
                parseXmlToMap(result, element);
            }
        }
    }
}
