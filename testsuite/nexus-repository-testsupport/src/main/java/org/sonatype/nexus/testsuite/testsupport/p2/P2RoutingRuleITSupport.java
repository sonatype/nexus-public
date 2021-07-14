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
package org.sonatype.nexus.testsuite.testsupport.p2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.goodies.httpfixture.server.api.Behaviour;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;
import org.sonatype.nexus.testsuite.testsupport.fixtures.RoutingRuleRule;

import org.junit.Rule;

/**
 * Support class for P2 Routing Rule ITs.
 */
public class P2RoutingRuleITSupport extends P2ITSupport
{
  @Inject
  private RoutingRuleStore ruleStore;

  @Rule
  public RoutingRuleRule routingRules = new RoutingRuleRule(() -> ruleStore);

  protected EntityId createBlockedRoutingRule(final String name, final String matcher) {
    return routingRules.create(name, matcher).id();
  }

  protected void attachRuleToRepository(final Repository repository, final EntityId routingRuleId) throws Exception {
    org.sonatype.nexus.repository.config.Configuration configuration = repository.getConfiguration().copy();
    configuration.setRoutingRuleId(routingRuleId);
    repositoryManager.update(configuration);
  }

  protected static class BehaviourSpy
      implements Behaviour
  {
    private static final String REQUEST_URI_PATTERN = "%s?%s";

    private Behaviour delegate;

    private List<String> requestUris = new ArrayList<>();

    public BehaviourSpy(final Behaviour delegate) {
      this.delegate = delegate;
    }

    public List<String> getRequestUris() {
      return requestUris;
    }

    @Override
    public boolean execute(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final Map<Object, Object> ctx) throws Exception
    {
      requestUris.add(String.format(REQUEST_URI_PATTERN, request.getRequestURI(), request.getQueryString()));
      return delegate.execute(request, response, ctx);
    }
  }
}
