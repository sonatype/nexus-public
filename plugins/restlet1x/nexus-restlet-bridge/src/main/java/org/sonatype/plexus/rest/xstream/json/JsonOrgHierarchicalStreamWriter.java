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
package org.sonatype.plexus.rest.xstream.json;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Date;

import com.thoughtworks.xstream.core.util.FastStack;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.StreamException;
import org.json.JSONException;
import org.json.JSONWriter;

/**
 * An XStream JSON Writer, that is using JSONWriter from http://www.json.org/java/ It is able to produce JSON in
 * identical manner as the original JsonHierarchicalStreamWriter, thus "encapsulated" into anonymous top level object,
 * but it can be parametrized to not do that too.
 * <p>
 * Heavily based on original JsonHierarchicalStreamWriter written by XSTream team.
 *
 * @author cstamas
 */
public class JsonOrgHierarchicalStreamWriter
    implements ExtendedHierarchicalStreamWriter
{

  private final Writer writer;

  private final FastStack elementStack = new FastStack(16);

  private final boolean createTopLevelEnvelope;

  private JSONWriter jsonWriter;

  public class Node
  {
    public final String name;

    public final Class clazz;

    public final NodeType nodeType;

    public Node(String name, Class clazz, NodeType type) {
      this.name = name;
      this.clazz = clazz;
      this.nodeType = type;
    }
  }

  public JsonOrgHierarchicalStreamWriter(Writer writer, boolean createTopLevelEnvelope) {
    this.writer = writer;
    this.createTopLevelEnvelope = createTopLevelEnvelope;
    this.jsonWriter = new JSONWriter(writer);
  }

  /**
   * @deprecated
   */
  public void startNode(String name) {
    startNode(name, Object.class);
  }

  public void startNode(String name, Class clazz) {
    Node currNode = (Node) elementStack.peek();
    NodeType nt = null;
    try {
      if (currNode == null && createTopLevelEnvelope) {
        jsonWriter.object();
        currNode = new Node("root", Object.class, NodeType.OBJECT);
      }

      if (currNode != null && currNode.nodeType == NodeType.OBJECT) {
        jsonWriter.key(name);
      }

      // OBJECT, ARRAY, NUMBER, STRING, BOOLEAN, DATE
      if (clazz == null) {
        jsonWriter.value(null);
      }
      else if (Collection.class.isAssignableFrom(clazz) || clazz.isArray()) {
        jsonWriter.array();
        nt = NodeType.ARRAY;
      }
      else if (Boolean.class.isAssignableFrom(clazz) || boolean.class.isAssignableFrom(clazz)) {
        // boolean
        nt = NodeType.BOOLEAN;
      }
      else if (Integer.class.isAssignableFrom(clazz) || Long.class.isAssignableFrom(clazz)
          || Float.class.isAssignableFrom(clazz) || Double.class.isAssignableFrom(clazz)
          || int.class.isAssignableFrom(clazz) || long.class.isAssignableFrom(clazz)
          || float.class.isAssignableFrom(clazz) || double.class.isAssignableFrom(clazz)) {
        // number
        nt = NodeType.NUMBER;
      }
      else if (String.class.isAssignableFrom(clazz) || Character.class.isAssignableFrom(clazz)) {
        // string
        nt = NodeType.STRING;
      }
      else if (Date.class.isAssignableFrom(clazz)) {
        // date
        nt = NodeType.DATE;
      }
      else {
        jsonWriter.object();
        nt = NodeType.OBJECT;
      }
    }
    catch (JSONException e) {
      throw new StreamException(e);
    }
    elementStack.push(new Node(name, clazz, nt));
  }

  public void addAttribute(String name, String value) {
    try {
      // ahem, not sure about this, stolen from original JSON driver
      jsonWriter.key('@' + name).value(value);
    }
    catch (JSONException e) {
      throw new StreamException(e);
    }
  }

  public void setValue(String text) {
    Node node = (Node) elementStack.peek();
    try {
      switch (node.nodeType) {
        case STRING:
          jsonWriter.value(text);
          break;
        case NUMBER:
          jsonWriter.value(Long.parseLong(text));
          break;
        case BOOLEAN:
          jsonWriter.value(Boolean.parseBoolean(text));
          break;
        case DATE:
          jsonWriter.value(text);
          break;
      }
    }
    catch (JSONException e) {
      throw new StreamException(e);
    }
  }

  public void endNode() {
    Node node = (Node) elementStack.pop();
    try {
      switch (node.nodeType) {
        case OBJECT:
          jsonWriter.endObject();
          break;
        case ARRAY:
          jsonWriter.endArray();
          break;
        default:
          // nothing
          break;
      }

      if (!elementStack.hasStuff() && createTopLevelEnvelope) {
        jsonWriter.endObject();
      }
    }
    catch (JSONException e) {
      throw new StreamException(e);
    }
  }

  public void flush() {
    try {
      writer.flush();
    }
    catch (IOException e) {
      throw new StreamException(e);
    }
  }

  public void close() {
    try {
      writer.close();
    }
    catch (IOException e) {
      throw new StreamException(e);
    }
  }

  public HierarchicalStreamWriter underlyingWriter() {
    return this;
  }

}
