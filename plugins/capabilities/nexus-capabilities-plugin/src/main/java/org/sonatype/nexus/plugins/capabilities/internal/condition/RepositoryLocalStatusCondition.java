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

import org.sonatype.nexus.plugins.capabilities.support.condition.RepositoryConditions;
import org.sonatype.nexus.proxy.events.RepositoryConfigurationUpdatedEvent;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventAdd;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventRemove;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.LocalStatus;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A condition that is satisfied when a repository has a specified local status.
 *
 * @since capabilities 2.0
 */
public class RepositoryLocalStatusCondition
    extends RepositoryConditionSupport
{

  private final LocalStatus localStatus;

  public RepositoryLocalStatusCondition(final EventBus eventBus,
                                        final RepositoryRegistry repositoryRegistry,
                                        final LocalStatus localStatus,
                                        final RepositoryConditions.RepositoryId repositoryId)
  {
    super(eventBus, repositoryRegistry, repositoryId);
    this.localStatus = checkNotNull(localStatus);
  }

  @Override
  @AllowConcurrentEvents
  @Subscribe
  public void handle(final RepositoryRegistryEventAdd event) {
    if (sameRepositoryAs(event.getRepository().getId())) {
      setSatisfied(localStatus.equals(event.getRepository().getLocalStatus()));
    }
  }

  @AllowConcurrentEvents
  @Subscribe
  public void handle(final RepositoryConfigurationUpdatedEvent event) {
    if (sameRepositoryAs(event.getRepository().getId())) {
      setSatisfied(localStatus.equals(event.getRepository().getLocalStatus()));
    }
  }

  @AllowConcurrentEvents
  @Subscribe
  public void handle(final RepositoryRegistryEventRemove event) {
    if (sameRepositoryAs(event.getRepository().getId())) {
      setSatisfied(false);
    }
  }

  @Override
  public String toString() {
    try {
      final String id = getRepositoryId();
      return String.format("Repository '%s' is %s", id, localStatus.toString());
    }
    catch (Exception ignore) {
      return String.format("Repository '(could not be evaluated)' is %s", localStatus.toString());
    }
  }

  @Override
  public String explainSatisfied() {
    final String state = localStatus.equals(LocalStatus.OUT_OF_SERVICE) ? "out of" : "in";
    try {
      final String id = getRepositoryId();
      return String.format("Repository '%s' is %s service", id, state);
    }
    catch (Exception ignore) {
      return String.format("Repository '(could not be evaluated)' is %s service", state);
    }
  }

  @Override
  public String explainUnsatisfied() {
    final String state = localStatus.equals(LocalStatus.OUT_OF_SERVICE) ? "in" : "out of";
    try {
      final String id = getRepositoryId();
      return String.format("Repository '%s' is %s service", id, state);
    }
    catch (Exception ignore) {
      return String.format("Repository '(could not be evaluated)' is %s service", state);
    }
  }

}
