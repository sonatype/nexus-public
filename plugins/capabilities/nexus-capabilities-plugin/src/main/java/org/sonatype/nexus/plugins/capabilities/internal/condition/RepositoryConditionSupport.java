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
package org.sonatype.nexus.plugins.capabilities.internal.condition;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.sonatype.nexus.plugins.capabilities.CapabilityContext;
import org.sonatype.nexus.plugins.capabilities.CapabilityContextAware;
import org.sonatype.nexus.plugins.capabilities.CapabilityEvent;
import org.sonatype.nexus.plugins.capabilities.CapabilityIdentity;
import org.sonatype.nexus.plugins.capabilities.support.condition.ConditionSupport;
import org.sonatype.nexus.plugins.capabilities.support.condition.RepositoryConditions;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventAdd;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Support class for repository conditions.
 *
 * @since capabilities 2.0
 */
public abstract class RepositoryConditionSupport
    extends ConditionSupport
    implements CapabilityContextAware
{

  private final RepositoryRegistry repositoryRegistry;

  private final RepositoryConditions.RepositoryId repositoryId;

  private final ReentrantReadWriteLock bindLock;

  private CapabilityIdentity capabilityIdentity;

  private String repositoryBeforeLastUpdate;

  public RepositoryConditionSupport(final EventBus eventBus,
                                    final RepositoryRegistry repositoryRegistry,
                                    final RepositoryConditions.RepositoryId repositoryId)
  {
    super(eventBus, false);
    this.repositoryRegistry = checkNotNull(repositoryRegistry);
    this.repositoryId = checkNotNull(repositoryId);
    bindLock = new ReentrantReadWriteLock();
  }

  @Override
  public RepositoryConditionSupport setContext(final CapabilityContext context) {
    checkState(!isActive(), "Cannot contextualize when already bounded");
    checkState(capabilityIdentity == null, "Already contextualized with id '" + capabilityIdentity + "'");
    capabilityIdentity = context.id();

    return this;
  }

  @Override
  protected void doBind() {
    try {
      bindLock.writeLock().lock();
      for (final Repository repository : repositoryRegistry.getRepositories()) {
        handle(new RepositoryRegistryEventAdd(repositoryRegistry, repository));
      }
    }
    finally {
      bindLock.writeLock().unlock();
    }
    getEventBus().register(this);
  }

  @Override
  public void doRelease() {
    getEventBus().unregister(this);
  }

  public abstract void handle(final RepositoryRegistryEventAdd event);

  @Override
  protected void setSatisfied(final boolean satisfied) {
    try {
      bindLock.readLock().lock();
      super.setSatisfied(satisfied);
    }
    finally {
      bindLock.readLock().unlock();
    }
  }

  @AllowConcurrentEvents
  @Subscribe
  public void handle(final CapabilityEvent.BeforeUpdate event) {
    if (event.getReference().context().id().equals(capabilityIdentity)) {
      repositoryBeforeLastUpdate = getRepositoryId();
    }
  }

  @AllowConcurrentEvents
  @Subscribe
  public void handle(final CapabilityEvent.AfterUpdate event) {
    if (event.getReference().context().id().equals(capabilityIdentity)) {
      if (!sameRepositoryAs(repositoryBeforeLastUpdate)) {
        try {
          bindLock.writeLock().lock();
          for (final Repository repository : repositoryRegistry.getRepositories()) {
            handle(new RepositoryRegistryEventAdd(repositoryRegistry, repository));
          }
        }
        finally {
          bindLock.writeLock().unlock();
        }
      }
    }
  }

  /**
   * Checks that condition is about the passed in repository id.
   *
   * @param repositoryId to check
   * @return true, if condition repository matches the specified repository id
   */
  protected boolean sameRepositoryAs(final String repositoryId) {
    return repositoryId != null && repositoryId.equals(getRepositoryId());
  }

  protected String getRepositoryId() {
    return repositoryId.get();
  }

}
