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
package org.sonatype.nexus.repository.maven.internal.content;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.common.failure.MultipleFailures;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.HashType;
import org.sonatype.nexus.repository.maven.internal.Constants;
import org.sonatype.nexus.repository.maven.internal.hosted.metadata.MetadataRebuilder;
import org.sonatype.nexus.thread.ExceptionAwareThreadFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;
import static org.sonatype.nexus.repository.maven.internal.hosted.metadata.MetadataUtils.metadataPath;
import org.springframework.stereotype.Component;

/**
 * A maven2 metadata rebuilder written to take advantage of the SQL database design.
 */
@Component
public class MavenMetadataRebuilder
    implements MetadataRebuilder
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private static final String PATH_PREFIX = "/";

  private final int bufferSize;

  private final ExecutorService executor;

  @Autowired
  public MavenMetadataRebuilder(
      @Value("${nexus.maven.metadata.rebuild.bufferSize:1000}") final int bufferSize,
      @Value("${nexus.maven.metadata.rebuild.threadPoolSize:1}") final int maxTreads)
  {
    checkArgument(bufferSize > 0, "Buffer size must be greater than 0");

    this.bufferSize = bufferSize;
    executor = Executors.newFixedThreadPool(maxTreads,
        new ExceptionAwareThreadFactory("metadata-rebuild-tasks", "metadata-rebuild-tasks"));
  }

  @Override
  public boolean rebuild(
      final Repository repository,
      final boolean update,
      final boolean rebuildChecksums,
      final boolean cascadeUpdate,
      @Nullable final String groupId,
      @Nullable final String artifactId,
      @Nullable final String baseVersion)
  {
    checkNotNull(repository);
    MetadataRebuildWorker worker =
        new MetadataRebuildWorker(repository, update, groupId, artifactId, baseVersion, bufferSize);
    return rebuildWithWorker(worker, rebuildChecksums, cascadeUpdate, groupId, artifactId, baseVersion);
  }

  @VisibleForTesting
  boolean rebuildWithWorker(
      final MetadataRebuildWorker worker,
      final boolean rebuildChecksums,
      final boolean cascadeUpdate,
      @Nullable final String groupId,
      @Nullable final String artifactId,
      @Nullable final String baseVersion)
  {
    boolean rebuiltMetadata = false;
    try {
      if (StringUtils.isNoneBlank(groupId, artifactId)) {
        Collection<String> baseVersions = worker.rebuildGA(groupId, artifactId);
        if (StringUtils.isNotBlank(baseVersion)) {
          worker.rebuildBaseVersionsAndChecksums(groupId, artifactId, Collections.singletonList(baseVersion),
              rebuildChecksums);
        }
        else {
          if (cascadeUpdate) {
            rebuildBaseVersionsAndChecksumsAsync(worker, groupId, artifactId, baseVersions, rebuildChecksums);
          }
        }
      }
      else {
        rebuiltMetadata = worker.rebuildMetadata();
        if (rebuildChecksums) {
          worker.rebuildChecksums();
        }
      }
    }
    finally {
      maybeLogFailures(worker.getFailures());
    }
    return rebuiltMetadata;
  }

  /*
   * This exists only for API compatibility with Orient. On SQL databases we don't do work inside an open transaction
   */
  @Deprecated
  @Override
  public boolean rebuildInTransaction(
      final Repository repository,
      final boolean update,
      final boolean rebuildChecksums,
      final boolean cascadeUpdate,
      @Nullable final String groupId,
      @Nullable final String artifactId,
      @Nullable final String baseVersion)
  {
    return rebuild(repository, update, rebuildChecksums, cascadeUpdate, groupId, artifactId, baseVersion);
  }

  @Override
  public Set<String> deleteMetadata(final Repository repository, final List<String[]> gavs) {
    checkNotNull(repository);
    checkNotNull(gavs);

    MavenContentFacet mavenContentFacet = repository.facet(MavenContentFacet.class);
    Set<String> deletedPaths = Sets.newHashSet();

    for (String[] gav : gavs) {
      MavenPath mavenPath = metadataPath(gav[0], gav[1], gav[2]);

      // Check if we should delete this metadata (NEXUS-47092)
      if (!isPathUsedForGAMetadata(mavenContentFacet, mavenPath, gav)) {
        List<String> paths = Lists.newArrayList();
        paths.add(prependIfMissing(mavenPath.main().getPath(), PATH_PREFIX));
        for (HashType hashType : HashType.values()) {
          paths.add(prependIfMissing(mavenPath.main().hash(hashType).getPath(), PATH_PREFIX));
        }

        if (mavenContentFacet.delete(paths)) {
          deletedPaths.addAll(paths);
        }
      }
      else {
        log.info("Skipping delete of {} - metadata type mismatch (group vs artifact/version level)",
            mavenPath.getPath());
      }
    }

    return deletedPaths;
  }

  /**
   * Checks if the given G-level metadata path is also being used as GA-level metadata.
   * This happens when a groupId like "a.b" creates metadata at /a/b/maven-metadata.xml,
   * which collides with GA-level metadata for namespace="a" and name="b".
   *
   * @param mavenContentFacet the Maven content facet
   * @param mavenPath the metadata path
   * @param gav the groupId, artifactId, baseVersion coordinates
   * @return true if the path is being used for GA-level metadata (don't delete), false otherwise (safe to delete)
   */
  private boolean isPathUsedForGAMetadata(
      final MavenContentFacet mavenContentFacet,
      final MavenPath mavenPath,
      final String[] gav)
  {
    try {
      boolean isGroupLevelDelete = gav[1] == null && gav[2] == null; // Only groupId specified

      if (!isGroupLevelDelete) {
        return false; // Not a group-level delete, safe to proceed
      }

      // This is a group-level delete - check for path collision with GA-level metadata
      // Extract the path segments
      String path = mavenPath.getPath();
      String metadataFileName = "/" + Constants.METADATA_FILENAME;
      if (!path.endsWith(metadataFileName)) {
        return false; // Not a metadata file, safe to delete
      }

      // Remove "/maven-metadata.xml" to get the base path
      String basePath = path.substring(0, path.length() - metadataFileName.length());

      // Split into segments: a/b -> ["a", "b"]
      String[] segments = basePath.split("/");

      // Filter empty segments (in case of leading slash)
      List<String> nonEmptySegments = Lists.newArrayList();
      for (String segment : segments) {
        if (!segment.isEmpty()) {
          nonEmptySegments.add(segment);
        }
      }

      if (nonEmptySegments.size() < 2) {
        return false; // Need at least 2 segments for GA collision, safe to delete
      }

      // Check if the last segment is being used as an artifactId
      // by treating everything before it as the namespace
      String potentialArtifactId = nonEmptySegments.get(nonEmptySegments.size() - 1);

      // Build the namespace from all segments except the last one
      StringBuilder namespaceBuilder = new StringBuilder();
      for (int i = 0; i < nonEmptySegments.size() - 1; i++) {
        if (i > 0) {
          namespaceBuilder.append(".");
        }
        namespaceBuilder.append(nonEmptySegments.get(i));
      }
      String potentialNamespace = namespaceBuilder.toString();

      // Check if components exist with this namespace+name combination
      Collection<String> baseVersions = mavenContentFacet.getBaseVersions(potentialNamespace, potentialArtifactId);

      if (!baseVersions.isEmpty()) {
        log.info(
            "Skipping delete of {} - path is also used for GA-level metadata (namespace={}, name={})",
            path, potentialNamespace, potentialArtifactId);
        return true; // Do NOT delete - path collision with GA-level metadata
      }

      return false; // Safe to delete - no GA-level metadata at this path
    }
    catch (Exception e) {
      log.warn("Error checking metadata type at {}, allowing delete", mavenPath.getPath(), e);
      return false; // If we can't read it, allow deletion (fail-safe)
    }
  }

  private void rebuildBaseVersionsAndChecksumsAsync(
      final MetadataRebuildWorker worker,
      final String namespace,
      final String name,
      final Collection<String> baseVersions,
      final boolean rebuildChecksums)
  {
    executor.submit(() -> {
      log.debug(
          "Started asynchronously rebuild metadata/recalculate checksums for GAVs. Namespace: {}, name: {}, baseVersions {}",
          namespace, name, baseVersions);
      worker.rebuildBaseVersionsAndChecksums(namespace, name, baseVersions, rebuildChecksums);
      log.debug(
          "Finished asynchronously rebuild metadata/recalculate checksums for GAVs. Namespace: {}, name: {}, baseVersions {}",
          namespace, name, baseVersions);
    });
  }

  /*
   * Logs any failures recorded during metadata
   */
  private void maybeLogFailures(final MultipleFailures failures) {
    if (failures.isEmpty()) {
      return;
    }
    log.warn("Errors encountered during metadata rebuild:");
    failures.getFailures().forEach(failure -> log.warn(failure.getMessage(), failure));
  }

}
