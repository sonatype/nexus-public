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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response.Status;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.rest.api.RoutingRuleXO;
import org.sonatype.nexus.repository.rest.internal.resources.doc.RoutingRulesApiResourceDoc;
import org.sonatype.nexus.repository.routing.RoutingRule;
import org.sonatype.nexus.repository.routing.RoutingRuleHelper;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.WebApplicationMessageException;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.common.entity.EntityHelper.id;
import static org.sonatype.nexus.rest.APIConstants.V1_API_PREFIX;

/**
 * @since 3.16
 */
@Named
@Singleton
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(RoutingRulesApiResource.RESOURCE_URI)
public class RoutingRulesApiResource
    extends ComponentSupport
    implements Resource, RoutingRulesApiResourceDoc
{
  public static final String RESOURCE_URI = V1_API_PREFIX + "/routing-rules";

  private final RoutingRuleStore routingRuleStore;

  private final RoutingRuleHelper routingRuleHelper;

  @Inject
  public RoutingRulesApiResource(final RoutingRuleStore routingRuleStore, final RoutingRuleHelper routingRuleHelper) {
    this.routingRuleStore = checkNotNull(routingRuleStore);
    this.routingRuleHelper = checkNotNull(routingRuleHelper);
  }

  @POST
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  public void createRoutingRule(@NotNull final RoutingRuleXO routingRuleXO)
  {
    RoutingRule routingRule = new RoutingRule(routingRuleXO.getName(),
        routingRuleXO.getDescription(),
        routingRuleXO.getMode(),
        routingRuleXO.getMatchers());

    routingRuleStore.create(routingRule);
  }

  @GET
  public List<RoutingRuleXO> getRoutingRules() {
    routingRuleHelper.ensureUserHasPermissionToRead();
    return routingRuleStore.list()
            .stream()
            .map(RoutingRuleXO::fromRoutingRule)
            .collect(Collectors.toList());
  }

  @GET
  @Path("/{name}")
  public RoutingRuleXO getRoutingRule(@PathParam("name") final String name) {
    routingRuleHelper.ensureUserHasPermissionToRead();
    RoutingRule routingRule = getRuleFromStore(name);
    return RoutingRuleXO.fromRoutingRule(routingRule);
  }

  @PUT
  @Path("/{name}")
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  public void updateRoutingRule(@PathParam("name") final String name,
                                @NotNull final RoutingRuleXO routingRuleXO)
  {
    RoutingRule routingRule = getRuleFromStore(name);

    routingRule.name(routingRuleXO.getName())
        .description(routingRuleXO.getDescription())
        .mode(routingRuleXO.getMode())
        .matchers(routingRuleXO.getMatchers());

    routingRuleStore.update(routingRule);
  }

  @DELETE
  @Path("/{name}")
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  public void deleteRoutingRule(@PathParam("name") final String name) {
    RoutingRule routingRule = getRuleFromStore(name);
    EntityId routingRuleId = id(routingRule);
    Map<EntityId, List<String>> assignedRepositories = routingRuleHelper.calculateAssignedRepositories();

    List<String> repositoryNames = assignedRepositories.computeIfAbsent(routingRuleId, id -> emptyList());
    if (!repositoryNames.isEmpty()) {
      throw new WebApplicationMessageException(
          Status.BAD_REQUEST,
          "\"Routing rule is still in use by " + repositoryNames.size() + " repositories.\"",
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
          APPLICATION_JSON
      );
    }
    return routingRule;
  }
}
