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
package org.sonatype.nexus.rest.jackson2.internal;

import jakarta.inject.Provider;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.collect.json.NestedAttributesMapDeserializer;
import org.sonatype.nexus.common.collect.json.NestedAttributesMapSerializer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jackson {@link ObjectMapper} provider for use with Siesta.
 *
 * @since 3.0
 */
@Component
@Qualifier("siesta")
public class ObjectMapperProvider
    implements Provider<ObjectMapper>, FactoryBean<ObjectMapper>
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final ObjectMapper mapper;

  public ObjectMapperProvider() {
    this.mapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.registerModule(new JavaTimeModule());
    mapper.registerModule(new JodaModule());
    mapper.registerModule(new SimpleModule()
        .addSerializer(NestedAttributesMap.class, new NestedAttributesMapSerializer())
        .addDeserializer(NestedAttributesMap.class, new NestedAttributesMapDeserializer()));
  }

  @Override
  public ObjectMapper get() {
    return mapper;
  }

  @Override
  public ObjectMapper getObject() throws Exception {
    return mapper;
  }

  @Override
  public Class<?> getObjectType() {
    return ObjectMapper.class;
  }
}
