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
package org.sonatype.nexus.repository.cocoapods.orient.internal;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;
import javax.inject.Named;

import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.cocoapods.CocoapodsFacet;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalDeleteBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.Iterables;
import org.apache.commons.lang.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA256;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_VERSION;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;
import static org.sonatype.nexus.repository.storage.Query.builder;

/**
 * @since 3.19
 */
@Named
public class OrientCocoapodsFacetImpl
    extends FacetSupport
    implements CocoapodsFacet
{
  private static final List<HashAlgorithm> HASH_ALGORITHMS = Arrays.asList(MD5, SHA1, SHA256);

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    super.doInit(configuration);
    getRepository().facet(StorageFacet.class);
  }

  @Override
  @Nullable
  @TransactionalTouchBlob
  public Content get(final String assetPath) {
    final StorageTx tx = UnitOfWork.currentTx();
    final Asset asset = tx.findAssetWithProperty(P_NAME, assetPath, tx.findBucket(getRepository()));
    if (asset == null) {
      return null;
    }
    Content content = new Content(new BlobPayload(tx.requireBlob(asset.requireBlobRef()), asset.requireContentType()));
    Content.extractFromAsset(asset, HASH_ALGORITHMS, content.getAttributes());
    return content;
  }

  @Override
  @TransactionalDeleteBlob
  public boolean delete(final String assetPath) throws IOException {
    //to be implemented
    return false;
  }

  @Override
  @TransactionalStoreBlob
  public Content getOrCreateAsset(final String assetPath,
                                  final Content content,
                                  final String componentName,
                                  final String componentVersion)
      throws IOException
  {
    return getOrCreateAsset(assetPath, content, componentName, componentVersion, null);
  }

  @Override
  @TransactionalStoreBlob
  public Content getOrCreateAsset(final String assetPath,
                                  final Content content)
      throws IOException
  {
    return getOrCreateAsset(assetPath, content, null, null, null);
  }

  @Override
  @TransactionalStoreBlob
  public Content getOrCreateAsset(final String assetPath,
                                  final Content content,
                                  @Nullable final Map<String, String> formatAttributes)
      throws IOException
  {
    return getOrCreateAsset(assetPath, content, null, null, formatAttributes);
  }

  private Content getOrCreateAsset(final String assetPath,
                                   final Content content,
                                   @Nullable final String componentName,
                                   @Nullable final String componentVersion,
                                   @Nullable final Map<String, String> formatAttributes)
      throws IOException
  {
    checkNotNull(assetPath);
    checkNotNull(content);

    StorageFacet storageFacet = facet(StorageFacet.class);
    try (final TempBlob tempBlob = storageFacet.createTempBlob(content, HASH_ALGORITHMS)) {
      StorageTx tx = UnitOfWork.currentTx();
      Bucket bucket = tx.findBucket(getRepository());

      Asset asset = tx.findAssetWithProperty(P_NAME, assetPath, bucket);
      if (asset == null) {
        if (StringUtils.isNotEmpty(componentName)) {
          Component component = findOrCreateComponent(tx, bucket, componentName, componentVersion);
          asset = tx.createAsset(bucket, component).name(assetPath);
        }
        else {
          asset = tx.createAsset(bucket, getRepository().getFormat()).name(assetPath);
        }
      }
      Content.applyToAsset(asset, content.getAttributes());
      if (formatAttributes != null) {
        for (Entry<String, String> pair : formatAttributes.entrySet()) {
          asset.formatAttributes().set(pair.getKey(), pair.getValue());
        }
      }
      AssetBlob blob = tx.setBlob(asset, assetPath, tempBlob, HASH_ALGORITHMS, null, null, false);
      tx.saveAsset(asset);

      final Content updatedContent = new Content(new BlobPayload(blob.getBlob(), asset.requireContentType()));
      Content.extractFromAsset(asset, HASH_ALGORITHMS, updatedContent.getAttributes());
      return updatedContent;
    }
  }

  @Override
  @Nullable
  @TransactionalTouchBlob
  public <T> T getAssetFormatAttribute(final String assetPath, final String attributeName) {
    checkNotNull(assetPath);
    checkNotNull(attributeName);

    final StorageTx tx = UnitOfWork.currentTx();
    final Asset asset = tx.findAssetWithProperty(P_NAME, assetPath, tx.findBucket(getRepository()));
    if (asset == null) {
      return null;
    }

    return (T) asset.formatAttributes().get(attributeName);
  }

  private Component findOrCreateComponent(final StorageTx tx,
                                          final Bucket bucket,
                                          final String componentName,
                                          final String componentVersion)
  {
    Iterable<Component> components = tx.findComponents(
        builder()
            .where(P_NAME).eq(componentName)
            .and(P_VERSION).eq(componentVersion)
            .build(),
        singletonList(getRepository())
    );

    Component component = Iterables.getFirst(components, null); // NOSONAR
    if (component == null) {
      component = tx.createComponent(bucket, getRepository().getFormat())
          .name(componentName)
          .version(componentVersion);
      tx.saveComponent(component);
    }
    return component;
  }
}
