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
import org.sonatype.nexus.proxy.events.RepositoryEventProxyModeChanged;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventAdd;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventRemove;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.ProxyMode;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A condition that is satisfied when a repository has a specified proxy mode.
 *
 * @since capabilities 2.0
 */
public class RepositoryProxyModeCondition
    extends RepositoryConditionSupport
{

  private final ProxyMode proxyMode;

  public RepositoryProxyModeCondition(final EventBus eventBus,
                                      final RepositoryRegistry repositoryRegistry,
                                      final ProxyMode proxyMode,
                                      final RepositoryConditions.RepositoryId repositoryId)
  {
    super(eventBus, repositoryRegistry, repositoryId);
    this.proxyMode = checkNotNull(proxyMode);
  }

  @Override
  @AllowConcurrentEvents
  @Subscribe
  public void handle(final RepositoryRegistryEventAdd event) {
    if (sameRepositoryAs(event.getRepository().getId())
        && event.getRepository().getRepositoryKind().isFacetAvailable(ProxyRepository.class)) {
      setSatisfied(proxyMode.equals(
          event.getRepository().adaptToFacet(ProxyRepository.class).getProxyMode())
      );
    }
  }

  @AllowConcurrentEvents
  @Subscribe
  public void handle(final RepositoryEventProxyModeChanged event) {
    if (sameRepositoryAs(event.getRepository().getId())) {
      setSatisfied(proxyMode.equals(event.getNewProxyMode()));
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
      return String.format("Repository '%s' is %s", id, proxyMode.toString());
    }
    catch (Exception ignore) {
      return String.format("Repository '(could not be evaluated)' is %s", proxyMode.toString());
    }
  }

  @Override
  public String explainSatisfied() {
    String mode = "not blocked";
    if (proxyMode.equals(ProxyMode.BLOCKED_MANUAL)) {
      mode = "manually blocked";
    }
    else if (proxyMode.equals(ProxyMode.BLOCKED_AUTO)) {
      mode = "auto blocked";
    }
    try {
      final String id = getRepositoryId();
      return String.format("Repository '%s' is %s", id, mode);
    }
    catch (Exception ignore) {
      return String.format("Repository '(could not be evaluated)' is %s", mode);
    }
  }

  @Override
  public String explainUnsatisfied() {
    String mode = "blocked";
    if (proxyMode.equals(ProxyMode.BLOCKED_MANUAL)) {
      mode = "not manually blocked";
    }
    else if (proxyMode.equals(ProxyMode.BLOCKED_AUTO)) {
      mode = "not auto blocked";
    }
    try {
      final String id = getRepositoryId();
      return String.format("Repository '%s' is %s", id, mode);
    }
    catch (Exception ignore) {
      return String.format("Repository '(could not be evaluated)' is %s", mode);
    }
  }

}
