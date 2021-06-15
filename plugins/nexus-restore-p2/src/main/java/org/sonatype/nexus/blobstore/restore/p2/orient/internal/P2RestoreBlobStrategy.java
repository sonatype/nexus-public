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
package org.sonatype.nexus.blobstore.restore.p2.orient.internal;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.restore.RestoreBlobData;
import org.sonatype.nexus.blobstore.restore.orient.OrientBaseRestoreBlobStrategy;
import org.sonatype.nexus.blobstore.restore.p2.internal.P2RestoreBlobData;
import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.p2.internal.P2Format;
import org.sonatype.nexus.repository.p2.orient.P2RestoreFacet;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Query;

import static com.google.common.base.Preconditions.checkState;
import static org.eclipse.aether.util.StringUtils.isEmpty;
import static org.sonatype.nexus.common.app.FeatureFlags.ORIENT_ENABLED;

/**
 * @since 0.next
 */
@FeatureFlag(name = ORIENT_ENABLED)
@Priority(Integer.MAX_VALUE)
@Named(P2Format.NAME)
@Singleton
public class P2RestoreBlobStrategy
    extends OrientBaseRestoreBlobStrategy<P2RestoreBlobData>
{
  @Inject
  public P2RestoreBlobStrategy(final NodeAccess nodeAccess,
                               final RepositoryManager repositoryManager,
                               final BlobStoreManager blobStoreManager,
                               final DryRunPrefix dryRunPrefix)
  {
    super(nodeAccess, repositoryManager, blobStoreManager, dryRunPrefix);
  }

  @Override
  protected P2RestoreBlobData createRestoreData(final RestoreBlobData restoreBlobData) {
    checkState(!isEmpty(restoreBlobData.getBlobName()), "Blob name cannot be empty");

    return new P2RestoreBlobData(restoreBlobData);
  }

  @Override
  protected boolean canAttemptRestore(@Nonnull final P2RestoreBlobData p2RestoreBlobData) {
    Repository repository = getRepository(p2RestoreBlobData);
    Optional<P2RestoreFacet> p2RestoreFacetFacet = repository.optionalFacet(P2RestoreFacet.class);

    if (!p2RestoreFacetFacet.isPresent()) {
      log.warn("Skipping as P2 Restore Facet not found on repository: {}", repository.getName());
      return false;
    }
    return true;
  }

  @Override
  protected String getAssetPath(
      @Nonnull final P2RestoreBlobData p2RestoreBlobData)
  {
    return p2RestoreBlobData.getBlobData().getBlobName();
  }

  @Override
  protected boolean assetExists(@Nonnull final P2RestoreBlobData p2RestoreBlobData) throws IOException {
    P2RestoreFacet facet = getRestoreFacet(p2RestoreBlobData);
    return facet.assetExists(getAssetPath(p2RestoreBlobData));
  }

  @Nonnull
  @Override
  protected List<HashAlgorithm> getHashAlgorithms() {
    return HashAlgorithm.ALL_HASH_ALGORITHMS.values().stream().collect(Collectors.toList());
  }

  @Override
  protected boolean componentRequired(final P2RestoreBlobData data) {
    P2RestoreFacet facet = getRestoreFacet(data);
    final String path = data.getBlobData().getBlobName();

    return facet.componentRequired(path);
  }

  @Override
  protected Query getComponentQuery(final P2RestoreBlobData data) throws IOException {
    P2RestoreFacet facet = getRestoreFacet(data);
    RestoreBlobData blobData = data.getBlobData();
    return facet.getComponentQuery(blobData.getBlob(), blobData.getBlobName(), blobData.getBlobStore().getBlobStoreConfiguration().getName());
  }

  @Override
  protected Repository getRepository(@Nonnull final P2RestoreBlobData data) {
    return data.getBlobData().getRepository();
  }

  @Override
  protected void createAssetFromBlob(@Nonnull final AssetBlob assetBlob,
                                     @Nonnull final
                                     P2RestoreBlobData p2RestoreBlobData)
      throws IOException
  {
    P2RestoreFacet facet = getRestoreFacet(p2RestoreBlobData);
    final String path = getAssetPath(p2RestoreBlobData);

    facet.restore(assetBlob, path);
  }

  private P2RestoreFacet getRestoreFacet(@Nonnull final P2RestoreBlobData p2RestoreBlobData) {
    final Repository repository = getRepository(p2RestoreBlobData);
    return repository.facet(P2RestoreFacet.class);
  }
}
