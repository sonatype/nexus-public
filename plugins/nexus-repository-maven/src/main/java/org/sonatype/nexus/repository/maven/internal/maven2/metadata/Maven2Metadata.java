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
package org.sonatype.nexus.repository.maven.internal.maven2.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Maven 2 metadata VO.
 *
 * @since 3.0
 */
public class Maven2Metadata
{
  public enum Level
  {
    GROUP, ARTIFACT, BASEVERSION;
  }

  // G level

  public static class Plugin
  {
    private final String artifactId;

    private final String prefix;

    private final String name;

    private Plugin(final String artifactId, final String prefix, final String name) {
      this.artifactId = artifactId;
      this.prefix = prefix;
      this.name = name;
    }

    public String getArtifactId() {
      return artifactId;
    }

    public String getPrefix() {
      return prefix;
    }

    public String getName() {
      return name;
    }

    public boolean keyEquals(final Plugin o) {
      return artifactId.equals(o.artifactId) && prefix.equals(o.prefix);
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Plugin)) {
        return false;
      }

      Plugin plugin = (Plugin) o;

      if (!artifactId.equals(plugin.artifactId)) {
        return false;
      }
      if (!prefix.equals(plugin.prefix)) {
        return false;
      }
      return name.equals(plugin.name);
    }

    @Override
    public int hashCode() {
      int result = artifactId.hashCode();
      result = 31 * result + prefix.hashCode();
      result = 31 * result + name.hashCode();
      return result;
    }
  }

  // A level

  public static class BaseVersions
  {
    private final String latest;

    @Nullable
    private final String release;

    private final List<String> versions;

    private BaseVersions(final String latest,
                         @Nullable final String release,
                         final Iterable<String> versions)
    {
      this.latest = latest;
      this.release = release;
      this.versions = ImmutableList.copyOf(versions);
    }

    public String getLatest() {
      return latest;
    }

    @Nullable
    public String getRelease() {
      return release;
    }

    public List<String> getVersions() {
      return versions;
    }
  }

  // V level

  public static class Snapshot
  {
    private final DateTime lastUpdated;

    private final String extension;

    @Nullable
    private final String classifier;

    private final String version;

    private Snapshot(final DateTime lastUpdated,
                     final String extension,
                     @Nullable final String classifier,
                     final String version)
    {
      this.lastUpdated = lastUpdated;
      this.extension = extension;
      this.classifier = classifier;
      this.version = version;
    }

    public DateTime getLastUpdated() {
      return lastUpdated;
    }

    public String getExtension() {
      return extension;
    }

    @Nullable
    public String getClassifier() {
      return classifier;
    }

    public String getVersion() {
      return version;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Snapshot)) {
        return false;
      }

      Snapshot snapshot = (Snapshot) o;

      if (!lastUpdated.equals(snapshot.lastUpdated)) {
        return false;
      }
      if (!extension.equals(snapshot.extension)) {
        return false;
      }
      if (!Objects.equals(classifier, snapshot.classifier)) {
        return false;
      }
      return version.equals(snapshot.version);
    }

    @Override
    public int hashCode() {
      int result = lastUpdated.hashCode();
      result = 31 * result + extension.hashCode();
      result = 31 * result + (classifier != null ? classifier.hashCode() : 0);
      result = 31 * result + version.hashCode();
      return result;
    }
  }

  public static class Snapshots
  {
    private final long snapshotTimestamp;

    private final int snapshotBuildNumber;

    private final List<Snapshot> snapshots;

    private Snapshots(final long snapshotTimestamp,
                      final int snapshotBuildNumber,
                      final List<Snapshot> snapshots)
    {
      this.snapshotTimestamp = snapshotTimestamp;
      this.snapshotBuildNumber = snapshotBuildNumber;
      this.snapshots = ImmutableList.copyOf(snapshots);
    }

    public long getSnapshotTimestamp() {
      return snapshotTimestamp;
    }

    public int getSnapshotBuildNumber() {
      return snapshotBuildNumber;
    }

    public List<Snapshot> getSnapshots() {
      return snapshots;
    }
  }

  // instance

  private final Level level;

  private final DateTime lastUpdated;

  private final String groupId;

  @Nullable
  private final String artifactId;

  @Nullable
  private final String version;

  @Nullable
  private final List<Plugin> plugins;

  @Nullable
  private final BaseVersions baseVersions;

  @Nullable
  private final Snapshots snapshots;

  private Maven2Metadata(final Level level,
                         final DateTime lastUpdated,
                         final String groupId,
                         @Nullable final String artifactId,
                         @Nullable final String version,
                         @Nullable final List<Plugin> plugins,
                         @Nullable final BaseVersions baseVersions,
                         @Nullable final Snapshots snapshots)
  {
    this.level = level;
    this.lastUpdated = lastUpdated;
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    if (plugins != null) {
      this.plugins = ImmutableList.copyOf(plugins);
    }
    else {
      this.plugins = null;
    }
    this.baseVersions = baseVersions;
    this.snapshots = snapshots;
  }

  public Level getLevel() {
    return level;
  }

  public DateTime getLastUpdated() {
    return lastUpdated;
  }

  public String getGroupId() {
    return groupId;
  }

  @Nullable
  public String getArtifactId() {
    return artifactId;
  }

  @Nullable
  public String getVersion() {
    return version;
  }

  @Nullable
  public List<Plugin> getPlugins() {
    return plugins;
  }

  @Nullable
  public BaseVersions getBaseVersions() {
    return baseVersions;
  }

  @Nullable
  public Snapshots getSnapshots() {
    return snapshots;
  }

  public static Plugin newPlugin(final String artifactId, final String prefix, final String name) {
    checkNotNull(artifactId);
    checkNotNull(prefix);
    return new Plugin(artifactId, prefix, Strings.isNullOrEmpty(name) ? artifactId : name);
  }

  public static Maven2Metadata newGroupLevel(final DateTime lastUpdated,
                                             final String groupId,
                                             final List<Plugin> plugins)
  {
    checkNotNull(lastUpdated);
    checkNotNull(groupId);
    checkNotNull(plugins);
    return new Maven2Metadata(Level.GROUP, lastUpdated, groupId, null, null, plugins, null, null);
  }

  public static Maven2Metadata newArtifactLevel(final DateTime lastUpdated,
                                                final String groupId,
                                                final String artifactId,
                                                final String latest,
                                                @Nullable final String release,
                                                final Iterable<String> versions)
  {
    checkNotNull(lastUpdated);
    checkNotNull(groupId);
    checkNotNull(artifactId);
    checkNotNull(latest);
    checkNotNull(versions);
    final BaseVersions bvs = new BaseVersions(latest, release, versions);
    return new Maven2Metadata(Level.ARTIFACT, lastUpdated, groupId, artifactId, null, null, bvs, null);
  }

  public static Snapshot newSnapshot(final DateTime lastUpdated,
                                     final String extension,
                                     @Nullable final String classifier,
                                     final String version)
  {
    checkNotNull(lastUpdated);
    checkNotNull(extension);
    checkNotNull(version);
    return new Snapshot(lastUpdated, extension, classifier, version);
  }

  public static Maven2Metadata newVersionLevel(final DateTime lastUpdated,
                                               final String groupId,
                                               final String artifactId,
                                               final String version,
                                               final long snapshotTimestamp,
                                               final int snapshotBuildNumber,
                                               @Nullable final List<Snapshot> snapshots)
  {
    checkNotNull(lastUpdated);
    checkNotNull(groupId);
    checkNotNull(artifactId);
    checkNotNull(version);
    checkArgument(snapshotTimestamp > 0);
    checkArgument(snapshotBuildNumber > 0);
    List<Snapshot> ss = new ArrayList<>();
    if (snapshots != null) {
      ss.addAll(snapshots);
    }
    final Snapshots snaps = new Snapshots(snapshotTimestamp, snapshotBuildNumber, ss);
    return new Maven2Metadata(Level.BASEVERSION, lastUpdated, groupId, artifactId, version, null, null, snaps);
  }
}
