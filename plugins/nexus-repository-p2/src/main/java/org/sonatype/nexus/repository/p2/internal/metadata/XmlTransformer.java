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
package org.sonatype.nexus.repository.p2.internal.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.Nullable;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.io.SafeXml;
import org.sonatype.nexus.repository.p2.internal.proxy.StreamCopier.StreamTransformer;

import org.apache.commons.io.IOUtils;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 * @since 3.28
 */
public abstract class XmlTransformer
    extends ComponentSupport
    implements StreamTransformer
{

  @Override
  public void transform(final InputStream in, final OutputStream out) throws IOException {
    XMLInputFactory inputFactory = SafeXml.newXmlInputFactory();
    XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
    XMLEventReader reader = null;
    XMLEventWriter writer = null;

    // try-with-resources will be better here, but XMLEventReader and XMLEventWriter are not AutoCloseable
    try {
      reader = inputFactory.createXMLEventReader(in);
      writer = outputFactory.createXMLEventWriter(out);
      transform(reader, writer);
      writer.flush();
    }
    catch (XMLStreamException e) {
      throw new IOException(e);
    }
    finally {
      if (reader != null) {
        close(reader);
      }
      if (writer != null) {
        close(writer);
      }
      IOUtils.close(in);
      IOUtils.close(out);
    }
  }

  private void close(final XMLEventReader reader) {
    try {
      reader.close();
    }
    catch (Exception e) {
      // do nothing
    }
  }

  private void close(final XMLEventWriter writer) {
    try {
      writer.close();
    }
    catch (Exception e) {
      // do nothing
    }
  }

  protected abstract void transform(XMLEventReader reader, XMLEventWriter writer) throws XMLStreamException;

  static boolean isStartTagWithName(@Nullable final XMLEvent tag, final String name) {
    if (tag != null && tag.getEventType() == START_ELEMENT) {
      StartElement startElement = tag.asStartElement();
      if (startElement.getName().getLocalPart().equals(name)) {
        return true;
      }
    }
    return false;
  }

  static boolean isEndTagWithName(@Nullable final XMLEvent tag, final String name) {
    if (tag != null && tag.getEventType() == END_ELEMENT) {
      EndElement endElement = tag.asEndElement();
      if (endElement.getName().getLocalPart().equals(name)) {
        return true;
      }
    }
    return false;
  }
}
