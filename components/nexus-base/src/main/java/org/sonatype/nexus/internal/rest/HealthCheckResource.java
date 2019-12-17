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
package org.sonatype.nexus.internal.rest;

import java.util.SortedMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.rest.Resource;

import com.codahale.metrics.health.HealthCheck.Result;
import com.codahale.metrics.health.HealthCheckRegistry;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * @since 3.20
 */
@Named
@Singleton
@Path(HealthCheckResource.RESOURCE_URI)
@Produces(APPLICATION_JSON)
public class HealthCheckResource
    extends ComponentSupport
    implements Resource
{
  public static final String RESOURCE_URI = "/internal/ui/status-check";

  private HealthCheckRegistry registry;

  @Inject
  public HealthCheckResource(HealthCheckRegistry registry) {
    this.registry = checkNotNull(registry);
  }

  @GET
  @RequiresAuthentication
  @RequiresPermissions("nexus:metrics:read")
  public SortedMap<String, Result> getSystemStatusChecks() {
    return registry.runHealthChecks();
  }
}
