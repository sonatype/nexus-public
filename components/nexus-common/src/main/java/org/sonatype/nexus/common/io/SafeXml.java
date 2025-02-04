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

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import static javax.xml.stream.XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES;
import static javax.xml.stream.XMLInputFactory.SUPPORT_DTD;

/**
 * Provides various XML factories that are configured to protect against XXE attacks.
 *
 * @since 3.24
 */
public final class SafeXml
{
  private static final String APACHE_DISALLOW_DOCTYPE_DECL = "http://apache.org/xml/features/disallow-doctype-decl";

  private static final String APACHE_LOAD_EXTERNAL_DTD =
      "http://apache.org/xml/features/nonvalidating/load-external-dtd";

  private static final String XML_EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities";

  private static final String XML_EXTERNAL_PARAMETER_ENTITIES =
      "http://xml.org/sax/features/external-parameter-entities";

  private SafeXml() {
  }

  public static XMLInputFactory newXmlInputFactory() {
    final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
    xmlInputFactory.setProperty(SUPPORT_DTD, false);
    xmlInputFactory.setProperty(IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    return xmlInputFactory;
  }

  public static DocumentBuilderFactory newdocumentBuilderFactory() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    factory.setFeature(APACHE_DISALLOW_DOCTYPE_DECL, false);
    factory.setFeature(APACHE_LOAD_EXTERNAL_DTD, false);
    factory.setFeature(XML_EXTERNAL_GENERAL_ENTITIES, false);
    factory.setFeature(XML_EXTERNAL_PARAMETER_ENTITIES, false);

    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);

    return factory;
  }

  /**
   * A stricter version of {@link #documentBuilderFactory} which will throw an exception when encountering a doctype
   * declaration. Unfortunately this is probably not suitable for many use cases.
   */
  public static DocumentBuilderFactory newStrictDocumentBuilderFactory() throws ParserConfigurationException {
    DocumentBuilderFactory factory = newdocumentBuilderFactory();

    factory.setFeature(APACHE_DISALLOW_DOCTYPE_DECL, true);

    return factory;
  }

  public static TransformerFactory newTransformerFactory() {
    TransformerFactory factory = TransformerFactory.newInstance();
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

    return factory;
  }

  public static SAXTransformerFactory newSaxTransformerFactory() {
    SAXTransformerFactory factory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    return factory;
  }

  public static SAXParserFactory newSaxParserFactory() throws SAXException, ParserConfigurationException {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setFeature(APACHE_LOAD_EXTERNAL_DTD, false);
    factory.setFeature(XML_EXTERNAL_GENERAL_ENTITIES, false);
    factory.setFeature(XML_EXTERNAL_PARAMETER_ENTITIES, false);
    return factory;
  }

  /**
   * A stricter version of {@link #saxParserFactory} which will throw an exception when encountering a doctype
   * declaration. Unfortunately this is probably not suitable for many use cases.
   */
  public static SAXParserFactory newStrictSaxParserFactory() throws SAXException, ParserConfigurationException {
    SAXParserFactory factory = newSaxParserFactory();
    factory.setFeature(APACHE_DISALLOW_DOCTYPE_DECL, true);
    return factory;
  }

  public static void configureValidator(
      Validator validator) throws SAXNotRecognizedException, SAXNotSupportedException
  {
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
  }
}
