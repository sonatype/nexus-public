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

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.internal.Constants;
import org.sonatype.nexus.repository.maven.internal.group.RepositoryMetadataMerger;
import org.sonatype.nexus.repository.maven.internal.group.RepositoryMetadataMerger.Envelope;
import org.sonatype.nexus.repository.maven.internal.hosted.metadata.Maven2Metadata.Plugin;
import org.sonatype.nexus.repository.maven.internal.hosted.metadata.Maven2Metadata.Snapshot;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Maven 2 repository metadata updater.
 *
 * @since 3.0
 */
abstract public class AbstractMetadataUpdater
    extends ComponentSupport
{
  private final boolean update;

  protected final Repository repository;

  private final RepositoryMetadataMerger repositoryMetadataMerger;

  /**
   * @param update Determines whether existing content should be updated or replaced.
   * @param repository The {@link Repository} to update.
   */
  public AbstractMetadataUpdater(final boolean update, final Repository repository) {
    this.update = update;
    this.repository = checkNotNull(repository);
    this.repositoryMetadataMerger = new RepositoryMetadataMerger();
  }

  /**
   * Processes metadata, depending on {@link #update} value and input value of metadata parameter. If input is
   * non-null, will update or replace depending on value of {@link #update}. If input is null, will delete if {@link
   * #update} is {@code false}.
   */
  public void processMetadata(final MavenPath metadataPath, final Maven2Metadata metadata) {
    if (metadata != null) {
      log.debug("Processing Metadata {} to Path {}", metadata, metadataPath.getPath());
      if (update) {
        update(metadataPath, metadata);
      }
      else {
        replace(metadataPath, metadata);
      }
    }
    else if (!update) {
      log.debug("Deleting Metadata from {}", metadataPath.getPath());
      delete(metadataPath);
    }
  }

  /**
   * Writes/updates metadata, merges existing one, if any.
   */
  @VisibleForTesting
  protected void update(final MavenPath mavenPath, final Maven2Metadata metadata) {
    log.debug("Updating Metadata in {}", mavenPath.getPath());
    try {
      checkNotNull(mavenPath);
      checkNotNull(metadata);

      final Optional<Metadata> oldMetadata = safeRead(mavenPath);
      if (oldMetadata.isPresent()) {
        final Metadata updated = repositoryMetadataMerger.merge(
            ImmutableList.of(
                new Envelope(repository.getName() + ":" + mavenPath.getPath(), oldMetadata.get()),
                new Envelope("new:" + mavenPath.getPath(), toMetadata(metadata))
            )
        );
        writeIfChanged(mavenPath, oldMetadata.get(), updated);
      }
      else {
        write(mavenPath, toMetadata(metadata));
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Writes/overwrites metadata, replacing existing one, if any.
   */
  protected void replace(final MavenPath mavenPath, final Maven2Metadata metadata) {
    log.debug("Replacing Metadata in {}", mavenPath.getPath());
    try {
      checkNotNull(mavenPath);
      checkNotNull(metadata);

      final Optional<Metadata> oldMetadata = safeRead(mavenPath);
      if (oldMetadata.isPresent()) {
        final Metadata updated = toMetadata(metadata);
        writeIfChanged(mavenPath, oldMetadata.get(), updated);
      }
      else {
        write(mavenPath, toMetadata(metadata));
        log.debug("Wrote new metadata for {}", mavenPath.getPath());
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /*
   * Attempts to read the metadata, if an error occurs an empty optional is returned. This should be used when
   * operations can safely proceed if the metadata can be overwritten.
   */
  private Optional<Metadata> safeRead(final MavenPath mavenPath) {
    try {
      return read(mavenPath);
    }
    catch (Exception e) {
      log.warn("Encountered error reading {}", mavenPath, e);
      return Optional.empty();
    }
  }

  /**
   * Converts NX VO into Apache Maven {@link Metadata}.
   */
  private Metadata toMetadata(final Maven2Metadata maven2Metadata) {
    final Metadata result = new Metadata();
    result.setModelVersion("1.1.0");
    result.setGroupId(maven2Metadata.getGroupId());
    result.setArtifactId(maven2Metadata.getArtifactId());
    result.setVersion(maven2Metadata.getVersion());
    if (maven2Metadata.getPlugins() != null) {
      for (Plugin plugin : maven2Metadata.getPlugins()) {
        final org.apache.maven.artifact.repository.metadata.Plugin mPlugin = new org.apache.maven.artifact.repository.metadata.Plugin();
        mPlugin.setArtifactId(plugin.getArtifactId());
        mPlugin.setPrefix(plugin.getPrefix());
        mPlugin.setName(plugin.getName());
        result.addPlugin(mPlugin);
      }
    }
    if (maven2Metadata.getBaseVersions() != null) {
      final Versioning versioning = new Versioning();
      versioning.setLatest(maven2Metadata.getBaseVersions().getLatest());
      versioning.setRelease(maven2Metadata.getBaseVersions().getRelease());
      versioning.setVersions(maven2Metadata.getBaseVersions().getVersions());
      versioning.setLastUpdated(Constants.METADATA_DOTLESS_TIMESTAMP.print(maven2Metadata.getLastUpdated()));
      result.setVersioning(versioning);
    }
    if (maven2Metadata.getSnapshots() != null) {
      final Versioning versioning = result.getVersioning() != null ? result.getVersioning() : new Versioning();
      final org.apache.maven.artifact.repository.metadata.Snapshot snapshot = new org.apache.maven.artifact.repository.metadata.Snapshot();
      if (maven2Metadata.getSnapshots().getSnapshotTimestamp() != null) {
        snapshot.setTimestamp(Constants.METADATA_DOTTED_TIMESTAMP.print(
            new DateTime(maven2Metadata.getSnapshots().getSnapshotTimestamp())));
      }
      snapshot.setBuildNumber(maven2Metadata.getSnapshots().getSnapshotBuildNumber());
      versioning.setSnapshot(snapshot);

      final List<SnapshotVersion> snapshotVersions = Lists.newArrayList();
      for (Snapshot snap : maven2Metadata.getSnapshots().getSnapshots()) {
        final SnapshotVersion snapshotVersion = new SnapshotVersion();
        snapshotVersion.setExtension(snap.getExtension());
        snapshotVersion.setClassifier(snap.getClassifier());
        snapshotVersion.setVersion(snap.getVersion());
        snapshotVersion.setUpdated(Constants.METADATA_DOTLESS_TIMESTAMP.print(snap.getLastUpdated()));
        snapshotVersions.add(snapshotVersion);
      }
      versioning.setSnapshotVersions(snapshotVersions);
      versioning.setLastUpdated(Constants.METADATA_DOTLESS_TIMESTAMP.print(maven2Metadata.getLastUpdated()));
      result.setVersioning(versioning);
    }
    return result;
  }

  private void writeIfChanged(final MavenPath mavenPath, final Metadata oldMetadata, final Metadata newMetadata)
      throws IOException
  {
    if (repositoryMetadataMerger.metadataEquals(oldMetadata, newMetadata)) {
      log.info("Metadata for {} hasn't changed, skipping", mavenPath.getPath());
    }
    else {
      write(mavenPath, newMetadata);
    }
  }

  /**
   * Writes passed in metadata as XML.
   */
  abstract protected void write(final MavenPath mavenPath, final Metadata metadata) throws IOException;

  /**
   * Reads metadata from XML.
   */
  abstract protected Optional<Metadata> read(final MavenPath mavenPath) throws IOException;

  /**
   * Deletes metadata.
   */
  abstract protected void delete(final MavenPath mavenPath);
}
