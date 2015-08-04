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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.AbstractCollectionConverter;
import com.thoughtworks.xstream.core.util.Primitives;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

/**
 * Handy converter for creating ad hoc JSON objects. This converter converts a Maps with keys (as String) as properties
 * and values (as Objects) to a values. Usable in reading and in writing. Warning! This converter will alter the
 * objects
 * structure in XStream output, and will be deserializable only with using this same converter. Note: this is somehow
 * JSON specific, altough it works with XML pretty well to.
 *
 * @author cstamas
 */
public class PrimitiveKeyedMapConverter
    extends AbstractCollectionConverter
{
  public PrimitiveKeyedMapConverter(Mapper mapper) {
    super(mapper);
  }

  public boolean canConvert(Class type) {
    return type.equals(HashMap.class) || type.equals(Hashtable.class)
        || type.getName().equals("java.util.LinkedHashMap") || type.getName().equals("sun.font.AttributeMap");
  }

  public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
    Map map = (Map) source;
    for (Iterator iterator = map.entrySet().iterator(); iterator.hasNext(); ) {
      Map.Entry entry = (Map.Entry) iterator.next();

      Class pClass = Primitives.unbox(entry.getKey().getClass());
      if (String.class.equals(entry.getKey().getClass()) || entry.getKey().getClass().isPrimitive()
          || (pClass != null && pClass.isPrimitive())) {
        ExtendedHierarchicalStreamWriterHelper.startNode(writer, entry.getKey().toString(), entry
            .getValue().getClass());
      }
      else {
        throw new IllegalArgumentException("Cannot convert maps with non-String keys!");

      }
      context.convertAnother(entry.getValue());
      writer.endNode();
    }
  }

  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
    Map map = (Map) createCollection(context.getRequiredType());
    populateMap(reader, context, map);
    return map;
  }

  protected void populateMap(HierarchicalStreamReader reader, UnmarshallingContext context, Map map) {
    while (reader.hasMoreChildren()) {
      reader.moveDown();
      String key = reader.getNodeName();
      Object value;
      if (reader.hasMoreChildren()) {
        // map value is object
        // XXX not working in this way!
        value = readItem(reader, context, map);
      }
      else {
        String classAttribute = reader.getAttribute(mapper().aliasForAttribute("class"));
        Class type;
        if (classAttribute == null) {
          type = mapper().realClass(key);
        }
        else {
          type = mapper().realClass(classAttribute);
        }
        value = context.convertAnother(reader.getValue(), type);
      }
      map.put(key, value);
      reader.moveUp();
    }
  }
}
