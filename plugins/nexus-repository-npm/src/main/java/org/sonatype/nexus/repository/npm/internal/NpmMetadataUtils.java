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
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.view.Content;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import static com.google.common.collect.Lists.newArrayList;
import static org.sonatype.nexus.repository.npm.internal.NpmVersionComparator.versionComparator;

/**
 * npm utilities for manipulating npm metadata.
 *
 * @since 3.0
 */
public final class NpmMetadataUtils
{
  private NpmMetadataUtils() {
    // nop
  }

  @VisibleForTesting
  static final DateTimeFormatter NPM_TIMESTAMP_FORMAT = ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC);

  static final String NAME = "name";

  static final String DIST_TAGS = "dist-tags";

  static final String VERSIONS = "versions";

  static final String VERSION = "version";

  static final String DEPRECATED = "deprecated";

  static final String TIME = "time";

  private static final String MODIFIED = "modified";

  private static final String CREATED = "created";

  static final String LATEST = "latest";

  static final String DIST = "dist";

  static final String TARBALL = "tarball";

  static final String META_ID = "_id";

  static final String META_REV = "_rev";

  public static final String META_UPDATED = "_updated";

  private static final String AUTHOR = "author";

  private static final String DEPENDENCIES = "dependencies";

  private static final String DEV_DEPENDENCIES = "devDependencies";

  private static final String PUBLISH_CONFIG = "publishConfig";

  private static final String SCRIPTS = "scripts";

  /**
   * Extracts the tarball filename from tarball "url-ish" strings.
   */
  @Nonnull
  static String extractTarballName(final String tarballUrl) {
    int idx = tarballUrl.lastIndexOf('/');
    if (idx != -1) {
      return tarballUrl.substring(idx + 1);
    }
    else {
      return tarballUrl;
    }
  }

  /**
   * Extracts a tarball name from the latest version. Metadata may be modified if the path does not exist.
   *
   * @since 3.7
   */
  @Nullable
  static String extractTarballName(final NestedAttributesMap packageMetadata) {
    String version = packageMetadata.child(DIST_TAGS).get(LATEST, String.class);
    if (version == null) {
      return null;
    }
    String url = packageMetadata.child(VERSIONS).child(version).child(DIST).get(TARBALL, String.class);
    if (url == null) {
      return null;
    }
    return extractTarballName(url);
  }

  /**
   * Selects and returns version metadata object based on tarball name.
   */
  @Nullable
  static NestedAttributesMap selectVersionByTarballName(
      final NestedAttributesMap packageRoot,
      final String tarballName)
  {
    String extractedTarballName = extractTarballName(tarballName);
    NestedAttributesMap versions = packageRoot.child(VERSIONS);
    for (String v : versions.keys()) {
      NestedAttributesMap version = versions.child(v);
      String versionTarballUrl = version.child(DIST).get(TARBALL, String.class);
      if (extractedTarballName.equals(extractTarballName(versionTarballUrl))) {
        return version;
      }
    }
    return null;
  }

  /**
   * Maintains the time fields of npm package root. Sets created time if it doesn't exist, updates the modified time.
   */
  static DateTime maintainTime(final NestedAttributesMap packageRoot) {
    final NestedAttributesMap time = packageRoot.child(TIME);
    final DateTime now = DateTime.now();
    final String nowString = NPM_TIMESTAMP_FORMAT.print(now);
    if (!time.contains(CREATED)) {
      time.set(CREATED, nowString);
    }
    time.set(MODIFIED, nowString);
    for (String version : packageRoot.child(VERSIONS).keys()) {
      if (!time.contains(version)) {
        time.set(version, nowString);
      }
    }
    return now;
  }

  /**
   * Gets the last modified time field of npm package root. May return {@code null} if no such field.
   */
  @Nullable
  public static DateTime lastModified(final NestedAttributesMap packageRoot) {
    final NestedAttributesMap time = packageRoot.child(TIME);
    final String modified = time.get(MODIFIED, String.class);
    if (modified != null) {
      return NPM_TIMESTAMP_FORMAT.parseDateTime(modified);
    }
    return null;
  }

  /**
   * Rewrites dist/tarball entry URLs to point back to this Nexus instance and given repository.
   */
  static void rewriteTarballUrl(final String repositoryName, final NestedAttributesMap packageRoot) {
    if (BaseUrlHolder.isSet()) {
      NestedAttributesMap versions = packageRoot.child(VERSIONS);
      for (String v : versions.keys()) {
        if (versions.get(v) instanceof Map) { // only if not incomplete
          NestedAttributesMap version = versions.child(v);
          NestedAttributesMap dist = version.child(DIST);
          String tarballName = extractTarballName(dist.get(TARBALL, String.class));
          dist.set(
              TARBALL,
              String.format(
                  "%s/repository/%s/%s/-/%s", BaseUrlHolder.get(), repositoryName, version.get(NAME), tarballName
              )
          );
        }
      }
    }
  }

  /**
   * Rewrites dist/tarball entry URLs to point back to this Nexus instance and given repository.
   *
   * @param repositoryName    Name of the repository we want to point this at.
   * @param name              Name of the Asset/NPM Package ID
   * @param currentTarballUrl The real/original tarball url
   * @return String with local NXRM url
   */
  static String rewriteTarballUrl(final String repositoryName, final String name, final String currentTarballUrl) {
    if (BaseUrlHolder.isSet()) {
      return String.format(
          "%s/repository/%s/%s/-/%s",
          BaseUrlHolder.get(),
          repositoryName,
          name,
          extractTarballName(currentTarballUrl));
    }

    return currentTarballUrl;
  }

  /**
   * Create a path within a repository for a tarball.
   *
   * @since 3.8
   */
  public static String createRepositoryPath(final String name, final String version) {
    NpmPackageId packageId = NpmPackageId.parse(name);
    return String.format("%s/-/%s-%s.tgz", packageId.id(), packageId.name(), version);
  }

  /**
   * Shrinks the package root JSON object as required for npm search operations (basically shaves off version entry
   * values replacing them with tags. Package roots transformed like this must NOT be persisted back into storage
   * of Nexus, they are meant only by npm CLI downstream consumption.
   */
  @Nonnull
  public static NestedAttributesMap shrink(final NestedAttributesMap packageRoot) {
    final NestedAttributesMap versions = packageRoot.child(VERSIONS);
    for (Entry<String, Object> version : versions) {
      version.setValue(resolveVersionToTag(packageRoot, version.getKey()));
    }
    return packageRoot;
  }

  /**
   * Overlays 2nd npm metadata object on top of 1st, with care about "shrunk" (version object-less) input. The {@code
   * recessive} input parameter backing map is modified and returned, while the {@code dominant} input parameter is
   * unmodified. This method does not care about anything else, unlike {@link #merge(String, List)} does.
   */
  @Nonnull
  static NestedAttributesMap overlay(final NestedAttributesMap recessive, final NestedAttributesMap dominant) {
    overlay(recessive.backing(), dominant.backing(), true);
    return recessive;
  }

  /**
   * Merges package metadata into a new metadata object in given order: last one prevails. Also, since merged document
   * should not be edited (should be considered Read Only), this method removes attributes like {@code _id} and
   * {@code _rev} to make it un-editable by npm client. Package versions are NOT merged, they are replaced. Finally,
   * this method maintains the "dist-tags/latest" tag, setting it to latest version that won over the merge. The
   * packages must have same name, this is enforced.
   *
   * @param key      The key that resulting (newly created) {@link NestedAttributesMap} will have, non null.
   * @param packages The list of packages to merge in order.
   */
  @Nonnull
  public static NestedAttributesMap merge(final String key, final List<NestedAttributesMap> packages) {
    final NestedAttributesMap result = new NestedAttributesMap(key, Maps.<String, Object>newHashMap());
    String latestVersion = null;
    for (NestedAttributesMap pkg : packages) {
      String pkgLatestVersion = pkg.child(DIST_TAGS).get(LATEST, String.class);
      if (pkgLatestVersion != null
          && (latestVersion == null || versionComparator.compare(pkgLatestVersion, latestVersion) > 0)) {
        latestVersion = pkgLatestVersion;
      }
      overlay(result.backing(), pkg.backing(), false);
    }
    result.child(DIST_TAGS).set(LATEST, latestVersion);
    result.remove(META_ID);
    result.remove(META_REV);

    return result;
  }

  /**
   * Merges package metadata into a new metadata object in given order: last one prevails. Also, since merged document
   * should not be edited (should be considered Read Only), this method removes attributes like {@code _id} and
   * {@code _rev} to make it un-editable by npm client. Package versions are NOT merged, they are replaced. Finally,
   * this method maintains the "dist-tags/latest" tag, setting it to latest version that won over the merge. The
   * packages must have same name, this is enforced.
   *
   * @param contents The list of contents to merge in order.
   */
  public static NestedAttributesMap mergeContents(final List<Content> contents) throws IOException {
    List<InputStream> streams = newArrayList();
    for (Content content : contents) {
      streams.add(content.openInputStream());
    }
    return new NpmMergeObjectMapper().merge(streams);
  }

  /**
   * Similar to {@link #mergeContents(List)} but only for a single content, allowing for the same manner of parsing
   * the output map as the merged ones.
   *
   * @param content The content to parse into a {@link NestedAttributesMap}
   */
  public static NestedAttributesMap parseContent(final Content content) throws IOException {
    return new NpmMergeObjectMapper().read(content.openInputStream());
  }

  /**
   * Overlays two npm metadata objects, with care about "shrunk" (version document-less) input. The {@code recessive}
   * input is changed, by overlaying the {@code dominant} input, while {@code dominant} is NOT changed.
   *
   * @param recessive             The "target" where to which to overlay the {@code dominant} source (is changed at
   *                              method return)
   * @param dominant              The "source" which is being overlaid (is unchanged at method return)
   * @param mergeVersionDocuments if {@code true}, the same versioned version documents are overlaid recursively.
   *                              If {@code false}, the {@code dominant} version documents are just put into recessive,
   *                              hence are replacing {@code recessive} same version version documents.
   */
  public static Map<String, Object> overlay(
      final Map<String, Object> recessive,
      final Map<String, Object> dominant,
      final boolean mergeVersionDocuments)
  {
    for (Entry<String, Object> dominantEntry : dominant.entrySet()) {
      final String key = dominantEntry.getKey();
      final Object recessiveValue = recessive.get(key);
      final Object dominantValue = dominantEntry.getValue();
      if (dominantValue instanceof Map && recessiveValue instanceof Map) {
        Map<String, Object> recessiveChild = (Map) recessiveValue;
        Map<String, Object> dominantChild = (Map) dominantValue;
        if (isSpecialOverlayKey(key)) {
          recessive.put(key, dominantValue);
        }
        else if (mergeVersionDocuments || !VERSIONS.equals(key)) {
          recessive.put(key, overlay(recessiveChild, dominantChild, mergeVersionDocuments));
        }
        else {
          for (Entry dominantVersion : dominantChild.entrySet()) {
            recessiveChild.put(String.valueOf(dominantVersion.getKey()), dominantVersion.getValue());
          }
        }
      }
      else if (dominantValue instanceof String && recessiveValue instanceof Map) {
        continue; // skip, this is usually versions : { "x.x.x" : "latest" } on incomplete documents
      }
      else {
        recessive.put(key, dominantValue);
      }
    }
    return recessive;
  }

  /**
   * Returns a boolean indicating if this key is a "special" overlay key that should just be directly copied rather
   * than recursively combined.
   */
  private static boolean isSpecialOverlayKey(final String key) {
    return DEPENDENCIES.equals(key) || DEV_DEPENDENCIES.equals(key) || SCRIPTS.equals(key) || AUTHOR.equals(key) ||
        PUBLISH_CONFIG.equals(key);
  }

  /**
   * Resolves version to a corresponding tag in "dist-tags" section, if any, or the version itself if no tag assigned.
   */
  private static String resolveVersionToTag(final NestedAttributesMap packageRoot, final String version) {
    final NestedAttributesMap distTags = packageRoot.child(DIST_TAGS);
    if (!distTags.isEmpty()) {
      for (Entry<String, Object> distTag : distTags) {
        if (version.equals(distTag.getValue())) {
          return distTag.getKey();
        }
      }
    }
    return version;
  }

  public static String getLatestVersionFromPackageRoot(NestedAttributesMap pkg) {
    return pkg.child(DIST_TAGS).get(LATEST, String.class);
  }
}
