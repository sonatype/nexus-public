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
package org.sonatype.nexus.repository.apt.internal.snapshot;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.apt.AptFacet;
import org.sonatype.nexus.repository.apt.internal.FacetHelper;
import org.sonatype.nexus.repository.apt.internal.debian.ControlFile;
import org.sonatype.nexus.repository.apt.internal.debian.ControlFileParser;
import org.sonatype.nexus.repository.apt.internal.debian.Release;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.orientechnologies.common.concur.ONeedRetryException;
import org.bouncycastle.bcpg.ArmoredInputStream;

import static java.util.Collections.singletonList;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;
import static org.sonatype.nexus.repository.storage.Query.builder;

/**
 * @since 3.next
 */
public abstract class AptSnapshotFacetSupport
    extends FacetSupport
    implements AptSnapshotFacet
{
  @Override
  public boolean isSnapshotableFile(String path) {
    return !path.endsWith(".deb") && !path.endsWith(".DEB");
  }

  @Override
  public void createSnapshot(String id, SnapshotComponentSelector selector) throws IOException {
    Iterable<SnapshotItem> snapshots = collectSnapshotItems(selector);
    createSnapshot(id, snapshots);
  }

  @Transactional(retryOn = {ONeedRetryException.class})
  protected void createSnapshot(String id, Iterable<SnapshotItem> snapshots) throws IOException {
    StorageTx tx = UnitOfWork.currentTx();
    StorageFacet storageFacet = facet(StorageFacet.class);
    Bucket bucket = tx.findBucket(getRepository());
    for (SnapshotItem item : snapshots) {
      String assetName = createAssetPath(id, item.specifier.path);
      Asset asset = tx.createAsset(bucket, getRepository().getFormat()).name(assetName);
      try (final TempBlob streamSupplier = storageFacet
          .createTempBlob(item.content.openInputStream(), FacetHelper.hashAlgorithms)) {
        AssetBlob blob = tx.createBlob(item.specifier.path, streamSupplier, FacetHelper.hashAlgorithms, null,
            item.specifier.role.getMimeType(), true);
        tx.attachBlob(asset, blob);
      }
      finally {
        item.content.close();
      }
      tx.saveAsset(asset);
    }
  }

  @Transactional(retryOn = {ONeedRetryException.class})
  @Override
  @Nullable
  public Content getSnapshotFile(String id, String path) throws IOException {
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());
    final Asset asset = tx.findAssetWithProperty(P_NAME, createAssetPath(id, path), bucket);
    if (asset == null) {
      return null;
    }

    final Blob blob = tx.requireBlob(asset.requireBlobRef());
    return FacetHelper.toContent(asset, blob);
  }

  @Transactional(retryOn = {ONeedRetryException.class})
  @Override
  public void deleteSnapshot(String id) throws IOException {
    String path = createAssetPath(id, "");

    StorageTx tx = UnitOfWork.currentTx();
    Query query = builder()
        .where(P_NAME).like(path + "%")
        .build();

    tx.findAssets(query, singletonList(getRepository()))
        .spliterator()
        .forEachRemaining(tx::deleteAsset);
  }

  protected Iterable<SnapshotItem> collectSnapshotItems(SnapshotComponentSelector selector) throws IOException {
    AptFacet aptFacet = getRepository().facet(AptFacet.class);

    List<SnapshotItem> result = new ArrayList<>();
    List<SnapshotItem> releaseIndexItems = fetchSnapshotItems(FacetHelper.getReleaseIndexSpecifiers(aptFacet));
    Map<SnapshotItem.Role, SnapshotItem> itemsByRole = new HashMap<>(
        releaseIndexItems.stream().collect(Collectors.toMap((SnapshotItem item) -> item.specifier.role, item -> item)));
    InputStream releaseStream = null;
    if (itemsByRole.containsKey(SnapshotItem.Role.RELEASE_INDEX)) {
      releaseStream = itemsByRole.get(SnapshotItem.Role.RELEASE_INDEX).content.openInputStream();
    }
    else {
      InputStream is = itemsByRole.get(SnapshotItem.Role.RELEASE_INLINE_INDEX).content.openInputStream();
      if (is != null) {
        ArmoredInputStream aIs = new ArmoredInputStream(is);
        releaseStream = new FilterInputStream(aIs)
        {
          boolean done = false;

          @Override
          public int read() throws IOException {
            if (done) {
              return -1;
            }
            int c = aIs.read();
            if (c < 0 || !aIs.isClearText()) {
              done = true;
              return -1;
            }
            return c;
          }

          @Override
          public int read(byte[] b, int off, int len) throws IOException {
            for (int i = 0; i < len; i++) {
              int c = read();
              if (c == -1) {
                return i == 0 ? -1 : i;
              }
              b[off + i] = (byte) c;
            }
            return len;
          }
        };
      }
    }

    if (releaseStream == null) {
      throw new IOException("Invalid upstream repository:  no release index present");
    }

    Release release;
    try {
      ControlFile index = new ControlFileParser().parseControlFile(releaseStream);
      release = new Release(index);
    }
    finally {
      releaseStream.close();
    }

    result.addAll(releaseIndexItems);

    if (aptFacet.isFlat()) {
      result.addAll(fetchSnapshotItems(FacetHelper.getReleasePackageIndexes(aptFacet, null, null)));
    }
    else {
      List<String> archs = selector.getArchitectures(release);
      List<String> comps = selector.getComponents(release);
      for (String arch : archs) {
        for (String comp : comps) {
          result.addAll(fetchSnapshotItems(FacetHelper.getReleasePackageIndexes(aptFacet, comp, arch)));
        }
      }
    }

    return result;
  }

  private String createAssetPath(String id, String path) {
    return "snapshots/" + id + "/" + path;
  }

  protected abstract List<SnapshotItem> fetchSnapshotItems(List<SnapshotItem.ContentSpecifier> specs)
      throws IOException;
}
