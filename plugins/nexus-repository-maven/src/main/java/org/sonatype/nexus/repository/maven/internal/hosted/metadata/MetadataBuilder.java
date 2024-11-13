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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.Coordinates;
import org.sonatype.nexus.repository.maven.internal.Constants;
import org.sonatype.nexus.repository.maven.internal.hosted.metadata.Maven2Metadata.Plugin;
import org.sonatype.nexus.repository.maven.internal.hosted.metadata.Maven2Metadata.Snapshot;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionScheme;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Maven 2 repository metadata builder.
 *
 * @since 3.0
 */
public class MetadataBuilder
    extends ComponentSupport
{
  private final VersionScheme versionScheme;

  private String groupId;

  private String artifactId;

  private String baseVersion;

  // G level

  private final List<Plugin> plugins;

  // A level

  private final NavigableSet<Version> baseVersions;

  // V level

  private final Map<String, VersionCoordinates> latestVersionCoordinatesMap;

  private VersionCoordinates latestVersionCoordinates;

  public MetadataBuilder() {
    this.versionScheme = new GenericVersionScheme();
    // G
    this.plugins = new ArrayList<>();
    // A
    this.baseVersions = new TreeSet<>();
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
    baseVersions.clear();
    log.debug("-> GA: {}:{}", groupId, artifactId);
    return true;
  }

  @Nullable
  public Maven2Metadata onExitArtifactId() {
    checkState(artifactId != null);
    log.debug("<- GA: {}:{}", groupId, artifactId);
    if (baseVersions.isEmpty()) {
      log.debug("Nothing to generate: {}:{}", groupId, artifactId);
      return null;
    }
    Iterator<Version> vi = baseVersions.descendingIterator();
    String latest = vi.next().toString();
    String release = latest;
    while (release.endsWith(Constants.SNAPSHOT_VERSION_SUFFIX) && vi.hasNext()) {
      release = vi.next().toString();
    }
    if (release.endsWith(Constants.SNAPSHOT_VERSION_SUFFIX)) {
      release = null;
    }
    return Maven2Metadata.newArtifactLevel(
        DateTime.now(),
        groupId,
        artifactId,
        latest,
        release,
        Iterables.transform(baseVersions, new Function<Version, String>()
        {
          @Override
          public String apply(final Version input) {
            return input.toString();
          }
        }));
  }

  public void addBaseVersion(final String baseVersion) {
    checkNotNull(baseVersion);
    try {
      if (baseVersions.add(versionScheme.parseVersion(baseVersion))) {
        log.debug("Added base version {}:{}:{}", groupId, artifactId, baseVersion);
      }
    }
    catch (InvalidVersionSpecificationException e) {
      // According to versionScheme implementation we use, this exception will never happen
      // log + ignore
      log.info("Invalid baseVersion discovered: " + baseVersion, e);
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
    //this would be the case where unique timestamp snapshots are disabled
    else if (latestVersionCoordinates == null) {
      return Maven2Metadata.newNonUniqueVersionLevel(
          groupId,
          artifactId,
          baseVersion
      );
    }
    final List<Snapshot> snapshots = new ArrayList<>();
    for (VersionCoordinates versionCoordinates : latestVersionCoordinatesMap.values()) {
      final Coordinates coordinates = versionCoordinates.coordinates;
      final Snapshot snapshotVersion = Maven2Metadata.newSnapshot(
          new DateTime(coordinates.getTimestamp()),
          coordinates.getExtension(),
          coordinates.getClassifier(),
          coordinates.getVersion()
      );
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
        snapshots
    );
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
