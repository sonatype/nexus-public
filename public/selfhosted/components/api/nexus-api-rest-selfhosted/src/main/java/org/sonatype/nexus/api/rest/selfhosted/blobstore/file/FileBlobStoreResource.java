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
package org.sonatype.nexus.api.rest.selfhosted.blobstore.file;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import org.sonatype.nexus.api.rest.common.blobstore.BlobStoreResourceUtil;
import org.sonatype.nexus.api.rest.common.blobstore.file.model.FileBlobStoreApiCreateRequest;
import org.sonatype.nexus.api.rest.common.blobstore.file.model.FileBlobStoreApiModel;
import org.sonatype.nexus.api.rest.common.blobstore.file.model.FileBlobStoreApiUpdateRequest;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.file.FileBlobStore;

import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.validation.Validate;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

/**
 * @since 3.19
 */
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class FileBlobStoreResource
    implements Resource, FileBlobStoreResourceDoc
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private BlobStoreManager blobStoreManager;

  @Autowired
  public FileBlobStoreResource(final BlobStoreManager blobStoreManager) {
    this.blobStoreManager = checkNotNull(blobStoreManager);
  }

  @Override
  @RequiresAuthentication
  @RequiresPermissions("nexus:blobstores:create")
  @POST
  @Path("/file")
  @Validate
  public void createFileBlobStore(@Valid final FileBlobStoreApiCreateRequest request) throws Exception {
    BlobStoreConfiguration configuration = request.toBlobStoreConfiguration(blobStoreManager.newConfiguration());

    if (blobStoreManager.exists(request.getName())) {
      throw new ValidationErrorsException("name", "Name is already used, must be unique (ignoring case)");
    }

    blobStoreManager.create(configuration);
  }

  @Override
  @RequiresAuthentication
  @RequiresPermissions("nexus:blobstores:update")
  @PUT
  @Path("/file/{name}")
  @Validate
  public void updateFileBlobStore(
      @PathParam("name") final String name,
      @Valid final FileBlobStoreApiUpdateRequest request) throws Exception
  {
    // Confirm that the blobstore name and type are the expected name and type
    getBlobStoreConfiguration(name);

    BlobStoreConfiguration configuration = request.toBlobStoreConfiguration(blobStoreManager.newConfiguration());
    configuration.setName(name);

    blobStoreManager.update(configuration);
  }

  @Override
  @RequiresAuthentication
  @RequiresPermissions("nexus:blobstores:read")
  @GET
  @Path("/file/{name}")
  public FileBlobStoreApiModel getFileBlobStoreConfiguration(@PathParam("name") final String name) {
    BlobStoreConfiguration configuration = getBlobStoreConfiguration(name);

    return new FileBlobStoreApiModel(configuration);
  }

  private BlobStoreConfiguration getBlobStoreConfiguration(final String name) {
    BlobStoreConfiguration configuration = Optional.ofNullable(blobStoreManager.get(name))
        .map(BlobStore::getBlobStoreConfiguration)
        .orElseThrow(() -> BlobStoreResourceUtil.createBlobStoreNotFoundException(FileBlobStore.TYPE, name));

    if (!configuration.getType().equals(FileBlobStore.TYPE)) {
      throw new WebApplicationMessageException(
          BAD_REQUEST,
          "\"Unable to read non-file blob store configuration (type was " + configuration.getType() + ")\"",
          APPLICATION_JSON);
    }
    return configuration;
  }
}
