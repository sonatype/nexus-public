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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.property.PropertiesFile;
import org.sonatype.nexus.datastore.api.DataStoreConfiguration;

import com.google.common.collect.ImmutableSet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Maps.filterKeys;
import static com.google.common.collect.Maps.fromProperties;
import static java.lang.Integer.MIN_VALUE;
import static java.util.Arrays.stream;
import static org.sonatype.nexus.datastore.DataStoreConfigurationLocalSource.LOCAL;
import static org.sonatype.nexus.datastore.DataStoreConfigurationSourceSupport.VALID_NAME_PATTERN;
import static org.sonatype.nexus.datastore.DataStoreConfigurationSourceSupport.checkName;
import static org.sonatype.nexus.datastore.api.DataStoreManager.CONFIG_DATASTORE_NAME;
import static org.sonatype.nexus.datastore.api.DataStoreManager.CONTENT_DATASTORE_NAME;

/**
 * Source of {@link DataStoreConfiguration}s from property files in the fabric working directory.
 *
 * @since 3.19
 */
@Named(LOCAL)
@Priority(MIN_VALUE)
@Singleton
public class DataStoreConfigurationLocalSource
    extends ComponentSupport
    implements DataStoreConfigurationSource
{
  static final String LOCAL = "local";

  private static final String JDBC_TEMPLATE_URL = "jdbc:h2:file:${karaf.data}/db/${storeName}";

  private static final String NAME_KEY = "name";

  private static final String TYPE_KEY = "type";

  private static final Set<String> TOP_LEVEL_KEYS = ImmutableSet.of(NAME_KEY, TYPE_KEY);

  private static final String STORE_PROPERTIES_SUFFIX = "-store.properties";

  private static final String SYSTEM_PROPERTY_PREFIX = "nexus.datastore.";

  private final Map<String, PropertiesFile> propertiesByStoreName = new ConcurrentHashMap<>();

  private final File fabricWorkDirectory;

  @Inject
  public DataStoreConfigurationLocalSource(final ApplicationDirectories directories) {
    this.fabricWorkDirectory = checkNotNull(directories).getWorkDirectory("etc/fabric");
  }

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
    ImmutableSet.Builder<String> storeNames = ImmutableSet.builder();

    storeNames.add(CONFIG_DATASTORE_NAME);
    storeNames.add(CONTENT_DATASTORE_NAME);

    File[] files = fabricWorkDirectory.listFiles();
    if (files != null) {
      stream(files)
          .filter(File::canRead)
          .map(File::getName)
          .filter(name -> name.endsWith(STORE_PROPERTIES_SUFFIX))
          .map(name -> name.substring(0, name.indexOf(STORE_PROPERTIES_SUFFIX)))
          .filter(VALID_NAME_PATTERN.asPredicate())
          .forEach(storeNames::add);
    }

    return storeNames.build();
  }

  @Override
  public DataStoreConfiguration load(final String storeName) {

    checkName(storeName);

    PropertiesFile storeProperties = findStoreProperties(storeName);
    synchronized (storeProperties) {

      try {
        if (storeProperties.exists()) {
          storeProperties.load();

          // quick sanity check that the filename matches the content
          checkArgument(storeName.equals(storeProperties.getProperty(NAME_KEY)),
              "%s is for a different data store", storeProperties);
        }
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }

      String storeType = storeProperties.getProperty(TYPE_KEY);
      if (storeType == null) {
        storeType = "jdbc";
        storeProperties.setProperty(TYPE_KEY, storeType);
        storeProperties.setProperty("jdbcUrl", JDBC_TEMPLATE_URL);
      }

      DataStoreConfiguration configuration = new DataStoreConfiguration();

      configuration.setName(storeName);
      configuration.setType(storeType);
      configuration.setAttributes(filterKeys(fromProperties(storeProperties), not(in(TOP_LEVEL_KEYS))));
      configuration.setSource(LOCAL);

      return configuration;
    }
  }

  @Override
  public void save(final DataStoreConfiguration configuration) {
    String storeName = configuration.getName();

    checkName(storeName);

    PropertiesFile storeProperties = findStoreProperties(storeName);
    synchronized (storeProperties) {

      storeProperties.setProperty(NAME_KEY, storeName);
      storeProperties.setProperty(TYPE_KEY, configuration.getType());
      storeProperties.putAll(configuration.getAttributes());

      try {
        storeProperties.store();
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  @Override
  public void delete(final DataStoreConfiguration configuration) {
    String storeName = configuration.getName();

    PropertiesFile storeProperties = propertiesByStoreName.get(storeName);
    if (storeProperties != null) {
      synchronized (storeProperties) {
        storeProperties.getFile().delete();
      }
    }
  }

  /**
   * Checks the cache for the named properties file, initializing it from system properties if new.
   */
  private PropertiesFile findStoreProperties(final String storeName) {
    return propertiesByStoreName.computeIfAbsent(storeName, this::initializeStoreProperties);
  }

  /**
   * Initialize properties for the named store; note any defaults gleaned
   * from system properties may be overloaded by the actual properties file.
   */
  private PropertiesFile initializeStoreProperties(final String storeName) {
    PropertiesFile storeProperties = new PropertiesFile(
        new File(fabricWorkDirectory, storeName + STORE_PROPERTIES_SUFFIX));

    String systemPrefix = SYSTEM_PROPERTY_PREFIX + storeName + '.';
    System.getProperties().stringPropertyNames().stream()
        .filter(k -> k.startsWith(systemPrefix))
        .forEach(k -> storeProperties.setProperty(k.substring(systemPrefix.length()), System.getProperty(k)));

    return storeProperties;
  }
}
