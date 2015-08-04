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
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventAdd;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventRemove;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

/**
 * A condition that is satisfied when a repository exists.
 *
 * @since capabilities 2.0
 */
public class RepositoryExistsCondition
    extends RepositoryConditionSupport
{

  public RepositoryExistsCondition(final EventBus eventBus,
                                   final RepositoryRegistry repositoryRegistry,
                                   final RepositoryConditions.RepositoryId repositoryId)
  {
    super(eventBus, repositoryRegistry, repositoryId);
  }

  @Override
  @AllowConcurrentEvents
  @Subscribe
  public void handle(final RepositoryRegistryEventAdd event) {
    if (sameRepositoryAs(event.getRepository().getId())) {
      setSatisfied(true);
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
      return String.format("Repository '%s' exists", id);
    }
    catch (Exception ignore) {
      return "Repository '(could not be evaluated)' exists";
    }
  }

  @Override
  public String explainSatisfied() {
    try {
      final String id = getRepositoryId();
      return String.format("Repository '%s' exists", id);
    }
    catch (Exception ignore) {
      return "Repository '(could not be evaluated)' exists";
    }
  }

  @Override
  public String explainUnsatisfied() {
    try {
      final String id = getRepositoryId();
      return String.format("Repository '%s' does not exist", id);
    }
    catch (Exception ignore) {
      return "Repository '(could not be evaluated)' does not exist";
    }
  }

}
