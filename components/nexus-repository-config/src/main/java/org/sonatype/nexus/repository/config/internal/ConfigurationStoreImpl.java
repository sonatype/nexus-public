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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationDeletedEvent;
import org.sonatype.nexus.repository.config.ConfigurationStore;
import org.sonatype.nexus.transaction.Transactional;

import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;

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
  private final EventManager eventManager;

  @Inject
  public ConfigurationStoreImpl(final DataSessionSupplier sessionSupplier, final EventManager eventManager) {
    super(sessionSupplier);
    this.eventManager = checkNotNull(eventManager);
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
    dao().create((ConfigurationData) configuration);
  }

  @Transactional
  @Override
  public void update(final Configuration configuration) {
    dao().update((ConfigurationData) configuration);
  }

  @Override
  public void delete(final Configuration configuration) {
    doDelete(configuration);
    postDeletedEvent(configuration);
  }

  @Transactional
  protected void doDelete(final Configuration configuration) {
    dao().deleteByName(configuration.getRepositoryName());
  }

  private void postDeletedEvent(final Configuration configuration) {
    // trigger deletion of browse nodes
    eventManager.post(new ConfigurationDeletedEvent()
    {
      @Override
      public boolean isLocal() {
        return true;
      }

      @Override
      public String getRepositoryName() {
        return configuration.getRepositoryName();
      }

      @Override
      public Configuration getConfiguration() {
        return configuration;
      }
    });
  }
}
