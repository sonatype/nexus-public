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
package org.sonatype.nexus.blobstore.restore.helm.internal;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.restore.BaseRestoreBlobStrategy;
import org.sonatype.nexus.blobstore.restore.RestoreBlobData;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.repository.helm.HelmAttributes;
import org.sonatype.repository.helm.HelmRestoreFacet;
import org.sonatype.repository.helm.internal.HelmFormat;

import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkState;
import static org.eclipse.aether.util.StringUtils.isEmpty;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA256;

/**
 * @since 3.next
 */
@Named(HelmFormat.NAME)
@Singleton
public class HelmRestoreBlobStrategy
    extends BaseRestoreBlobStrategy<HelmRestoreBlobData>
{
  @Inject
  public HelmRestoreBlobStrategy(final NodeAccess nodeAccess,
                                 final RepositoryManager repositoryManager,
                                 final BlobStoreManager blobStoreManager,
                                 final DryRunPrefix dryRunPrefix)
  {
    super(nodeAccess, repositoryManager, blobStoreManager, dryRunPrefix);
  }

  @Override
  protected HelmRestoreBlobData createRestoreData(final RestoreBlobData restoreBlobData) {
    checkState(!isEmpty(restoreBlobData.getBlobName()), "Blob name cannot be empty");

    return new HelmRestoreBlobData(restoreBlobData);
  }

  @Override
  protected boolean canAttemptRestore(@Nonnull final HelmRestoreBlobData helmRestoreBlobData) {
    Repository repository = getRepository(helmRestoreBlobData);
    Optional<HelmRestoreFacet> helmRestoreFacetFacet = repository.optionalFacet(HelmRestoreFacet.class);

    if (!helmRestoreFacetFacet.isPresent()) {
      log.warn("Skipping as Helm Restore Facet not found on repository: {}", repository.getName());
      return false;
    }
    return true;
  }

  @Override
  protected String getAssetPath(@Nonnull final HelmRestoreBlobData HelmRestoreBlobData) {
    return HelmRestoreBlobData.getBlobData().getBlobName();
  }

  @Override
  protected boolean assetExists(@Nonnull final HelmRestoreBlobData helmRestoreBlobData) {
    HelmRestoreFacet facet = getRestoreFacet(helmRestoreBlobData);
    return facet.assetExists(getAssetPath(helmRestoreBlobData));
  }

  @Override
  protected void createAssetFromBlob(@Nonnull final AssetBlob assetBlob,
                                     @Nonnull final HelmRestoreBlobData HelmRestoreBlobData)
      throws IOException
  {
    HelmRestoreFacet facet = getRestoreFacet(HelmRestoreBlobData);
    final String path = getAssetPath(HelmRestoreBlobData);

    facet.restore(assetBlob, path);
  }

  @Nonnull
  @Override
  protected List<HashAlgorithm> getHashAlgorithms() {
    return ImmutableList.of(SHA1, SHA256);
  }

  @Override
  protected boolean componentRequired(final HelmRestoreBlobData data) {
    HelmRestoreFacet facet = getRestoreFacet(data);
    final String path = data.getBlobData().getBlobName();
    return facet.componentRequired(path);
  }

  @Override
  protected Query getComponentQuery(final HelmRestoreBlobData data) throws IOException {
    HelmRestoreFacet facet = getRestoreFacet(data);
    RestoreBlobData blobData = data.getBlobData();
    HelmAttributes attributes = facet.extractComponentAttributesFromArchive(blobData.getBlobName(), blobData.getBlob().getInputStream());
    return facet.getComponentQuery(attributes);
  }

  @Override
  protected Repository getRepository(@Nonnull final HelmRestoreBlobData data) {
    return data.getBlobData().getRepository();
  }

  private HelmRestoreFacet getRestoreFacet(@Nonnull final HelmRestoreBlobData HelmRestoreBlobData) {
    final Repository repository = getRepository(HelmRestoreBlobData);

    return repository.facet(HelmRestoreFacet.class);
  }
}
