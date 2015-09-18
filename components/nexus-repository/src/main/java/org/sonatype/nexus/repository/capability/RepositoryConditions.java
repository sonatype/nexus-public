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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.capability.Condition;
import org.sonatype.nexus.common.event.EventBus;
import org.sonatype.nexus.repository.manager.RepositoryManager;

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

  private final RepositoryManager repositoryManager;

  @Inject
  public RepositoryConditions(final EventBus eventBus,
                              final RepositoryManager repositoryManager)
  {
    this.eventBus = checkNotNull(eventBus);
    this.repositoryManager = checkNotNull(repositoryManager);
  }

  /**
   * Creates a new condition that is satisfied when a repository is in service.
   *
   * @param repositoryName getter for repository name (usually condition specific property)
   * @return created condition
   */
  public Condition repositoryIsOnline(final RepositoryName repositoryName) {
    return new RepositoryOnlineCondition(eventBus, repositoryManager, repositoryName);
  }

  /**
   * Creates a new condition that is satisfied when a repository exists.
   *
   * @param repositoryName getter for repository name (usually condition specific property)
   * @return created condition
   */
  public Condition repositoryExists(final RepositoryName repositoryName) {
    return new RepositoryExistsCondition(eventBus, repositoryManager, repositoryName);
  }

  public static interface RepositoryName
  {
    String get();
  }

}
