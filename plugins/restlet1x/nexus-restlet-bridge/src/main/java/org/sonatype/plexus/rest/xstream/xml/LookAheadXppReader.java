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
package org.sonatype.plexus.rest.xstream.xml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.sonatype.plexus.rest.xstream.LookAheadStreamReader;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.ErrorWriter;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.xml.AbstractPullReader;
import com.thoughtworks.xstream.io.xml.XmlFriendlyReplacer;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * A wrapper around an XML Pull Parser that allows read events to be queued so it can implement the {@link
 * LookAheadStreamReader} interface.
 */
public class LookAheadXppReader
    extends AbstractPullReader
    implements LookAheadStreamReader
{
  private final XmlPullParser parser;

  private final BufferedReader reader;

  private LinkedList<ReadEvent> queue = new LinkedList<ReadEvent>();

  private ReadEvent currentReadEvent = null;

  HashMap<String, String> lookaheadMap = new HashMap<String, String>();

  public LookAheadXppReader(Reader reader) {
    this(reader, new XmlFriendlyReplacer());
  }

  /**
   * @since restlet-bridge 1.2
   */
  public LookAheadXppReader(Reader reader, XmlFriendlyReplacer replacer) {
    super(replacer);
    try {
      parser = createParser();
      this.reader = new BufferedReader(reader);
      parser.setInput(this.reader);

      moveDown();
    }
    catch (XmlPullParserException e) {
      throw new StreamException(e);
    }

  }

  /**
   * To use another implementation of org.xmlpull.v1.XmlPullParser, override this method.
   */
  protected XmlPullParser createParser() {
    return new MXParser();
  }

  protected int pullNextEvent() {
    try {

      int eventType;
      if (this.queue.isEmpty()) {
        eventType = parser.next();
        this.currentReadEvent = createReadEventFromCurrentParserPosition(eventType);
      }
      else {
        this.currentReadEvent = this.queue.remove();
        eventType = this.currentReadEvent.type;
      }

      switch (eventType) {
        case XmlPullParser.START_DOCUMENT:
        case XmlPullParser.START_TAG:
          return START_NODE;
        case XmlPullParser.END_DOCUMENT:
        case XmlPullParser.END_TAG:
          return END_NODE;
        case XmlPullParser.TEXT:
          return TEXT;
        case XmlPullParser.COMMENT:
          return COMMENT;
        default:
          return OTHER;
      }
    }
    catch (XmlPullParserException e) {
      throw new StreamException(e);
    }
    catch (IOException e) {
      throw new StreamException(e);
    }
  }

  public String getFieldValue(String field) {

    if (this.queue.isEmpty()) {
      String node = "";
      String nodeValue = "";

      int eventType = 0;
      do {
        try {
          eventType = this.parser.next();
        }
        catch (Exception e) {
          throw new ConversionException(e.getMessage(), e);
        }

        if (eventType == XmlPullParser.START_TAG) {
          node = this.parser.getName();
        }
        else if (eventType == XmlPullParser.END_TAG) {
          this.lookaheadMap.put(node, nodeValue);
          node = null;
        }
        else if (eventType == XmlPullParser.TEXT) {
          nodeValue = this.parser.getText();
        }

        ReadEvent item = createReadEventFromCurrentParserPosition(eventType);
        this.queue.add(item);

      }
      while (eventType != XmlPullParser.END_DOCUMENT);

    }

    return this.lookaheadMap.get(field);
  }

  private ReadEvent createReadEventFromCurrentParserPosition(int eventType) {
    ReadEvent item = new ReadEvent();
    item.type = eventType;
    item.name = this.parser.getName();
    item.text = this.parser.getText();

    for (int ii = 0; ii < this.parser.getAttributeCount(); ii++) {
      String attributeName = this.parser.getAttributeName(ii);
      String attributeValue = this.parser.getAttributeValue(ii);
      item.attributes.put(unescapeXmlName(attributeName), attributeValue);
    }

    return item;
  }

  protected String pullElementName() {
    return this.currentReadEvent.name;
  }

  protected String pullText() {
    return this.currentReadEvent.text;
  }

  public String getAttribute(String name) {
    return this.currentReadEvent.attributes.get(name);
  }

  public String getAttribute(int index) {
    return this.currentReadEvent.attributes.get(this.getAttributeName(index));
  }

  public int getAttributeCount() {
    return this.currentReadEvent.attributes.size();
  }

  public String getAttributeName(int index) {
    List<Entry<String, String>> entries = new ArrayList<Entry<String, String>>();
    for (Iterator<Entry<String, String>> iterator = this.currentReadEvent.attributes.entrySet().iterator();
         iterator.hasNext(); ) {
      entries.add(iterator.next());
    }
    return entries.get(index).getValue();

  }

  public void appendErrors(ErrorWriter errorWriter) {
    errorWriter.add("line number", String.valueOf(parser.getLineNumber()));
  }

  public void close() {
    try {
      reader.close();
    }
    catch (IOException e) {
      throw new StreamException(e);
    }
  }

  class ReadEvent
  {
    int type;

    String text;

    String name;

    LinkedHashMap<String, String> attributes = new LinkedHashMap<String, String>();
  }

}
