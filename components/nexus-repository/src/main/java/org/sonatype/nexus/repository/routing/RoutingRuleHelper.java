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

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;

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

  @Inject
  public RoutingRuleHelper(final RoutingRuleStore routingRuleStore) {
    this.routingRuleStore = routingRuleStore;
  }

  /**
   * Determine if the path is allowed if the repository has a routing rule configured.
   *
   * @param repository the repository for the context of this request
   * @param path the path of the component (must include leading slash)
   * @return true if the request is allowed, false if it should be blocked
   */
  public boolean isAllowed(final Repository repository, final String path) {
    RoutingRule routingRule = getRoutingRule(repository);
    return isAllowed(routingRule, path);
  }

  /**
   * Determine if the path is allowed by the RoutingRule.
   *
   * @param routingRule the routing rule to test the path against
   * @param path the path of the component (must include leading slash)
   * @return true if the request is allowed, false if it should be blocked
   */
  public boolean isAllowed(final RoutingRule routingRule, final String path) {
    if (routingRule == null) {
      return true;
    }

    boolean matches = routingRule.matchers().stream().anyMatch(rule -> path.matches(rule));
    RoutingMode mode = routingRule.mode();

    return (!matches && mode == RoutingMode.BLOCK) || (matches && mode == RoutingMode.ALLOW);
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
