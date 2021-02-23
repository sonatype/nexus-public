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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.goodies.common.MultipleFailures;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.HashType;
import org.sonatype.nexus.repository.maven.MavenPathParser;
import org.sonatype.nexus.repository.maven.internal.Constants;
import org.sonatype.nexus.repository.maven.internal.DigestExtractor;
import org.sonatype.nexus.repository.maven.internal.MavenModels;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.StringPayload;

import com.google.common.collect.Sets;
import com.google.common.hash.HashCode;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptySet;
import static java.util.Objects.nonNull;
import static org.sonatype.nexus.repository.maven.internal.hosted.metadata.MetadataUtils.metadataPath;
import static org.sonatype.nexus.scheduling.CancelableHelper.checkCancellation;

/**
 * @since 3.26
 */
public abstract class AbstractMetadataRebuilder
    extends ComponentSupport
    implements MetadataRebuilder
{
  protected final int bufferSize;

  protected final int timeoutSeconds;

  @Inject
  public AbstractMetadataRebuilder(
      @Named("${nexus.maven.metadata.rebuild.bufferSize:-1000}") final int bufferSize,
      @Named("${nexus.maven.metadata.rebuild.timeoutSeconds:-60}") final int timeoutSeconds)
  {
    this.bufferSize = bufferSize;
    this.timeoutSeconds = timeoutSeconds;
  }

  /**
   * Collect all {@link MavenPath} for the provided GAbV.
   *
   * @param repository  The repository associated with the provided GAbV (Maven2 format, Hosted type only).
   * @param groupId     scope the work to given groupId.
   * @param artifactId  scope the work to given artifactId (groupId must be given).
   * @param baseVersion scope the work to given baseVersion (groupId and artifactId must ge given).
   * @return list of all paths for the input coordinates
   * @since 3.14
   */
  protected List<MavenPath> getPathsByGav(
      final Repository repository,
      final String groupId,
      final String artifactId,
      final String baseVersion)
  {
    checkNotNull(groupId);
    checkNotNull(artifactId);
    checkNotNull(baseVersion);

    log.debug("Collecting MavenPaths for Maven2 hosted repository metadata: repository={}, g={}, a={}, bV={}",
        repository.getName(), groupId, artifactId, baseVersion);

    List<MavenPath> paths = new ArrayList<>();
    // Build path for specific GAV
    paths.add(metadataPath(groupId, artifactId, baseVersion));

    // Build path for the GA; will be rebuilt as necessary but may hold the last GAV in which case rebuild would ignore it
    paths.add(metadataPath(groupId, artifactId, null));

    // Check explicitly for whether or not we have Group level metadata that might need rebuilding, since this
    // is potentially the most expensive possible path to take.
    MavenPath groupPath = metadataPath(groupId, null, null);
    if (exists(repository, groupPath)) {
      paths.add(groupPath);
    }
    return paths;
  }

  protected Set<String> deleteAllMetadataFiles(
      final Repository repository,
      final String groupId,
      final String artifactId,
      final String baseVersion)
  {
    Set<String> deletedPaths = Sets.newHashSet();
    deletedPaths.addAll(deleteGavMetadata(repository, groupId, artifactId, baseVersion));
    deletedPaths.addAll(deleteGavMetadata(repository, groupId, artifactId, null));
    deletedPaths.addAll(deleteGavMetadata(repository, groupId, null, null));
    return deletedPaths;
  }

  abstract protected Set<String> deleteGavMetadata(
      final Repository repository,
      final String groupId,
      final String artifactId,
      final String baseVersion);

  abstract public boolean exists(final Repository repository, final MavenPath mavenPath);

  /**
   * Inner class that encapsulates the work, as metadata builder is stateful.
   */
  protected abstract static class Worker
      extends ComponentSupport
  {
    protected final Repository repository;

    protected final MavenPathParser mavenPathParser;

    protected final MetadataBuilder metadataBuilder;

    protected final AbstractMetadataUpdater metadataUpdater;

    protected final String groupId;

    protected final String artifactId;

    protected final String baseVersion;

    protected final boolean rebuildChecksums;

    protected final int bufferSize;

    protected final long timeoutSeconds;

    public Worker(
        final Repository repository, // NOSONAR
        final boolean update,
        final boolean rebuildChecksums,
        @Nullable final String groupId,
        @Nullable final String artifactId,
        @Nullable final String baseVersion,
        final int bufferSize,
        final int timeoutSeconds,
        final AbstractMetadataUpdater metadataUpdater,
        final MavenPathParser mavenPathParser
    )
    {
      this.repository = repository;
      this.metadataBuilder = new MetadataBuilder();
      this.metadataUpdater = metadataUpdater;
      this.groupId = groupId;
      this.artifactId = artifactId;
      this.baseVersion = baseVersion;
      this.rebuildChecksums = rebuildChecksums;
      this.bufferSize = bufferSize;
      this.timeoutSeconds = timeoutSeconds;
      this.mavenPathParser = mavenPathParser;
    }

    /**
     * Returns {@link Iterable} with Maps for GAVs, keys used are: groupId, artifactId, baseVersions
     */
    protected abstract List<Map<String, Object>> browseGAVs();

    protected abstract Content get(final MavenPath mavenPath) throws IOException;

    protected abstract void put(final MavenPath mavenPath, final Payload payload) throws IOException;

    /**
     * Method rebuilding metadata that performs the group level processing. It uses memory conservative "async" SQL
     * approach, and calls {@link #rebuildMetadataInner(String, String, Set, MultipleFailures)} method as results are
     * arriving.
     */
    public boolean rebuildMetadata()
    {
      final MultipleFailures failures = new MultipleFailures();
      boolean metadataRebuilt = false;

      checkCancellation();
      String currentGroupId = null;

      try {
        List<Map<String, Object>> gavs = browseGAVs();

        if (!gavs.isEmpty() && nonNull(groupId)) {
          metadataBuilder.onEnterGroupId(groupId);
          if (nonNull(artifactId) && nonNull(baseVersion)) {
            rebuildMetadataInner(groupId, artifactId, emptySet(), failures);
          }
          rebuildMetadataExitGroup(groupId, failures);
        }

        for (Map<String, Object> gav : gavs) {
          checkCancellation();
          final String groupId = (String) gav.get("groupId");
          final String artifactId = (String) gav.get("artifactId");
          final Set<String> baseVersions = (Set<String>) gav.get("baseVersions");

          final boolean groupChange = !Objects.equals(currentGroupId, groupId);
          if (groupChange) {
            if (currentGroupId != null) {
              rebuildMetadataExitGroup(currentGroupId, failures);
            }
            currentGroupId = groupId;
            metadataBuilder.onEnterGroupId(groupId);
          }
          rebuildMetadataInner(groupId, artifactId, baseVersions, failures);
          metadataRebuilt = true;
        }

        if (currentGroupId != null) {
          rebuildMetadataExitGroup(currentGroupId, failures);
          metadataRebuilt = true;
        }
      }
      finally {
        maybeLogFailures(failures);
      }

      return metadataRebuilt;
    }

    /**
     * Logs any failures recorded during metadata
     */
    protected void maybeLogFailures(final MultipleFailures failures) {
      if (failures.isEmpty()) {
        return;
      }

      log.warn("Errors encountered during metadata rebuild:");
      failures.getFailures().forEach(failure -> log.warn(failure.getMessage(), failure));
    }

    /**
     * Process exits from group level, executed in isolation.
     */
    protected void rebuildMetadataExitGroup(final String currentGroupId, final MultipleFailures failures) {
      processMetadata(metadataPath(currentGroupId, null, null), metadataBuilder.onExitGroupId(), failures);
    }

    /**
     * Helper method that will capture exceptions that occur from the {@link AbstractMetadataUpdater} in a
     * {@link MultipleFailures} store
     */
    protected void processMetadata(
        final MavenPath metadataPath,
        final Maven2Metadata metadata,
        final MultipleFailures failures)
    {
      try {
        metadataUpdater.processMetadata(metadataPath, metadata);
      }
      catch (Exception e) {
        failures.add(new MetadataException("Error processing metadata for path: " + metadataPath.getPath(), e));
      }
    }

    /**
     * Method rebuilding metadata that performs artifact and baseVersion processing. While it is called from {@link
     * #rebuildMetadata()} method, it will use a separate TX/DB to perform writes, it does NOT
     * accept the TX from caller. Executed in isolation.
     */
    protected abstract void rebuildMetadataInner(
        final String groupId,
        final String artifactId,
        final Set<String> baseVersions,
        final MultipleFailures failures);

    /**
     * Verifies and may fix/create the broken/non-existent Maven hashes (.sha1/.md5 files).
     */
    protected void mayUpdateChecksum(final MavenPath mavenPath, final HashType hashType) {
      Optional<HashCode> checksum = getChecksum(mavenPath, hashType);
      if (!checksum.isPresent()) {
        // this means that an asset stored in maven repository lacks checksum required by maven repository (see maven facet)
        log.warn("Asset with path {} lacks checksum {}", mavenPath, hashType);
        return;
      }

      String assetChecksum = checksum.get().toString();
      final MavenPath checksumPath = mavenPath.hash(hashType);
      try {
        final Content content = get(checksumPath);
        if (content != null) {
          try (InputStream is = content.openInputStream()) {
            final String mavenChecksum = DigestExtractor.extract(is);
            if (Objects.equals(assetChecksum, mavenChecksum)) {
              return; // all is OK: exists and matches
            }
          }
        }
      }
      catch (IOException e) {
        log.warn("Error reading {}", checksumPath, e);
      }

      // we need to generate/write it
      try {
        log.debug("Generating checksum file: {}", checksumPath);
        final StringPayload mavenChecksum = new StringPayload(assetChecksum, Constants.CHECKSUM_CONTENT_TYPE);
        put(checksumPath, mavenChecksum);
      }
      catch (IOException e) {
        log.warn("Error writing {}", checksumPath, e);
        throw new RuntimeException(e);
      }
    }

    protected abstract Optional<HashCode> getChecksum(final MavenPath mavenPath, final HashType hashType);

    /**
     * Parses the DOM of a XML.
     */
    protected Xpp3Dom parse(final MavenPath mavenPath, final InputStream is) {
      try {
        return MavenModels.parseDom(is);
      }
      catch (IOException e) {
        log.warn("Could not parse POM: {} @ {}", repository.getName(), mavenPath.getPath(), e);
        throw new RuntimeException(e);
      }
    }

    /**
     * Returns the plugin prefix of a Maven plugin, by opening up the plugin JAR, and reading the Maven Plugin
     * Descriptor. If fails, falls back to mangle artifactId (ie. extract XXX from XXX-maven-plugin or
     * maven-XXX-plugin).
     */
    protected String getPluginPrefix(final MavenPath mavenPath) {
      // sanity checks: is artifact and extension is "jar", only possibility for maven plugins currently
      checkArgument(mavenPath.getCoordinates() != null);
      checkArgument(Objects.equals(mavenPath.getCoordinates().getExtension(), "jar"));
      String prefix = null;
      try {
        final Content jarFile = get(mavenPath);
        if (jarFile != null) {
          try (ZipInputStream zip = new ZipInputStream(jarFile.openInputStream())) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
              if (!entry.isDirectory() && "META-INF/maven/plugin.xml".equals(entry.getName())) {
                final Xpp3Dom dom = parse(mavenPath, zip);
                prefix = getChildValue(dom, "goalPrefix", null);
                break;
              }
              zip.closeEntry();
            }
          }
        }
      }
      catch (IOException e) {
        log.warn("Unable to read plugin.xml of {}", mavenPath, e);
      }
      if (prefix != null) {
        return prefix;
      }
      if ("maven-plugin-plugin".equals(mavenPath.getCoordinates().getArtifactId())) {
        return "plugin";
      }
      else {
        return mavenPath.getCoordinates().getArtifactId().replaceAll("-?maven-?", "").replaceAll("-?plugin-?", "");
      }
    }

    /**
     * Helper method to get node's immediate child or default.
     */
    protected String getChildValue(final Xpp3Dom doc, final String childName, final String defaultValue) {
      Xpp3Dom child = doc.getChild(childName);
      if (child == null) {
        return defaultValue;
      }
      return child.getValue();
    }
  }
}
