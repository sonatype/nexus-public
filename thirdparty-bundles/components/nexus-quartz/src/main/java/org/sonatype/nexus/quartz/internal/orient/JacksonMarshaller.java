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
package org.sonatype.nexus.quartz.internal.orient;

import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.orientechnologies.orient.core.metadata.schema.OType;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Jackson {@link Marshaller} using {@link ObjectMapper}.
 *
 * Needs configuration to cope with objects that do not have no-args constructors, but marshals to Map structure.
 *
 * @since 3.0
 */
public class JacksonMarshaller
  implements Marshaller
{
  /**
   * Type-reference for marshalled data.
   */
  private static final TypeReference<Map<String, Object>> MAP_STRING_OBJECT_TYPE =
      new TypeReference<Map<String, Object>>() {};

  private final ObjectMapper objectMapper;

  private final InstanceCreator instanceCreator;

  public JacksonMarshaller(final ObjectMapper objectMapper) {
    this.objectMapper = checkNotNull(objectMapper);
    this.instanceCreator = new InstanceCreator();
  }

  @Override
  public OType getType() {
    return OType.EMBEDDEDMAP;
  }

  @Override
  public Object marshall(final Object value) throws Exception {
    return objectMapper.convertValue(value, MAP_STRING_OBJECT_TYPE);
  }

  @Override
  public <T> T unmarshall(final Object marshalled, final Class<T> type) throws Exception {
    checkNotNull(marshalled);
    checkState(marshalled instanceof Map, "Marshalled data must be a Map; found: %s", marshalled.getClass());

    // FIXME: This allows the top-level object to be created, but if any children objects of this are missing
    // FIXME: ... no-arg CTOR then Jackson will fail to construct them.
    // FIXME: Is there any way to configure the basic instance creation for Jackson?
    Object value = instanceCreator.newInstance(type);

    // performs same basic logic as ObjectMapper.convertValue(Object, Class) helper
    ObjectReader reader = objectMapper.readerForUpdating(value);
    TokenBuffer buff = new TokenBuffer(objectMapper, false);
    objectMapper.writeValue(buff, marshalled);
    reader.readValue(buff.asParser());

    return type.cast(value);
  }
}
