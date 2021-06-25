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
package org.sonatype.repository.conan.internal.datastore;

import java.io.IOException;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.repository.conan.internal.AssetKind;
import org.sonatype.repository.conan.internal.metadata.ConanManifest;

import com.google.common.hash.HashCode;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.32
 */
public class ConanHashVerifier
    extends ComponentSupport
{
  /**
   * Retrieves the HashCode which is stored as an attribute of the conanmanifest file
   *
   * @param assetPath
   * @return hashcode of the file
   */
  public HashCode lookupHashFromAsset(final Repository repository, final String assetPath) {
    checkNotNull(assetPath);

    AttributesMap attributes = getConanmanifestHashes(repository, assetPath);

    if (attributes != null) {
      String filename = getFilenameFromPath(assetPath);
      if (attributes.contains(filename)) {
        return HashCode.fromString((String) attributes.get(filename));
      }
    }
    return null;
  }

  private AttributesMap getConanmanifestHashes(final Repository repository, final String assetPath) {
    String originalFilename = getFilenameFromPath(assetPath);
    String manifestFile = assetPath.replace(originalFilename, AssetKind.CONAN_MANIFEST.getFilename());

    return repository
        .facet(ContentFacet.class)
        .assets()
        .path(manifestFile)
        .find()
        .map(fluentAsset -> {
          try {
            return ConanManifest.parse(fluentAsset.download().openInputStream());
          }
          catch (IOException e) {
            log.warn("Failed to open manifest blob: {}", e.getMessage(), log.isDebugEnabled() ? e : null);
            return null;
          }
        })
        .orElse(null);
  }

  private static String getFilenameFromPath(final String assetPath) {
    String[] split = assetPath.split("/");
    return split[split.length - 1];
  }

  /**
   * Verifies that the hashes match when both hashes are supplied
   *
   * @param me
   * @param you
   * @return
   */
  public boolean verify(final HashCode me, final HashCode you) {
    return me == null || you == null || me.equals(you);
  }
}
