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
package org.sonatype.nexus.repository.p2.datastore.internal;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssetBuilder;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.p2.datastore.P2ContentFacet;
import org.sonatype.nexus.repository.p2.internal.AssetKind;
import org.sonatype.nexus.repository.p2.internal.P2Format;
import org.sonatype.nexus.repository.p2.internal.metadata.P2Attributes;
import org.sonatype.nexus.repository.p2.internal.util.P2TempBlobUtils;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.repository.p2.internal.P2Format.NAME;
import static org.sonatype.nexus.repository.p2.internal.util.P2PathUtils.PLUGIN_NAME;

/**
 * @since 3.next
 */
@Named(NAME)
public class P2ContentFacetImpl
    extends ContentFacetSupport
    implements P2ContentFacet
{
  private static final Collection<HashAlgorithm> HASH_ALGORITHMS = ImmutableList.of(SHA1);

  private final P2TempBlobUtils p2TempBlobUtils;

  @Inject
  public P2ContentFacetImpl(
      @Named(NAME) final FormatStoreManager formatStoreManager,
      final P2TempBlobUtils p2TempBlobUtils)
  {
    super(formatStoreManager);
    this.p2TempBlobUtils = checkNotNull(p2TempBlobUtils);
  }

  @Override
  public boolean delete(final String path) throws IOException {
    return assets().path(path).find().map(FluentAsset::delete).orElse(false);
  }

  @Override
  public Optional<Content> get(final String path) throws IOException {
    return assets().path(path).find().map(FluentAsset::download);
  }

  @Override
  public Content putArtifactsMetadata(
      final String path,
      final Payload payload,
      final Map<String, Object> attributes) throws IOException
  {
    return putMetadata(path, payload, attributes, AssetKind.ARTIFACTS_METADATA);
  }

  @Override
  public Content putBinary(final P2Attributes p2Attributes, final Payload payload) throws IOException {
    try (TempBlob blob = blobs().ingest(payload, HASH_ALGORITHMS)) {
      return putComponent(p2Attributes, blob, payload, AssetKind.BINARY_BUNDLE);
    }
  }

  @Override
  public Content putBundle(final P2Attributes p2Attributes, final Payload payload) throws IOException {
    try (TempBlob blob = blobs().ingest(payload, HASH_ALGORITHMS)) {
      P2Attributes mergedP2Attributes = p2TempBlobUtils.mergeAttributesFromTempBlob(blob, p2Attributes);
      return putComponent(mergedP2Attributes, blob, payload, AssetKind.BUNDLE);
    }
  }

  @Override
  public Content putCompositeArtifactsMetadata(
      final String path,
      final Payload payload,
      final Map<String, Object> attributes) throws IOException
  {
    return putMetadata(path, payload, attributes, AssetKind.COMPOSITE_ARTIFACTS);
  }

  @Override
  public Content putCompositeContentMetadata(
      final String path,
      final Payload payload,
      final Map<String, Object> attributes) throws IOException
  {
    return putMetadata(path, payload, attributes, AssetKind.COMPOSITE_CONTENT);
  }

  @Override
  public Content putContentMetadata(
      final String path,
      final Payload payload,
      final Map<String, Object> attributes) throws IOException
  {
    return putMetadata(path, payload, attributes, AssetKind.CONTENT_METADATA);
  }

  @Override
  public Content putP2Index(final String path, final Payload payload, final Map<String, Object> attributes) {
    return putMetadata(path, payload, attributes, AssetKind.P2_INDEX);
  }

  private Content putComponent(
      final P2Attributes attributes,
      final TempBlob blob,
      final Payload payload,
      final AssetKind assetKind)
  {
    Component component = createComponent(attributes, assetKind);

    Map<String, Object> assetAttributes = Collections.emptyMap();
    String pluginName = attributes.getPluginName();
    if (pluginName != null) {
      assetAttributes = Collections.singletonMap(PLUGIN_NAME,  pluginName);
    }

    return createAsset(component, attributes.getPath(), assetAttributes, assetKind, blob, payload)
        .download();
  }

  private Content putMetadata(
      final String path,
      final Payload payload,
      final Map<String, Object> attributes,
      final AssetKind kind)
  {
    try (TempBlob blob = blobs().ingest(payload, HASH_ALGORITHMS)) {
      return createAsset(null, path, attributes, kind, blob, payload)
          .download();
    }
  }

  private FluentAsset createAsset(
      @Nullable final Component component,
      final String path,
      final Map<String, Object> attributes,
      final AssetKind kind,
      final TempBlob blob,
      final Payload payload)
  {
    FluentAssetBuilder builder = assets().path(path)
        .kind(kind.name())
        .attributes(P2Format.NAME, attributes)
        .blob(blob);

    if (component != null) {
      builder.component(component);
    }

    return builder
        .save()
        .markAsCached(payload);
  }

  private Component createComponent(final P2Attributes attributes, final AssetKind assetKind) {
    Map<String, String> formatAttributes = new HashMap<>();

    if (attributes.getPluginName() != null) {
      formatAttributes.put(PLUGIN_NAME, attributes.getPluginName());
    }

    return components()
        .name(attributes.getComponentName())
        .version(attributes.getComponentVersion())
        .kind(assetKind.name())
        .attributes(P2Format.NAME, attributes)
        .getOrCreate();
  }
}
