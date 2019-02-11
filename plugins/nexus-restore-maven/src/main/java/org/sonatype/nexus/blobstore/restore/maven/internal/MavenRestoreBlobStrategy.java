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
package org.sonatype.nexus.blobstore.restore.maven.internal;

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
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.Coordinates;
import org.sonatype.nexus.repository.maven.MavenPathParser;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.transaction.TransactionalStoreMetadata;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_GROUP;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_VERSION;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

/**
 * @since 3.4
 */
@Named("maven2")
@Singleton
public class MavenRestoreBlobStrategy
    extends BaseRestoreBlobStrategy<MavenRestoreBlobData>
{
  private final MavenPathParser mavenPathParser;

  @Inject
  public MavenRestoreBlobStrategy(final MavenPathParser mavenPathParser,
                                  final NodeAccess nodeAccess,
                                  final RepositoryManager repositoryManager,
                                  final BlobStoreManager blobStoreManager,
                                  final DryRunPrefix dryRunPrefix)
  {
    super(nodeAccess, repositoryManager, blobStoreManager, dryRunPrefix);
    this.mavenPathParser = checkNotNull(mavenPathParser);
  }

  @Override
  protected MavenRestoreBlobData createRestoreData(final RestoreBlobData blobData) {
    return new MavenRestoreBlobData(blobData, mavenPathParser.parsePath(blobData.getBlobName()));
  }

  @Override
  protected boolean canAttemptRestore(@Nonnull final MavenRestoreBlobData data) {
    MavenPath mavenPath = data.getMavenPath();
    Repository repository = data.getBlobData().getRepository();

    if (mavenPath.getCoordinates() == null && !mavenPathParser.isRepositoryMetadata(mavenPath)) {
      log.warn("Skipping as no maven coordinates found and is not maven metadata");
      return false;
    }

    Optional<MavenFacet> mavenFacet = repository.optionalFacet(MavenFacet.class);

    if (!mavenFacet.isPresent()) {
      log.warn("Skipping as Maven Facet not found on repository: {}", repository.getName());
      return false;
    }

    return true;
  }

  @Override
  @Nonnull
  protected List<HashAlgorithm> getHashAlgorithms() {
    return newArrayList(MD5, SHA1);
  }

  @Override
  protected String getAssetPath(@Nonnull final MavenRestoreBlobData data) {
    return data.getMavenPath().getPath();
  }

  @Override
  @TransactionalTouchBlob
  protected boolean assetExists(@Nonnull final MavenRestoreBlobData data) throws IOException {
    return data.getBlobData().getRepository().facet(MavenFacet.class).get(data.getMavenPath()) != null;
  }

  @Override
  protected boolean componentRequired(@Nonnull final MavenRestoreBlobData data) throws IOException {
    MavenPath path = data.getMavenPath();
    return !(mavenPathParser.isRepositoryIndex(path) || mavenPathParser.isRepositoryMetadata(path));
  }

  @Override
  protected Query getComponentQuery(@Nonnull final MavenRestoreBlobData data) {
    Coordinates coordinates = data.getMavenPath().getCoordinates();
    if (coordinates != null) {
      return Query.builder()
          .where(P_GROUP).eq(coordinates.getGroupId())
          .and(P_NAME).eq(coordinates.getArtifactId())
          .and(P_VERSION).eq(coordinates.getVersion())
          .build();
    }
    return null;
  }

  @Override
  @TransactionalStoreMetadata
  protected void createAssetFromBlob(@Nonnull final AssetBlob assetBlob, @Nonnull final MavenRestoreBlobData data)
      throws IOException
  {
    data.getBlobData().getRepository().facet(MavenFacet.class).put(data.getMavenPath(), assetBlob, null);
  }

  @Override
  protected Repository getRepository(@Nonnull final MavenRestoreBlobData data) {
    return data.getBlobData().getRepository();
  }
}
