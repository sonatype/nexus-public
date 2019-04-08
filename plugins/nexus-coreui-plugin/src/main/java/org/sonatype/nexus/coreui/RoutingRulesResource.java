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
package org.sonatype.nexus.coreui;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.routing.RoutingMode;
import org.sonatype.nexus.repository.routing.RoutingRule;
import org.sonatype.nexus.repository.routing.RoutingRuleHelper;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;
import org.sonatype.nexus.rest.Resource;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import static java.util.Collections.emptyList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.common.entity.EntityHelper.id;

/**
 * @since 3.next
 */
@Named
@Singleton
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(RoutingRulesResource.RESOURCE_PATH)
public class RoutingRulesResource
    extends ComponentSupport
    implements Resource
{
  static final String RESOURCE_PATH = "internal/ui/routing-rules";

  @Inject
  private RoutingRuleStore routingRuleStore;

  @Inject
  private RoutingRuleHelper routingRuleHelper;

  @POST
  @RequiresPermissions("nexus:repository-admin:*:*:add")
  public void createRoutingRule(RoutingRuleXO routingRuleXO)
  {
    routingRuleStore.create(fromXO(routingRuleXO));
  }

  @POST
  @Path("/test")
  @RequiresPermissions("nexus:repository-admin:*:*:read")
  public boolean isAllowed(RoutingRuleTestXO routingRuleTestXO)
  {
    String path = routingRuleTestXO.getPath();
    List<String> matchers = routingRuleTestXO.getMatchers();
    RoutingMode mode = routingRuleTestXO.getMode();
    return routingRuleHelper.isAllowed(mode, matchers, path);
  }

  @GET
  @RequiresPermissions("nexus:repository-admin:*:*:read")
  public List<RoutingRuleXO> getRoutingRules() {
    List<RoutingRuleXO> rules = routingRuleStore.list()
            .stream()
            .map(RoutingRulesResource::toXO)
            .collect(Collectors.toList());

    Map<EntityId, List<String>> assignedRepositories = routingRuleHelper.calculateAssignedRepositories();
    for (RoutingRuleXO rule : rules) {
      List<String> repositoryNames = assignedRepositories.computeIfAbsent(id(rule.getId()), id -> emptyList());
      repositoryNames.sort(String.CASE_INSENSITIVE_ORDER);
      rule.setAssignedRepositoryNames(repositoryNames);
    }

    return rules;
  }

  @PUT
  @Path("/{name}")
  @RequiresPermissions("nexus:repository-admin:*:*:edit")
  public void updateRoutingRule(@PathParam("name") final String name, RoutingRuleXO routingRuleXO) {
    RoutingRule routingRule = routingRuleStore.getByName(name);
    if (null == routingRule) {
      throw new WebApplicationException(Status.NOT_FOUND);
    }
    routingRule.name(routingRuleXO.getName());
    routingRule.description(routingRuleXO.getDescription());
    routingRule.mode(routingRuleXO.getMode());
    routingRule.matchers(routingRuleXO.getMatchers());
    routingRuleStore.update(routingRule);
  }

  @DELETE
  @Path("/{name}")
  @RequiresPermissions("nexus:repository-admin:*:*:delete")
  public void deleteRoutingRule(@PathParam("name") final String name) {
    RoutingRule routingRule = routingRuleStore.getByName(name);
    if (null == routingRule) {
      throw new WebApplicationException(Status.NOT_FOUND);
    }

    Map<EntityId, List<String>> assignedRepositories = routingRuleHelper.calculateAssignedRepositories();
    List<String> repositoryNames = assignedRepositories.getOrDefault(id(routingRule), emptyList());
    if (repositoryNames.size() > 0) {
      throw new WebApplicationException("Routing rule is still in use by " + repositoryNames.size() + " repositories.", Status.BAD_REQUEST);
    }

    routingRuleStore.delete(routingRule);
  }

  private static RoutingRule fromXO(RoutingRuleXO routingRuleXO) {
    return new RoutingRule(routingRuleXO.getName(),
        routingRuleXO.getDescription(),
        routingRuleXO.getMode(),
        routingRuleXO.getMatchers());
  }

  private static RoutingRuleXO toXO(RoutingRule routingRule) {
    RoutingRuleXO routingRuleXO = new RoutingRuleXO();
    routingRuleXO.setId(id(routingRule).getValue());
    routingRuleXO.setName(routingRule.name());
    routingRuleXO.setDescription(routingRule.description());
    routingRuleXO.setMode(routingRule.mode());
    routingRuleXO.setMatchers(routingRule.matchers());
    return routingRuleXO;
  }
}
