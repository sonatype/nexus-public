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
package org.sonatype.nexus.repository.npm.internal.orient;

import java.io.IOException;
import java.util.function.BiFunction;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.npm.internal.NpmPackageId;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.transaction.UnitOfWork;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.sonatype.nexus.repository.npm.internal.NpmAttributes.P_NAME;
import static org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils.getLatestVersionFromPackageRoot;
import static org.sonatype.nexus.repository.npm.internal.orient.NpmFacetUtils.findPackageRootAsset;
import static org.sonatype.nexus.repository.npm.internal.orient.NpmFacetUtils.loadPackageRoot;

/**
 * Helper for npm package root metadata.
 *
 * See https://github.com/npm/registry/blob/master/docs/responses/package-metadata.md
 *
 * @since 3.7
 */
public class NpmPackageRootMetadataUtils
{
  private NpmPackageRootMetadataUtils() {
    // sonar
  }

  /**
   * Creates full package data from the metadata of an individual version. May change <code>packageJson</code> if child
   * nodes do not exist.
   *
   * @param packageJson    the metadata for the version
   * @param repositoryName the repository name
   * @param sha1sum        the hash of the version
   * @since 3.7
   */
  public static NestedAttributesMap createFullPackageMetadata(final NestedAttributesMap packageJson,
                                                              final String repositoryName,
                                                              final String sha1sum,
                                                              @Nullable final Repository repository,
                                                              final BiFunction<String, String, String> function)
  {
    String packageRootLatestVersion = isNull(repository) ? "" : getPackageRootLatestVersion(packageJson, repository);

    return org.sonatype.nexus.repository.npm.internal.NpmPackageRootMetadataUtils
        .createFullPackageMetadata(packageJson, repositoryName, sha1sum, packageRootLatestVersion, function);
  }

  private static String getPackageRootLatestVersion(final NestedAttributesMap packageJson,
                                                    final Repository repository)
  {
    StorageTx tx = UnitOfWork.currentTx();
    NpmPackageId packageId = NpmPackageId.parse((String) packageJson.get(P_NAME));

    try {
      NestedAttributesMap packageRoot = getPackageRoot(tx, repository, packageId);
      if (nonNull(packageRoot)) {

        String latestVersion = getLatestVersionFromPackageRoot(packageRoot);
        if (nonNull(latestVersion)) {
          return latestVersion;
        }
      }
    }
    catch (IOException ignored) { // NOSONAR
    }
    return "";
  }

  /**
   * Fetches the package root as {@link NestedAttributesMap}
   *
   * @return package root if found otherwise null
   */
  @Nullable
  public static NestedAttributesMap getPackageRoot(final StorageTx tx,
                                                   final Repository repository,
                                                   final NpmPackageId packageId) throws IOException
  {
    Bucket bucket = tx.findBucket(repository);

    Asset packageRootAsset = findPackageRootAsset(tx, bucket, packageId);
    if (packageRootAsset != null) {
      return loadPackageRoot(tx, packageRootAsset);
    }
    return null;
  }
}
