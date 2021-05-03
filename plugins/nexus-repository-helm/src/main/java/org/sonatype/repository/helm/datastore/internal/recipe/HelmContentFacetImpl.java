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
package org.sonatype.repository.helm.datastore.internal.recipe;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.config.WritePolicy;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.repository.helm.HelmAttributes;
import org.sonatype.repository.helm.datastore.internal.HelmContentFacet;
import org.sonatype.repository.helm.internal.AssetKind;
import org.sonatype.repository.helm.internal.HelmFormat;
import org.sonatype.repository.helm.internal.metadata.IndexYamlAbsoluteUrlRewriter;
import org.sonatype.repository.helm.internal.util.HelmAttributeParser;

import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.repository.config.WritePolicy.ALLOW_ONCE;
import static org.sonatype.repository.helm.internal.AssetKind.HELM_PACKAGE;

/**
 * @since 3.28
 */
@Named(HelmFormat.NAME)
public class HelmContentFacetImpl
    extends ContentFacetSupport
    implements HelmContentFacet
{
  private static final Iterable<HashAlgorithm> HASHING = ImmutableList.of(MD5, SHA1);

  private final HelmAttributeParser helmAttributeParser;

  private final IndexYamlAbsoluteUrlRewriter indexYamlRewriter;

  @Inject
  public HelmContentFacetImpl(
      @Named(HelmFormat.NAME) final FormatStoreManager formatStoreManager,
      final HelmAttributeParser helmAttributeParser,
      final IndexYamlAbsoluteUrlRewriter indexYamlRewriter)
  {
    super(formatStoreManager);
    this.helmAttributeParser = checkNotNull(helmAttributeParser);
    this.indexYamlRewriter = checkNotNull(indexYamlRewriter);
  }

  @Override
  protected WritePolicy writePolicy(final Asset asset) {
    WritePolicy writePolicy = super.writePolicy(asset);
    if (writePolicy == ALLOW_ONCE && !Objects.equals(HELM_PACKAGE.name(), asset.kind())) {
      writePolicy = WritePolicy.ALLOW;
    }
    return writePolicy;
  }

  @Override
  public Iterable<FluentAsset> browseAssets() {
    return assets().browse(Integer.MAX_VALUE, null);
  }

  @Override
  public Optional<Content> getAsset(final String path) {
    return assets().path(path).find().map(FluentAsset::download);
  }

  @Override
  public Content putIndex(final String path, final Content content, final AssetKind assetKind)
  {
    // save original metadata and return modified
    try (TempBlob blob = blobs().ingest(content, HASHING)) {
        assets()
            .path(path)
            .kind(assetKind.name())
            .blob(blob)
            .save()
            .markAsCached(content);
      return indexYamlRewriter.removeUrlsFromIndexYaml(blob, content.getAttributes());
    }
  }

  @Override
  public TempBlob getTempBlob(final Payload payload) {
    return blobs().ingest(payload, HASHING);
  }

  @Override
  public Content putComponent(final String path, final Content content, final AssetKind assetKind) throws IOException
  {
    try (TempBlob blob = blobs().ingest(content, HASHING);
         InputStream in = blob.get()) {
      HelmAttributes helmAttributes = helmAttributeParser.getAttributes(assetKind, in);

      return assets()
          .path(path)
          .kind(assetKind.name())
          .component(components()
              .name(helmAttributes.getName())
              .version(helmAttributes.getVersion())
              .getOrCreate())
          .blob(blob)
          .save()
          .markAsCached(content)
          .withAttribute(HelmFormat.NAME, helmAttributes)
          .download();
    }
  }

  @Override
  public Content putComponent(
      final String path,
      final TempBlob tempBlob,
      final HelmAttributes helmAttributes,
      final Content content,
      final AssetKind assetKind)
  {
    return assets()
        .path(path)
        .kind(assetKind.name())
        .component(components()
            .name(helmAttributes.getName())
            .version(helmAttributes.getVersion())
            .getOrCreate())
        .blob(tempBlob)
        .save()
        .markAsCached(content)
        .withAttribute(HelmFormat.NAME, helmAttributes)
        .download();
  }

  @Override
  public boolean delete(final String path) {
    return assets().path(path).find().map(FluentAsset::delete).orElse(false);
  }
}
