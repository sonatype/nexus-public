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
package org.sonatype.nexus.repository.maven.internal.hosted.metadata;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.Coordinates;
import org.sonatype.nexus.repository.maven.internal.Constants;
import org.sonatype.nexus.repository.maven.internal.hosted.metadata.Maven2Metadata.Plugin;
import org.sonatype.nexus.repository.maven.internal.hosted.metadata.Maven2Metadata.Snapshot;

import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionScheme;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Maven 2 repository metadata builder.
 *
 * @since 3.0
 */
public class MetadataBuilder
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final VersionScheme versionScheme;

  private String groupId;

  private String artifactId;

  private String baseVersion;

  // G level

  private final List<Plugin> plugins;

  // A level

  /**
   * NEXUS-53161: Preserves original version strings to avoid silent drops when
   * ComparableVersion-equivalent versions exist (e.g., "1.0.0" vs "1.0.0-release").
   * LinkedHashSet maintains insertion order while ensuring uniqueness.
   */
  private final Set<String> originalBaseVersions;

  /**
   * NEXUS-53161: Cached parsed versions to avoid O(log N) re-parsing during comparator calls.
   */
  private final Map<String, Version> parsedVersions;

  // V level

  private final Map<String, VersionCoordinates> latestVersionCoordinatesMap;

  private VersionCoordinates latestVersionCoordinates;

  public MetadataBuilder() {
    this.versionScheme = new GenericVersionScheme();
    // G
    this.plugins = new ArrayList<>();
    // A
    this.originalBaseVersions = new LinkedHashSet<>();
    this.parsedVersions = new HashMap<>();
    // V
    this.latestVersionCoordinatesMap = new HashMap<>();
  }

  // -----------------------------------
  // groupId

  public boolean onEnterGroupId(final String groupId) {
    checkNotNull(groupId);
    if (Objects.equals(groupId, this.groupId)) {
      return false;
    }
    this.groupId = groupId;
    this.artifactId = null;
    this.baseVersion = null;
    plugins.clear();
    log.debug("-> G: {}", groupId);
    return true;
  }

  @Nullable
  public Maven2Metadata onExitGroupId() {
    checkState(groupId != null);
    log.debug("<- G: {}", groupId);
    if (plugins.isEmpty()) {
      log.debug("No plugins in group: {}", groupId);
      return null;
    }
    return Maven2Metadata.newGroupLevel(DateTime.now(), plugins);
  }

  public boolean addPlugin(final String prefix, final String artifactId, final String name) {
    final Plugin plugin = Maven2Metadata.newPlugin(artifactId, prefix, name);
    final Iterator<Plugin> pi = plugins.iterator();
    while (pi.hasNext()) {
      final Plugin p = pi.next();
      if (plugin.equals(p)) {
        return false; // bail out, is present already
      }
      if (plugin.keyEquals(p)) {
        pi.remove(); // remove it, will add it below
        break;
      }
    }
    plugins.add(plugin);
    log.debug("Added plugin {}:{} prefix:{}", groupId, artifactId, prefix);
    return true;
  }

  // -----------------------------------
  // artifactId

  public boolean onEnterArtifactId(final String artifactId) {
    checkState(groupId != null);
    checkNotNull(artifactId);
    if (Objects.equals(artifactId, this.artifactId)) {
      return false;
    }
    this.artifactId = artifactId;
    this.baseVersion = null;
    originalBaseVersions.clear();
    parsedVersions.clear();
    log.debug("-> GA: {}:{}", groupId, artifactId);
    return true;
  }

  @Nullable
  public Maven2Metadata onExitArtifactId() {
    checkState(artifactId != null);
    log.debug("<- GA: {}:{}", groupId, artifactId);
    if (originalBaseVersions.isEmpty()) {
      log.debug("Nothing to generate: {}:{}", groupId, artifactId);
      return null;
    }

    // NEXUS-53161: Sort original version strings using a comparator that:
    // 1. Compares using Maven's ComparableVersion first
    // 2. Applies canonical/bare-form tiebreaker second (bare form wins over labelled forms)
    List<String> sortedVersions = new ArrayList<>(originalBaseVersions);
    sortedVersions.sort(createVersionComparator());

    // Latest is the highest version (last in descending order)
    String latest = sortedVersions.get(sortedVersions.size() - 1);
    String release = latest;

    // Find the latest non-snapshot version for <release>
    for (int i = sortedVersions.size() - 1; i >= 0; i--) {
      String version = sortedVersions.get(i);
      if (!version.endsWith(Constants.SNAPSHOT_VERSION_SUFFIX)) {
        release = version;
        break;
      }
    }

    // If latest is a snapshot and we found no release, set release to null
    if (release.endsWith(Constants.SNAPSHOT_VERSION_SUFFIX)) {
      release = null;
    }

    return Maven2Metadata.newArtifactLevel(
        DateTime.now(),
        groupId,
        artifactId,
        latest,
        release,
        sortedVersions);
  }

  /**
   * NEXUS-53161: Creates a comparator that sorts versions using Maven's ComparableVersion
   * first, then applies a canonical/bare-form tiebreaker for equivalent versions.
   * The bare form (e.g., "1.0.0") sorts higher than labelled forms (e.g., "1.0.0-release").
   *
   * <p>
   * Uses pre-parsed versions from {@link #parsedVersions} cache to avoid O(log N) re-parsing.
   */
  private Comparator<String> createVersionComparator() {
    return (v1, v2) -> {
      Version version1 = parsedVersions.get(v1);
      Version version2 = parsedVersions.get(v2);

      // If either version is not in the cache (shouldn't happen), parse on demand as fallback
      if (version1 == null) {
        version1 = parseVersion(v1);
      }
      if (version2 == null) {
        version2 = parseVersion(v2);
      }

      // If either is null after parsing, fall back to lexicographic comparison
      if (version1 == null || version2 == null) {
        return v1.compareTo(v2);
      }

      int comparison = version1.compareTo(version2);
      if (comparison != 0) {
        return comparison;
      }
      // Versions are Maven-equivalent - apply tiebreaker
      // Bare/canonical form (no qualifier) should sort higher (i.e., appear later)
      return compareTiebreaker(v1, v2);
    };
  }

  /**
   * NEXUS-53161: Tiebreaker for Maven-equivalent versions.
   * Returns negative if v1 should sort before v2, positive if v1 should sort after v2.
   * Bare/canonical form (no qualifier suffix) sorts highest (i.e., last in ascending order).
   */
  private int compareTiebreaker(final String v1, final String v2) {
    boolean v1IsAlias = isReleaseAlias(v1);
    boolean v2IsAlias = isReleaseAlias(v2);

    // Bare form (NOT an alias) sorts higher than labelled forms (aliases)
    if (!v1IsAlias && v2IsAlias) {
      return 1; // v1 is bare, should sort higher (after)
    }
    if (v1IsAlias && !v2IsAlias) {
      return -1; // v2 is bare, should sort higher (after)
    }
    // Both are bare or both are aliases - use lexicographic order for determinism
    return v1.compareTo(v2);
  }

  /**
   * NEXUS-53161: Checks if a version string is a release alias (has -ga, -release, or -final qualifier).
   * Returns {@code true} for versions like "1.0.0-ga", "1.0.0-release", "1.0.0-final".
   * Returns {@code false} for bare/canonical versions like "1.0.0", and for snapshots, alphas, etc.
   */
  private boolean isReleaseAlias(final String version) {
    if (version == null || version.isEmpty()) {
      return false;
    }
    String lower = version.toLowerCase();
    // Only return true for release-bucket aliases (ga, release, final)
    // NOT for snapshots, alphas, betas, milestones, etc.
    return lower.endsWith("-ga") ||
        lower.endsWith("-release") ||
        lower.endsWith("-final") ||
        lower.endsWith(".ga") ||
        lower.endsWith(".release") ||
        lower.endsWith(".final");
  }

  public void addBaseVersion(final String baseVersion) {
    checkNotNull(baseVersion);
    // NEXUS-53161: Always store the original version string to preserve all distinct entries
    boolean isNew = originalBaseVersions.add(baseVersion);
    if (isNew) {
      // Parse and cache the version for use in comparator
      Version parsed = parseVersion(baseVersion);
      if (parsed != null) {
        parsedVersions.put(baseVersion, parsed);
      }
      log.debug("Added base version {}:{}:{}", groupId, artifactId, baseVersion);
    }
  }

  // -----------------------------------
  // baseVersion

  /**
   * Internal structure to hold parsed Aether {@link Version} and {@link Coordinates}.
   */
  private static class VersionCoordinates
  {
    private final Version version;

    private final Coordinates coordinates;

    private VersionCoordinates(final Version version, final Coordinates coordinates) {
      this.version = version;
      this.coordinates = coordinates;
    }
  }

  public boolean onEnterBaseVersion(final String baseVersion) {
    checkState(groupId != null);
    checkState(artifactId != null);
    checkNotNull(baseVersion);
    if (Objects.equals(baseVersion, this.baseVersion)) {
      return false;
    }
    this.baseVersion = baseVersion;
    latestVersionCoordinatesMap.clear();
    latestVersionCoordinates = null;
    log.debug("-> GAbV: {}:{}:{}", groupId, artifactId, baseVersion);
    return true;
  }

  @Nullable
  public Maven2Metadata onExitBaseVersion() {
    checkState(baseVersion != null);
    log.debug("<- GAbV: {}:{}:{}", groupId, artifactId, baseVersion);
    if (!baseVersion.endsWith(Constants.SNAPSHOT_VERSION_SUFFIX)) {
      // release version does not have version-level metadata
      log.debug("Not a snapshot or nothing to generate: {}:{}:{}", groupId, artifactId, baseVersion);
      return null;
    }
    // this would be the case where unique timestamp snapshots are disabled
    else if (latestVersionCoordinates == null) {
      return Maven2Metadata.newNonUniqueVersionLevel(
          groupId,
          artifactId,
          baseVersion);
    }
    final List<Snapshot> snapshots = new ArrayList<>();
    for (VersionCoordinates versionCoordinates : latestVersionCoordinatesMap.values()) {
      final Coordinates coordinates = versionCoordinates.coordinates;
      final Snapshot snapshotVersion = Maven2Metadata.newSnapshot(
          new DateTime(coordinates.getTimestamp()),
          coordinates.getExtension(),
          coordinates.getClassifier(),
          coordinates.getVersion());
      snapshots.add(snapshotVersion);
    }

    Optional<Long> timestamp = Optional.ofNullable(latestVersionCoordinates.coordinates.getTimestamp());
    Optional<Integer> buildNumber = Optional.ofNullable(latestVersionCoordinates.coordinates.getBuildNumber());

    if (!timestamp.isPresent()) {
      log.warn("Unique timestamp snapshot {}:{}:{} is missing the timestamp and cannot be processed, " +
          "consider removing it manually.", groupId, artifactId, baseVersion);
      log.warn("Missing timestamps might be caused by an invalid version," +
          " check the timestamp in the version {}.", latestVersionCoordinates.version);
      return null;
    }

    return Maven2Metadata.newVersionLevel(
        DateTime.now(),
        groupId,
        artifactId,
        baseVersion,
        timestamp.get(),
        buildNumber.orElse(0),
        snapshots);
  }

  public void addArtifactVersion(final MavenPath mavenPath) {
    checkNotNull(mavenPath);
    Coordinates coordinates = mavenPath.getCoordinates();
    if (mavenPath.isSubordinate() || coordinates == null) {
      return;
    }

    String path = mavenPath.getPath();
    checkState(Objects.equals(groupId, coordinates.getGroupId()), "GroupId:%s Path:%s", groupId, path);
    checkState(Objects.equals(artifactId, coordinates.getArtifactId()), "ArtifactId:%s Path:%s", artifactId, path);
    checkState(Objects.equals(baseVersion, coordinates.getBaseVersion()), "Version:%s Path:%s", baseVersion, path);

    log.debug("Discovered {}:{}:{}:{}:{}",
        coordinates.getGroupId(),
        coordinates.getArtifactId(),
        coordinates.getVersion(),
        coordinates.getClassifier(),
        coordinates.getExtension());

    addBaseVersion(coordinates.getBaseVersion());

    if (!coordinates.isSnapshot()) {
      return;
    }
    if (Objects.equals(coordinates.getBaseVersion(), coordinates.getVersion())) {
      log.debug("Non-timestamped snapshot, ignoring it: {}", mavenPath);
      return;
    }

    final Version version = parseVersion(coordinates.getVersion());
    if (version == null) {
      return; // could not parse, omit it from "latest" maintenance
    }
    final VersionCoordinates versionCoordinates = new VersionCoordinates(version, coordinates);

    // maintain latestVersionCoordinates
    if (latestVersionCoordinates == null || latestVersionCoordinates.version.compareTo(version) < 0) {
      latestVersionCoordinates = versionCoordinates;
    }

    // maintain latestVersionCoordinatesMap
    final String key = key(coordinates);
    final VersionCoordinates other = latestVersionCoordinatesMap.get(key);
    // add if contained version is less than version
    if (other == null || other.version.compareTo(versionCoordinates.version) < 0) {
      latestVersionCoordinatesMap.put(key, versionCoordinates);
    }
  }

  private String key(final Coordinates coordinates) {
    if (coordinates.getClassifier() == null) {
      return coordinates.getExtension();
    }
    else {
      return coordinates.getExtension() + ":" + coordinates.getClassifier();
    }
  }

  @Nullable
  private Version parseVersion(final String version) {
    try {
      return versionScheme.parseVersion(version);
    }
    catch (InvalidVersionSpecificationException e) {
      log.warn("Invalid version: {}", version, e);
      return null;
    }
  }
}
