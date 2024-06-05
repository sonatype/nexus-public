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
package org.sonatype.nexus.blobstore.s3.rest.internal;

import java.util.Optional;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.rest.BlobStoreResourceUtil;
import org.sonatype.nexus.blobstore.s3.internal.S3BlobStore;
import org.sonatype.nexus.blobstore.s3.rest.internal.model.S3BlobStoreApiModel;
import org.sonatype.nexus.rapture.PasswordPlaceholder;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.rest.WebApplicationMessageException;

import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static java.util.Optional.ofNullable;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.TYPE;
import static org.sonatype.nexus.blobstore.s3.rest.internal.S3BlobStoreApiConstants.NOT_AN_S3_BLOB_STORE_MSG_FORMAT;
import static org.sonatype.nexus.blobstore.s3.rest.internal.S3BlobStoreApiModelMapper.map;

/**
 * REST API endpoints for creating, reading, updating and deleting an S3 blob store.
 *
 * @since 3.20
 */
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class S3BlobStoreApiResource
    extends ComponentSupport
    implements Resource, S3BlobStoreApiResourceDoc
{
  private final S3BlobStoreApiUpdateValidation s3BlobStoreApiUpdateValidation;

  private final BlobStoreManager blobStoreManager;

  public S3BlobStoreApiResource(
      final BlobStoreManager blobStoreManager,
      final S3BlobStoreApiUpdateValidation validation)
  {
    this.blobStoreManager = blobStoreManager;
    this.s3BlobStoreApiUpdateValidation = validation;
  }

  @POST
  @Override
  @RequiresAuthentication
  @Path("/s3")
  @RequiresPermissions("nexus:blobstores:create")
  public Response createBlobStore(@Valid final S3BlobStoreApiModel request) {
    try {
      s3BlobStoreApiUpdateValidation.validateCreateRequest(request);
      final BlobStoreConfiguration blobStoreConfiguration = map(blobStoreManager.newConfiguration(), request);
      blobStoreManager.create(blobStoreConfiguration);
      return status(CREATED).build();
    }
    catch (Exception e) {
      throw new WebApplicationMessageException(BAD_REQUEST, e.getMessage());
    }

  }

  @PUT
  @Override
  @RequiresAuthentication
  @Path("/s3/{name}")
  @RequiresPermissions("nexus:blobstores:update")
  public void updateBlobStore(
      @Valid final S3BlobStoreApiModel request,
      @PathParam("name") final String blobStoreName) throws Exception
  {
    s3BlobStoreApiUpdateValidation.validateUpdateRequest(request, blobStoreName);

    if (isPasswordUntouched(request)) {
      //Did not update the password, just use the password we already have
      BlobStore currentS3Blobstore = blobStoreManager.get(blobStoreName);
      request.getBucketConfiguration().getBucketSecurity().setSecretAccessKey(
          currentS3Blobstore.getBlobStoreConfiguration().getAttributes().get("s3").get("secretAccessKey").toString());
    }

    try {
      final BlobStoreConfiguration blobStoreConfiguration = map(blobStoreManager.newConfiguration(), request);
      blobStoreManager.update(blobStoreConfiguration);
    }
    catch (Exception e) {
      throw new WebApplicationMessageException(INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  private boolean isPasswordUntouched(final S3BlobStoreApiModel request) {
    return request.getBucketConfiguration() != null && request.getBucketConfiguration().getBucketSecurity() != null &&
        PasswordPlaceholder.is(request.getBucketConfiguration().getBucketSecurity().getSecretAccessKey());
  }

  @GET
  @Override
  @RequiresAuthentication
  @Path("/s3/{name}")
  @RequiresPermissions("nexus:blobstores:read")
  public S3BlobStoreApiModel getBlobStore(@PathParam("name") final String blobStoreName) {
    return fetchBlobStoreConfiguration(blobStoreName)
        .orElseThrow(() -> BlobStoreResourceUtil.createBlobStoreNotFoundException(S3BlobStore.TYPE, blobStoreName));
  }

  private Optional<S3BlobStoreApiModel> fetchBlobStoreConfiguration(final String blobStoreName) {
    Optional<S3BlobStoreApiModel> result = ofNullable(blobStoreManager.get(blobStoreName))
        .map(BlobStore::getBlobStoreConfiguration)
        .map(this::ensureBlobStoreTypeIsS3)
        .map(S3BlobStoreApiConfigurationMapper::map);
    if (result.isPresent() && isAuthenticationDataPresent(result.get())) {
      result.get().getBucketConfiguration().getBucketSecurity().setSecretAccessKey(PasswordPlaceholder.get());
    }
    return result;
  }

  private boolean isAuthenticationDataPresent(final S3BlobStoreApiModel s3BlobStoreApiModel) {
    return s3BlobStoreApiModel.getBucketConfiguration().getBucketSecurity() != null &&
        s3BlobStoreApiModel.getBucketConfiguration().getBucketSecurity().getAccessKeyId() != null &&
        isNotEmpty(s3BlobStoreApiModel.getBucketConfiguration().getBucketSecurity().getAccessKeyId());
  }

  private BlobStoreConfiguration ensureBlobStoreTypeIsS3(final BlobStoreConfiguration configuration) {
    final String type = configuration.getType();
    if (!equalsIgnoreCase(TYPE, type)) {
      throw new WebApplicationMessageException(BAD_REQUEST,
          String.format(NOT_AN_S3_BLOB_STORE_MSG_FORMAT, configuration.getName()), APPLICATION_JSON);
    }
    return configuration;
  }

  @DELETE
  @RequiresAuthentication
  @Path("/s3")
  @RequiresPermissions("nexus:blobstores:delete")
  @ApiOperation(value = "Delete a blob store with an empty name", hidden = true)
  public Response deleteBlobStoreWithEmptyName() {
    String blobStoreName = "";
    try {
      BlobStore blobStore = blobStoreManager.get(blobStoreName);
      if (blobStore == null) {
        return Response.status(Response.Status.NOT_FOUND)
            .entity("Blob store not found")
            .build();
      }
      blobStoreManager.delete(blobStoreName);
      return Response.status(Response.Status.NO_CONTENT).build();
    }
    catch (Exception e) {
      throw new WebApplicationMessageException(BAD_REQUEST, e.getMessage());
    }
  }
}


