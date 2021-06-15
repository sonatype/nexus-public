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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Find and remove the p2.mirrorsUrl property
 *
 * @since 3.28
 */
public class RemoveMirrorTransformer
    extends XmlTransformer
{
  private static final String MIRRORS_URL_PROPERTY = "p2.mirrorsURL";

  private static final String PROPERTIES_KEY = "properties";

  private String mirrorsUrl;

  /**
   * The extracted mirrors URL.
   */
  public Optional<String> getMirrorsUrl() {
    return Optional.ofNullable(mirrorsUrl);
  }

  @Override
  protected void transform(final XMLEventReader reader, final XMLEventWriter writer) throws XMLStreamException {
    final XMLEventFactory eventFactory = XMLEventFactory.newInstance();
    XMLEvent previous = null;

    // We need to buffer events so that we can also update properties size when removing the mirrorsUrl property
    List<XMLEvent> buffer = new ArrayList<>();
    while (reader.hasNext()) {
      XMLEvent event = reader.nextEvent();

      // If xml tag is "properties" then start buffering. If started buffering then keep buffering until the buffer
      // is cleared
      if (!buffer.isEmpty() || isStartTagWithName(event, PROPERTIES_KEY)) {
        // Exclude the mirrorsURL property
        if (!(isMirrorsUrlProperty(event) || isMirrorsUrlProperty(previous))) {
          buffer.add(event);
        }
      }

      if (buffer.isEmpty()) {
        writer.add(event);
      }

      // When reached end of <properties> section, update count and flush buffer to writer.
      if (isEndTagWithName(event, PROPERTIES_KEY)) {
        for (XMLEvent xmlEvent : buffer) {
          if (isStartTagWithName(xmlEvent, PROPERTIES_KEY)) {
            xmlEvent = updateSize(xmlEvent.asStartElement(), countPropertyTags(buffer), eventFactory);
          }
          writer.add(xmlEvent);
        }
        buffer.clear();
      }

      previous = event;
    }
  }

  private boolean isMirrorsUrlProperty(@Nullable final XMLEvent event) {
    if (isStartTagWithName(event, "property")) {
      Attribute name = event.asStartElement().getAttributeByName(new QName("name"));
      if (name != null && name.getValue().equals(MIRRORS_URL_PROPERTY)) {
        Attribute value = event.asStartElement().getAttributeByName(new QName("value"));
        if (value != null) {
          mirrorsUrl = value.getValue();
        }
        return true;
      }
    }
    return false;
  }

  private int countPropertyTags(final List<XMLEvent> buffer) {
    int count = 0;
    for (XMLEvent xmlEvent : buffer) {
      if (isStartTagWithName(xmlEvent, "property")) {
        count++;
      }
    }
    return count;
  }

  @SuppressWarnings("unchecked")
  private static XMLEvent updateSize(final StartElement tag, final int size, final XMLEventFactory eventFactory) {
    Iterator<Attribute> updatedAttributes = updateSizeAttribute(tag.getAttributes(), size, eventFactory);
    return eventFactory.createStartElement(tag.getName().getPrefix(), tag.getName().getNamespaceURI(),
        tag.getName().getLocalPart(), updatedAttributes, null);
  }

  private static Iterator<Attribute> updateSizeAttribute(
      final Iterator<Attribute> attributes,
      final int size,
      final XMLEventFactory eventFactory)
  {
    List<Attribute> processedAttributes = new ArrayList<>();
    while (attributes.hasNext()) {
      Attribute attribute = attributes.next();
      if (attribute.getName().getLocalPart().equals("size")) {
        Attribute sizeAttribute = eventFactory.createAttribute(attribute.getName(), Integer.toString(size));
        if (sizeAttribute != null) {
          processedAttributes.add(sizeAttribute);
        }
      }
      else {
        processedAttributes.add(attribute);
      }
    }
    return processedAttributes.iterator();
  }
}
