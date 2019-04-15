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
package org.sonatype.nexus.internal.app;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.ManagedLifecycle.Phase;
import org.sonatype.nexus.common.app.ManagedLifecycleManager;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.WebApplicationMessageException;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.sonatype.nexus.rest.APIConstants.V1_API_PREFIX;

/**
 * REST implementation to manage the Nexus application lifecycle. (also exposed as JMX)
 *
 * @since 3.16
 */
@Path(ManagedLifecycleResource.RESOURCE_URI)
@Named
@Singleton
public class ManagedLifecycleResource
    extends ComponentSupport
    implements Resource, ManagedLifecycleResourceDoc
{
  public static final String RESOURCE_URI = V1_API_PREFIX + "/lifecycle";

  private final ManagedLifecycleManager lifecycleManager;

  @Inject
  public ManagedLifecycleResource(final ManagedLifecycleManager lifecycleManager) {
    this.lifecycleManager = checkNotNull(lifecycleManager);
  }

  @GET
  @Path("phase")
  @Produces(TEXT_PLAIN)
  @Override
  public String getPhase() {
    return lifecycleManager.getCurrentPhase().name();
  }

  @PUT
  @Path("phase")
  @Consumes(TEXT_PLAIN)
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @Override
  public void setPhase(final String phase) {
    try {
      lifecycleManager.to(Phase.valueOf(phase));
    }
    catch (Exception e) {
      log.warn("Problem moving to phase {}", phase, e);
      throw new WebApplicationMessageException(INTERNAL_SERVER_ERROR, "Problem moving to phase " + phase + ": " + e);
    }
  }

  @PUT
  @Path("bounce")
  @Consumes(TEXT_PLAIN)
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @Override
  public void bounce(final String phase) {
    try {
      lifecycleManager.bounce(Phase.valueOf(phase));
    }
    catch (Exception e) {
      log.warn("Problem bouncing phase {}", phase, e);
      throw new WebApplicationMessageException(INTERNAL_SERVER_ERROR, "Problem bouncing phase " + phase + ": " + e);
    }
  }
}
