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
package org.sonatype.repository.conan.internal.orient.metadata;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.repository.conan.internal.AssetKind;

import com.google.common.hash.HashCode;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.repository.conan.internal.orient.utils.ConanFacetUtils.findAsset;

/**
 * Responsible for looking up the hashes to verify against
 *
 * @since 3.next
 */
public class ConanHashVerifier
    extends ComponentSupport
{
  /**
   * Retrieves the hash maps which are stored as key, value pairs within the conanmanifest file
   * @param tx
   * @param bucket
   * @param assetPath
   * @return hashcode of the file
   */
  public HashCode lookupHashFromAsset(final StorageTx tx, final Bucket bucket, final String assetPath) {
    checkNotNull(tx);
    checkNotNull(bucket);
    checkNotNull(assetPath);

    AttributesMap attributes = getConanmanifestHashes(tx, bucket, assetPath);

    if(attributes != null) {
      String filename = getFilenameFromPath(assetPath);
      if (attributes.contains(filename)) {
        return HashCode.fromString((String) attributes.get(filename));
      }
    }
    return null;
  }

  private AttributesMap getConanmanifestHashes(final StorageTx tx, final Bucket bucket, final String assetPath) {
    String originalFilename = getFilenameFromPath(assetPath);
    String manifestFile = assetPath.replace(originalFilename, AssetKind.CONAN_MANIFEST.getFilename());
    Asset asset = findAsset(tx, bucket, manifestFile);
    if(asset == null) {
      return null;
    }
    return ConanManifest.parse(tx.getBlob(asset.blobRef()).getInputStream());
  }

  private static String getFilenameFromPath(final String assetPath) {
    String[] split = assetPath.split("/");
    return split[split.length - 1];
  }

  /**
   * Verifies that the hashes match when both hashes are supplied
   * @param me
   * @param you
   * @return
   */
  public boolean verify(final HashCode me, final HashCode you) {
    return me == null || you == null || me.equals(you);
  }
}
