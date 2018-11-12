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
package org.sonatype.nexus.repository.pypi.internal;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA256;
import static org.sonatype.nexus.repository.pypi.internal.PyPiAttributes.*;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_VERSION;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

/**
 * Utility methods for working with PyPI data.
 *
 * @since 3.1
 */
public final class PyPiDataUtils
{
  public static final List<HashAlgorithm> HASH_ALGORITHMS = ImmutableList.of(SHA1, SHA256, MD5);

  /**
   * The supported format attributes for PyPI.
   */
  private static final List<String> SUPPORTED_ATTRIBUTES = ImmutableList.of(
      P_NAME,
      P_VERSION,
      P_PYVERSION,
      P_PLATFORM,
      P_FILETYPE,
      P_DESCRIPTION,
      P_SUMMARY,
      P_LICENSE,
      P_KEYWORDS,
      P_AUTHOR,
      P_AUTHOR_EMAIL,
      P_MAINTAINER,
      P_MAINTAINER_EMAIL,
      P_HOME_PAGE,
      P_DOWNLOAD_URL,
      P_CLASSIFIERS,
      P_REQUIRES_DIST,
      P_PROVIDES_EXTRA,
      P_SUPPORTED_PLATFORM,
      P_PROVIDES_DIST,
      P_OBSOLETES_DIST,
      P_REQUIRES_PYTHON,
      P_REQUIRES_EXTERNAL,
      P_PROJECT_URL,
      P_GENERATOR
  );

  /**
   * Convert an asset blob to {@link Content}.
   *
   * @return content of asset blob
   */
  static Content toContent(final Asset asset, final Blob blob) {
    Content content = new Content(new BlobPayload(blob, asset.requireContentType()));
    Content.extractFromAsset(asset, HASH_ALGORITHMS, content.getAttributes());
    return content;
  }

  /**
   * Find a component by its name and tag (version)
   *
   * @return found component of null if not found
   */
  @Nullable
  static Component findComponent(final StorageTx tx,
                                 final Repository repository,
                                 final String name,
                                 final String version)
  {
    Iterable<Component> components = tx.findComponents(
        Query.builder()
            .where(P_NAME).eq(name)
            .and(P_VERSION).eq(version)
            .build(),
        singletonList(repository)
    );
    if (components.iterator().hasNext()) {
      return components.iterator().next();
    }
    return null;
  }

  /**
   * Find whether or not any component with the specified name exists. If true, we can assume that the Python package
   * was "registered" and should not 404 (even if no assets have been uploaded). If false, we should likely 404.
   */
  static boolean findComponentExists(final StorageTx tx, final Repository repository, final String name) {
    return tx.countComponents(Query.builder()
            .where(P_NAME).eq(name)
            .build(),
        singletonList(repository)) > 0;
  }

  /**
   * Finds all the assets for a particular component name.
   *
   * @return list of assets
   */
  static Iterable<Asset> findAssetsByComponentName(final StorageTx tx, final Repository repository, final String name)
  {
    return tx.findAssets(
        Query.builder()
            .where("component.name").eq(name)
            .suffix("order by name desc")
            .build(),
        singletonList(repository)
    );
  }

  /**
   * Find an asset by its name.
   *
   * @return found asset or null if not found
   */
  @Nullable
  static Asset findAsset(final StorageTx tx, final Bucket bucket, final String assetName) {
    return tx.findAssetWithProperty(MetadataNodeEntityAdapter.P_NAME, assetName, bucket);
  }

  /**
   * Save an asset and create blob.
   *
   * @return blob content
   */
  static Content saveAsset(final StorageTx tx,
                           final Asset asset,
                           final TempBlob tempBlob,
                           final Payload payload) throws IOException
  {
    AttributesMap contentAttributes = null;
    String contentType = null;
    if (payload instanceof Content) {
      contentAttributes = ((Content) payload).getAttributes();
      contentType = payload.getContentType();
    }
    return saveAsset(tx, asset, tempBlob, contentType, contentAttributes);
  }

  /**
   * Save an asset and create a blob with the specified content attributes.
   *
   * @return blob content
   */
  static Content saveAsset(final StorageTx tx,
                           final Asset asset,
                           final TempBlob tempBlob,
                           @Nullable final String contentType,
                           @Nullable final AttributesMap contentAttributes) throws IOException
  {
    Content.applyToAsset(asset, Content.maintainLastModified(asset, contentAttributes));
    AssetBlob assetBlob = tx.setBlob(asset, asset.name(), tempBlob, null, contentType, false);

    tx.saveAsset(asset);
    return toContent(asset, assetBlob.getBlob());
  }

  /**
   * Copies PyPI attributes from a map into the format attributes for the asset. We put almost all the format info
   * on the asset, not the component. While most should not differ between uploads for the same name and version, it is
   * possible, so mitigate by associating with assets.
   */
  static void copyAttributes(final Asset asset, final Map<String, String> attributes) {
    checkNotNull(asset);
    checkNotNull(attributes);
    for (String attribute : SUPPORTED_ATTRIBUTES) {
      String value = Strings.nullToEmpty(attributes.get(attribute)).trim();
      if (!value.isEmpty()) {
        asset.formatAttributes().set(attribute, value);
      }
    }
  }

  private PyPiDataUtils() {
    // empty
  }
}
