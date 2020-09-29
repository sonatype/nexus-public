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
package org.sonatype.repository.conan.internal.orient.proxy.v1

import org.sonatype.nexus.blobstore.api.Blob
import org.sonatype.nexus.common.hash.HashAlgorithm
import org.sonatype.nexus.repository.storage.Asset
import org.sonatype.nexus.repository.view.Content
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher
import org.sonatype.nexus.repository.view.payloads.BlobPayload
import org.sonatype.repository.conan.internal.AssetKind
import org.sonatype.repository.conan.internal.metadata.ConanCoords

import com.google.common.collect.ImmutableList
import org.apache.commons.lang.StringUtils

import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA256
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA512
import static org.sonatype.repository.conan.internal.metadata.ConanMetadata.DIGEST
import static org.sonatype.repository.conan.internal.metadata.ConanMetadata.GROUP
import static org.sonatype.repository.conan.internal.metadata.ConanMetadata.PATH
import static org.sonatype.repository.conan.internal.metadata.ConanMetadata.PROJECT
import static org.sonatype.repository.conan.internal.metadata.ConanMetadata.STATE
import static org.sonatype.repository.conan.internal.metadata.ConanMetadata.VERSION

/**
 * @since 3.28
 */
class OrientConanProxyHelper
{
  public static final List<AssetKind> DOWNLOAD_ASSET_KINDS = ImmutableList.of(
      AssetKind.DOWNLOAD_URL,
      AssetKind.DIGEST,
      AssetKind.CONAN_PACKAGE_SNAPSHOT
  )

  public static final List<HashAlgorithm> HASH_ALGORITHMS = ImmutableList.of(SHA256, SHA1, SHA512, MD5)

  public static final Map<String, AssetKind> ASSET_KIND_FILENAMES =
      Collections.unmodifiableMap(AssetKind.values().collectEntries { [it.filename, it] })

  static String getProxyAssetPath(final ConanCoords conanCoords, final AssetKind assetKind) {
    if (DOWNLOAD_ASSET_KINDS.contains(assetKind)) {
      return getRecipePathWithPackages(conanCoords, assetKind)
    }
    return getPath(conanCoords) + "/" + assetKind.getFilename()
  }

  static ConanCoords convertFromState(final TokenMatcher.State state) {
    return new ConanCoords(
        state.getTokens().get(PATH),
        state.getTokens().get(GROUP),
        state.getTokens().get(PROJECT),
        state.getTokens().get(VERSION),
        state.getTokens().get(STATE),
        state.getTokens().getOrDefault(DIGEST, null)
    )
  }

  private static String getPath(final ConanCoords coord) {
    return String.format("%s/%s/%s/%s/%s%s",
        coord.getPath(),
        coord.getGroup(),
        coord.getProject(),
        coord.getVersion(),
        coord.getChannel(),
        coord.getSha() == null ? StringUtils.EMPTY : "/packages/" + coord.getSha())
  }

  /*
   * Gets the path in this format: https://github.com/conan-io/conan/blob/14f84411ddf5106b86be4464ccd76aea865ecd45/conans/model/rest_routes.py#L30
   */
  private static String getRecipePathWithPackages(final ConanCoords coord, final AssetKind assetKind) {
    String filename = assetKind.getFilename()
    return String.format("%s/%s/%s/%s/%s%s%s",
        coord.getPath(),
        coord.getProject(),
        coord.getVersion(),
        coord.getGroup(),
        coord.getChannel(),
        coord.getSha() == null ? StringUtils.EMPTY : "/packages/" + coord.getSha(),
        filename == null ? StringUtils.EMPTY : "/" + filename)
  }

  static Content toContent(final Asset asset, final Blob blob) {
    Content content = new Content(new BlobPayload(blob, asset.requireContentType()))
    Content.extractFromAsset(asset, HASH_ALGORITHMS, content.getAttributes())
    return content
  }

  static String getComponentVersion(ConanCoords coords){
    return String.format("%s-%s", coords.getVersion(), coords.getChannel());
  }
}
