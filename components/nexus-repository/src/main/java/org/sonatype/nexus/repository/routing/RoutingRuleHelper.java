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
package org.sonatype.nexus.repository.routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.next
 */
@Named
@Singleton
public class RoutingRuleHelper
{
  private static final String CONFIG_RULE_KEY = "routingRuleId";

  private static final String CONFIG_MAP_KEY = "routingRules";

  private final RoutingRuleStore routingRuleStore;

  private final RoutingRulesConfiguration configuration;

  private final RepositoryManager repositoryManager;

  @Inject
  public RoutingRuleHelper(
      final RoutingRuleStore routingRuleStore,
      final RepositoryManager repositoryManager,
      final RoutingRulesConfiguration configuration)
  {
    this.routingRuleStore = checkNotNull(routingRuleStore);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.configuration = checkNotNull(configuration);
  }

  /**
   * Determine if the path is allowed if the repository has a routing rule configured.
   *
   * @param repository the repository for the context of this request
   * @param path the path of the component (must include leading slash)
   * @return true if the request is allowed, false if it should be blocked
   */
  public boolean isAllowed(final Repository repository, final String path) {
    if (configuration.isEnabled()) {
      RoutingRule routingRule = getRoutingRule(repository);

      if (routingRule == null) {
        return true;
      }

      return isAllowed(routingRule.mode(), routingRule.matchers(), path);
    }
    return true;
  }

  /**
   * Determine if the path is allowed by the RoutingRule, does not consider whether the configuration is enabled.
   *
   * @param mode the routing mode to test the path against
   * @param matchers the list of matchers to test the path against
   * @param path the path of the component (must include leading slash)
   * @return true if the request is allowed, false if it should be blocked
   */
  public boolean isAllowed(final RoutingMode mode, final List<String> matchers, final String path) {
    boolean matches = matchers.stream().anyMatch(path::matches);
    return (!matches && mode == RoutingMode.BLOCK) || (matches && mode == RoutingMode.ALLOW);
  }

  /**
   * Iterates through all repositories to find which routing rules are assigned
   *
   * @return A map of routing rule ids to a list of repository names that are using them
   */
  public Map<String, List<String>> calculateAssignedRepositories() {
    Map<String, List<String>> assignedRepositories = new HashMap<>();
    for (Repository repository : repositoryManager.browse()) {
      String routingRuleId = getRoutingRuleId(repository);
      if (null != routingRuleId) {
        List<String> repositoryNames = assignedRepositories.computeIfAbsent(routingRuleId, k -> new ArrayList<>());
        repositoryNames.add(repository.getName());
      }
    }
    return assignedRepositories;
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
