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
package org.sonatype.nexus.blobstore.restore.r.internal.orient;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.restore.RestoreBlobData;
import org.sonatype.nexus.blobstore.restore.orient.OrientBaseRestoreBlobStrategy;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.r.orient.OrientRRestoreFacet;
import org.sonatype.nexus.repository.r.RFormat;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Query;

import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkState;
import static org.eclipse.aether.util.StringUtils.isEmpty;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;

/**
 * @since 3.28
 */
@Named(RFormat.NAME)
@Singleton
public class OrientRRestoreBlobStrategy
    extends OrientBaseRestoreBlobStrategy<OrientRRestoreBlobData>
{
  @Inject
  public OrientRRestoreBlobStrategy(
      final NodeAccess nodeAccess,
      final RepositoryManager repositoryManager,
      final BlobStoreManager blobStoreManager,
      final DryRunPrefix dryRunPrefix)
  {
    super(nodeAccess, repositoryManager, blobStoreManager, dryRunPrefix);
  }

  @Override
  protected OrientRRestoreBlobData createRestoreData(final RestoreBlobData restoreBlobData) {
    checkState(!isEmpty(restoreBlobData.getBlobName()), "Blob name cannot be empty");

    return new OrientRRestoreBlobData(restoreBlobData);
  }

  @Override
  protected boolean canAttemptRestore(@Nonnull final OrientRRestoreBlobData rRestoreBlobData) {
    Repository repository = getRepository(rRestoreBlobData);
    Optional<OrientRRestoreFacet> rRestoreFacetFacet = repository.optionalFacet(OrientRRestoreFacet.class);

    if (!rRestoreFacetFacet.isPresent()) {
      log.warn("Skipping as R Restore Facet not found on repository: {}", repository.getName());
      return false;
    }
    return true;
  }

  @Override
  protected String getAssetPath(@Nonnull final OrientRRestoreBlobData rRestoreBlobData) {
    return rRestoreBlobData.getBlobData().getBlobName();
  }

  @Override
  protected boolean assetExists(@Nonnull final OrientRRestoreBlobData rRestoreBlobData) {
    OrientRRestoreFacet facet = getRestoreFacet(rRestoreBlobData);

    return facet.assetExists(getAssetPath(rRestoreBlobData));
  }

  @Override
  protected void createAssetFromBlob(
      @Nonnull final AssetBlob assetBlob,
      @Nonnull final OrientRRestoreBlobData rRestoreBlobData)
      throws IOException
  {
    OrientRRestoreFacet facet = getRestoreFacet(rRestoreBlobData);
    final String path = getAssetPath(rRestoreBlobData);

    facet.restore(assetBlob, path);
  }

  @Nonnull
  @Override
  protected List<HashAlgorithm> getHashAlgorithms() {
    return ImmutableList.of(SHA1);
  }

  @Override
  protected boolean componentRequired(final OrientRRestoreBlobData data) {
    OrientRRestoreFacet facet = getRestoreFacet(data);
    final String path = data.getBlobData().getBlobName();

    return facet.componentRequired(path);
  }

  @Override
  protected Query getComponentQuery(final OrientRRestoreBlobData data) throws IOException {
    OrientRRestoreFacet facet = getRestoreFacet(data);
    RestoreBlobData blobData = data.getBlobData();
    Map<String, String> attributes;
    try (InputStream inputStream = blobData.getBlob().getInputStream()) {
      attributes = facet.extractComponentAttributesFromArchive(blobData.getBlobName(), inputStream);
    }

    return facet.getComponentQuery(attributes);
  }

  @Override
  protected Repository getRepository(@Nonnull final OrientRRestoreBlobData data) {
    return data.getBlobData().getRepository();
  }

  private OrientRRestoreFacet getRestoreFacet(@Nonnull final OrientRRestoreBlobData rRestoreBlobData) {
    final Repository repository = getRepository(rRestoreBlobData);

    return repository.facet(OrientRRestoreFacet.class);
  }
}
