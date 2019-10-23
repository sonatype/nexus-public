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
package org.sonatype.nexus.repository.cocoapods.internal;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.cocoapods.CocoapodsFacet;
import org.sonatype.nexus.repository.cocoapods.internal.pod.PodInfo;
import org.sonatype.nexus.repository.cocoapods.internal.pod.PodPathParser;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.transaction.TransactionalDeleteBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.Iterables;

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
public class CocoapodsFacetImpl
    extends FacetSupport
    implements CocoapodsFacet
{
  private static final List<HashAlgorithm> HASH_ALGORITHMS = Arrays.asList(MD5, SHA1, SHA256);


  private PodPathParser podPathParser;

  @Inject
  public CocoapodsFacetImpl(final PodPathParser podPathParser) {
    this.podPathParser = podPathParser;
  }

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    super.doInit(configuration);
    getRepository().facet(StorageFacet.class);
  }

  @Override
  @Nullable
  @TransactionalTouchBlob
  public Content get(final String path) {
    final StorageTx tx = UnitOfWork.currentTx();
    final Asset asset = tx.findAssetWithProperty(P_NAME, path, tx.findBucket(getRepository()));
    if (asset == null) {
      return null;
    }
    Content content = new Content(new BlobPayload(tx.requireBlob(asset.requireBlobRef()), asset.requireContentType()));
    Content.extractFromAsset(asset, HASH_ALGORITHMS, content.getAttributes());
    return content;
  }

  @Override
  @TransactionalDeleteBlob
  public boolean delete(final String path) throws IOException {
    //to be implemented
    return false;
  }

  @Override
  @TransactionalStoreBlob
  public Content getOrCreateAsset(final String path, final Content content, boolean toAttachComponent)
      throws IOException
  {
    StorageFacet storageFacet = facet(StorageFacet.class);
    try (final TempBlob tempBlob = storageFacet.createTempBlob(content, HASH_ALGORITHMS)) {
      StorageTx tx = UnitOfWork.currentTx();
      Bucket bucket = tx.findBucket(getRepository());

      Asset asset = tx.findAssetWithProperty(P_NAME, path, bucket);
      if (asset == null) {
        if (toAttachComponent) {
          Component component = findOrCreateComponent(tx, bucket, path);
          asset = tx.createAsset(bucket, component).name(path);
        }
        else {
          asset = tx.createAsset(bucket, getRepository().getFormat()).name(path);
        }
      }
      Content.applyToAsset(asset, content.getAttributes());
      AssetBlob blob = tx.setBlob(asset, path, tempBlob, HASH_ALGORITHMS, null, null, false);
      tx.saveAsset(asset);

      final Content updatedContent = new Content(new BlobPayload(blob.getBlob(), asset.requireContentType()));
      Content.extractFromAsset(asset, HASH_ALGORITHMS, updatedContent.getAttributes());
      return updatedContent;
    }
  }

  private Component findOrCreateComponent(final StorageTx tx, final Bucket bucket, final String path) {
    PodInfo podInfo = podPathParser.parse(path);

    Iterable<Component> components = tx.findComponents(
        builder()
            .where(P_NAME).eq(podInfo.getName())
            .and(P_VERSION).eq(podInfo.getVersion())
            .build(),
        singletonList(getRepository())
    );

    Component component = Iterables.getFirst(components, null); // NOSONAR
    if (component == null) {
      component = tx.createComponent(bucket, getRepository().getFormat())
          .name(podInfo.getName())
          .version(podInfo.getVersion());
      tx.saveComponent(component);
    }
    return component;
  }
}
