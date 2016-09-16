package de.akubix.keyminder.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import de.akubix.keyminder.lib.XMLCore;

public class XMLCoreTest {

	@Test
	public void testLoadWriteXml() throws SAXException, IOException, TransformerException {

		final String xml =	"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
							"<root>\n" +
							"\t<test value=\"true\">Hello world</test>\n" +
							"\t<test2>Hello, again</test2>\n" +
							"</root>\n";

		Assert.assertEquals(withoutNewlines(xml), withoutNewlines(XMLCore.writeXmlDocumentToString(XMLCore.loadXmlDocument(xml))));
	}

	@Test
	public void testMap2Xml() throws ParserConfigurationException {

		final Map<String, String> map = new HashMap<>();
		map.put("hello", "world");
		map.put("hello.world", "...");
		map.put("abc.def.ghi", "123.456");

		final Map<String, String> map2 = new HashMap<>();
		map2.put("hello", "world");
		map2.put("abc.xyz", "...");
		map2.put("abc.def.ghi", "123.456");
		map2.put("abc123:456", "abc");
		map2.put("any.name:with_a_colon", "Hello world!");

		Document xmlDoc = XMLCore.createXmlDocument("test");

		XMLCore.convertMapToFlatXml(map, xmlDoc.getDocumentElement());
		Map<String, String> result = new HashMap<>();
		XMLCore.convertXmlToMap(xmlDoc.getDocumentElement(), result, false);
		compareMaps(map, result);

		result.clear();
		XMLCore.convertXmlToMap(XMLCore.convertMapToXmlDocument(map2, "test"), result, false);
		compareMaps(map2, result);
	}

	private static <K, V> void compareMaps(Map<K, V> original, Map<K, V> copy){

		Assert.assertTrue(original.keySet().containsAll(copy.keySet()));
		Assert.assertTrue(copy.keySet().containsAll(original.keySet()));

		for(K key: original.keySet()){
			Assert.assertEquals(original.get(key), copy.get(key));
		}
	}

	private static String withoutNewlines(String src){
		return src.replaceAll("\n|\t|\r", "");
	}
}
