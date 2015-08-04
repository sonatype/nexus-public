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
package org.sonatype.nexus.rest.repositories;

import org.sonatype.nexus.rest.model.RepositoryResourceResponse;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.reflection.AbstractReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import org.codehaus.plexus.util.StringUtils;

public class RepositoryResourceResponseConverter
    extends AbstractReflectionConverter
{

  public RepositoryResourceResponseConverter(Mapper mapper, ReflectionProvider reflectionProvider) {
    super(mapper, reflectionProvider);
  }

  public boolean canConvert(Class type) {
    return RepositoryResourceResponse.class.equals(type);
  }

  public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {

    // removes the class="class.name" attribute
    RepositoryResourceResponse top = (RepositoryResourceResponse) value;
    if (top.getData() != null) {
      // make sure the data's repoType field is valid, or we wont be able to deserialize it on the other side
      if (StringUtils.isEmpty(top.getData().getRepoType())) {
        throw new ConversionException("Missing value for field: RepositoryResourceResponse.data.repoType.");
      }

      writer.startNode("data");
      context.convertAnother(top.getData());
      writer.endNode();
    }

  }
}
