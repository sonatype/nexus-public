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

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.orient.freeze.DatabaseFreezeService;
import org.sonatype.nexus.orient.freeze.ReadOnlyState;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.security.SecurityHelper;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.rest.APIConstants.BETA_API_PREFIX;

/**
 * @deprecated since 3.next, use {@link DatabaseFreezeResource} instead.
 */
@Deprecated
@Named
@Singleton
@Path(DatabaseFreezeResourceBeta.RESOURCE_URI)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class DatabaseFreezeResourceBeta
    extends ComponentSupport
    implements Resource
{
  static final String RESOURCE_URI = BETA_API_PREFIX + "/read-only";

  private final DatabaseFreezeResource delegate;

  @Inject
  public DatabaseFreezeResourceBeta(final DatabaseFreezeService freezeService, final SecurityHelper securityHelper) {
    delegate = new DatabaseFreezeResource(freezeService, securityHelper);
  }

  @GET
  public ReadOnlyState get() {
    log.warn("Deprecated endpoint: {}, please use: {}", RESOURCE_URI, DatabaseFreezeResource.RESOURCE_URI);
    return delegate.get();
  }

  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @POST
  @Path("/freeze")
  public void freeze() {
    log.warn("Deprecated endpoint: {}, please use: {}", RESOURCE_URI, DatabaseFreezeResource.RESOURCE_URI);
    delegate.freeze();
  }

  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @POST
  @Path("/release")
  public void release() {
    log.warn("Deprecated endpoint: {}, please use: {}", RESOURCE_URI, DatabaseFreezeResource.RESOURCE_URI);
    delegate.release();
  }

  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @POST
  @Path("/force-release")
  public void forceRelease() {
    log.warn("Deprecated endpoint: {}, please use: {}", RESOURCE_URI, DatabaseFreezeResource.RESOURCE_URI);
    delegate.forceRelease();
  }
}
