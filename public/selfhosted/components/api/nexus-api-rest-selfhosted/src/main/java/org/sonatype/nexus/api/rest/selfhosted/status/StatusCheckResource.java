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
package org.sonatype.nexus.api.rest.selfhosted.status;

import java.util.SortedMap;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.nexus.rest.Resource;

import com.codahale.metrics.annotation.Timed;
import com.codahale.metrics.health.HealthCheck.Result;
import com.codahale.metrics.health.HealthCheckRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.api.rest.selfhosted.status.StatusCheckResource.RESOURCE_PATH;
import static org.sonatype.nexus.rest.APIConstants.V1_API_PREFIX;

@Component
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@Path(RESOURCE_PATH)
public class StatusCheckResource
    implements Resource, StatusCheckResourceDoc
{
  static final String RESOURCE_PATH = V1_API_PREFIX + "/status/check";

  private final HealthCheckRegistry registry;

  @Autowired
  public StatusCheckResource(final HealthCheckRegistry registry) {
    this.registry = checkNotNull(registry);
  }

  @GET
  @Timed
  @RequiresAuthentication
  @RequiresPermissions("nexus:metrics:read")
  @Override
  public SortedMap<String, Result> getSystemStatusChecks() {
    return registry.runHealthChecks();
  }
}
