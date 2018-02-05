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
package org.sonatype.nexus.orient.internal.rest;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.orient.freeze.DatabaseFreezeService;
import org.sonatype.nexus.orient.freeze.ReadOnlyState;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.security.SecurityHelper;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.orient.freeze.FreezeRequest.InitiatorType.USER_INITIATED;
import static org.sonatype.nexus.rest.APIConstants.BETA_API_PREFIX;

/**
 * @since 3.6
 */
@Named
@Singleton
@Path(BETA_API_PREFIX + "/read-only")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class DatabaseFreezeResource
    extends ComponentSupport
    implements Resource, DatabaseFreezeResourceDoc
{
  public static final String RESOURCE_URI = BETA_API_PREFIX + "/read-only";

  private final DatabaseFreezeService freezeService;

  private final SecurityHelper securityHelper;
  @Inject
  public DatabaseFreezeResource(final DatabaseFreezeService freezeService, final SecurityHelper securityHelper) {
    this.freezeService = checkNotNull(freezeService);
    this.securityHelper = checkNotNull(securityHelper);
  }

  @Override
  @GET
  public ReadOnlyState get() {
    return freezeService.getReadOnlyState();
  }

  @Override
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @POST
  @Path("/freeze")
  public void freeze() {
    if (freezeService.requestFreeze(USER_INITIATED, principalAsString()) == null) {
      throw new WebApplicationException("Attempt to enable read-only failed", 404);
    }
  }

  @Override
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @POST
  @Path("/release")
  public void release() {
    boolean success = freezeService.releaseUserInitiatedIfPresent();
    if (!success) {
      throw new WebApplicationException("Attempt to release read-only failed", 404);
    }
  }

  @Override
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @POST
  @Path("/force-release")
  public void forceRelease() {
    if (freezeService.releaseAllRequests().isEmpty()) {
      throw new WebApplicationException("Attempt to force release read-only failed", 404);
    }
  }

  private String principalAsString() {
    Object principal = securityHelper.subject().getPrincipal();
    if (principal == null) {
      throw new WebApplicationException("Unauthorized", 401);
    }
    return principal.toString();
  }
}
