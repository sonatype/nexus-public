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
package org.sonatype.nexus.repository.capability;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.sonatype.nexus.capability.CapabilityContext;
import org.sonatype.nexus.capability.CapabilityContextAware;
import org.sonatype.nexus.capability.CapabilityEvent;
import org.sonatype.nexus.capability.CapabilityIdentity;
import org.sonatype.nexus.capability.condition.ConditionSupport;
import org.sonatype.nexus.common.event.EventBus;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.capability.RepositoryConditions.RepositoryName;
import org.sonatype.nexus.repository.manager.RepositoryCreatedEvent;
import org.sonatype.nexus.repository.manager.RepositoryLoadedEvent;
import org.sonatype.nexus.repository.manager.RepositoryManager;

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

  private final RepositoryManager repositoryManager;

  private final RepositoryName repositoryName;

  private final ReentrantReadWriteLock bindLock;

  private CapabilityIdentity capabilityIdentity;

  private String repositoryBeforeLastUpdate;

  public RepositoryConditionSupport(final EventBus eventBus,
                                    final RepositoryManager repositoryManager,
                                    final RepositoryName repositoryName)
  {
    super(eventBus, false);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.repositoryName = checkNotNull(repositoryName);
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
      for (final Repository repository : repositoryManager.browse()) {
        handle(new RepositoryCreatedEvent(repository));
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

  public abstract void handle(final RepositoryCreatedEvent event);

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
  public void handle(final RepositoryLoadedEvent event) {
    handle(new RepositoryCreatedEvent(event.getRepository()));
  }

  @AllowConcurrentEvents
  @Subscribe
  public void handle(final CapabilityEvent.BeforeUpdate event) {
    if (event.getReference().context().id().equals(capabilityIdentity)) {
      repositoryBeforeLastUpdate = getRepositoryName();
    }
  }

  @AllowConcurrentEvents
  @Subscribe
  public void handle(final CapabilityEvent.AfterUpdate event) {
    if (event.getReference().context().id().equals(capabilityIdentity)) {
      if (!sameRepositoryAs(repositoryBeforeLastUpdate)) {
        try {
          bindLock.writeLock().lock();
          for (final Repository repository : repositoryManager.browse()) {
            handle(new RepositoryCreatedEvent(repository));
          }
        }
        finally {
          bindLock.writeLock().unlock();
        }
      }
    }
  }

  /**
   * Checks that condition is about the passed in repository name.
   *
   * @param repositoryName to check
   * @return true, if condition repository matches the specified repository name
   */
  protected boolean sameRepositoryAs(final String repositoryName) {
    return repositoryName != null && repositoryName.equals(getRepositoryName());
  }

  protected String getRepositoryName() {
    return repositoryName.get();
  }

}
