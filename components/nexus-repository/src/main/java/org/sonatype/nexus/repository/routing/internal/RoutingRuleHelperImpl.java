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
package org.sonatype.nexus.repository.routing.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.routing.RoutingMode;
import org.sonatype.nexus.repository.routing.RoutingRule;
import org.sonatype.nexus.repository.routing.RoutingRuleHelper;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;
import org.sonatype.nexus.repository.routing.RoutingRulesConfiguration;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

/**
 * @since 3.next
 */
@Named
@Singleton
public class RoutingRuleHelperImpl
    implements RoutingRuleHelper
{

  private final RoutingRuleStore routingRuleStore;

  private final RepositoryManager repositoryManager;

  private final RoutingRulesConfiguration configuration;

  @Inject
  public RoutingRuleHelperImpl(
      final RoutingRuleStore routingRuleStore,
      final RepositoryManager repositoryManager,
      final RoutingRulesConfiguration configuration)
  {
    this.routingRuleStore = checkNotNull(routingRuleStore);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.configuration = checkNotNull(configuration);
  }

  public boolean isAllowed(final Repository repository, final String path) {
    if (!configuration.isEnabled()) {
      return true;
    }

    RoutingRule routingRule = getRoutingRule(repository);

    if (routingRule == null) {
      return true;
    }

    return isAllowed(routingRule.mode(), routingRule.matchers(), path);
  }

  public boolean isAllowed(final RoutingMode mode, List<String> matchers, final String path) {
    boolean matches = matchers.stream().anyMatch(path::matches);
    return (!matches && mode == RoutingMode.BLOCK) || (matches && mode == RoutingMode.ALLOW);
  }

  public Map<String, List<String>> calculateAssignedRepositories() {
    return StreamSupport.stream(repositoryManager.browse().spliterator(), false)
        .filter(repository -> getRoutingRuleId(repository) != null)
        .collect(groupingBy(
            RoutingRuleHelperImpl::getRoutingRuleId,
            mapping(Repository::getName, toList())
        ));
  }

  private RoutingRule getRoutingRule(final Repository repository) {
    String routingRuleId = getRoutingRuleId(repository);
    if (routingRuleId != null) {
      return routingRuleStore.getById(routingRuleId);
    }
    return null;
  }

  private static String getRoutingRuleId(final Repository repository) {
    Map<String, Object> routingConfiguration = repository.getConfiguration().getAttributes()
        .getOrDefault(CONFIG_MAP_KEY, Collections.emptyMap());

    return (String) routingConfiguration.get(CONFIG_RULE_KEY);
  }
}
