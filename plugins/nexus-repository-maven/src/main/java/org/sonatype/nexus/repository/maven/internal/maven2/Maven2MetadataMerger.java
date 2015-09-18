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
package org.sonatype.nexus.repository.maven.internal.maven2;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionScheme;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Maven 2 repository metadata merge.
 *
 * @since 3.0
 */
public class Maven2MetadataMerger
    extends ComponentSupport
{
  /**
   * Model version, since Maven 3.x it is "1.1.0".
   */
  private enum ModelVersion
  {
    V1_0_0("1.0.0"), V1_1_0("1.1.0");

    private final String versionString;

    ModelVersion(final String versionString) {
      this.versionString = versionString;
    }

    public String getVersionString() {
      return versionString;
    }
  }

  /**
   * Generic version scheme for parsing versions from string.
   */
  private final VersionScheme versionScheme = new GenericVersionScheme();

  /**
   * Version comparator that uses version scheme to sort version strings.
   */
  private final Comparator<String> versionStringComparator = new Comparator<String>()
  {
    @Override
    public int compare(final String o1, final String o2) {
      return version(o1).compareTo(version(o2));
    }
  };

  /**
   * Plugin comparator that uses artifactId to sort plugin elements.
   */
  private final Comparator<Plugin> pluginComparator = new Comparator<Plugin>()
  {
    @Override
    public int compare(final Plugin p1, final Plugin p2) {
      return p1.getArtifactId().compareTo(p2.getArtifactId());
    }
  };

  /**
   * Parses out Aether version from a string.
   */
  private Version version(final String version) {
    try {
      return versionScheme.parseVersion(version);
    }
    catch (InvalidVersionSpecificationException e) {
      // this never happens, see implementation
      throw Throwables.propagate(e);
    }
  }

  public static class MetadataEnvelope {
    private final String origin;

    private final Metadata metadata;

    public MetadataEnvelope(final String origin, final Metadata metadata) {
      this.origin = checkNotNull(origin);
      this.metadata = checkNotNull(metadata);
    }

    @Nonnull
    public String getOrigin() {
      return origin;
    }

    @Nonnull
    public Metadata getMetadata() {
      return metadata;
    }
  }

  /**
   * Merges Maven2 repository metadata in received order. Returns {@code null} if passed in iterable is
   * empty. The passed in instances are not mutated.
   */
  @Nullable
  public Metadata merge(final Iterable<MetadataEnvelope> metadatas) {
    checkNotNull(metadatas);
    Metadata result = null;
    for (MetadataEnvelope metadataEnvelope : metadatas) {
      if (result == null) {
        result = metadataEnvelope.getMetadata().clone();
      }
      else {
        try {
          result = merge(result, metadataEnvelope.getMetadata().clone());
        }
        catch (IllegalArgumentException e) {
          // leave out, log it
          log.warn("Bad metadata {}", metadataEnvelope.getOrigin(), e);
        }
      }
    }
    if (result == null) {
      return null;
    }
    if (result.getVersioning() != null && !result.getVersioning().getVersions().isEmpty()) {
      Collections.sort(result.getVersioning().getVersions(), versionStringComparator);
      // the last in ordered list
      String latest = result.getVersioning().getVersions().get(result.getVersioning().getVersions().size() - 1);
      // the last non-snapshot in ordered list, may be null
      String release = null;
      for (int i = result.getVersioning().getVersions().size() - 1; i >= 0; i--) {
        if (!result.getVersioning().getVersions().get(i).endsWith(Constants.SNAPSHOT_VERSION_SUFFIX)) {
          release = result.getVersioning().getVersions().get(i);
          break;
        }
      }
      result.getVersioning().setLatest(latest);
      result.getVersioning().setRelease(release);
    }
    if (!result.getPlugins().isEmpty()) {
      Collections.sort(result.getPlugins(), pluginComparator);
    }

    // model version, just set it to latest, we don't care about it now
    result.setModelVersion(ModelVersion.V1_1_0.getVersionString());

    return result;
  }

  /**
   * Merges the "right" on top of "left" and returns the result, the instances are mutated.
   */
  private Metadata merge(final Metadata left, final Metadata right) {
    // sanity checks
    if (Strings.isNullOrEmpty(right.getGroupId())) {
      right.setGroupId(left.getGroupId());
    }
    if (Strings.isNullOrEmpty(right.getArtifactId())) {
      right.setArtifactId(left.getArtifactId());
    }

    // version differs: we do it "both ways" if set at all
    if (Strings.isNullOrEmpty(left.getVersion())) {
      left.setVersion(right.getVersion());
    }
    if (Strings.isNullOrEmpty(right.getVersion())) {
      right.setVersion(left.getVersion());
    }
    checkArgument(
        Objects.equals(nullOrEmptyStringFilter(left.getGroupId()), nullOrEmptyStringFilter(right.getGroupId())),
        "GroupId mismatch: %s vs %s", left.getGroupId(), right.getGroupId());
    checkArgument(
        Objects.equals(nullOrEmptyStringFilter(left.getArtifactId()), nullOrEmptyStringFilter(right.getArtifactId())),
        "ArtifactId mismatch: %s vs %s", left.getArtifactId(), right.getArtifactId());
    checkArgument(
        Objects.equals(nullOrEmptyStringFilter(left.getVersion()), nullOrEmptyStringFilter(right.getVersion())),
        "Version mismatch: %s vs %s", left.getVersion(), right.getVersion());

    mergePlugins(left, right);
    mergeVersioning(left, right);
    return left;
  }

  /**
   * Merges "right" plugins into "left", the instances are mutated.
   */
  private void mergePlugins(final Metadata left, final Metadata right) {
    nullElementFilter(left.getPlugins());
    nullElementFilter(right.getPlugins());
    for (Plugin plugin : right.getPlugins()) {
      boolean found = false;
      for (Plugin preExisting : left.getPlugins()) {
        if (Objects.equals(preExisting.getArtifactId(), plugin.getArtifactId())
            && Objects.equals(preExisting.getPrefix(), plugin.getPrefix())) {
          found = true;
          preExisting.setName(plugin.getName());
          break;
        }
      }
      if (!found) {
        Plugin newPlugin = new Plugin();
        newPlugin.setArtifactId(plugin.getArtifactId());
        newPlugin.setPrefix(plugin.getPrefix());
        newPlugin.setName(plugin.getName());
        left.addPlugin(newPlugin);
      }
    }
  }

  /**
   * Merges "right" versioning into "left", the instances are mutated.
   */
  private void mergeVersioning(final Metadata left, final Metadata right) {
    if (right.getVersioning() == null) {
      return; // nothing to do
    }
    if (left.getVersioning() == null) {
      left.setVersioning(new Versioning());
    }
    final Versioning rv = right.getVersioning();
    final Versioning lv = left.getVersioning();

    // lastUpdated: if left not set, set from right, otherwise newer
    if (rv.getLastUpdated() != null) {
      if (lv.getLastUpdated() == null) {
        lv.setLastUpdated(rv.getLastUpdated());
      }
      else {
        final long lts = ts(lv.getLastUpdated());
        final long rts = ts(rv.getLastUpdated());
        if (rts > lts) {
          lv.setLastUpdated(rv.getLastUpdated());
        }
      }
    }

    // versions: just add strings not in list
    nullElementFilter(lv.getVersions());
    nullElementFilter(rv.getVersions());
    for (String version : rv.getVersions()) {
      if (!lv.getVersions().contains(version)) {
        lv.getVersions().add(version);
      }
    }

    // snapshot: add if right has it, and left does not have it, or left is older
    if (rv.getSnapshot() != null) {
      if (lv.getSnapshot() == null) {
        lv.setSnapshot(rv.getSnapshot());
      }
      else {
        final long lts = ts(lv.getSnapshot().getTimestamp());
        final long rts = ts(rv.getSnapshot().getTimestamp());
        if (rts > lts) {
          lv.setSnapshot(rv.getSnapshot());
        }
      }
    }

    // snapshotVersions: add ext+classifier combos, if not exist, or are older version
    nullElementFilter(lv.getSnapshotVersions());
    nullElementFilter(rv.getSnapshotVersions());
    for (SnapshotVersion snapshotVersion : rv.getSnapshotVersions()) {
      boolean found = false;
      for (SnapshotVersion preExisting : lv.getSnapshotVersions()) {
        if (Objects.equals(snapshotVersion.getExtension(), preExisting.getExtension())
            && Objects.equals(nullOrEmptyStringFilter(snapshotVersion.getClassifier()),
            nullOrEmptyStringFilter(preExisting.getClassifier()))) {
          found = true;
          if (version(snapshotVersion.getVersion()).compareTo(version(preExisting.getVersion())) > 0) {
            preExisting.setClassifier(nullOrEmptyStringFilter(snapshotVersion.getClassifier()));
            preExisting.setVersion(snapshotVersion.getVersion());
            preExisting.setUpdated(snapshotVersion.getUpdated());
          }
          break;
        }
      }
      if (!found) {
        final SnapshotVersion newSnapshotVersion = new SnapshotVersion();
        newSnapshotVersion.setExtension(snapshotVersion.getExtension());
        newSnapshotVersion.setClassifier(snapshotVersion.getClassifier());
        newSnapshotVersion.setVersion(snapshotVersion.getVersion());
        newSnapshotVersion.setUpdated(snapshotVersion.getUpdated());
        lv.getSnapshotVersions().add(newSnapshotVersion);
      }
    }
  }

  /**
   * If string Null-Or-Empty returns empty string, otherwise the string.
   */
  @Nonnull
  private String nullOrEmptyStringFilter(final String str) {
    if (Strings.isNullOrEmpty(str)) {
      return "";
    }
    return str.trim();
  }

  /**
   * Filters a list for {@code null} (and "null" string) elements, removes them.
   */
  private <T> void nullElementFilter(final List<T> list) {
    for (Iterator<T> i = list.iterator(); i.hasNext(); ) {
      T element = i.next();
      if (element == null || Objects.equals(element, "null")) {
        i.remove();
      }
    }
  }

  /**
   * Parses string into a long (accepts strings with dots too, like maven timestamp is, where dot is between date and
   * time). If fails, returns -1.
   */
  private long ts(final String ts) {
    try {
      return Long.parseLong(ts.replace(".", ""));
    }
    catch (NumberFormatException e) {
      return -1;
    }
  }
}
