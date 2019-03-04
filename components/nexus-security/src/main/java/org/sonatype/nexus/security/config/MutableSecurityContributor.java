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
package org.sonatype.nexus.security.config;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.goodies.common.Locks;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.security.internal.SecurityContributionChangedEvent;

import com.google.common.base.Preconditions;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Mutable {@link SecurityContributor}.
 *
 * @since 3.1
 */
public class MutableSecurityContributor
    extends ComponentSupport
    implements SecurityContributor
{
  private final SecurityConfiguration model = new MemorySecurityConfiguration();

  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

  private boolean initialized;

  @Nullable
  private EventManager eventManager;

  @Nullable
  private SecurityConfigurationManager configurationManager;

  @Inject
  protected void initialize(final EventManager eventManager, final SecurityConfigurationManager configurationManager) {
    checkState(!initialized, "already initialized");
    this.eventManager = Preconditions.checkNotNull(eventManager);
    this.configurationManager = Preconditions.checkNotNull(configurationManager);
    initial(model);
    initialized = true;
  }

  /**
   * Initial security contribution.
   *
   * @since 3.1
   */
  protected void initial(final SecurityConfiguration model) {
    // defaults to no initial contribution
  }

  @Override
  public SecurityConfiguration getContribution() {
    checkState(initialized, "not initialized");
    Lock lock = Locks.read(readWriteLock);
    try {
      return model;
    }
    finally {
      lock.unlock();
    }
  }

  /**
   * Exposes ability to mutate {@link SecurityConfiguration}.
   */
  public interface Mutator
  {
    void apply(SecurityConfiguration model, SecurityConfigurationManager configurationManager);
  }

  public void apply(final Mutator mutator) {
    checkState(initialized, "not initialized");
    checkNotNull(mutator);

    Lock lock = Locks.write(readWriteLock);
    try {
      mutator.apply(model, configurationManager);
    }
    finally {
      lock.unlock();
    }

    eventManager.post(new SecurityContributionChangedEvent());
  }

  /**
   * @since 3.next
   */
  protected void maybeAddPrivilege(final SecurityConfiguration model, final CPrivilege privilege) {
    if (model.getPrivilege(privilege.getId()) == null) {
      model.addPrivilege(privilege);
    }
  }
}
