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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Rewrites a p2 composite site collecting child site references and replacing them with site hashes.
 *
 * @since 3.28
 */
public class CompositeRepositoryRewriter
    extends XmlTransformer
{
  private final URI baseUri;

  private final boolean isRoot;

  private List<String> urls = new ArrayList<>();

  private final Function<String, String> uriConverter;

  /**
   * This constructor is intended for use by an upgrade step, not general use.
   */
  public CompositeRepositoryRewriter(final URI baseUri, final boolean isRoot, final UnaryOperator<String> uriConverter ) {
    this.baseUri = baseUri;
    this.isRoot = isRoot;
    this.uriConverter = uriConverter;
  }

  /**
   * @param baseUri the base URI of the remote composite repository
   * @param isRoot indicates whether this composite  site occurs at the root of the NXRM repository
   *
   */
  public CompositeRepositoryRewriter(final URI baseUri, final boolean isRoot) {
    this(baseUri, isRoot, UnaryOperator.identity());
  }

  /**
   * Get the referenced URLs in the composite site.
   */
  public List<String> getUrls() {
    return urls;
  }

  @Override
  protected void transform(final XMLEventReader reader, final XMLEventWriter writer) throws XMLStreamException {
    List<XMLEvent> buffer = new ArrayList<>();

    while (reader.hasNext()) {
      XMLEvent event = reader.nextEvent();

      if (isStartTagWithName(event, "child")) {
        buffer.add(changeLocationAttribute((StartElement) event));
      }
      else {
        buffer.add(event);
      }

      for (XMLEvent xmlEvent : buffer) {
        writer.add(xmlEvent);
      }
      buffer.clear();
    }
  }

  private XMLEvent changeLocationAttribute(final StartElement locationElement) {
    QName locationAttrName = new QName("location");
    Attribute locationAttribute = locationElement.getAttributeByName(locationAttrName);

    if (locationAttribute == null) {
      return locationElement;
    }

    String location = uriConverter.apply(locationAttribute.getValue());
    String remoteUrl = baseUri.resolve(location).toString();
    if (!remoteUrl.endsWith("/")) {
      remoteUrl += '/';
    }
    urls.add(remoteUrl);
    String path = derivePath(remoteUrl);

    return XMLEventFactory.newInstance().createStartElement(
        new QName("child"), Collections
            .singletonList(XMLEventFactory.newInstance().createAttribute(locationAttrName, path))
            .iterator(),
        Collections.emptyIterator());
  }

  private String derivePath(final String remoteUrl) {
    StringBuilder path = new StringBuilder();
    if (!isRoot) {
      path.append("../");
    }
    path.append(UriToSiteHashUtil.map(remoteUrl));
    return path.append('/').toString();
  }
}
