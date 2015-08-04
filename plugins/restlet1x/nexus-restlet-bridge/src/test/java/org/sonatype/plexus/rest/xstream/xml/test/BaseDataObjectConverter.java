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
package org.sonatype.plexus.rest.xstream.xml.test;

import org.sonatype.plexus.rest.xstream.LookAheadStreamReader;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.AbstractReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.mapper.Mapper;

public class BaseDataObjectConverter
    extends AbstractReflectionConverter
{

  public BaseDataObjectConverter(Mapper mapper, ReflectionProvider reflectionProvider) {
    super(mapper, reflectionProvider);
  }

  public boolean canConvert(Class type) {
    return BaseDataObject.class.equals(type);
  }

  protected Object instantiateNewInstance(HierarchicalStreamReader reader, UnmarshallingContext context) {
    BaseDataObject data = null;

    reader = reader.underlyingReader();
    LookAheadStreamReader xppReader = null;

    if (reader instanceof LookAheadStreamReader) {
      xppReader = (LookAheadStreamReader) reader;
    }
    else {
      throw new RuntimeException("reader: " + reader.getClass());
    }

    String type = xppReader.getFieldValue("type");

    if ("type-one".equals(type)) {
      data = new DataObject1();
    }
    else if ("type-two".equals(type)) {
      data = new DataObject2();
    }

    return data;
  }

}
