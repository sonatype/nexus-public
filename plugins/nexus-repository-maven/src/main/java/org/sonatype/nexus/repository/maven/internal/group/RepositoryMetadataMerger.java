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
package org.sonatype.nexus.repository.maven.internal.group;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.VersionComparator;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.internal.Constants;
import org.sonatype.nexus.repository.maven.internal.MavenModels;
import org.sonatype.nexus.repository.view.Content;

import com.google.common.base.Strings;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.Versioning;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.VersionComparator.version;

/**
 * Maven 2 repository metadata merger.
 *
 * @since 3.0
 */
public class RepositoryMetadataMerger
    extends ComponentSupport
{
  /**
   * Merges the contents of passed in metadata.
   */
  public void merge(final OutputStream outputStream,
                    final MavenPath mavenPath,
                    final Map<Repository, Content> contents)
  {
    log.debug("Merge metadata for {}", mavenPath.getPath());
    List<Envelope> metadatas = new ArrayList<>(contents.size());
    try {
      for (Map.Entry<Repository, Content> entry : contents.entrySet()) {
        addReadMetadata(mavenPath, metadatas, entry);
      }

      final Metadata mergedMetadata = merge(metadatas);
      if (mergedMetadata == null) {
        return;
      }
      MavenModels.writeMetadata(outputStream, mergedMetadata);
    }
    catch (IOException e) {
      log.error("Unable to merge {}", mavenPath, e);
    }
  }

  private void addReadMetadata(final MavenPath mavenPath,
                               final List<Envelope> metadatas,
                               final Entry<Repository, Content> entry) throws IOException
  {
    final String origin = entry.getKey().getName() + " @ " + mavenPath.getPath();
    try {
      final Metadata metadata = MavenModels.readMetadata(entry.getValue().openInputStream());
      if (metadata == null) {
        log.debug("Corrupted repository metadata: {}, source: {}", origin, entry.getValue());
        return;
      }
      metadatas.add(new Envelope(origin, metadata));
    }
    catch (IOException e) {
      log.debug("Error downloading repository metadata: {}, source: {}", origin, entry.getValue());
      throw new IOException("Error downloading repository metadata for " + origin + ": " + e.getMessage(), e);
    }
  }

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
   * Plugin comparator that uses artifactId to sort plugin elements.
   */
  private static final Comparator<Plugin> pluginComparator = new Comparator<Plugin>()
  {
    @Override
    public int compare(final Plugin p1, final Plugin p2) {
      return p1.getArtifactId().compareTo(p2.getArtifactId());
    }
  };

  /**
   * Envelope that tracks origin of the data.
   */
  public static class Envelope
  {
    private final String origin;

    private final Metadata data;

    public Envelope(final String origin, final Metadata data) {
      this.origin = checkNotNull(origin);
      this.data = checkNotNull(data);
    }

    @Nonnull
    public String getOrigin() {
      return origin;
    }

    @Nonnull
    public Metadata getData() {
      return data;
    }
  }

  /**
   * If string Null-Or-Empty returns empty string, otherwise the string.
   */
  @Nonnull
  private static String nullOrEmptyStringFilter(final String str) {
    if (Strings.isNullOrEmpty(str)) {
      return "";
    }
    return str.trim();
  }

  /**
   * Filters a list for {@code null} (and "null" string) elements, removes them.
   */
  private static <E> void nullElementFilter(final List<E> list) {
    for (Iterator<E> i = list.iterator(); i.hasNext(); ) {
      E element = i.next();
      if (element == null || Objects.equals(element, "null")) {
        i.remove();
      }
    }
  }

  /**
   * Test metadata for equality.  Note timestamp is not considered.
   */
  public boolean metadataEquals(final Metadata md1, final Metadata md2) {
    checkNotNull(md1);
    checkNotNull(md2);
    return
      Objects.equals(md1.getGroupId(), md2.getGroupId()) && // NOSONAR
      Objects.equals(md1.getArtifactId(), md2.getArtifactId()) &&
      Objects.equals(md1.getVersion(), md2.getVersion()) &&
      versioningEquals(md1.getVersioning(), md2.getVersioning()) &&
      pluginsEquals(md1.getPlugins(), md2.getPlugins()); // NOSONAR
  }

  private boolean versioningEquals(@Nullable final Versioning v1,
                                   @Nullable final Versioning v2) { // NOSONAR
    if (v1 == null || v2 == null) {
      return v1 == v2; // NOSONAR
    }
    else {
      return
        Objects.equals(v1.getLatest(), v2.getLatest()) && // NOSONAR
        Objects.equals(v1.getRelease(), v2.getRelease()) &&
        snapshotEquals(v1.getSnapshot(), v2.getSnapshot()) &&
        Objects.equals(v1.getVersions(), v2.getVersions()) &&
        snapshotVersionsEquals(v1.getSnapshotVersions(), v2.getSnapshotVersions());
    }
  }

  private boolean snapshotEquals(@Nullable final Snapshot s1,
                                 @Nullable final Snapshot s2) {
    if (s1 == null || s2 == null) {
      return s1 == s2; // NOSONAR
    }
    else {
      return
        Objects.equals(s1.getTimestamp(), s2.getTimestamp()) &&
        s1.getBuildNumber() == s2.getBuildNumber() &&
        s1.isLocalCopy() == s2.isLocalCopy();
    }
  }

  private boolean snapshotVersionsEquals(@Nullable final List<SnapshotVersion> s1,
                                         @Nullable final List<SnapshotVersion> s2) {
    if (s1 == null || s2 == null) {
      return s1 == s2; // NOSONAR
    }
    else if (s1.size() != s2.size()) {
      return false;
    }
    else {
      return IntStream.range(0, s1.size()).allMatch(i -> snapshotVersionEquals(s1.get(i), s2.get(i)));
    }
  }

  private boolean snapshotVersionEquals(final SnapshotVersion s1, final SnapshotVersion s2) {
    return
      Objects.equals(s1.getClassifier(), s2.getClassifier()) && // NOSONAR
      Objects.equals(s1.getExtension(), s2.getExtension()) &&
      Objects.equals(s1.getVersion(), s2.getVersion()) &&
      Objects.equals(s1.getUpdated(), s2.getUpdated());
  }

  private boolean pluginsEquals(@Nullable final List<Plugin> p1,
                                @Nullable final List<Plugin> p2) {
    if (p1 == null || p2 == null) {
      return p1 == p2; // NOSONAR
    }
    else if (p1.size() != p2.size()) {
      return false;
    }
    else {
      return IntStream.range(0, p1.size()).allMatch(i -> pluginEquals(p1.get(i), p2.get(i)));
    }
  }

  private boolean pluginEquals(final Plugin p1, final Plugin p2) {
    return
      Objects.equals(p1.getName(), p2.getName()) &&
      Objects.equals(p1.getPrefix(), p2.getPrefix()) &&
      Objects.equals(p1.getArtifactId(), p2.getArtifactId());
  }

  /**
   * Merges Maven2 repository metadata in received order. Returns {@code null} if passed in iterable is
   * empty. The passed in instances are not mutated.
   */
  @Nullable
  public Metadata merge(final Iterable<Envelope> metadatas) {
    checkNotNull(metadatas);
    Metadata result = null;
    for (Envelope envelope : metadatas) {
      if (result == null) {
        result = envelope.getData().clone();
      }
      else {
        try {
          result = merge(result, envelope.getData().clone());
        }
        catch (IllegalArgumentException e) {
          // leave out, log it
          log.warn("Bad data {}", envelope.getOrigin(), e);
        }
      }
    }
    if (result == null) {
      return null;
    }
    if (result.getVersioning() != null && !result.getVersioning().getVersions().isEmpty()) {
      Collections.sort(result.getVersioning().getVersions(), VersionComparator.INSTANCE);
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
   * Merges the "source" on top of "target" and returns the "target", the instances are mutated.
   */
  private Metadata merge(final Metadata target, final Metadata source) {
    // sanity checks
    if (Strings.isNullOrEmpty(source.getGroupId())) {
      source.setGroupId(target.getGroupId());
    }
    if (Strings.isNullOrEmpty(source.getArtifactId())) {
      source.setArtifactId(target.getArtifactId());
    }

    // version differs: we do it "both ways" if set at all
    if (Strings.isNullOrEmpty(target.getVersion())) {
      target.setVersion(source.getVersion());
    }
    if (Strings.isNullOrEmpty(source.getVersion())) {
      source.setVersion(target.getVersion());
    }
    checkArgument(
        Objects.equals(nullOrEmptyStringFilter(target.getGroupId()), nullOrEmptyStringFilter(source.getGroupId())),
        "GroupId mismatch: %s vs %s", target.getGroupId(), source.getGroupId());
    checkArgument(
        Objects
            .equals(nullOrEmptyStringFilter(target.getArtifactId()), nullOrEmptyStringFilter(source.getArtifactId())),
        "ArtifactId mismatch: %s vs %s", target.getArtifactId(), source.getArtifactId());

    String targetVersion = nullOrEmptyStringFilter(target.getVersion());
    String sourceVersion = nullOrEmptyStringFilter(source.getVersion());

    // As per NEXUS-13085 allow this and log for support in case the resulting merge leads to downstream problems
    if (!Objects.equals(targetVersion, sourceVersion)) {
      log.warn("Merging with version mismatch for GA={}:{}, {} vs {}", target.getGroupId(), target.getArtifactId(),
          targetVersion, sourceVersion);
    }
    
    mergePlugins(target, source);
    mergeVersioning(target, source);
    return target;
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
   * Parses string into a long (accepts strings with dots too, like maven timestamp is, where dot is between date and
   * time). If fails or is null, returns -1.
   */
  private long ts(final String ts) {
    try {
      if (ts != null) {
        return Long.parseLong(ts.replace(".", ""));
      }
    }
    catch (NumberFormatException e) {
      // Just fall through and return -1 just as if ts were null
    }

    return -1;
  }
}
