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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.goodies.common.MultipleFailures;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.Continuations;
import org.sonatype.nexus.common.stateguard.InvalidStateException;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.content.maven.store.GAV;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.Coordinates;
import org.sonatype.nexus.repository.maven.MavenPath.HashType;
import org.sonatype.nexus.repository.maven.MavenPathParser;
import org.sonatype.nexus.repository.maven.internal.Attributes;
import org.sonatype.nexus.repository.maven.internal.Constants;
import org.sonatype.nexus.repository.maven.internal.DigestExtractor;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.maven.internal.hosted.metadata.Maven2Metadata;
import org.sonatype.nexus.repository.maven.internal.hosted.metadata.MetadataBuilder;
import org.sonatype.nexus.repository.maven.internal.hosted.metadata.MetadataException;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.hash.HashCode;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionScheme;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonatype.nexus.repository.maven.internal.hosted.metadata.MetadataUtils.getPluginPrefix;
import static org.sonatype.nexus.repository.maven.internal.hosted.metadata.MetadataUtils.metadataPath;
import static org.sonatype.nexus.scheduling.CancelableHelper.checkCancellation;

// TODO good to have: an additional refactoring + unit tests

/*
 * Worker class to hold the context of a particular rebuild.
 */
public class MetadataRebuildWorker
    extends ComponentSupport
{
  private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

  private final MultipleFailures failures = new MultipleFailures();

  private final Repository repository;

  private final MavenContentFacet content;

  private final MavenPathParser mavenPathParser;

  private final Optional<String> groupId;

  private final Optional<String> artifactId;

  private final Optional<String> baseVersion;

  private final int bufferSize;

  private final VersionScheme versionScheme = new GenericVersionScheme();

  private DatastoreMetadataUpdater metadataUpdater;

  private boolean rebuilt = false;

  public MetadataRebuildWorker(
      final Repository repository, // NOSONAR
      final boolean update,
      @Nullable final String groupId,
      @Nullable final String artifactId,
      @Nullable final String baseVersion,
      final int bufferSize)
  {
    metadataUpdater = new DatastoreMetadataUpdater(update, repository);
    content = repository.facet(MavenContentFacet.class);
    mavenPathParser = repository.facet(MavenContentFacet.class).getMavenPathParser();

    this.repository = repository;
    this.groupId = Optional.ofNullable(groupId);
    this.artifactId = chain(this.groupId, artifactId);
    this.baseVersion = chain(this.artifactId, baseVersion);
    this.bufferSize = bufferSize;
  }

  /*
   * returns true if metadata was rebuilt
   */
  public boolean rebuildMetadata() {
    FluentComponents components = content.components();

    log.debug("Beginning rebuild provided: r {} g {} a {} bv {}", repository.getName(), groupId, artifactId,
        baseVersion);

    groupId.map(Collections::singleton)
        .map(Collection::stream)
        // If no groupId provided we do a full build
        .orElseGet(() -> components.namespaces().stream())
        .flatMap(namespace -> {
          rebuildGroupMetadata(namespace);

          return artifactId.map(Collections::singleton)
              .map(Collection::stream)
              // If the artifactId wasn't provided iterate over all names.
              .orElseGet(() -> components.names(namespace).stream())
              // Create Pairs where left=group, right=artifact
              .map(name -> Pair.of(namespace, name));
        })
        .flatMap(ga -> {
          Collection<String> gaBaseVersions = rebuildArtifactMetadata(repository, ga.getLeft(), ga.getRight());

          return baseVersion.map(Collections::singleton)
              .map(Collection::stream)
              // If the baseVersion wasn't provided, we use the list of baseVersions returned from the GA rebuild
              .orElseGet(gaBaseVersions::stream)
              // combine the current GA to return a list of GAVs
              .map(bv -> new GAV(ga.getLeft(), ga.getRight(), bv, 0));
        })
        // Version level metadata is only relevant for snapshot base versions
        .filter(gabv -> gabv.baseVersion.endsWith(SNAPSHOT_SUFFIX))
        .forEach(this::rebuildVersionMetadata);

    return rebuilt;
  }

  public Collection<String> rebuildGA(final String namespace, final String name) {
    rebuildGroupMetadata(namespace);
    return rebuildArtifactMetadata(repository, namespace, name);
  }

  public void rebuildBaseVersionsAndChecksums(
      final String namespace,
      final String name,
      final Collection<String> baseVersions,
      final boolean rebuildChecksums)
  {
    rebuildVersionsMetadata(namespace, name, baseVersions);
    if (rebuildChecksums) {
      rebuildChecksumsForGAV(namespace, name, baseVersions);
    }
  }

  public void rebuildChecksums() {
    FluentComponents components = content.components();

    groupId.map(Collections::singleton)
        .map(Collection::stream)
        .orElseGet(() -> components.namespaces().stream())
        .flatMap(namespace -> (Stream<Pair<String, String>>) artifactId.map(Collections::singleton)
            .map(Collection::stream)
            .orElseGet(() -> components.names(namespace).stream())
            .map(name -> Pair.of(namespace, name)))
        .flatMap(ga -> baseVersion
            .map(
                bv -> Continuations.streamOf(findComponentsForBaseVersion(ga.getLeft(), ga.getRight(), bv), bufferSize))
            .orElseGet(() -> componentsIfVersionNotPresent(ga.getLeft(), ga.getRight())))
        .map(components::with)
        .map(FluentComponent::assets)
        .flatMap(Collection::stream)
        .forEach(this::maybeRebuildChecksum);
  }

  public void rebuildChecksumsForGAV(final String namespace, final String name, final Collection<String> baseVersions) {
    FluentComponents components = content.components();
    baseVersions.stream()
        .flatMap(bv -> Continuations.streamOf(findComponentsForBaseVersion(namespace, name, bv),
            bufferSize))
        .map(components::with)
        .map(FluentComponent::assets)
        .flatMap(Collection::stream)
        .forEach(this::maybeRebuildChecksum);
  }

  // for testing purpose
  void setMetadataUpdater(final DatastoreMetadataUpdater metadataUpdater) {
    this.metadataUpdater = metadataUpdater;
  }

  public MultipleFailures getFailures() {
    return failures;
  }

  /*
   * Rebuild group metadata, returns true if metadata is updated.
   */
  void rebuildGroupMetadata(final String namespace) {
    checkCancellation();

    MavenPath metadataPath = metadataPath(namespace, null, null);
    log.debug("Starting rebuild for repo {} g {}", repository.getName(), namespace);
    try {
      MetadataBuilder metadataBuilder = new MetadataBuilder();
      metadataBuilder.onEnterGroupId(namespace);
      // Loop rather than stream due to exception handling.
      for (Asset asset : Continuations.iterableOf(findMavenPluginsForNamespace(repository, namespace), bufferSize)) {
        checkCancellation();
        processGroupPlugin(metadataBuilder, asset);
      }
      log.debug("Updating metadata for r {} g {}", repository.getName(), namespace);
      metadataUpdater.processMetadata(metadataPath, metadataBuilder.onExitGroupId());
      log.debug("Completed rebuild for repo {} g {}", repository.getName(), namespace);
      updateRebuilt(true);
    }
    catch (Exception e) {
      maybeRethrow(e);
      log.debug("Failed rebuild for repo {} g {}", repository.getName(), namespace);
      failures.add(new MetadataException("Error processing metadata for path: " + metadataPath.getPath(), e));
      updateRebuilt(false);
    }
  }

  /*
   * Process an individual plugin for inclusion in group metadata
   */
  private void processGroupPlugin(final MetadataBuilder metadataBuilder, final Asset asset) {
    try {
      MavenPath mavenPath = mavenPathParser.parsePath(asset.path());
      Coordinates coordinates = mavenPath.getCoordinates();
      // null coordinates should never happen, but ...
      // Only "main" jar artifacts should be considered
      if (coordinates == null || !mavenPath.locateMainArtifact("jar").equals(mavenPath)) {
        log.trace("Found unnecessary asset {}", asset.path());
        return;
      }
      log.debug("Loading plugin data for asset {}", asset.path());
      metadataBuilder.addPlugin(getPluginPrefix(mavenPath,
              () -> content.assets().with(asset).download().openInputStream()),
          coordinates.getArtifactId(),
          asset.component().get().attributes(Maven2Format.NAME).get(Attributes.P_POM_NAME, String.class));
    }
    catch (Exception e) {
      maybeRethrow(e);

      failures.add(new MetadataException(
          "Error processing maven plugin in " + repository.getName() + " path  " + asset.path(), e));
    }
  }

  /*
   * Rebuilds group level metadata. Returns a list of base versions.
   */
  @VisibleForTesting
  Collection<String> rebuildArtifactMetadata(
      final Repository repository,
      final String namespace,
      final String name)
  {
    checkCancellation();
    MavenPath metadataPath = metadataPath(namespace, name, null);
    Collection<String> baseVersions = content.getBaseVersions(namespace, name);
    log.debug("Starting rebuild for repo {} g {} a {}", repository.getName(), namespace, name);
    try {
      MetadataBuilder metadataBuilder = new MetadataBuilder();
      metadataBuilder.onEnterGroupId(namespace);
      metadataBuilder.onEnterArtifactId(name);
      log.trace("Found base versions {}", baseVersions);
      baseVersions.stream()
          .forEach(metadataBuilder::addBaseVersion);
      Maven2Metadata metadata = metadataBuilder.onExitArtifactId();
      metadataUpdater.processMetadata(metadataPath, metadata);
      log.debug("Finished rebuild for repo {} g {} a {}", repository.getName(), namespace, name);
      updateRebuilt(true);
      return metadata.getBaseVersions().getVersions();
    }
    catch (Exception e) {
      maybeRethrow(e);
      failures.add(new MetadataException("Error processing metadata for path: " + metadataPath.getPath(), e));
      updateRebuilt(false);
      return baseVersions != null ? baseVersions : Collections.emptySet();
    }
  }

  @VisibleForTesting
  void rebuildVersionsMetadata(final String namespace, final String name, final Collection<String> bVersions)
  {
    bVersions.stream()
        // Version level metadata is only relevant for snapshot base versions
        .filter(version -> version.endsWith(SNAPSHOT_SUFFIX))
        .forEach(version -> rebuildVersionMetadata(namespace, name, version));
  }

  /*
   * Rebuild version level metadata.
   *
   * Note: only applicable for snapshot base versions
   */
  private void rebuildVersionMetadata(final GAV gabv)
  {
    rebuildVersionMetadata(gabv.group, gabv.name, gabv.baseVersion);
  }

  /*
   * Rebuild version level metadata.
   *
   * Note: only applicable for snapshot base versions
   */
  private void rebuildVersionMetadata(final String namespace, final String name, final String bVersion) {
    checkCancellation();
    MavenPath metadataPath = metadataPath(namespace, name, bVersion);
    log.debug("Starting rebuild for repo {} g {} a {} v {}", repository.getName(), namespace, name, bVersion);
    try {
      checkArgument(bVersion.endsWith(SNAPSHOT_SUFFIX));
      MetadataBuilder metadataBuilder = new MetadataBuilder();
      metadataBuilder.onEnterGroupId(namespace);
      metadataBuilder.onEnterArtifactId(name);
      metadataBuilder.onEnterBaseVersion(bVersion);

      Continuations.streamOf(findComponentsForBaseVersion(namespace, name, bVersion), bufferSize)
          .min(this::reverseSortByVersion)
          .map(FluentComponent::assets)
          .map(Collection::stream)
          .ifPresent(assets -> assets.map(Asset::path)
              .map(mavenPathParser::parsePath)
              .forEach(metadataBuilder::addArtifactVersion)
          );
      Maven2Metadata metadata = metadataBuilder.onExitBaseVersion();
      metadataUpdater.processMetadata(metadataPath, metadata);
      log.debug("Finished rebuild for repo {} g {} a {} v {}", repository.getName(), namespace, name, bVersion);
      updateRebuilt(true);
    }
    catch (Exception e) {
      maybeRethrow(e);
      failures.add(new MetadataException("Error processing metadata for path: " + metadataPath.getPath(), e));
      updateRebuilt(false);
    }
  }

  /*
   * Rethrows the exception if it should prevent the metadata rebuild from continuing.
   */
  private static void maybeRethrow(final Exception e) {
    if (e instanceof TaskInterruptedException) {
      // We're inside a cancelled task.
      throw (TaskInterruptedException) e;
    }
    else if (e instanceof InvalidStateException) {
      // Something we're interacting with didn't start properly, or is being destroyed.
      // Likely we'll repeatedly encounter this problem.
      throw (InvalidStateException) e;
    }
  }

  /*
   * If the first arg is present return an optional containing the second argument, otherwise return empty.
   */
  private static Optional<String> chain(final Optional<String> original, @Nullable final String next) {
    if (original.isPresent()) {
      return Optional.ofNullable(next);
    }
    return Optional.empty();
  }

  /*
   * Component comparator to inverse sort by version
   */
  private int reverseSortByVersion(final Component a, final Component b) {
    Version aVersion = parseVersion(a.version());
    Version bVersion = parseVersion(b.version());
    if (bVersion == null) {
      return -1;
    }
    else if (aVersion == null) {
      return 1;
    }
    // b first for reverse sorting
    return bVersion.compareTo(aVersion);
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

  /*
   * Helper function for Continuations
   */
  private BiFunction<Integer, String, Continuation<FluentComponent>> findComponentsForBaseVersion(
      final String namespace,
      final String name,
      final String baseVersion)
  {
    return (limit, continuationToken) -> content.findComponentsForBaseVersion(limit, continuationToken, namespace,
        name, baseVersion);
  }

  /*
   * Helper function for Continuations
   */
  private BiFunction<Integer, String, Continuation<Asset>> findMavenPluginsForNamespace(
      final Repository repository,
      final String namespace)
  {
    return (limit, continuationToken) -> content
        .findMavenPluginAssetsForNamespace(limit, continuationToken, namespace);
  }

  /*
   * Flag that metadata has been rebuilt
   */
  private void updateRebuilt(final boolean rebuilt) {
    this.rebuilt |= rebuilt;
  }

  private void maybeRebuildChecksum(final Asset asset) {
    checkCancellation();
    MavenPath mavenPath = mavenPathParser.parsePath(asset.path());
    if (mavenPath.isSubordinate()) {
      log.trace("Skipping subordinate asset r {} {}", repository.getName(), asset.path());
      return;
    }
    log.debug("Verifying checksum for repository {} path {}", repository.getName(), asset.path());
    try {
      Optional<AssetBlob> assestBlob = asset.blob();
      Function<HashType, Optional<HashCode>> accessor = (hashType) -> getChecksum(assestBlob, hashType);

      boolean sha1ChecksumWasRebuilt = mayUpdateChecksum(accessor, mavenPath, HashType.SHA1);
      if (sha1ChecksumWasRebuilt) {
        // Rebuilding checksums is expensive so only rebuild the others if the first one was rebuilt
        mayUpdateChecksum(accessor, mavenPath, HashType.SHA256);
        mayUpdateChecksum(accessor, mavenPath, HashType.SHA512);
        mayUpdateChecksum(accessor, mavenPath, HashType.MD5);
      }
    }
    catch (Exception e) {
      maybeRethrow(e);
    }
  }

  private static Optional<HashCode> getChecksum(final Optional<AssetBlob> assetBlob, final HashType hashType) {
    return assetBlob
        .map(AssetBlob::checksums)
        .map(checksums -> checksums.get(hashType.name().toLowerCase()))
        .map(HashCode::fromString);
  }

  /*
   * Verifies and may fix/create the broken/non-existent Maven hashes (.sha1/.md5 files).
   * @return true if the checksum was rebuilt
   */
  private boolean mayUpdateChecksum(
      final Function<HashType, Optional<HashCode>> accessor,
      final MavenPath mavenPath,
      final HashType hashType)
  {
    Optional<HashCode> checksum = accessor.apply(hashType);
    if (!checksum.isPresent()) {
      // this means that an asset stored in maven repository lacks checksum required by maven repository (see maven facet)
      log.warn("Asset with path {} lacks checksum {}", mavenPath, hashType);
      return false;
    }
    String assetChecksum = checksum.get().toString();
    final MavenPath checksumPath = mavenPath.hash(hashType);
    try {
      final Content checksumContent = this.content.get(checksumPath).orElse(null);
      if (checksumContent != null) {
        try (InputStream is = checksumContent.openInputStream()) {
          final String mavenChecksum = DigestExtractor.extract(is);
          if (Objects.equals(assetChecksum, mavenChecksum)) {
            return false; // all is OK: exists and matches
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
      content.put(checksumPath, mavenChecksum);
    }
    catch (IOException e) {
      log.warn("Error writing {}", checksumPath, e);
      throw new RuntimeException(e);
    }
    return true;
  }

  private Stream<FluentComponent> componentsIfVersionNotPresent(
      final String namespace,
      final String name)
  {
    BiFunction<Integer, String, Continuation<FluentComponent>> fn =
        (limit, continuationToken) -> content.findComponentsInGA(limit, continuationToken, namespace, name);
    return Continuations.streamOf(fn, bufferSize);
  }
}
