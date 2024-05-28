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
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.BlobStoreDescriptor;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.blobstore.group.BlobStoreGroup;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuota;
import org.sonatype.nexus.repository.blobstore.BlobStoreConfigurationStore;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.rest.Resource;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.coreui.internal.blobstore.BlobStoreUIResource.RESOURCE_PATH;

/**
 * @since 3.30
 */
@Named
@Singleton
@Path(RESOURCE_PATH)
public class BlobStoreUIResource
    extends ComponentSupport
    implements Resource
{
  static final String RESOURCE_PATH = "/internal/ui/blobstores";

  private final BlobStoreManager blobStoreManager;

  private final BlobStoreConfigurationStore store;

  private final Map<String, BlobStoreDescriptor> blobStoreDescriptors;

  private final List<BlobStoreQuotaTypesUIResponse> blobStoreQuotaTypes;

  private final RepositoryManager repositoryManager;

  @Inject
  public BlobStoreUIResource(
      final BlobStoreManager blobStoreManager,
      final BlobStoreConfigurationStore store,
      final Map<String, BlobStoreDescriptor> blobStoreDescriptors,
      final Map<String, BlobStoreQuota> quotaFactories,
      final RepositoryManager repositoryManager)
  {
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.store = checkNotNull(store);
    this.blobStoreDescriptors = checkNotNull(blobStoreDescriptors);
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
            final String typeId = blobStoreDescriptors.get(configuration.getType()).getId();
            BlobStoreMetrics metrics = Optional.ofNullable(blobStoreManager.get(configuration.getName()))
                .map(BlobStoreUIResource::getBlobStoreMetrics)
                .orElse(null);
            return new BlobStoreUIResponse(typeId, configuration, metrics);
        }).collect(toList());
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

  @RequiresAuthentication
  @RequiresPermissions("nexus:blobstores:read")
  @GET
  @Path("/types")
  public List<BlobStoreTypesUIResponse> listBlobStoreTypes() {
    return blobStoreDescriptors.entrySet().stream().filter(entry -> entry.getValue().isEnabled())
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


