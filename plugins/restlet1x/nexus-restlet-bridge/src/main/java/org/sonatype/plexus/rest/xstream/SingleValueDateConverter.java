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
package org.sonatype.plexus.rest.xstream;

import java.util.Date;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class SingleValueDateConverter
    implements Converter
{
  public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
    Date date = (Date) source;
    writer.setValue(String.valueOf(date.getTime()));
  }

  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
    return new Date(Long.parseLong(reader.getValue()));
  }

  public boolean canConvert(Class type) {
    return type.equals(Long.class)
        || type.equals(Integer.class)
        || type.equals(Date.class)
        || type.equals(String.class);
  }

}
