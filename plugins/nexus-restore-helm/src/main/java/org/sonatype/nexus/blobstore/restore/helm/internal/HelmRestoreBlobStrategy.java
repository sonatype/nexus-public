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
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.repository.helm.datastore.HelmRestoreFacet;
import org.sonatype.repository.helm.internal.HelmFormat;

import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_ENABLED;

/**
 * @since 3.next
 */
@FeatureFlag(name = DATASTORE_ENABLED)
@Named(HelmFormat.NAME)
@Singleton
public class HelmRestoreBlobStrategy
    extends BaseRestoreBlobStrategy<HelmRestoreBlobData>
{
  private final RepositoryManager repositoryManager;

  @Inject
  protected HelmRestoreBlobStrategy(final DryRunPrefix dryRunPrefix,
                                    final RepositoryManager repositoryManager)
  {
    super(dryRunPrefix);
    this.repositoryManager = repositoryManager;
  }

  @Override
  public void after(final boolean updateAssets, final Repository repository) {
    // no-op
  }

  @Override
  protected boolean canAttemptRestore(@Nonnull final HelmRestoreBlobData data) {
    Repository repository = data.getRepository();
    Optional<HelmRestoreFacet> maybeHelmRestoreFacet = repository.optionalFacet(HelmRestoreFacet.class);

    if (!maybeHelmRestoreFacet.isPresent()) {
      log.warn("Skipping as Helm Restore Facet not found on repository: {}", repository.getName());
      return false;
    }

    HelmRestoreFacet helmRestoreFacet = maybeHelmRestoreFacet.get();
    return helmRestoreFacet.isRestorable(getAssetPath(data));
  }

  @Override
  protected void createAssetFromBlob(final Blob blob, final HelmRestoreBlobData data) throws IOException {
    Repository repository = data.getRepository();
    HelmRestoreFacet helmRestoreFacet = repository.facet(HelmRestoreFacet.class);
    String path = getAssetPath(data);
    helmRestoreFacet.restore(blob, path);
  }

  @Override
  protected String getAssetPath(@Nonnull final HelmRestoreBlobData data) {
    return data.getBlobName();
  }

  @Override
  protected HelmRestoreBlobData createRestoreData(final Properties properties,
                                                  final Blob blob,
                                                  final BlobStore blobStore)
  {
    return new HelmRestoreBlobData(blob, properties, blobStore, repositoryManager);
  }

  @Override
  protected boolean isComponentRequired(final HelmRestoreBlobData data) {
    return true;
  }
}
