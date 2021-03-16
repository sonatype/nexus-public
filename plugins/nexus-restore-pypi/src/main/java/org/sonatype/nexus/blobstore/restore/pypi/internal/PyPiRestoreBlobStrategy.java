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
package org.sonatype.nexus.blobstore.restore.pypi.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.restore.RestoreBlobData;
import org.sonatype.nexus.blobstore.restore.datastore.BaseRestoreBlobStrategy;
import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.pypi.AssetKind;
import org.sonatype.nexus.repository.pypi.PyPiFormat;
import org.sonatype.nexus.repository.pypi.PyPiInfoUtils;
import org.sonatype.nexus.repository.pypi.datastore.PypiContentFacet;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.FeatureFlags.EARLY_ACCESS_DATASTORE_DEVELOPER;
import static org.sonatype.nexus.repository.pypi.PyPiAttributes.P_NAME;
import static org.sonatype.nexus.repository.pypi.PyPiAttributes.P_SUMMARY;
import static org.sonatype.nexus.repository.pypi.PyPiAttributes.P_VERSION;
import static org.sonatype.nexus.repository.pypi.PyPiPathUtils.normalizeName;
import static org.sonatype.nexus.repository.pypi.PyPiRestoreUtil.isIndex;
import static org.sonatype.nexus.repository.pypi.PyPiRestoreUtil.isRootIndex;
import static org.sonatype.nexus.repository.pypi.datastore.PyPiDataUtils.copyFormatAttributes;
import static org.sonatype.nexus.repository.pypi.datastore.PyPiDataUtils.setFormatAttribute;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;

/**
 * @since 3.next
 */
@FeatureFlag(name = EARLY_ACCESS_DATASTORE_DEVELOPER)
@Named(PyPiFormat.NAME)
@Singleton
public class PyPiRestoreBlobStrategy
    extends BaseRestoreBlobStrategy<RestoreBlobData>
{
  private final RepositoryManager repositoryManager;

  @Inject
  public PyPiRestoreBlobStrategy(
      final DryRunPrefix dryRunPrefix,
      final RepositoryManager repositoryManager)
  {
    super(dryRunPrefix);
    this.repositoryManager = repositoryManager;
  }

  @Override
  protected boolean canAttemptRestore(@Nonnull final RestoreBlobData data)
  {
    Repository repository = data.getRepository();

    if (!repository.optionalFacet(PypiContentFacet.class).isPresent()) {
      log.warn("Skipping as PyPI Facet not found on repository: {}", repository.getName());
      return false;
    }

    return true;
  }

  @Override
  protected void createAssetFromBlob(final Blob assetBlob, final RestoreBlobData data) throws IOException
  {
    PypiContentFacet pypiContentFacet = data.getRepository().facet(PypiContentFacet.class);
    try (InputStream inputStream = data.getBlob().getInputStream();
         TempBlob tempBlob = pypiContentFacet.getTempBlob(inputStream, data.getBlobType())) {
      if (isComponentRequired(data)) {
        Map<String, String> attributes = getAttributes(tempBlob);
        String name = checkNotNull(attributes.get(P_NAME));
        String version = checkNotNull(attributes.get(P_VERSION));
        String normalizedName = normalizeName(name);

        FluentComponent component = pypiContentFacet.findOrCreateComponent(name, version, normalizedName);
        setFormatAttribute(component, P_SUMMARY, attributes.get(P_SUMMARY));
        setFormatAttribute(component, P_VERSION, version);

        FluentAsset asset =
            pypiContentFacet.saveAsset(data.getBlobName(), component, AssetKind.PACKAGE.name(), tempBlob);
        setFormatAttribute(asset, P_ASSET_KIND, AssetKind.PACKAGE.name());
        copyFormatAttributes(asset, attributes);
      }
      else {
        AssetKind metadataAssetKind = isRootIndex(data.getBlobName()) ? AssetKind.ROOT_INDEX : AssetKind.INDEX;
        pypiContentFacet.saveAsset(data.getBlobName(), metadataAssetKind.name(), tempBlob);
      }
    }
  }

  private Map<String, String> getAttributes(final TempBlob tempBlob) {
    try (InputStream inputStream = tempBlob.get()) {
      return PyPiInfoUtils.extractMetadata(inputStream);
    }
    catch (IOException e) {
      log.warn("Skipping invalid pypi package blob: {}", tempBlob.getBlob().getId());
      return Collections.emptyMap();
    }
  }

  @Override
  protected String getAssetPath(@Nonnull final RestoreBlobData data)
  {
    return data.getBlobName();
  }

  @Override
  protected RestoreBlobData createRestoreData(
      final Properties properties, final Blob blob, final BlobStore blobStore)
  {
    return new RestoreBlobData(blob, properties, blobStore, repositoryManager);
  }

  @Override
  protected boolean isComponentRequired(final RestoreBlobData data) {
    return !isIndex(data.getBlobName());
  }

  @Override
  public void after(final boolean updateAssets, final Repository repository) {
    // no-op
  }
}
