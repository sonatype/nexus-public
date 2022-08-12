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
import java.util.Optional;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.restore.datastore.BaseRestoreBlobStrategy;
import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPathParser;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.view.payloads.DetachedBlobPayload;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_ENABLED;

/**
 * @since 3.29
 */
@FeatureFlag(name = DATASTORE_ENABLED)
@Named(Maven2Format.NAME)
@Singleton
public class MavenRestoreBlobStrategy
    extends BaseRestoreBlobStrategy<MavenRestoreBlobData>
{
  private final RepositoryManager repositoryManager;

  private final MavenPathParser mavenPathParser;

  @Inject
  protected MavenRestoreBlobStrategy(
      final DryRunPrefix dryRunPrefix,
      final RepositoryManager repositoryManager,
      final MavenPathParser mavenPathParser)
  {
    super(dryRunPrefix);
    this.repositoryManager = checkNotNull(repositoryManager);
    this.mavenPathParser = checkNotNull(mavenPathParser);
  }

  @Override
  protected boolean canAttemptRestore(@Nonnull final MavenRestoreBlobData data) {
    MavenPath mavenPath = data.getMavenPath();
    Repository repository = data.getRepository();

    if (mavenPath.getCoordinates() == null && !mavenPathParser.isRepositoryMetadata(mavenPath)) {
      log.warn(
          "Skipping blob in repository named {}, because no maven coordinates found for blob named {} in blob store named {} and the blob not maven metadata",
          repository.getName(),
          data.getBlobName(),
          data.getBlobStore().getBlobStoreConfiguration().getName());
      return false;
    }

    Optional<MavenContentFacet> mavenFacet = repository.optionalFacet(MavenContentFacet.class);

    if (!mavenFacet.isPresent()) {
      if (log.isWarnEnabled()) {
        log.warn("Skipping as Maven Content Facet not found on repository: {}", repository.getName());
      }
      return false;
    }

    return true;
  }

  @Override
  protected void createAssetFromBlob(final Blob assetBlob, final MavenRestoreBlobData data) throws IOException {
    MavenContentFacet mavenFacet = data.getRepository().facet(MavenContentFacet.class);
    mavenFacet.put(data.getMavenPath(), new DetachedBlobPayload(assetBlob));
  }

  @Override
  protected String getAssetPath(@Nonnull final MavenRestoreBlobData data) {
    return data.getMavenPath().getPath();
  }

  @Override
  protected MavenRestoreBlobData createRestoreData(
      final Properties properties,
      final Blob blob,
      final BlobStore blobStore)
  {
    return new MavenRestoreBlobData(blob, properties, blobStore, repositoryManager, mavenPathParser);
  }

  @Override
  protected boolean isComponentRequired(final MavenRestoreBlobData data) {
    MavenPath path = data.getMavenPath();
    return !(mavenPathParser.isRepositoryIndex(path) || mavenPathParser.isRepositoryMetadata(path));
  }

  @Override
  public void after(final boolean updateAssets, final Repository repository) {
    //no-op
  }
}
