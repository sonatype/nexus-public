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
package org.sonatype.nexus.coreui.internal.blobstore;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.BlobStoreDescriptor;
import org.sonatype.nexus.blobstore.BlobStoreDescriptorProvider;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.blobstore.group.BlobStoreGroup;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuota;
import org.sonatype.nexus.blobstore.file.FileBlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper;
import org.sonatype.nexus.repository.blobstore.BlobStoreConfigurationStore;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.rest.Resource;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.coreui.internal.blobstore.BlobStoreInternalResource.RESOURCE_PATH;

/**
 * @since 3.30
 */
@Named
@Singleton
@Path(RESOURCE_PATH)
public class BlobStoreInternalResource
    extends ComponentSupport
    implements Resource
{
  static final String RESOURCE_PATH = "/internal/ui/blobstores";

  public static final String GOOGLE_CONFIG = "google cloud storage";

  public static final String GOOGLE_TYPE = "google";

  public static final String GOOGLE_BUCKET_KEY = "bucketName";

  public static final String AZURE_CONFIG = "azure cloud storage";

  public static final String AZURE_TYPE = "azure";

  public static final String CONTAINER_NAME = "containerName";

  private final BlobStoreManager blobStoreManager;

  private final BlobStoreConfigurationStore store;

  private final BlobStoreDescriptorProvider blobStoreDescriptorProvider;

  private final List<BlobStoreQuotaTypesUIResponse> blobStoreQuotaTypes;

  private final RepositoryManager repositoryManager;

  private static final Logger logger = LoggerFactory.getLogger(BlobStoreInternalResource.class);

  @Inject
  public BlobStoreInternalResource(
      final BlobStoreManager blobStoreManager,
      final BlobStoreConfigurationStore store,
      final BlobStoreDescriptorProvider blobStoreDescriptorProvider,
      final Map<String, BlobStoreQuota> quotaFactories,
      final RepositoryManager repositoryManager)
  {
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.store = checkNotNull(store);
    this.blobStoreDescriptorProvider = checkNotNull(blobStoreDescriptorProvider);
    this.blobStoreQuotaTypes = quotaFactories.entrySet().stream()
        .map(BlobStoreQuotaTypesUIResponse::new).collect(toList());
    this.repositoryManager = checkNotNull(repositoryManager);
  }

  @RequiresAuthentication
  @RequiresPermissions("nexus:blobstores:read")
  @GET
  public List<BlobStoreUIResponse> listBlobStores() {

    return store.list().stream()
        .map(configuration -> {
          String blobStoreType = configuration.getType();
          BlobStoreDescriptor blobStoreDescriptor = Optional.ofNullable(blobStoreDescriptorProvider.get())
              .map(it -> it.get(blobStoreType))
              .orElse(null);
          if (blobStoreDescriptor == null) {
            return null;
          }
          String typeId = blobStoreDescriptor.getId();

            final String path = getPath(typeId.toLowerCase(), configuration);
            BlobStoreMetrics metrics = Optional.ofNullable(blobStoreManager.get(configuration.getName()))
                .map(BlobStoreInternalResource::getBlobStoreMetrics)
                .orElse(null);
            return new BlobStoreUIResponse(typeId, configuration, metrics, path);
        })
        .filter(Objects::nonNull)
        .collect(toList());
  }

  // If a blobstore hasn't started due to an error we still want to return it from the api.
  // To achieve this, we use a null metrics object which will show the BlobStore as unavailable.
  private static BlobStoreMetrics getBlobStoreMetrics(final BlobStore bs) {
    if (bs.isGroupable()) {
      return bs.isStarted() ? bs.getMetrics() : null;
    }
    else {
      return ((BlobStoreGroup) bs).getMembers().stream()
          .map(BlobStore::isStarted)
          .reduce(Boolean::logicalAnd)
          .orElse(false) ? bs.getMetrics() : null;
    }
  }

  private static String getPath(final String typeId, BlobStoreConfiguration configuration) {
    if (typeId.equals(FileBlobStore.TYPE.toLowerCase())) {
      return configuration.attributes(FileBlobStore.CONFIG_KEY).get(FileBlobStore.PATH_KEY, String.class);
    }
    else if (typeId.equals(S3BlobStoreConfigurationHelper.CONFIG_KEY)) {
      return S3BlobStoreConfigurationHelper.getBucketPrefix(configuration) + configuration
         .attributes(S3BlobStoreConfigurationHelper.CONFIG_KEY).get(S3BlobStoreConfigurationHelper.BUCKET_KEY, String.class);
    }
    else if (typeId.equals(AZURE_TYPE)) {
      return configuration.attributes(AZURE_CONFIG).get(CONTAINER_NAME, String.class);
    }
    else if (typeId.equals(BlobStoreGroup.TYPE.toLowerCase())) {
      return "N/A";
    }
    else if (typeId.equals(GOOGLE_TYPE)) {
      return configuration.attributes(GOOGLE_CONFIG).get(GOOGLE_BUCKET_KEY, String.class);
    }
    logger.warn("blob store type {} unknown, defaulting to N/A for path", typeId);
    return "N/A";
  }

  @RequiresAuthentication
  @RequiresPermissions("nexus:blobstores:read")
  @GET
  @Path("/types")
  public List<BlobStoreTypesUIResponse> listBlobStoreTypes() {
    return blobStoreDescriptorProvider.get().entrySet().stream().filter(entry -> entry.getValue().isEnabled())
        .map(BlobStoreTypesUIResponse::new).collect(toList());
  }

  @RequiresAuthentication
  @RequiresPermissions("nexus:blobstores:read")
  @GET
  @Path("/usage/{name}")
  public BlobStoreUsageUIResponse getBlobStoreUsage(@PathParam("name") final String name) {
    long repositoryUsage = repositoryManager.blobstoreUsageCount(name);
    long blobStoreUsage = blobStoreManager.blobStoreUsageCount(name);

    return new BlobStoreUsageUIResponse(repositoryUsage, blobStoreUsage);
  }

  @RequiresAuthentication
  @RequiresPermissions("nexus:blobstores:read")
  @GET
  @Path("/quotaTypes")
  public List<BlobStoreQuotaTypesUIResponse> listQuotaTypes() {
    return blobStoreQuotaTypes;
  }
}
