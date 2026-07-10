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
package org.sonatype.nexus.repository.rest.internal.resources;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response.Status;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.rest.api.RoutingRuleXO;
import org.sonatype.nexus.repository.rest.internal.resources.doc.RoutingRulesApiResourceDoc;
import org.sonatype.nexus.repository.routing.RoutingRule;
import org.sonatype.nexus.repository.routing.RoutingRuleHelper;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.WebApplicationMessageException;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * @since 3.16
 */
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class RoutingRulesApiResource
    implements Resource, RoutingRulesApiResourceDoc
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final RoutingRuleStore routingRuleStore;

  private final RoutingRuleHelper routingRuleHelper;

  @Autowired
  public RoutingRulesApiResource(final RoutingRuleStore routingRuleStore, final RoutingRuleHelper routingRuleHelper) {
    this.routingRuleStore = checkNotNull(routingRuleStore);
    this.routingRuleHelper = checkNotNull(routingRuleHelper);
  }

  @Override
  @POST
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  public void createRoutingRule(@NotNull final RoutingRuleXO routingRuleXO) {
    final RoutingRule routingRule = routingRuleStore.newRoutingRule();
    routingRule.name(routingRuleXO.getName());
    routingRule.description(routingRuleXO.getDescription());
    routingRule.mode(routingRuleXO.getMode());
    routingRule.matchers(routingRuleXO.getMatchers());

    routingRuleStore.create(routingRule);
  }

  @Override
  @GET
  public List<RoutingRuleXO> getRoutingRules() {
    routingRuleHelper.ensureUserHasPermissionToRead();
    return routingRuleStore.list()
        .stream()
        .map(RoutingRuleXO::fromRoutingRule)
        .collect(Collectors.toList());
  }

  @Override
  @GET
  @Path("/{name}")
  public RoutingRuleXO getRoutingRule(@PathParam("name") final String name) {
    routingRuleHelper.ensureUserHasPermissionToRead();
    RoutingRule routingRule = getRuleFromStore(name);
    return RoutingRuleXO.fromRoutingRule(routingRule);
  }

  @Override
  @PUT
  @Path("/{name}")
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  public void updateRoutingRule(
      @PathParam("name") final String name,
      @NotNull final RoutingRuleXO routingRuleXO)
  {
    RoutingRule routingRule = getRuleFromStore(name);

    routingRule.name(routingRuleXO.getName())
        .description(routingRuleXO.getDescription())
        .mode(routingRuleXO.getMode())
        .matchers(routingRuleXO.getMatchers());

    routingRuleStore.update(routingRule);
  }

  @Override
  @DELETE
  @Path("/{name}")
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  public void deleteRoutingRule(@PathParam("name") final String name) {
    RoutingRule routingRule = getRuleFromStore(name);
    EntityId routingRuleId = routingRule.id();
    Map<EntityId, List<Repository>> assignedRepositories = routingRuleHelper.calculateAssignedRepositories();

    List<Repository> repositories = assignedRepositories.computeIfAbsent(routingRuleId, id -> emptyList());
    if (!repositories.isEmpty()) {
      throw new WebApplicationMessageException(
          Status.BAD_REQUEST,
          "\"Routing rule is still in use by " + repositories.size() + " repositories.\"",
          APPLICATION_JSON);
    }

    routingRuleStore.delete(routingRule);
  }

  private RoutingRule getRuleFromStore(final String name) {
    RoutingRule routingRule = routingRuleStore.getByName(name);
    if (null == routingRule) {
      throw new WebApplicationMessageException(
          Status.NOT_FOUND,
          "\"Did not find a routing rule with the name '" + name + "'\"",
          APPLICATION_JSON);
    }
    return routingRule;
  }
}
