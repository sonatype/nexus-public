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
package org.sonatype.nexus.repository.npm.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.transaction.UnitOfWork;

import org.joda.time.DateTime;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.sonatype.nexus.repository.npm.internal.NpmAttributes.P_NAME;
import static org.sonatype.nexus.repository.npm.internal.NpmFacetUtils.findPackageRootAsset;
import static org.sonatype.nexus.repository.npm.internal.NpmFacetUtils.loadPackageRoot;
import static org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils.DIST;
import static org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils.DIST_TAGS;
import static org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils.META_ID;
import static org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils.NPM_TIMESTAMP_FORMAT;
import static org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils.TARBALL;
import static org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils.TIME;
import static org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils.VERSIONS;
import static org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils.getLatestVersionFromPackageRoot;
import static org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils.rewriteTarballUrl;

/**
 * Helper for npm package root metadata.
 *
 * See https://github.com/npm/registry/blob/master/docs/responses/package-metadata.md
 *
 * @since 3.7
 */
public class NpmPackageRootMetadataUtils
{
  private static final String MODIFIED = "modified";

  private static final String CREATED = "created";

  private static final String LATEST = "latest";

  private static final String DEPENDENCIES = "dependencies";

  private static final String DEV_DEPENDENCIES = "devDependencies";

  private static final String[] FULL_HOISTED_FIELDS = new String[] { NpmAttributes.P_AUTHOR,
      NpmAttributes.P_CONTRIBUTORS, NpmAttributes.P_DESCRIPTION, NpmAttributes.P_HOMEPAGE, NpmAttributes.P_KEYWORDS,
      NpmAttributes.P_LICENSE, NpmAttributes.P_MAINTAINERS, NpmAttributes.P_NAME, NpmAttributes.P_README,
      NpmAttributes.P_README_FILENAME, NpmAttributes.P_REPOSITORY };

  private static final String[] FULL_VERSION_MAP_FIELDS = new String[] { NpmAttributes.P_AUTHOR,
      NpmAttributes.P_CONTRIBUTORS,
      NpmAttributes.P_DEPRECATED, DEPENDENCIES, NpmAttributes.P_DESCRIPTION, NpmAttributes.P_LICENSE,
      NpmAttributes.P_MAIN, NpmAttributes.P_MAINTAINERS, NpmAttributes.P_NAME, NpmAttributes.P_VERSION,
      NpmAttributes.P_OPTIONAL_DEPENDENCIES, DEV_DEPENDENCIES, NpmAttributes.P_BUNDLE_DEPENDENCIES,
      NpmAttributes.P_PEER_DEPENDENCIES, NpmAttributes.P_BIN, NpmAttributes.P_DIRECTORIES, NpmAttributes.P_ENGINES,
      NpmAttributes.P_README, NpmAttributes.P_README_FILENAME,
      // This isn't currently in package.json but could be determined by the presence
      // of npm-shrinkwrap.json
      NpmAttributes.P_HAS_SHRINK_WRAP };

  private NpmPackageRootMetadataUtils() {
    // sonar
  }

  /**
   * Creates full package data from the metadata of an individual version. May change <code>packageJson</code> if child
   * nodes do not exist.
   *
   * @param packageJson the metadata for the version
   * @param repositoryName the repository name
   * @param sha1sum the hash of the version
   *
   * @since 3.7
   */
  public static NestedAttributesMap createFullPackageMetadata(final NestedAttributesMap packageJson,
                                                              final String repositoryName,
                                                              final String sha1sum,
                                                              @Nullable final Repository repository,
                                                              final BiFunction<String, String, String> function)
  {
    String name = packageJson.get(NpmAttributes.P_NAME, String.class);
    String version = packageJson.get(NpmAttributes.P_VERSION, String.class);
    String now = NPM_TIMESTAMP_FORMAT.print(DateTime.now());

    NestedAttributesMap packageRoot = new NestedAttributesMap("metadata", new HashMap<String, Object>());

    packageRoot.set(META_ID, name);

    String packageRootLatestVersion = isNull(repository) ? "" : getPackageRootLatestVersion(packageJson, repository);

    packageRoot.child(DIST_TAGS).set(LATEST, function.apply(packageRootLatestVersion, version));

    packageRoot.child(NpmAttributes.P_USERS);

    NestedAttributesMap time = packageRoot.child(TIME);
    time.set(version, now);
    time.set(MODIFIED, now);
    time.set(CREATED, now);

    // Hoisting fields from version metadata
    setBugsUrl(packageJson, packageRoot);

    for (String field : FULL_HOISTED_FIELDS) {
      copy(packageRoot, packageJson, field);
    }

    // Copy version specific metadata fields
    NestedAttributesMap versionMap = packageRoot.child(VERSIONS).child(version);
    versionMap.set(META_ID, name + "@" + version);

    // required fields
    versionMap.child(DIST).set(NpmAttributes.P_SHASUM, sha1sum);
    versionMap.child(DIST).set(TARBALL,
        String.format("%s/repository/%s", repositoryName, NpmMetadataUtils.createRepositoryPath(name, version)));

    // optional fields
    for (String field : FULL_VERSION_MAP_FIELDS) {
      copy(versionMap, packageJson, field);
    }

    // needs to happen after copying fields
    rewriteTarballUrl(repositoryName, packageRoot);

    return packageRoot;
  }

  private static String getPackageRootLatestVersion(final NestedAttributesMap packageJson,
                                                    final Repository repository)
  {
    StorageTx tx = UnitOfWork.currentTx();
    NpmPackageId packageId = NpmPackageId.parse((String) packageJson.get(P_NAME));

    try {
      NestedAttributesMap packageRoot = getPackageRoot(tx, repository, packageId);
      if(nonNull(packageRoot)) {

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
   * @param tx
   * @param repository
   * @param packageId
   * @return package root if found otherwise null
   * @throws IOException
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

  private static void copy(final NestedAttributesMap map, final NestedAttributesMap src, final String field) {
    Object object = src.get(field);
    if (object instanceof Map) {
      NestedAttributesMap destChild = map.child(field);
      NestedAttributesMap srcChild = src.child(field);
      for (String key : srcChild.keys()) {
        if (srcChild.get(field) instanceof Map) {
          copy(destChild, srcChild, key);
        }
        else {
          destChild.set(key, srcChild.get(key));
        }
      }
    }
    else if (object != null) {
      map.set(field, object);
    }
  }

  private static void setBugsUrl(NestedAttributesMap packageJson, NestedAttributesMap packageRoot) {
    Object bugs = packageJson.get(NpmAttributes.P_BUGS);
    String bugsUrl = null;

    if (bugs instanceof String) {
      bugsUrl = (String) bugs;
    } else if (bugs != null) {
      bugsUrl = packageJson.child(NpmAttributes.P_BUGS).get(NpmAttributes.P_URL, String.class);
    }

    if (bugsUrl != null) {
      packageRoot.set(NpmAttributes.P_BUGS, bugsUrl);
    }
  }
}
