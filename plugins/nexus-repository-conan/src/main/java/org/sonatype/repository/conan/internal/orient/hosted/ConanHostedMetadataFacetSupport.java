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
package org.sonatype.repository.conan.internal.orient.hosted;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.transaction.UnitOfWork;
import org.sonatype.repository.conan.internal.AssetKind;
import org.sonatype.repository.conan.internal.metadata.ConanCoords;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.hash.HashCode;
import org.apache.commons.lang3.tuple.Pair;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.repository.conan.internal.hosted.ConanHostedHelper.getHostedAssetPath;
import static org.sonatype.repository.conan.internal.orient.utils.ConanFacetUtils.findAsset;
import static org.sonatype.repository.conan.internal.hosted.ConanHostedHelper.MAPPER;
import static java.util.stream.Collectors.toMap;

/**
 * @since 3.28
 */
public class ConanHostedMetadataFacetSupport
    extends FacetSupport
{
  private static final List<AssetKind> DOWNLOAD_URL_ASSET_KINDS = Collections
      .unmodifiableList(Arrays.asList(
          AssetKind.CONAN_EXPORT,
          AssetKind.CONAN_FILE,
          AssetKind.CONAN_SOURCES,
          AssetKind.CONAN_MANIFEST)
      );

  private static final List<AssetKind> DOWNLOAD_URL_PACKAGE_ASSET_KINDS = Collections
      .unmodifiableList(Arrays.asList(
          AssetKind.CONAN_PACKAGE,
          AssetKind.CONAN_INFO,
          AssetKind.CONAN_MANIFEST)
      );

  private Map<String, String> generateDownloadUrls(
      final List<AssetKind> assetKinds,
      final ConanCoords coords)
  {
    Repository repository = getRepository();
    StorageTx tx = UnitOfWork.currentTx();
    return assetKinds
        .stream().filter(x -> tx.assetExists(getHostedAssetPath(coords, x), repository))
        .collect(toMap(AssetKind::getFilename,
            x -> repository.getUrl() + "/" + getHostedAssetPath(coords, x)));
  }

  @Nullable
  public String generateDownloadPackagesUrlsAsJson(final ConanCoords coords)
      throws JsonProcessingException
  {
    Map<String, String> downloadUrls = generateDownloadUrls(DOWNLOAD_URL_PACKAGE_ASSET_KINDS, coords);
    if (downloadUrls.isEmpty()){
      return null;
    }
    return MAPPER.writeValueAsString(downloadUrls);
  }

  @Nullable
  public String generateDownloadUrlsAsJson(final ConanCoords coords)
      throws JsonProcessingException
  {
    Map<String, String> downloadUrls = generateDownloadUrls(DOWNLOAD_URL_ASSET_KINDS, coords);
    if (downloadUrls.isEmpty()){
      return null;
    }
    return MAPPER.writeValueAsString(downloadUrls);
  }

  public String generateDigestAsJson(final ConanCoords coords,
                                      final String repositoryUrl)
      throws JsonProcessingException
  {
    Map<String, String> digest = new HashMap<>();
    digest.put(AssetKind.CONAN_MANIFEST.getFilename(),
        repositoryUrl + "/" + getHostedAssetPath(coords, AssetKind.CONAN_MANIFEST));
    return MAPPER.writeValueAsString(digest);
  }

  @Nullable
  public String generatePackageSnapshotAsJson(final ConanCoords coords) throws JsonProcessingException {
    Map<String, String> downloadUrls = DOWNLOAD_URL_PACKAGE_ASSET_KINDS
        .stream()
        .collect(
            toMap(AssetKind::getFilename, x -> getHostedAssetPath(coords, x)));

    Map<String, String> packageSnapshot = downloadUrls
        .entrySet()
        .stream()
        .flatMap(entry -> {
          String value = entry.getValue();
          String hash = getHash(value);
          if (hash != null) {
            return Stream.of(Pair.of(entry.getKey(), hash));
          }
          return Stream.empty();
        })
        .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    if (packageSnapshot.isEmpty()) {
      return null;
    }
    return MAPPER.writeValueAsString(packageSnapshot);
  }

  @Nullable
  @TransactionalTouchBlob
  public String getHash(final String path) {
    checkNotNull(path);

    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = findAsset(tx, tx.findBucket(getRepository()), path);
    if (asset == null) {
      return null;
    }
    HashCode checksum = asset.getChecksum(HashAlgorithm.MD5);
    if (checksum == null) {
      return null;
    }
    return checksum.toString();
  }
}
