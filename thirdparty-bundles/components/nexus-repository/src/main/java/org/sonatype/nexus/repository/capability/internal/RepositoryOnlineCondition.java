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
package org.sonatype.nexus.repository.capability.internal;

import java.util.function.Supplier;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.manager.RepositoryCreatedEvent;
import org.sonatype.nexus.repository.manager.RepositoryDeletedEvent;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.manager.RepositoryUpdatedEvent;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

/**
 * A condition that is satisfied when a repository is online.
 *
 * @since 3.0
 */
public class RepositoryOnlineCondition
    extends RepositoryConditionSupport
{

  public RepositoryOnlineCondition(final EventManager eventManager,
                                   final RepositoryManager repositoryManager,
                                   final Supplier<String> repositoryName)
  {
    super(eventManager, repositoryManager, repositoryName);
  }

  @Override
  @AllowConcurrentEvents
  @Subscribe
  public void handle(final RepositoryCreatedEvent event) {
    if (sameRepositoryAs(event.getRepository().getName())) {
      setSatisfied(event.getRepository().getConfiguration().isOnline());
    }
  }

  @AllowConcurrentEvents
  @Subscribe
  public void handle(final RepositoryUpdatedEvent event) {
    if (sameRepositoryAs(event.getRepository().getName())) {
      setSatisfied(event.getRepository().getConfiguration().isOnline());
    }
  }

  @AllowConcurrentEvents
  @Subscribe
  public void handle(final RepositoryDeletedEvent event) {
    if (sameRepositoryAs(event.getRepository().getName())) {
      setSatisfied(false);
    }
  }

  @Override
  public String toString() {
    try {
      final String repositoryName = getRepositoryName();
      return String.format("Repository '%s' is online", repositoryName);
    }
    catch (Exception ignore) {
      return "Repository '(could not be evaluated)' is online";
    }
  }

  @Override
  public String explainSatisfied() {
    try {
      final String repositoryName = getRepositoryName();
      return String.format("Repository '%s' is online", repositoryName);
    }
    catch (Exception ignore) {
      return "Repository '(could not be evaluated)' is online";
    }
  }

  @Override
  public String explainUnsatisfied() {
    try {
      final String repositoryName = getRepositoryName();
      return String.format("Repository '%s' is offline", repositoryName);
    }
    catch (Exception ignore) {
      return "Repository '(could not be evaluated)' is offline";
    }
  }

}
