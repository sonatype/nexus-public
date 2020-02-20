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
package org.sonatype.nexus.repository.config.internal;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.config.ConfigurationObjectMapperCustomizer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides {@link ObjectMapper} for repository configuration.
 *
 * @since 3.0
 */
@Named(ConfigurationObjectMapperProvider.NAME)
@Singleton
public class ConfigurationObjectMapperProvider
    extends ComponentSupport
    implements Provider<ObjectMapper>
{
  public static final String NAME = "repository-configuration";

  private final Map<String, ConfigurationObjectMapperCustomizer> customizers;

  @Inject
  public ConfigurationObjectMapperProvider(final Map<String, ConfigurationObjectMapperCustomizer> customizers)
  {
    this.customizers = checkNotNull(customizers);
  }

  @Override
  public ObjectMapper get() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    for (ConfigurationObjectMapperCustomizer customizer : customizers.values()) {
      customizer.customize(mapper);
    }
    // TODO: ISO-8601, joda
    // TODO: null handling
    return mapper;
  }
}
