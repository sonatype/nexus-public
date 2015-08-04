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
package org.sonatype.nexus.rest.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * This Converter allows changing the alias of an element in a list.
 * <p>
 * Usage:
 * <p>
 * <code>
 * &nbsp;&nbsp;&nbsp;xstream.registerLocalConverter( &lt;class containing list&gt;, "listOfStrings", new
 * AliasingListConverter( String.class, "value"));
 * </code>
 * <p>
 * NOTE: only tested with lists of Strings.
 */
public class AliasingListConverter
    implements Converter
{

  /**
   * The type of object list is expected to convert.
   */
  private Class<?> type;

  /**
   *
   */
  private String alias;

  public AliasingListConverter(Class<?> type, String alias) {
    this.type = type;
    this.alias = alias;
  }

  /*
   * (non-Javadoc)
   * @see com.thoughtworks.xstream.converters.ConverterMatcher#canConvert(java.lang.Class)
   */
  @SuppressWarnings("rawtypes")
  public boolean canConvert(Class type) {
    return List.class.isAssignableFrom(type);
  }

  /*
   * (non-Javadoc)
   * @see com.thoughtworks.xstream.converters.Converter#marshal(java.lang.Object,
   * com.thoughtworks.xstream.io.HierarchicalStreamWriter, com.thoughtworks.xstream.converters.MarshallingContext)
   */
  public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
    List<?> list = (List<?>) source;
    for (Iterator<?> iter = list.iterator(); iter.hasNext(); ) {
      Object elem = iter.next();
      if (!elem.getClass().isAssignableFrom(type)) {
        throw new ConversionException("Found " + elem.getClass() + ", expected to find: " + this.type
            + " in List.");
      }

      ExtendedHierarchicalStreamWriterHelper.startNode(writer, alias, elem.getClass());
      context.convertAnother(elem);
      writer.endNode();
    }
  }

  /*
   * (non-Javadoc)
   * @see
   * com.thoughtworks.xstream.converters.Converter#unmarshal(com.thoughtworks.xstream.io.HierarchicalStreamReader,
   * com.thoughtworks.xstream.converters.UnmarshallingContext)
   */
  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
    List<Object> list = new ArrayList<Object>();
    while (reader.hasMoreChildren()) {
      reader.moveDown();
      list.add(context.convertAnother(list, type));
      reader.moveUp();
    }
    return list;
  }
}
