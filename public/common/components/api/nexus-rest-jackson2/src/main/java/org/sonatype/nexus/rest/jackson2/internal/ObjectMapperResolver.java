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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import org.sonatype.nexus.rest.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Jackson {@link ObjectMapper} resolver.
 *
 * This will use the mapper bound to the name "siesta".
 *
 * @since 3.0
 * @see ObjectMapperProvider
 */
@org.springframework.stereotype.Component
@Singleton
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class ObjectMapperResolver
    implements ContextResolver<ObjectMapper>, Component
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final jakarta.inject.Provider<ObjectMapper> mapperProvider;

  @Inject
  public ObjectMapperResolver(@Qualifier("siesta") final jakarta.inject.Provider<ObjectMapper> mapperProvider) {
    this.mapperProvider = checkNotNull(mapperProvider);
  }

  @Override
  public ObjectMapper getContext(final Class<?> type) {
    return mapperProvider.get();
  }
}
