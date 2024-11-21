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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.datastore.api.DataStoreConfiguration;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.stream;
import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.util.Comparator.comparingInt;

/**
 * Manages {@link DataStoreConfiguration}s supplied by one or more sources.
 *
 * @since 3.19
 */
@Named
@Singleton
public class DataStoreConfigurationManager
    extends ComponentSupport
{
  private final Map<String, DataStoreConfigurationSource> configurationSources;

  @Inject
  public DataStoreConfigurationManager(final Map<String, DataStoreConfigurationSource> configurationSources) {
    this.configurationSources = checkNotNull(configurationSources);
  }

  /**
   * Loads {@link DataStoreConfiguration}s from all enabled sources.
   */
  public Iterable<DataStoreConfiguration> load() {
    Set<String> configuredStores = new TreeSet<>(CASE_INSENSITIVE_ORDER);
    // only attempt to load a named store once from the first store that has it
    // (if the first attempt fails then that store is considered not available)
    return configurationSources.values()
        .stream()
        .filter(DataStoreConfigurationSource::isEnabled)
        .sorted(comparingInt(this::getPriority).reversed())
        .flatMap(source -> stream(source.browseStoreNames())
            .filter(configuredStores::add)
            .map(configLoader(source)))
        .filter(Objects::nonNull)
        .collect(toImmutableList());
  }

  private int getPriority(final DataStoreConfigurationSource configSource) {
    if (configSource.getClass().isAnnotationPresent(Priority.class)) {
      Priority priority = configSource.getClass().getAnnotation(Priority.class);
      return priority.value();
    }
    else {
      log.warn("Loaded config source {} without priority, assuming last", configSource.getName());
      return Integer.MIN_VALUE;
    }
  }

  /**
   * Saves the given configuration back to the source that originally loaded it.
   */
  public void save(final DataStoreConfiguration configuration) {
    findModifiableSource(configuration).ifPresent(source -> source.save(configuration));
  }

  /**
   * Deletes the given configuration from the source that originally loaded it.
   */
  public void delete(final DataStoreConfiguration configuration) {
    findModifiableSource(configuration).ifPresent(source -> source.delete(configuration));
  }

  /**
   * @return loader that returns {@code null} if a configuration cannot be loaded
   */
  private Function<String, DataStoreConfiguration> configLoader(final DataStoreConfigurationSource source) {
    return storeName -> {
      try {
        return source.load(storeName);
      }
      catch (RuntimeException e) {
        log.warn("Problem reading configuration of data store {} from {}", storeName, source, e);
        return null;
      }
    };
  }

  /**
   * Attempts to find the modifiable source that originally loaded the given configuration.
   */
  private Optional<DataStoreConfigurationSource> findModifiableSource(final DataStoreConfiguration configuration) {
    DataStoreConfigurationSource source = configurationSources.get(configuration.getSource());

    checkArgument(source != null, "%s refers to a missing source", configuration);

    return Optional.of(source)
        .filter(DataStoreConfigurationSource::isModifiable);
  }
}
