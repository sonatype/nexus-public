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
package org.sonatype.nexus.api.rest.selfhosted.blobstore.s3;

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

import org.sonatype.nexus.api.rest.common.blobstore.BlobStoreResourceUtil;
import org.sonatype.nexus.api.rest.common.blobstore.s3.PreSignedUrlNotAllowedException;
import org.sonatype.nexus.api.rest.common.blobstore.s3.S3BlobStoreApiConfigurationMapper;
import org.sonatype.nexus.api.rest.common.blobstore.s3.S3BlobStoreApiConstants;
import org.sonatype.nexus.api.rest.common.blobstore.s3.S3BlobStoreApiUpdateValidation;
import org.sonatype.nexus.api.rest.common.blobstore.s3.model.S3BlobStoreApiBucketConfiguration;
import org.sonatype.nexus.api.rest.common.blobstore.s3.model.S3BlobStoreApiBucketSecurity;
import org.sonatype.nexus.api.rest.common.blobstore.s3.model.S3BlobStoreApiModel;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.s3.internal.S3BlobStore;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.crypto.secrets.SecretsFactory;
import org.sonatype.nexus.rapture.PasswordPlaceholder;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.WebApplicationMessageException;

import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Optional.ofNullable;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.PAYMENT_REQUIRED;
import static javax.ws.rs.core.Response.status;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.sonatype.nexus.api.rest.common.blobstore.s3.S3BlobStoreApiModelMapper.map;

/**
 * REST API endpoints for creating, reading, updating and deleting an S3 blob store.
 *
 * @since 3.20
 */
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class S3BlobStoreApiResource
    implements Resource, S3BlobStoreApiResourceDoc
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private static final String S3_PRE_SIGNED_URL_PAYMENT_REQUIRED = "S3 pre-signed URL is a Pro only feature.";

  private final S3BlobStoreApiUpdateValidation s3BlobStoreApiUpdateValidation;

  private final BlobStoreManager blobStoreManager;

  private final SecretsFactory secretsFactory;

  private final ApplicationVersion applicationVersion;

  public S3BlobStoreApiResource(
      final BlobStoreManager blobStoreManager,
      final S3BlobStoreApiUpdateValidation validation,
      final SecretsFactory secretsFactory,
      final ApplicationVersion applicationVersion)
  {
    this.blobStoreManager = blobStoreManager;
    this.s3BlobStoreApiUpdateValidation = validation;
    this.secretsFactory = checkNotNull(secretsFactory);
    this.applicationVersion = checkNotNull(applicationVersion);
  }

  @POST
  @Override
  @RequiresAuthentication
  @Path("/s3")
  @RequiresPermissions("nexus:blobstores:create")
  public Response createBlobStore(@Valid final S3BlobStoreApiModel request) {
    try {
      s3BlobStoreApiUpdateValidation.validateCreateRequest(request);
      final BlobStoreConfiguration blobStoreConfiguration =
          map(blobStoreManager.newConfiguration(), request, applicationVersion);
      blobStoreManager.create(blobStoreConfiguration);
      return status(CREATED).build();
    }
    catch (PreSignedUrlNotAllowedException ex) {
      throw new WebApplicationMessageException(PAYMENT_REQUIRED, S3_PRE_SIGNED_URL_PAYMENT_REQUIRED);
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
    maybeFetchSecrets(request, blobStoreName);

    try {
      final BlobStoreConfiguration blobStoreConfiguration =
          map(blobStoreManager.newConfiguration(), request, applicationVersion);
      blobStoreManager.update(blobStoreConfiguration);
    }
    catch (PreSignedUrlNotAllowedException ex) {
      throw new WebApplicationMessageException(PAYMENT_REQUIRED, S3_PRE_SIGNED_URL_PAYMENT_REQUIRED);
    }
    catch (Exception e) {
      throw new WebApplicationMessageException(INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  private void maybeFetchSecrets(final S3BlobStoreApiModel request, final String blobStoreName) {
    BlobStore blobStore = blobStoreManager.get(blobStoreName);
    if (hasSecretAccessKeyPasswordPlaceholder(request)) {
      request.getBucketConfiguration()
          .getBucketSecurity()
          .setSecretAccessKey(decryptSecret(blobStore, S3BlobStore.SECRET_ACCESS_KEY_KEY));
    }

    if (hasSessionTokenPasswordPlaceholder(request)) {
      request.getBucketConfiguration()
          .getBucketSecurity()
          .setSessionToken(decryptSecret(blobStore, S3BlobStore.SESSION_TOKEN_KEY));
    }
  }

  private String decryptSecret(final BlobStore blobStore, final String secretKey) {
    String secretId = blobStore.getBlobStoreConfiguration()
        .getAttributes()
        .get(S3BlobStore.TYPE.toLowerCase())
        .get(secretKey)
        .toString();

    return new String(secretsFactory.from(secretId).decrypt());
  }

  private static boolean hasSecretAccessKeyPasswordPlaceholder(final S3BlobStoreApiModel request) {
    return Optional.ofNullable(request.getBucketConfiguration())
        .map(S3BlobStoreApiBucketConfiguration::getBucketSecurity)
        .map(S3BlobStoreApiBucketSecurity::getSecretAccessKey)
        .map(PasswordPlaceholder::is)
        .orElse(false);
  }

  private static boolean hasSessionTokenPasswordPlaceholder(final S3BlobStoreApiModel request) {
    return Optional.ofNullable(request.getBucketConfiguration())
        .map(S3BlobStoreApiBucketConfiguration::getBucketSecurity)
        .map(S3BlobStoreApiBucketSecurity::getSessionToken)
        .map(PasswordPlaceholder::is)
        .orElse(false);
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

      if (hasSessionToken(result.get())) {
        result.get().getBucketConfiguration().getBucketSecurity().setSessionToken(PasswordPlaceholder.get());
      }
    }
    return result;
  }

  private boolean isAuthenticationDataPresent(final S3BlobStoreApiModel s3BlobStoreApiModel) {
    return s3BlobStoreApiModel.getBucketConfiguration().getBucketSecurity() != null &&
        s3BlobStoreApiModel.getBucketConfiguration().getBucketSecurity().getAccessKeyId() != null &&
        isNotEmpty(s3BlobStoreApiModel.getBucketConfiguration().getBucketSecurity().getAccessKeyId());
  }

  private boolean hasSessionToken(final S3BlobStoreApiModel s3BlobStoreApiModel) {
    return s3BlobStoreApiModel.getBucketConfiguration().getBucketSecurity().getSessionToken() != null &&
        isNotEmpty(s3BlobStoreApiModel.getBucketConfiguration().getBucketSecurity().getSessionToken());
  }

  private BlobStoreConfiguration ensureBlobStoreTypeIsS3(final BlobStoreConfiguration configuration) {
    final String type = configuration.getType();
    if (!equalsIgnoreCase(S3BlobStore.TYPE, type)) {
      throw new WebApplicationMessageException(BAD_REQUEST,
          String.format(S3BlobStoreApiConstants.NOT_AN_S3_BLOB_STORE_MSG_FORMAT, configuration.getName()),
          APPLICATION_JSON);
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
