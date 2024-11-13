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
package org.sonatype.nexus.datastore.mybatis;

import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.TypeHandler;

/**
 * Abstract {@link TypeHandler} that maps types directly to JSON without any additional behaviour/processing.
 *
 * @since 3.21
 */
public abstract class AbstractRawJsonTypeHandler<T>
    extends AbstractJsonTypeHandler<T>
{
  private static final ObjectMapper RAW_OBJECT_MAPPER = new ObjectMapper();

  @Override
  protected ObjectMapper buildObjectMapper(final Supplier<ObjectMapper> unused) {
    return RAW_OBJECT_MAPPER;
  }

}
