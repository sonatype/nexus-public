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
package org.sonatype.nexus.content.testsupport.rest;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import org.sonatype.nexus.repository.rest.api.ComponentXO;
import org.sonatype.nexus.repository.rest.api.ComponentXODeserializer;
import org.sonatype.nexus.repository.rest.api.ComponentXODeserializerExtension;
import org.sonatype.nexus.repository.rest.api.ComponentXOFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Jackson {@link ContextResolver} to customize the {@link ObjectMapper} for the rest clients such as
 * SearchClient within the testsuite.
 *
 * @since 3.8
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
@Named
public class TestSuiteObjectMapperResolver
    implements ContextResolver<ObjectMapper>
{
  private final ObjectMapper objectMapper;

  @Inject
  public TestSuiteObjectMapperResolver(
      final ComponentXOFactory componentXOFactory,
      final Set<ComponentXODeserializerExtension> componentXODeserializerExtensions)
  {
    this.objectMapper = new ObjectMapper();

    // the json will have extra fields that the ComponentXO doesn't have, we don't want to fail on these
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    this.objectMapper.registerModule(new SimpleModule()
        // add the deserializer for the ComponentXO class
        .addDeserializer(ComponentXO.class, new ComponentXODeserializer(componentXOFactory, objectMapper,
            componentXODeserializerExtensions)));
  }

  @Override
  public ObjectMapper getContext(final Class<?> type) {
    return objectMapper;
  }
}
