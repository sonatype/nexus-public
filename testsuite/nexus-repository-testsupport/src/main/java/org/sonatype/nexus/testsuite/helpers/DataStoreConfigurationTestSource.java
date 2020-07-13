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
package org.sonatype.nexus.testsuite.helpers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.annotation.Priority;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.datastore.DataStoreConfigurationSource;
import org.sonatype.nexus.datastore.DataStoreConfigurationSourceSupport;
import org.sonatype.nexus.datastore.api.DataStoreConfiguration;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.sonatype.nexus.pax.exam.NexusPaxExamSupport.TEST_JDBC_URL_PROPERTY;
import static org.sonatype.nexus.testsuite.helpers.DataStoreConfigurationTestSource.TEST;
import static org.sonatype.nexus.datastore.api.DataStoreManager.CONFIG_DATASTORE_NAME;
import static org.sonatype.nexus.datastore.api.DataStoreManager.CONTENT_DATASTORE_NAME;

/**
 * Programmatic source of {@link DataStoreConfiguration}s
 *
 * @since 3.25
 */
@Named(TEST)
@Priority(MAX_VALUE)
@Singleton
public class DataStoreConfigurationTestSource
    extends DataStoreConfigurationSourceSupport
{
  private static final String DB_USER = "nxrmUser";

  private static final String DB_PASSWORD = "nxrmPassword";

  static final String TEST = "test";

  private final Optional<String> jdbcUrl;

  private final Function<String, DataStoreConfiguration> configBuilder;

  private final ConcurrentHashMap<String, DataStoreConfiguration> configurations = new ConcurrentHashMap<>();

  public DataStoreConfigurationTestSource() {
    this.jdbcUrl = ofNullable(System.getProperty(TEST_JDBC_URL_PROPERTY, null));

    this.configBuilder = jdbcUrl
        .map(url -> (Function<String, DataStoreConfiguration>) name -> buildConfigFor(name, url))
        .orElse(name -> { throw new IllegalStateException(format("Tried to add a store %s without a jdbcUrl present", name));});

    jdbcUrl.ifPresent(url -> {
      load(CONFIG_DATASTORE_NAME);
      load(CONTENT_DATASTORE_NAME);
    });
  }

  @Override
  public boolean isEnabled() {
    return jdbcUrl.isPresent();
  }

  private DataStoreConfiguration buildConfigFor(final String storeName, final String jdbcUrl) {
    Map<String, String> attributes = new HashMap<>();
    attributes.put("username", DB_USER);
    attributes.put("password", DB_PASSWORD);
    attributes.put("jdbcUrl", jdbcUrl + storeName);

    DataStoreConfiguration configuration = new DataStoreConfiguration();
    configuration.setName(storeName);
    configuration.setType("jdbc");
    configuration.setSource(TEST);
    configuration.setAttributes(attributes);
    return configuration;
  }

  @Override
  public String getName() {
    return "Test";
  }

  @Override
  public boolean isModifiable() {
    return true;
  }

  @Override
  public Iterable<String> browseStoreNames() {
    return Collections.list(configurations.keys());
  }

  @Override
  public DataStoreConfiguration load(final String storeName) {
    checkName(storeName);
    return configurations.computeIfAbsent(storeName, configBuilder);
  }

  @Override
  public void save(final DataStoreConfiguration configuration) {
    String storeName = configuration.getName();
    checkName(storeName);
    configuration.setSource(TEST);
    configurations.put(storeName, configuration);
  }

  @Override
  public void delete(final DataStoreConfiguration configuration) {
    String storeName = configuration.getName();
    configurations.remove(storeName);
  }
}
