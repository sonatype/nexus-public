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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.sonatype.plexus.rest.xstream.LookAheadStreamReader;

import com.thoughtworks.xstream.converters.ErrorWriter;
import com.thoughtworks.xstream.core.util.FastStack;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.StreamException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * HierarchicalStreamReader for JSON that uses json.org/java stuff for reading.
 *
 * @author cstamas
 */
public class JsonOrgHierarchicalStreamReader
    implements HierarchicalStreamReader, LookAheadStreamReader
{

  private ClassHintProvider classHintProvider;

  private FastStack objects = new FastStack(16);

  private Node currentNode;

  private String currentKey;

  public class EmptyIterator
      implements Iterator
  {

    public boolean hasNext() {
      return false;
    }

    public Object next() {
      throw new UnsupportedOperationException("Operation remove not supported!");
    }

    public void remove() {
      throw new UnsupportedOperationException("Operation remove not supported!");
    }
  }

  public class IntegerIterator
      implements Iterator
  {

    private int max;

    private int ptr;

    public IntegerIterator(int max) {
      this.max = max;
      this.ptr = -1;
    }

    public boolean hasNext() {
      return ptr < max - 1;
    }

    public Object next() {
      return Integer.toString(++ptr);
    }

    public void remove() {
      throw new UnsupportedOperationException("Operation remove not supported!");
    }
  }

  public class ArrayValuesIterator
      implements Iterator
  {

    private JSONArray array;

    private int ptr;

    public ArrayValuesIterator(JSONArray array) {
      this.array = array;
      this.ptr = 0;
    }

    public boolean hasNext() {
      return ptr < array.length() - 1;
    }

    public Object next() {
      try {
        return array.get(ptr++);
      }
      catch (JSONException e) {
        throw new StreamException(e);
      }
    }

    public void remove() {
      throw new UnsupportedOperationException("Operation remove not supported!");
    }
  }

  public class ObjectValuesIterator
      implements Iterator
  {

    private JSONObject object;

    private Iterator keys;

    public ObjectValuesIterator(JSONObject object, Iterator keys) {
      this.object = object;
      this.keys = keys;
    }

    public boolean hasNext() {
      return keys.hasNext();
    }

    public Object next() {
      try {
        return object.get((String) keys.next());
      }
      catch (JSONException e) {
        throw new StreamException(e);
      }
    }

    public void remove() {
      throw new UnsupportedOperationException("Operation remove not supported!");
    }
  }

  public class Node
  {

    public final String name;

    public final NodeType nodeType;

    public final Object jsonObject;

    public final boolean valueNull;

    public final Iterator keys;

    public final Iterator values;

    public final List<String> attributes;

    public Node(String name, Object object) {
      this.name = name;
      this.jsonObject = object;
      if (object instanceof Boolean) {
        nodeType = NodeType.BOOLEAN;
        valueNull = false;
        keys = null;
        values = null;
        attributes = null;
      }
      else if (object instanceof Number) {
        nodeType = NodeType.NUMBER;
        valueNull = false;
        keys = null;
        values = null;
        attributes = null;
      }
      else if (object.getClass().isAssignableFrom(String.class)) {
        nodeType = NodeType.STRING;
        valueNull = false;
        keys = null;
        values = null;
        attributes = null;
      }
      else if (object.getClass().isAssignableFrom(JSONArray.class)) {
        nodeType = NodeType.ARRAY;
        valueNull = false;
        keys = new IntegerIterator(((JSONArray) object).length());
        values = new ArrayValuesIterator((JSONArray) object);
        attributes = null;
      }
      else if (object.getClass().isAssignableFrom(Date.class)) {
        nodeType = NodeType.DATE;
        valueNull = false;
        keys = null;
        values = null;
        attributes = null;
      }
      else {
        nodeType = NodeType.OBJECT;
        valueNull = JSONObject.NULL.equals(object);

        if (valueNull) {
          keys = null;
          values = null;
          attributes = null;
        }
        else {
          attributes = new ArrayList<String>();
          final Collection<String> filteredKeys = new ArrayList<String>();
          for (Iterator keys = ((JSONObject) object).keys(); keys.hasNext(); ) {
            String key = (String) keys.next();
            if (key.startsWith("@")) {
              attributes.add(key.substring(1));
            }
            else {
              filteredKeys.add(key);
            }
          }
          keys = filteredKeys.iterator();
          values = new ObjectValuesIterator((JSONObject) object, filteredKeys.iterator());
        }
      }
    }

    public String toString() {
      return name + " :: " + (jsonObject != null ? jsonObject.toString() : "");
    }
  }

  public JsonOrgHierarchicalStreamReader(Reader reader, boolean expectTopLevelEnvelope) {
    super();

    setUp(reader, expectTopLevelEnvelope);
  }

  public JsonOrgHierarchicalStreamReader(Reader reader, boolean expectTopLevelEnvelope,
                                         ClassHintProvider classHintProvider)
  {
    super();

    this.classHintProvider = classHintProvider;

    setUp(reader, expectTopLevelEnvelope);
  }

  protected void setUp(Reader reader, boolean expectTopLevelEnvelope) {
    StringBuffer jsonStringB = new StringBuffer();
    BufferedReader in = new BufferedReader(reader);
    String x = null;
    try {
      x = in.readLine();
      while (x != null) {
        jsonStringB.append(x);
        x = in.readLine();
      }
      in.close();

      String jsonString = jsonStringB.toString().trim();

      if (expectTopLevelEnvelope) {
        if (jsonString.startsWith("[")) {
          // if using "envelopes", array cannot be root object
          throw new StreamException(
              "JSON root element must be JSONObject with one member, and it must start with '{' (expectTopLevelEnvelope is TRUE)!"
          );
        }
        else if (jsonString.startsWith("{")) {
          JSONObject jsonObject = new JSONObject(jsonString.toString());
          String[] keys = JSONObject.getNames(jsonObject);
          if (keys.length == 1) {
            // this is an "envelope"
            currentKey = keys[0];
            currentNode = new Node(currentKey, jsonObject.get(currentKey));
          }
          else {
            throw new StreamException(
                "JSON root element must be JSONObject with one member, and it must start with '{' (expectTopLevelEnvelope is TRUE)!"
            );
          }
        }
        else {
          throw new StreamException("JSON root element must be JSONObject, it must start with '{'!");
        }
      }
      else {
        // hint needed insted of "this"! JSON is "naked", but we dont know the type of deserialized object
        // this would involve XStream changes and maybe violates XStream promise.
        // but for short term solution it would enable us to read even naked JSON
        // string encoded objects via passing "root" objects to fill up
        // xstream.fromXML( reader, root );
        if (this.classHintProvider == null) {
          currentKey = "this";
        }
        else {
          currentKey = classHintProvider.getRootClass();
        }

        if (jsonString.startsWith("[")) {
          currentNode = new Node(currentKey, new JSONArray(jsonString.toString()));
        }
        else if (jsonString.startsWith("{")) {
          currentNode = new Node(currentKey, new JSONObject(jsonString.toString()));
        }
        else {
          throw new StreamException(
              "JSON root element must be JSONObject or JSONArray, it must start with '{' or '['!"
          );
        }
      }

    }
    catch (IOException e) {
      throw new StreamException(e);
    }
    catch (JSONException e) {
      throw new StreamException(e);
    }

  }

  public void close() {
    // nothing
  }

  public void appendErrors(ErrorWriter errorWriter) {
  }

  public String getAttribute(String name) {
    if ("class".equals(name)) {
      if (classHintProvider != null) {
        classHintProvider.getFieldClass(currentNode.name);
      }

      if (currentNode.valueNull) {
        return "null";
      }
      // special case, when we should "help" xstream
      switch (currentNode.nodeType) {
        case STRING:
          return "string";
        case NUMBER:
          return "int";
        case BOOLEAN:
          return "boolean";
        case ARRAY:
          return null;
        case DATE:
          return "date";
        default:
          Object obj = ((JSONObject) currentNode.jsonObject).opt("@class");
          if (obj != null) {
            return obj.toString();
          }
          else {
            return null;
          }
      }
    }
    else {
      if (currentNode.nodeType == NodeType.OBJECT && !currentNode.valueNull) {
        // only JSON Object may have attrs
        // we are encoding attrs as "@" + fieldName
        Object obj = ((JSONObject) currentNode.jsonObject).opt("@" + name);
        if (obj != null) {
          return obj.toString();
        }
        else {
          return null;
        }
      }
      else {
        return null;
      }
    }
  }

  public String getAttribute(int index) {
    if (currentNode.nodeType == NodeType.OBJECT) {
      return getAttribute(getAttributeName(index));
    }
    else {
      return null;
    }
  }

  public int getAttributeCount() {
    if (currentNode.nodeType == NodeType.OBJECT && !currentNode.valueNull) {
      return currentNode.attributes.size();
    }
    else {
      return 0;
    }
  }

  public String getAttributeName(int index) {
    if (currentNode.nodeType == NodeType.OBJECT && !currentNode.valueNull) {
      return currentNode.attributes.get(index);
    }
    else {
      return null;
    }
  }

  public Iterator getAttributeNames() {
    if (currentNode.nodeType == NodeType.OBJECT && !currentNode.valueNull) {
      return currentNode.attributes.iterator();
    }
    else {
      // API contract obligates us not to return null
      return new EmptyIterator();
    }
  }

  public String getNodeName() {
    Node parent = (Node) objects.peek();
    if (parent != null && parent.nodeType == NodeType.ARRAY) {
      // if we are _in_ array, we are "simulating types" with elems
      switch (currentNode.nodeType) {
        case STRING:
          return "string";
        case NUMBER:
          return "int";
        case BOOLEAN:
          return "boolean";
        default:
          // this causes error
          return currentNode.name;
      }
    }
    else {
      return currentNode.name;
    }
  }

  public String getValue() {
    if (currentNode.nodeType == NodeType.STRING || currentNode.nodeType == NodeType.BOOLEAN
        || currentNode.nodeType == NodeType.NUMBER) {
      if (currentNode.valueNull) {
        return null;
      }
      else {
        return currentNode.jsonObject.toString();
      }
    }
    else {
      return null;
    }
  }

  public boolean hasMoreChildren() {
    return currentNode.keys != null && currentNode.keys.hasNext();
  }

  public void moveDown() {
    objects.push(currentNode);
    this.currentNode = new Node((String) currentNode.keys.next(), currentNode.values.next());
  }

  public void moveUp() {
    this.currentNode = (Node) objects.pop();
  }

  public HierarchicalStreamReader underlyingReader() {
    return this;
  }

  public String getFieldValue(String fieldName) {
    if (currentNode.nodeType == NodeType.OBJECT) {
      return ((JSONObject) currentNode.jsonObject).optString(fieldName);
    }
    else {
      return null;
    }
  }

}
