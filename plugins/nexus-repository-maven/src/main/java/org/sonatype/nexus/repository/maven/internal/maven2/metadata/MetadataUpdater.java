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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.HashType;
import org.sonatype.nexus.repository.maven.internal.maven2.Constants;
import org.sonatype.nexus.repository.maven.internal.maven2.Maven2MetadataMerger;
import org.sonatype.nexus.repository.maven.internal.maven2.Maven2MetadataMerger.MetadataEnvelope;
import org.sonatype.nexus.repository.maven.internal.maven2.Maven2MimeRulesSource;
import org.sonatype.nexus.repository.maven.internal.maven2.metadata.Maven2Metadata.Plugin;
import org.sonatype.nexus.repository.maven.internal.maven2.metadata.Maven2Metadata.Snapshot;
import org.sonatype.nexus.repository.util.TypeTokens;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.transaction.Operation;
import org.sonatype.nexus.transaction.Operations;
import org.sonatype.nexus.transaction.Transactional;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.orientechnologies.common.concur.ONeedRetryException;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Maven 2 repository metadata updater.
 *
 * @since 3.0
 */
public class MetadataUpdater
    extends ComponentSupport
{
  private final boolean update;

  private final Repository repository;

  private final MavenFacet mavenFacet;

  private final Maven2MetadataMerger metadataMerger;

  private final MetadataXpp3Reader metadataReader;

  private final MetadataXpp3Writer metadataWriter;

  public MetadataUpdater(final boolean update, final Repository repository) {
    this.update = update;
    this.repository = checkNotNull(repository);
    this.mavenFacet = repository.facet(MavenFacet.class);
    this.metadataMerger = new Maven2MetadataMerger();
    this.metadataReader = new MetadataXpp3Reader();
    this.metadataWriter = new MetadataXpp3Writer();
  }

  /**
   * Processes metadata, depending on {@link #update} value and input value of metadata parameter. If input is
   * non-null, will update or replace depending on value of {@link #update}. If update is null, will delete if {@link
   * #update} is {@code false}.
   */
  public void processMetadata(final MavenPath metadataPath, final Maven2Metadata metadata) {
    if (metadata != null) {
      if (update) {
        update(metadataPath, metadata);
      }
      else {
        replace(metadataPath, metadata);
      }
    }
    else if (!update) {
      delete(metadataPath);
    }
  }

  /**
   * Writes/updates metadata, merges existing one, if any.
   */
  @VisibleForTesting
  void update(final MavenPath mavenPath, final Maven2Metadata metadata) {
    Operations.transactional(new Operation<Void, RuntimeException>()
    {
      @Transactional(retryOn = ONeedRetryException.class)
      public Void call() {
        checkNotNull(mavenPath);
        checkNotNull(metadata);
        try {
          final Metadata oldMetadata = read(mavenPath);
          if (oldMetadata == null) {
            // old does not exists, just write it
            write(mavenPath, toMetadata(metadata));
          }
          else {
            final Metadata updated = metadataMerger.merge(
                ImmutableList.of(
                    new MetadataEnvelope(repository.getName() + ":" + mavenPath.getPath(), oldMetadata),
                    new MetadataEnvelope("new:" + mavenPath.getPath(), toMetadata(metadata))
                )
            );
            write(mavenPath, updated);
          }
          return null;
        }
        catch (IOException e) {
          throw Throwables.propagate(e);
        }
      }
    });
  }

  /**
   * Writes/overwrites metadata, replacing existing one, if any.
   */
  @VisibleForTesting
  void replace(final MavenPath mavenPath, final Maven2Metadata metadata) {
    checkNotNull(mavenPath);
    checkNotNull(metadata);
    try {
      write(mavenPath, toMetadata(metadata));
    }
    catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  /**
   * Deletes metadata.
   */
  @VisibleForTesting
  void delete(final MavenPath mavenPath) {
    checkNotNull(mavenPath);
    try {
      final List<MavenPath> paths = new ArrayList<>();
      paths.add(mavenPath);
      for (HashType hashType : HashType.values()) {
        paths.add(mavenPath.hash(hashType));
      }
      mavenFacet.delete(paths.toArray(new MavenPath[paths.size()]));
    }
    catch (IOException e) {
      throw Throwables.propagate(e);
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
      snapshot.setTimestamp(Constants.METADATA_DOTTED_TIMESTAMP.print(
          new DateTime(maven2Metadata.getSnapshots().getSnapshotTimestamp())));
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

  /**
   * Reads up existing metadata and parses it, or {@code null}. If metadata unparseable (corrupted) also {@code null}.
   */
  @Nullable
  private Metadata read(final MavenPath mavenPath) throws IOException {
    final Content content = mavenFacet.get(mavenPath);
    if (content == null) {
      return null;
    }
    else {
      try (InputStream is = content.openInputStream()) {
        return metadataReader.read(is);
      }
      catch (XmlPullParserException e) {
        log.warn("Corrupted metadata {}", mavenPath.getPath(), e);
        return null;
      }
    }
  }

  /**
   * Writes passed in metadata as XML.
   */
  private void write(final MavenPath mavenPath, final Metadata metadata)
      throws IOException
  {
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    metadataWriter.write(byteArrayOutputStream, metadata);
    mavenFacet.put(mavenPath, new BytesPayload(byteArrayOutputStream.toByteArray(),
        Maven2MimeRulesSource.METADATA_TYPE));
    final Map<HashAlgorithm, HashCode> hashCodes = mavenFacet.get(mavenPath).getAttributes()
        .require(Content.CONTENT_HASH_CODES_MAP, TypeTokens.HASH_CODES_MAP);
    checkState(hashCodes != null, "hashCodes");
    for (HashType hashType : HashType.values()) {
      final MavenPath checksumPath = mavenPath.hash(hashType);
      final HashCode hashCode = hashCodes.get(hashType.getHashAlgorithm());
      checkState(hashCode != null, "hashCode: type=%s", hashType);
      mavenFacet.put(checksumPath, new StringPayload(hashCode.toString(), Constants.CHECKSUM_CONTENT_TYPE));
    }
  }
}
