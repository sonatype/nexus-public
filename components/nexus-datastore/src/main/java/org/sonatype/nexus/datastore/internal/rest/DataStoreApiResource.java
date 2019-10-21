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
package org.sonatype.nexus.datastore.internal.rest;

import java.sql.SQLInvalidAuthorizationSpecException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.datastore.DataStoreUsageChecker;
import org.sonatype.nexus.datastore.api.DataStore;
import org.sonatype.nexus.datastore.api.DataStoreManager;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.WebApplicationMessageException;

import com.google.common.base.Throwables;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static org.sonatype.nexus.rest.APIConstants.BETA_API_PREFIX;

/**
 * REST API for manipulating Data Stores
 *
 * @since 3.next
 */
@Named
@Singleton
@Path(DataStoreApiResource.RESOURCE_URI)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DataStoreApiResource
    extends ComponentSupport
    implements Resource, DataStoreApiResourceDoc
{
  public static final String RESOURCE_URI = BETA_API_PREFIX + "/data-stores/";

  private final DataStoreManager dataStoreManager;

  private final List<DataStoreUsageChecker> dataStoreUsageCheckers;

  @Inject
  public DataStoreApiResource(
      final DataStoreManager dataStoreManager,
      final List<DataStoreUsageChecker> dataStoreUsageCheckers)
  {
    this.dataStoreManager = dataStoreManager;
    this.dataStoreUsageCheckers = dataStoreUsageCheckers;
  }

  @Override
  @GET
  @RequiresAuthentication
  @RequiresPermissions("nexus:datastores:read")
  public List<DataStoreApiXO> getDataStores() {
    return StreamSupport.stream(dataStoreManager.browse().spliterator(), true).map(DataStore::getConfiguration)
        .map(DataStoreApiXO::new).collect(Collectors.toList());
  }

  @Override
  @POST
  @RequiresAuthentication
  @RequiresPermissions("nexus:datastores:create")
  public Response createDataStore(@Valid final DataStoreApiUpdateXO apiDataStore) throws Exception {
    if (dataStoreManager.exists(apiDataStore.getName())) {
      throwWebException(Status.CONFLICT, "Data store '" + apiDataStore.getName() + "' already exists.");
    }

    try {
      dataStoreManager.create(apiDataStore.toDataStoreConfiguration());
    }
    catch (Exception e) {
      maybeHumanException(e, apiDataStore.getJdbcUrl());
    }
    return Response.status(Status.CREATED).build();
  }

  @Override
  @PUT
  @Path("{dataStoreName}")
  @RequiresAuthentication
  @RequiresPermissions("nexus:datastores:update")
  public void updateDataStore(
      @NotNull @PathParam("dataStoreName") final String dataStoreName,
      @Valid final DataStoreApiUpdateXO apiDataStore) throws Exception
  {
    if (!dataStoreName.equals(apiDataStore.getName())) {
      throwWebException(Status.BAD_REQUEST, "The request path name does not match the contents.");
    }

    ensureDataStoreExists(dataStoreName);

    try {
      dataStoreManager.update(apiDataStore.toDataStoreConfiguration());
    }
    catch (Exception e) {
      maybeHumanException(e, apiDataStore.getJdbcUrl());
    }
  }

  @Override
  @DELETE
  @Path("{dataStoreName}")
  @RequiresAuthentication
  @RequiresPermissions("nexus:datastores:delete")
  public void deleteDataStore(@NotNull @PathParam("dataStoreName") final String dataStoreName) throws Exception {
    boolean isInUse = dataStoreUsageCheckers.stream()
        .anyMatch(dataStoreUsageChecker -> dataStoreUsageChecker.isDataStoreUsed(dataStoreName));
    if (isInUse) {
      throwWebException(Status.BAD_REQUEST, "The data store '" + dataStoreName + "' is in use.");
    }
    ensureDataStoreExists(dataStoreName);
    dataStoreManager.delete(dataStoreName);
  }

  private void maybeHumanException(final Exception e, final String jdbcUrl) throws Exception {
    for (Throwable candidate : Throwables.getCausalChain(e)) {
      if (candidate instanceof SQLInvalidAuthorizationSpecException) {
        log.info("Failed to authenticate to {}", jdbcUrl, e);
        throwWebException(Status.BAD_REQUEST, "Authenticating with the database failed.");
      }
    }
    throw e;
  }

  private void ensureDataStoreExists(final String dataStoreName) {
    if (!dataStoreManager.exists(dataStoreName)) {
      throwWebException(Status.NOT_FOUND, "The data store '" + dataStoreName + "' is unknown.");
    }
  }

  private void throwWebException(final Status status, final String message) {
    throw new WebApplicationMessageException(status, "\"" + message + "\"", MediaType.APPLICATION_JSON);
  }
}
