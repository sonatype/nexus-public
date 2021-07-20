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
package org.sonatype.nexus.datastore;

import javax.annotation.Priority;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.datastore.api.DataStoreConfiguration;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Integer.MIN_VALUE;
import static org.sonatype.nexus.datastore.DataStoreConfigurationDefaultSource.LOCAL;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

/**
 * Source of {@link DataStoreConfiguration}s for the default datastore only
 *
 * @since 3.19
 */
@Named(LOCAL)
@Priority(MIN_VALUE)
@Singleton
public class DataStoreConfigurationDefaultSource
    extends ComponentSupport
    implements DataStoreConfigurationSource
{
  static final String LOCAL = "local";

  private static final String JDBC_TEMPLATE_URL = "jdbc:h2:file:${karaf.data}/db/" + DEFAULT_DATASTORE_NAME;

  private static final String JDBC = "jdbc";

  private static final String JDBC_URL = "jdbcUrl";

  @Override
  public String getName() {
    return "Local";
  }

  @Override
  public boolean isModifiable() {
    return true;
  }

  @Override
  public Iterable<String> browseStoreNames() {
    return ImmutableSet.<String>builder().add(DEFAULT_DATASTORE_NAME).build();
  }

  @Override
  public DataStoreConfiguration load(final String storeName) {
    checkArgument(DEFAULT_DATASTORE_NAME.equalsIgnoreCase(storeName),
        "%s is not valid, %s is the only valid data store name", storeName, DEFAULT_DATASTORE_NAME);
    DataStoreConfiguration configuration = new DataStoreConfiguration();
    configuration.setName(DEFAULT_DATASTORE_NAME);
    configuration.setType(JDBC);
    configuration.setSource(LOCAL);
    configuration.setAttributes(ImmutableMap.of(JDBC_URL, JDBC_TEMPLATE_URL));

    log.info("Loaded '{}' data store configuration defaults (Embedded H2)", storeName);

    return configuration;
  }
}
