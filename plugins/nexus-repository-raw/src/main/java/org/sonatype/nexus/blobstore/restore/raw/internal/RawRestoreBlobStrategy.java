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
package org.sonatype.nexus.blobstore.restore.raw.internal;

import java.io.IOException;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.restore.datastore.BaseRestoreBlobStrategy;
import org.sonatype.nexus.blobstore.restore.datastore.DataStoreRestoreBlobData;
import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.content.raw.RawContentFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.view.payloads.DetachedBlobPayload;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_ENABLED;

/**
 * @since 3.29
 */
@Named("raw")
@FeatureFlag(name = DATASTORE_ENABLED)
@Singleton
public class RawRestoreBlobStrategy
    extends BaseRestoreBlobStrategy<DataStoreRestoreBlobData>
{
  private final RepositoryManager repositoryManager;

  @Inject
  public RawRestoreBlobStrategy(
      final DryRunPrefix dryRunPrefix,
      final RepositoryManager repositoryManager)
  {
    super(dryRunPrefix);
    this.repositoryManager = checkNotNull(repositoryManager);
  }

  @Override
  protected boolean canAttemptRestore(@Nonnull final DataStoreRestoreBlobData data)
  {
    Repository repository = data.getRepository();

    if (repository.optionalFacet(RawContentFacet.class).isPresent()) {
      return true;
    }
    else {
      log.warn("Skipping as Raw Facet not found on repository: {}", repository.getName());
      return false;
    }
  }

  @Override
  protected void createAssetFromBlob(final Blob assetBlob, final DataStoreRestoreBlobData data) throws IOException
  {
    RawContentFacet rawContentFacet = data.getRepository().facet(RawContentFacet.class);
    rawContentFacet.put(data.getBlobName(), new DetachedBlobPayload(assetBlob));
  }

  @Override
  protected String getAssetPath(@Nonnull final DataStoreRestoreBlobData data) {
    return data.getBlobName();
  }

  @Override
  protected DataStoreRestoreBlobData createRestoreData(
      final Properties properties,
      final Blob blob,
      final BlobStore blobStore)
  {
    return new DataStoreRestoreBlobData(blob, properties, blobStore, repositoryManager);
  }

  @Override
  protected boolean isComponentRequired(final DataStoreRestoreBlobData data) {
    return true;
  }

  @Override
  public void after(final boolean updateAssets, final Repository repository) {
    //no-op
  }
}
