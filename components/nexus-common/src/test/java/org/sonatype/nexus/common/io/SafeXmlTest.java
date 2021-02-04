/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.common.io;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.StringJoiner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import static javax.xml.stream.XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES;
import static javax.xml.stream.XMLInputFactory.SUPPORT_DTD;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class SafeXmlTest
{
  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static File xmlDocument;

  @BeforeClass
  public static void setup() throws IOException {
    xmlDocument = temp.newFile();
    File externalFile = temp.newFile();

    Files.write(externalFile.toPath(), Collections.singleton("bar"), StandardOpenOption.TRUNCATE_EXISTING);

    try (InputStream content = SafeXmlTest.class.getResourceAsStream("xxe.xml")) {
      StringJoiner joiner = new StringJoiner(System.lineSeparator());
      IOUtils.readLines(content).forEach(joiner::add);

      String output = joiner.toString().replace("${fileUri}", externalFile.toURI().toString());
      Files.copy(new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8)), xmlDocument.toPath(),
          StandardCopyOption.REPLACE_EXISTING);
    }
  }

  @Test
  public void testDocumentBuilderFactory() throws IOException, SAXException, ParserConfigurationException {
    DocumentBuilder builder = SafeXml.newdocumentBuilderFactory().newDocumentBuilder();
    Document doc = builder.parse(xmlDocument);

    assertThat(doc.getElementsByTagName("foo").item(0).getTextContent(), not(containsString("bar")));
  }

  @Test
  public void testStrictDocumentBuilderFactory() throws IOException, SAXException, ParserConfigurationException {
    thrown.expect(SAXParseException.class);
    thrown.expectMessage(containsString("DOCTYPE is disallowed"));

    DocumentBuilder builder = SafeXml.newStrictDocumentBuilderFactory().newDocumentBuilder();
    builder.parse(xmlDocument);
  }

  @Test
  public void testTransformerFactory() throws TransformerException {
    thrown.expect(TransformerException.class);
    thrown.expectMessage(containsString("accessExternalDTD"));

    StreamSource xmlSource = new StreamSource(xmlDocument);
    Transformer transformer = SafeXml.newTransformerFactory().newTransformer();

    transformer.transform(xmlSource, new StreamResult(new StringWriter()));
  }

  @Test
  public void testSAXParserFactory() throws ParserConfigurationException, SAXException, IOException {
    StringBuilder sb = new StringBuilder();

    DefaultHandler handler = new DefaultHandler()
    {
      @Override
      public void characters(final char[] ch, final int start, final int length) {
        sb.append(ch);
      }
    };
    SafeXml.newSaxParserFactory().newSAXParser().parse(xmlDocument, handler);
    assertThat(sb.toString(), not(containsString("bar")));
  }

  @Test
  public void testStrictSAXParserFactory() throws ParserConfigurationException, SAXException, IOException {
    thrown.expect(SAXParseException.class);
    thrown.expectMessage(containsString("DOCTYPE is disallowed"));

    SafeXml.newStrictSaxParserFactory().newSAXParser().parse(xmlDocument, new DefaultHandler());
  }

  @Test
  public void testSaxTransformerFactory() throws TransformerException {
    thrown.expect(TransformerException.class);
    thrown.expectMessage(containsString("accessExternalDTD"));

    StreamSource xmlSource = new StreamSource(xmlDocument);
    Transformer transformer = SafeXml.newSaxTransformerFactory().newTransformer();

    transformer.transform(xmlSource, new StreamResult(new StringWriter()));
  }

  @Test
  public void testXmlInputFactory() throws XMLStreamException, IOException {
    thrown.expect(XMLStreamException.class);
    thrown.expectMessage(containsString("The entity \"xxe\" was referenced, but not declared."));

    // This code is more complicated than it needs to be to demonstrate finding the XXE content.
    try (Reader reader = Files.newBufferedReader(xmlDocument.toPath())) {
      XMLEventReader xmlEventReader = SafeXml.newXmlInputFactory().createXMLEventReader(reader);

      String content = "";
      boolean track = false;
      while (xmlEventReader.hasNext()) {
        XMLEvent xmlEvent = xmlEventReader.nextEvent();

        if (xmlEvent.isStartElement() && xmlEvent.asStartElement().getName().getLocalPart().equals("foo")) {
          track = true;
        }
        else if (xmlEvent.isEndElement() && xmlEvent.asEndElement().getName().getLocalPart().equals("foo")) {
          assertThat(content, not(containsString("bar")));
          return;
        }
        else if (track && xmlEvent.isCharacters()) {
          content += xmlEvent.asCharacters().getData();
        }
      }
      fail("Did not find element foo");
    }
  }

  @Test
  public void shouldNotSupportDocTypeDefinitions() {
    XMLInputFactory xmlInputFactory = SafeXml.newXmlInputFactory();

    Object supportDtd = xmlInputFactory.getProperty(SUPPORT_DTD);

    assertFalse(Boolean.parseBoolean(supportDtd.toString()));
  }

  @Test
  public void shouldNotSupportExternalEntities() {
    XMLInputFactory xmlInputFactory = SafeXml.newXmlInputFactory();

    Object supportExternalEntities = xmlInputFactory.getProperty(IS_SUPPORTING_EXTERNAL_ENTITIES);

    assertFalse(Boolean.parseBoolean(supportExternalEntities.toString()));
  }
}
