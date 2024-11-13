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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationDAO;
import org.sonatype.nexus.repository.config.ConfigurationStore;
import org.sonatype.nexus.transaction.Transactional;

import com.google.common.collect.ImmutableList;
import org.apache.commons.collections.CollectionUtils;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

/**
 * MyBatis {@link ConfigurationStore} implementation.
 *
 * @since 3.21
 */
@Named("mybatis")
@Singleton
public class ConfigurationStoreImpl
    extends ConfigStoreSupport<ConfigurationDAO>
    implements ConfigurationStore
{
  @Inject
  public ConfigurationStoreImpl(final DataSessionSupplier sessionSupplier) {
    super(sessionSupplier);
  }

  @Override
  public Configuration newConfiguration() {
    return new ConfigurationData();
  }

  @Transactional
  @Override
  public List<Configuration> list() {
    return ImmutableList.copyOf(dao().browse());
  }

  @Transactional
  @Override
  public void create(final Configuration configuration) {
    postCommitEvent(() -> new ConfigurationCreatedEvent((ConfigurationData) configuration));
    dao().create((ConfigurationData) configuration);

    dao().read(configuration.getRepositoryId())
        .ifPresent(persisted -> configuration.setAttributes(persisted.getAttributes()));
  }

  @Transactional
  @Override
  public void update(final Configuration configuration) {
    postCommitEvent(() -> new ConfigurationUpdatedEvent((ConfigurationData) configuration));
    dao().update((ConfigurationData) configuration);

    dao().read(configuration.getRepositoryId())
        .ifPresent(persisted -> configuration.setAttributes(persisted.getAttributes()));
  }

  @Transactional
  @Override
  public void delete(final Configuration configuration) {
    postCommitEvent(() -> new ConfigurationDeletedEvent((ConfigurationData) configuration));
    dao().deleteByName(configuration.getRepositoryName());
  }

  @Transactional
  @Override
  public Collection<Configuration> readByNames(final Set<String> repositoryNames) {
    return ofNullable(repositoryNames)
        .filter(CollectionUtils::isNotEmpty)
        .map(names -> dao().readByNames(names))
        .orElse(emptyList());
  }

  @Transactional
  @Override
  public boolean exists(final String repositoryName) {
    return dao().readByName(repositoryName).isPresent();
  }

  @Transactional
  @Override
  public Collection<Configuration> readByRecipe(final String recipeName) {
    return dao().readByRecipe(recipeName);
  }
}
