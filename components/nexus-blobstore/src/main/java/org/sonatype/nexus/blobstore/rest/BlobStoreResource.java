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

import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.blobstore.ConnectionChecker;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConnectionException;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaResult;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaService;
import org.sonatype.nexus.repository.blobstore.BlobStoreConfigurationStore;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.validation.Validate;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

/**
 * @since 3.14
 */
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class BlobStoreResource
    extends ComponentSupport
    implements Resource, BlobStoreResourceDoc
{
  private final BlobStoreManager blobStoreManager;

  private final BlobStoreConfigurationStore store;

  private final BlobStoreQuotaService quotaService;

  private final Map<String, ConnectionChecker> connectionCheckers;

  private interface Messages
      extends MessageBundle
  {
    @DefaultMessage("Connection failed, check the logs for more information.")
    String connectionError();
  }

  private static final Messages messages = I18N.create(Messages.class);

  public BlobStoreResource(
      final BlobStoreManager blobStoreManager,
      final BlobStoreConfigurationStore store,
      final BlobStoreQuotaService quotaService,
      final Map<String, ConnectionChecker> connectionCheckers)
  {
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.store = checkNotNull(store);
    this.quotaService = checkNotNull(quotaService);
    this.connectionCheckers = connectionCheckers;
  }

  @Override
  @RequiresAuthentication
  @RequiresPermissions("nexus:blobstores:read")
  @GET
  public List<GenericBlobStoreApiResponse> listBlobStores() {
    Map<String, BlobStore> blobstoresByName = blobStoreManager.getByName();
    return store.list()
        .stream()
        .map(
            configuration -> new GenericBlobStoreApiResponse(configuration,
                blobstoresByName.get(configuration.getName())))
        .collect(toList());
  }

  @Override
  @RequiresAuthentication
  @RequiresPermissions("nexus:blobstores:delete")
  @DELETE
  @Path("/{name}")
  public void deleteBlobStore(@PathParam("name") final String name) throws Exception {
    if (!blobStoreManager.exists(name)) {
      BlobStoreResourceUtil.throwCreateBlobStoreNotFoundException("", name);
    }
    try {
      blobStoreManager.delete(name);
    }
    catch (BlobStoreException e) {
      BlobStoreResourceUtil.throwBlobStoreBadRequestException(e.getMessage());
    }
  }

  @Override
  @RequiresAuthentication
  @RequiresPermissions("nexus:blobstores:read")
  @GET
  @Path("/{name}/quota-status")
  public BlobStoreQuotaResultXO quotaStatus(@PathParam("name") final String name) {
    BlobStore blobStore = blobStoreManager.get(name);

    if (blobStore == null) {
      throw new WebApplicationException(format("No blob store found for id '%s' ", name), NOT_FOUND);
    }

    BlobStoreQuotaResult result = quotaService.checkQuota(blobStore);

    return result != null ? BlobStoreQuotaResultXO.asQuotaXO(result) : BlobStoreQuotaResultXO.asNoQuotaXO(name);
  }

  @Override
  @POST
  @Path("test-connection")
  @RequiresAuthentication
  @RequiresPermissions("nexus:blobstores:read")
  @Validate
  public void verifyConnection(final @NotNull @Valid BlobStoreConnectionXO blobStoreConnectionXO) {
    try {
      ConnectionChecker conChecker = checkNotNull(connectionCheckers.get(blobStoreConnectionXO.getType()));
      conChecker.verifyConnection(blobStoreConnectionXO.getName(), blobStoreConnectionXO.getAttributes());
    }
    catch (BlobStoreConnectionException ce) { // NOSONAR
      log.error("Can't connect to {} blob store", blobStoreConnectionXO.getType(), ce);
      throw new WebApplicationException(Response.status(BAD_REQUEST).entity(ce.getMessage()).build());
    }
    catch (Exception e) {
      log.warn("Can't connect to {} blob store", blobStoreConnectionXO.getType(), e);
      throw new WebApplicationException(Response.status(BAD_REQUEST).entity(messages.connectionError()).build());
    }
  }
}
