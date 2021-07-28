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
package org.sonatype.nexus.repository.cocoapods.datastore.internal;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.cocoapods.CocoapodsFacet;
import org.sonatype.nexus.repository.cocoapods.internal.CocoapodsFormat;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.content.utils.FormatAttributesUtils;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA256;

/**
 * @since 3.next
 */
@Named
public class DatastoreCocoapodsFacet
    extends ContentFacetSupport
    implements CocoapodsFacet
{
  private static final List<HashAlgorithm> HASH_ALGORITHMS = ImmutableList.of(MD5, SHA1, SHA256);

  @Inject
  public DatastoreCocoapodsFacet(@Named(CocoapodsFormat.NAME) final FormatStoreManager formatStoreManager) {
    super(formatStoreManager);
  }

  @Override
  @Nullable
  public Content get(final String assetPath) {
    checkNotNull(assetPath);

    return assets().path(normalizeAssetPath(assetPath)).find().map(FluentAsset::download).orElse(null);
  }

  @Override
  public Content storePodFileContent(final String assetPath,
                                     final Content content,
                                     final String componentName,
                                     final String componentVersion)
  {
    FluentComponent component = components()
        .name(componentName)
        .version(componentVersion)
        .getOrCreate();

    try (TempBlob tempBlob = blobs().ingest(content, HASH_ALGORITHMS)) {
      return assets()
          .path(normalizeAssetPath(assetPath))
          .component(component)
          .blob(tempBlob)
          .save()
          .markAsCached(content)
          .download();
    }
  }

  @Override
  public Content storeCdnMetadataContent(final String assetPath,
                                         final Content content)
  {
    try (TempBlob tempBlob = blobs().ingest(content, HASH_ALGORITHMS)) {
      return assets()
          .path(normalizeAssetPath(assetPath))
          .blob(tempBlob)
          .save()
          .markAsCached(content)
          .download();
    }
  }

  @Override
  public Content storeSpecFileContent(final String assetPath,
                                      final Content content,
                                      final Map<String, Object> formatAttributes)
  {
    try (TempBlob tempBlob = blobs().ingest(content, HASH_ALGORITHMS)) {
      FluentAsset fluentAsset = assets()
          .path(normalizeAssetPath(assetPath))
          .blob(tempBlob)
          .attributes(CocoapodsFormat.NAME, formatAttributes)
          .save();

      return fluentAsset.markAsCached(content).download();
    }
  }

  @Override
  @Nullable
  public String getAssetFormatAttribute(final String assetPath, final String attributeName) {
    checkNotNull(assetPath);
    checkNotNull(attributeName);

    return (String) assets()
        .path(normalizeAssetPath(assetPath))
        .find()
        .map((asset -> FormatAttributesUtils.getFormatAttributes(asset).get(attributeName)))
        .orElse(null);
  }

  /**
   * Returns path with appended string on the beginning.
   *
   * @param path - Any path e.g. 'some/path/example'
   * @return - the path, e.g. '/some/path/example'
   */
  private String normalizeAssetPath(final String path) {
    return StringUtils.prependIfMissing(path, "/");
  }

}
