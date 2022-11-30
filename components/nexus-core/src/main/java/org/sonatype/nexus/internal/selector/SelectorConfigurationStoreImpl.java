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
package org.sonatype.nexus.internal.selector;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.distributed.event.service.api.EventType;
import org.sonatype.nexus.distributed.event.service.api.common.SelectorConfigurationChangedEvent;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorConfigurationStore;
import org.sonatype.nexus.transaction.Transactional;

import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * MyBatis {@link SelectorConfigurationStore} implementation.
 *
 * @since 3.21
 */
@Named("mybatis")
@Singleton
public class SelectorConfigurationStoreImpl
    extends ConfigStoreSupport<SelectorConfigurationDAO>
    implements SelectorConfigurationStore
{
  private final EventManager eventManager;

  @Inject
  public SelectorConfigurationStoreImpl(final DataSessionSupplier sessionSupplier, final EventManager eventManager) {
    super(sessionSupplier);
    this.eventManager = checkNotNull(eventManager);
  }

  @Override
  public SelectorConfiguration newSelectorConfiguration() {
    return new SelectorConfigurationData();
  }

  @Transactional
  @Override
  public List<SelectorConfiguration> browse() {
    return ImmutableList.copyOf(dao().browse());
  }

  @Override
  public void create(final SelectorConfiguration configuration) {
    doCreate(configuration);
    postEvent(configuration);
    postDesEvent(EventType.CREATED);
  }

  @Transactional
  protected void doCreate(final SelectorConfiguration configuration) {
    dao().create((SelectorConfigurationData) configuration);
  }

  @Override
  public SelectorConfiguration read(final EntityId entityId) {
    throw new UnsupportedOperationException("Use getByName instead");
  }

  @Override
  public void update(final SelectorConfiguration configuration) {
    if (doUpdate(configuration)) {
      postEvent(configuration);
      postDesEvent(EventType.UPDATED);
    }
  }

  @Transactional
  protected boolean doUpdate(final SelectorConfiguration configuration) {
    return dao().update((SelectorConfigurationData) configuration);
  }

  @Override
  public void delete(final SelectorConfiguration configuration) {
    if (doDelete(configuration)) {
      postEvent(configuration);
      postDesEvent(EventType.DELETED);
    }
  }

  @Transactional
  protected boolean doDelete(final SelectorConfiguration configuration) {
    return dao().delete(configuration.getName());
  }

  @Transactional
  @Override
  public SelectorConfiguration getByName(final String name) {
    return dao().read(name).orElse(null);
  }

  private void postEvent(final SelectorConfiguration configuration) {
    // trigger invalidation of SelectorManagerImpl caches
    eventManager.post(new SelectorConfigurationEvent()
    {
      @Override
      public boolean isLocal() {
        return true;
      }

      @Override
      public SelectorConfiguration getSelectorConfiguration() {
        return configuration;
      }
    });
  }

  private void postDesEvent(final EventType eventType) {
    eventManager.post(new SelectorConfigurationChangedEvent(eventType));
  }
}
