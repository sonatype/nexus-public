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
import org.sonatype.nexus.common.app.FreezeService;
import org.sonatype.nexus.common.app.ReadOnlyState;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.security.privilege.ApplicationPermission;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresUser;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.rest.APIConstants.V1_API_PREFIX;

/**
 * @since 3.6
 */
@Named
@Singleton
@Path(FreezeResource.RESOURCE_URI)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class FreezeResource
    extends ComponentSupport
    implements Resource, FreezeResourceDoc
{
  public static final String RESOURCE_URI = V1_API_PREFIX + "/read-only";

  private static final String ENABLE_READONLY_FAILED_MESSAGE = "Attempt to enable read-only failed";

  private static final String RELESE_READONLY_FAILED_MESSAGE = "Attempt to release read-only failed";

  private static final String FORCE_RELEASE_READONLY_FAILED_MESSAGE = "Attempt to force release read-only failed";

  private static final Permission READ_SETTINGS_PERMISSION = new ApplicationPermission("settings", asList("read"));

  private final FreezeService freezeService;

  private final SecurityHelper securityHelper;

  @Inject
  public FreezeResource(final FreezeService freezeService, final SecurityHelper securityHelper) {
    this.freezeService = checkNotNull(freezeService);
    this.securityHelper = checkNotNull(securityHelper);
  }

  @Override
  @RequiresUser
  @GET
  public ReadOnlyState get() {
    // provide extra detail when current user has access to settings
    return new ReadOnlyState(freezeService.currentFreezeRequests(),
        securityHelper.allPermitted(READ_SETTINGS_PERMISSION));
  }

  @Override
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @POST
  @Path("/freeze")
  public void freeze() {
    try {
      freezeService.requestFreeze("REST request");
    }
    catch (Exception e) {
      log.warn(ENABLE_READONLY_FAILED_MESSAGE, e);
      throw new WebApplicationException(ENABLE_READONLY_FAILED_MESSAGE, 404);
    }
  }

  @Override
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @POST
  @Path("/release")
  public void release() {
    try {
      freezeService.cancelFreeze();
    }
    catch (Exception e) {
      log.warn(RELESE_READONLY_FAILED_MESSAGE, e);
      throw new WebApplicationException(RELESE_READONLY_FAILED_MESSAGE, 404);
    }
  }

  @Override
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @POST
  @Path("/force-release")
  public void forceRelease() {
    try {
      freezeService.cancelAllFreezeRequests();
    }
    catch (Exception e) {
      log.warn(FORCE_RELEASE_READONLY_FAILED_MESSAGE, e);
      throw new WebApplicationException(FORCE_RELEASE_READONLY_FAILED_MESSAGE, 404);
    }
  }
}
