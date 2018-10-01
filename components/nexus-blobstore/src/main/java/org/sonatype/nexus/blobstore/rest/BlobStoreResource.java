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
package org.sonatype.nexus.blobstore.rest;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaResult;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaService;
import org.sonatype.nexus.rest.Resource;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.sonatype.nexus.rest.APIConstants.V1_API_PREFIX;

/**
 * @since 3.14
 */
@Named
@Singleton
@Path(BlobStoreResource.RESOURCE_URI)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class BlobStoreResource
    extends ComponentSupport
    implements Resource, BlobStoreResourceDoc
{
  public static final String RESOURCE_URI = V1_API_PREFIX + "/blobstores";

  private final BlobStoreManager blobStoreManager;

  private final BlobStoreQuotaService quotaService;

  @Inject
  public BlobStoreResource(final BlobStoreManager blobStoreManager,
                           final BlobStoreQuotaService quotaService)
  {
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.quotaService = checkNotNull(quotaService);
  }

  @Override
  @RequiresAuthentication
  @RequiresPermissions("nexus:blobstores:read")
  @GET
  @Path("/{id}/quota-status")
  public BlobStoreQuotaResultXO quotaStatus(@PathParam("id") final String id) {
    BlobStore blobStore = blobStoreManager.get(id);

    if (blobStore == null) {
      throw new WebApplicationException(format("No blob store found for id '%s' ", id), NOT_FOUND);
    }

    BlobStoreQuotaResult result = quotaService.checkQuota(blobStore);

    return result != null ? BlobStoreQuotaResultXO.asQuotaXO(result) : BlobStoreQuotaResultXO.asNoQuotaXO(id);
  }
}
