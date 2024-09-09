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
package org.sonatype.nexus.repository.content.facet;

import java.util.Optional;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.move.RepositoryMoveService;
import org.sonatype.nexus.repository.search.normalize.VersionNormalizerService;
import org.sonatype.nexus.repository.storage.BlobMetadataStorage;
import org.sonatype.nexus.security.ClientInfoProvider;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Internal dependencies injected into a repository's {@link ContentFacet}.
 *
 * @since 3.24
 */
@Named
@Singleton
public class ContentFacetDependencies
{
  private final BlobStoreManager blobStoreManager;

  private final DataSessionSupplier dataSessionSupplier;

  private final ConstraintViolationFactory constraintViolationFactory;

  private final ClientInfoProvider clientInfoProvider;

  private final NodeAccess nodeAccess;

  private final AssetBlobValidators assetBlobValidators;

  private final BlobMetadataStorage blobMetadataStorage;

  private final VersionNormalizerService versionNormalizerService;

  private final Optional<RepositoryMoveService> maybeMoveService;

  @Inject
  public ContentFacetDependencies(final BlobStoreManager blobStoreManager,
                                  final DataSessionSupplier dataSessionSupplier,
                                  final ConstraintViolationFactory constraintViolationFactory,
                                  final ClientInfoProvider clientInfoProvider,
                                  final NodeAccess nodeAccess,
                                  final AssetBlobValidators assetBlobValidators,
                                  final BlobMetadataStorage blobMetadataStorage,
                                  final VersionNormalizerService versionNormalizerService,
                                  @Nullable final RepositoryMoveService moveService)
  {
    this.blobStoreManager = checkNotNull(blobStoreManager);
    this.dataSessionSupplier = checkNotNull(dataSessionSupplier);
    this.constraintViolationFactory = checkNotNull(constraintViolationFactory);
    this.clientInfoProvider = checkNotNull(clientInfoProvider);
    this.nodeAccess = checkNotNull(nodeAccess);
    this.assetBlobValidators = checkNotNull(assetBlobValidators);
    this.blobMetadataStorage = checkNotNull(blobMetadataStorage);
    this.versionNormalizerService = versionNormalizerService;
    this.maybeMoveService = Optional.ofNullable(moveService);
  }

  public BlobStoreManager getBlobStoreManager() {
    return blobStoreManager;
  }

  public DataSessionSupplier getDataSessionSupplier() {
    return dataSessionSupplier;
  }

  public ConstraintViolationFactory getConstraintViolationFactory() {
    return constraintViolationFactory;
  }

  public ClientInfoProvider getClientInfoProvider() {
    return clientInfoProvider;
  }

  public NodeAccess getNodeAccess() {
    return nodeAccess;
  }

  public AssetBlobValidators getAssetBlobValidators() {
    return assetBlobValidators;
  }

  public BlobMetadataStorage getBlobMetadataStorage() {
    return blobMetadataStorage;
  }

  public VersionNormalizerService getVersionNormalizerService() {
    return versionNormalizerService;
  }

  public Optional<RepositoryMoveService> getMoveService() {
    return maybeMoveService;
  }
}
