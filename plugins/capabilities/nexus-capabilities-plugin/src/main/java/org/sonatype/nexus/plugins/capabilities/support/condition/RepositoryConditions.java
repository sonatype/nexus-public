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
package org.sonatype.nexus.plugins.capabilities.support.condition;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.plugins.capabilities.Condition;
import org.sonatype.nexus.plugins.capabilities.internal.condition.RepositoryExistsCondition;
import org.sonatype.nexus.plugins.capabilities.internal.condition.RepositoryLocalStatusCondition;
import org.sonatype.nexus.plugins.capabilities.internal.condition.RepositoryProxyModeCondition;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.LocalStatus;
import org.sonatype.nexus.proxy.repository.ProxyMode;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Factory of {@link Condition}s related to repositories.
 *
 * @since capabilities 2.0
 */
@Named
@Singleton
public class RepositoryConditions
{

  private final EventBus eventBus;

  private final RepositoryRegistry repositoryRegistry;

  @Inject
  public RepositoryConditions(final EventBus eventBus,
                              final RepositoryRegistry repositoryRegistry)
  {
    this.eventBus = checkNotNull(eventBus);
    this.repositoryRegistry = checkNotNull(repositoryRegistry);
  }

  /**
   * Creates a new condition that is satisfied when a repository is in service.
   *
   * @param repositoryId getter for repository id (usually condition specific property)
   * @return created condition
   */
  public Condition repositoryIsInService(final RepositoryId repositoryId) {
    return new RepositoryLocalStatusCondition(eventBus, repositoryRegistry, LocalStatus.IN_SERVICE, repositoryId);
  }

  /**
   * Creates a new condition that is satisfied when a proxy repository proxy mode allows proxy-ing (ALLOW).
   *
   * @param repositoryId getter for repository id (usually condition specific property)
   * @return created condition
   */
  public Condition repositoryIsNotBlocked(final RepositoryId repositoryId) {
    return new RepositoryProxyModeCondition(eventBus, repositoryRegistry, ProxyMode.ALLOW, repositoryId);
  }

  /**
   * Creates a new condition that is satisfied when a repository exists.
   *
   * @param repositoryId getter for repository id (usually condition specific property)
   * @return created condition
   */
  public Condition repositoryExists(final RepositoryId repositoryId) {
    return new RepositoryExistsCondition(eventBus, repositoryRegistry, repositoryId);
  }

  public static interface RepositoryId
  {

    String get();

  }

}
