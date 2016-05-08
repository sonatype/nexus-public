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
package org.sonatype.nexus.supportzip;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * {@link FileContentSourceSupport} that allows XSLT rewrites of existing XML content. Note that the transform
 * is performed in memory and the results saved for the lifetime of the instance, so use on large XML files is not
 * recommended.
 *
 * @since 3.0
 */
public class SanitizedXmlSourceSupport
    extends FileContentSourceSupport
{
  private final String stylesheet;

  private byte[] content;

  /**
   * Constructor.
   */
  public SanitizedXmlSourceSupport(final Type type,
                                   final String path,
                                   final File file,
                                   final Priority priority,
                                   final String stylesheet)
  {
    super(type, path, file, priority);
    this.stylesheet = checkNotNull(stylesheet);
  }

  @Override
  public void prepare() throws Exception {
    super.prepare();
    checkState(content == null);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    try (InputStream input = new BufferedInputStream(new FileInputStream(file))) {
      try (OutputStream output = new BufferedOutputStream(stream)) {

        StreamSource styleSource = new StreamSource(new StringReader(stylesheet));
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer(styleSource);

        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        parserFactory.setNamespaceAware(true);

        SAXParser parser = parserFactory.newSAXParser();
        XMLReader reader = parser.getXMLReader();
        reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
        reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        transformer.transform(new SAXSource(reader, new InputSource(input)), new StreamResult(output));
      }
    }
    content = stream.toByteArray();
  }

  @Override
  public long getSize() {
    checkState(content != null);
    return content.length;
  }

  @Override
  public InputStream getContent() throws Exception {
    checkState(content != null);
    log.debug("Reading: {} from memory", file);
    return new BufferedInputStream(new ByteArrayInputStream(content));
  }
}
