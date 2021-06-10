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
package org.sonatype.nexus.repository.conda.datastore.internal;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Facet.Exposed;
import org.sonatype.nexus.repository.conda.AssetKind;
import org.sonatype.nexus.repository.conda.CondaFormat;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonMap;
import static org.sonatype.nexus.repository.conda.util.CondaPathUtils.normalizeAssetPath;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;

/**
 * Conda content facet.
 *
 * @since 3.next
 */
@Exposed
@Named(CondaFormat.NAME)
public class CondaContentFacet
    extends ContentFacetSupport
{
  private static final List<HashAlgorithm> HASH_ALGORITHMS = Collections.singletonList(HashAlgorithm.SHA1);

  @Inject
  public CondaContentFacet(@Named(CondaFormat.NAME) final FormatStoreManager formatStoreManager)
  {
    super(formatStoreManager);
  }

  /**
   * Get an asset by path.
   *
   * @param assetPath the asset path
   * @return the {@link FluentAsset} object.
   */
  public Optional<FluentAsset> getAsset(final String assetPath) {
    checkNotNull(assetPath);

    return assets().path(normalizeAssetPath(assetPath)).find();
  }

  /**
   * Find or create the {@link FluentAsset} with a given payload.
   *
   * @param payload   the payload which will be attached to the asset.
   * @param assetKind the asset kind.
   * @param assetPath the asset path.
   * @return the {@link FluentAsset} object.
   */
  public FluentAsset findOrCreateAsset(
      final Payload payload,
      final AssetKind assetKind,
      final String assetPath)
  {
    checkNotNull(payload);
    checkNotNull(assetKind);
    checkNotNull(assetPath);

    try (TempBlob tempBlob = blobs().ingest(payload, HASH_ALGORITHMS)) {
      return assets()
          .path(normalizeAssetPath(assetPath))
          .kind(assetKind.name())
          .attributes(CondaFormat.NAME, singletonMap(P_ASSET_KIND, assetKind.name()))
          .blob(tempBlob)
          .save();
    }
  }

  /**
   * Find or create the {@link FluentAsset} and {@link FluentComponent} with a given payload.
   *
   * @param payload   the payload which will be attached to the asset.
   * @param assetPath the path of an asset.
   * @param name      the name of an asset.
   * @param namespace the namespace of an asset.
   * @param version   the version of an asset.
   * @return the {@link FluentAsset} object.
   */
  public FluentAsset findOrCreateAssetWithComponent(
      final Payload payload,
      final String assetPath,
      final String name,
      final String namespace,
      final String version,
      final AssetKind assetKind)
  {
    checkNotNull(name);
    checkNotNull(namespace);
    checkNotNull(version);

    FluentComponent component = findOrCreateComponent(name, namespace, version);
    try (TempBlob tempBlob = blobs().ingest(payload, HASH_ALGORITHMS)) {
      return component
          .asset(normalizeAssetPath(assetPath))
          .kind(assetKind.name())
          .attributes(CondaFormat.NAME, Collections.singletonMap(P_ASSET_KIND, assetKind.name()))
          .blob(tempBlob)
          .save();
    }
  }

  private FluentComponent findOrCreateComponent(final String name, final String namespace, final String version) {
    return components()
        .name(name)
        .namespace(namespace)
        .version(version)
        .getOrCreate();
  }
}
