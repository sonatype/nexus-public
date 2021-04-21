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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.MultipleFailures;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponentBuilder;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.HashType;
import org.sonatype.nexus.repository.maven.internal.Attributes;
import org.sonatype.nexus.repository.maven.internal.hosted.metadata.AbstractMetadataRebuilder;
import org.sonatype.nexus.repository.maven.internal.hosted.metadata.AbstractMetadataUpdater;
import org.sonatype.nexus.repository.maven.internal.hosted.metadata.Maven2Metadata;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.transaction.Transactional;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.hash.HashCode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;
import static org.sonatype.nexus.repository.maven.MavenMetadataRebuildFacet.METADATA_FORCE_REBUILD;
import static org.sonatype.nexus.repository.maven.MavenMetadataRebuildFacet.METADATA_REBUILD;
import static org.sonatype.nexus.repository.maven.internal.hosted.metadata.MetadataUtils.metadataPath;
import static org.sonatype.nexus.scheduling.CancelableHelper.checkCancellation;

/**
 * @since 3.26
 */
@Singleton
@Named
public class DatastoreMetadataRebuilder
    extends AbstractMetadataRebuilder
{
  private static final String PATH_PREFIX = "/";

  @Inject
  public DatastoreMetadataRebuilder(
      @Named("${nexus.maven.metadata.rebuild.bufferSize:-1000}") final int bufferSize,
      @Named("${nexus.maven.metadata.rebuild.timeoutSeconds:-60}") final int timeoutSeconds)
  {
    super(bufferSize, timeoutSeconds);
  }

  @Transactional
  @Override
  public boolean rebuild(
      final Repository repository,
      final boolean update,
      final boolean rebuildChecksums,
      @Nullable final String groupId,
      @Nullable final String artifactId,
      @Nullable final String baseVersion)
  {
    checkNotNull(repository);
    return rebuildInTransaction(repository, update, rebuildChecksums, groupId, artifactId, baseVersion);
  }

  @Override
  public boolean rebuildInTransaction(
      final Repository repository,
      final boolean update,
      final boolean rebuildChecksums,
      @Nullable final String groupId,
      @Nullable final String artifactId,
      @Nullable final String baseVersion)
  {
    checkNotNull(repository);
    return new DatastoreWorker(repository, update, rebuildChecksums, groupId, artifactId, baseVersion, bufferSize,
        timeoutSeconds, new DatastoreMetadataUpdater(update, repository)).rebuildMetadata();
  }

  @Override
  public boolean refreshInTransaction(
      final Repository repository,
      final boolean update,
      final boolean rebuildChecksums,
      @Nullable final String groupId,
      @Nullable final String artifactId,
      @Nullable final String baseVersion)
  {
    checkNotNull(repository);
    return new DatastoreWorker(repository, update, rebuildChecksums, groupId, artifactId, baseVersion, bufferSize,
        timeoutSeconds, new DatastoreMetadataUpdater(update, repository)).refreshMetadata();
  }

  @Transactional
  @Override
  protected Set<String> deleteAllMetadataFiles(
      final Repository repository,
      final String groupId,
      final String artifactId,
      final String baseVersion)
  {
    return super.deleteAllMetadataFiles(repository, groupId, artifactId, baseVersion);
  }

  @Override
  protected Set<String> deleteGavMetadata(final Repository repository, final String groupId, final String artifactId, final String baseVersion)
  {
    MavenPath gavMetadataPath = metadataPath(groupId, artifactId, baseVersion);
    MavenContentFacet mavenContentFacet = repository.facet(MavenContentFacet.class);
    try {
        return mavenContentFacet.deleteWithHashes(gavMetadataPath);
    }
    catch (IOException e) {
      log.warn("Error encountered when deleting metadata: repository={}", repository);
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean exists(final Repository repository, final MavenPath mavenPath) {
    return repository.facet(ContentFacet.class).assets().path(mavenPath.getPath()).find().isPresent();
  }

  protected static class DatastoreWorker
      extends Worker
  {
    public DatastoreWorker(
        final Repository repository,
        final boolean update,
        final boolean rebuildChecksums,
        @Nullable final String groupId,
        @Nullable final String artifactId,
        @Nullable final String baseVersion,
        final int bufferSize,
        final int timeoutSeconds,
        final AbstractMetadataUpdater metadataUpdater)
    {
      super(repository, update, rebuildChecksums, groupId, artifactId, baseVersion, bufferSize, timeoutSeconds,
          metadataUpdater, repository.facet(MavenContentFacet.class).getMavenPathParser());
    }

    @Override
    protected List<Map<String, Object>> browseGAVs() {
      ContentFacet contentFacet = repository.facet(ContentFacet.class);
      return contentFacet.components()
          .namespaces()
          .stream()
          .map(namespace -> contentFacet.components()
              .names(namespace)
              .stream()
              .map(name -> {
                    Set<String> baseVersions = contentFacet.components()
                        .versions(namespace, name)
                        .stream()
                        .map(version ->
                            contentFacet.components()
                                .name(name)
                                .namespace(namespace)
                                .version(version)
                                .find()
                                .map(component -> component.attributes("maven2").get("baseVersion", String.class)))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toSet());
                  return ImmutableMap.<String, Object>of(
                  "groupId", namespace,
                  "artifactId", name,
                  "baseVersions", baseVersions);
                  }
              ).collect(Collectors.toList())
          ).flatMap(Collection::stream)
          .collect(Collectors.toList());
    }

    @Override
    protected Content get(final MavenPath mavenPath) throws IOException {
      return repository.facet(MavenContentFacet.class).get(mavenPath).orElse(null);
    }

    @Override
    protected void put(final MavenPath mavenPath, final Payload payload) throws IOException {
      repository.facet(MavenContentFacet.class).put(mavenPath, payload);
    }

    @Override
    protected void rebuildMetadataInner(
        final String groupId,
        final String artifactId,
        final Set<String> baseVersions,
        final MultipleFailures failures)
    {
      metadataBuilder.onEnterArtifactId(artifactId);

      FluentComponents components = repository.facet(ContentFacet.class).components();
      Collection<String> allVersions = components.versions(groupId, artifactId);
      FluentComponentBuilder componentBuilder = components.name(artifactId).namespace(groupId);

      for (final String baseVersion : baseVersions) {
        checkCancellation();
        metadataBuilder.onEnterBaseVersion(baseVersion);
        fetchAssets(allVersions, componentBuilder, baseVersion).forEach(this::processAsset);

        processMetadata(metadataPath(groupId, artifactId, baseVersion), metadataBuilder.onExitBaseVersion(), failures);
      }

      processMetadata(metadataPath(groupId, artifactId, null), metadataBuilder.onExitArtifactId(), failures);
    }

    private Stream<Pair<FluentComponent, FluentAsset>> fetchAssets(
        final Collection<String> allVersions,
        final FluentComponentBuilder componentBuilder,
        final String v)
    {
      return allVersions.stream()
          .map(version -> componentBuilder.version(version).find())
          .filter(Optional::isPresent)
          .map(Optional::get)
          .filter(component -> v.equals(component.attributes("maven2").get("baseVersion", String.class)))
          .flatMap(fc -> fc.assets().stream()
              .filter(asset -> !mavenPathParser.parsePath(asset.path()).isSubordinate())
              .map(fa -> Pair.of(fc, fa)));
    }

    @Override
    protected boolean refreshArtifact(
        final String groupId,
        final String artifactId,
        final Set<String> baseVersions,
        final MultipleFailures failures)
    {
      MavenPath metadataPath = metadataPath(groupId, artifactId, null);

      FluentComponents components = repository.facet(ContentFacet.class).components();
      final Collection<String> allVersions = components.versions(groupId, artifactId);
      final FluentComponentBuilder componentBuilder = components.name(artifactId).namespace(groupId);

      metadataBuilder.onEnterArtifactId(artifactId);
      boolean rebuiltAtLeastOneVersion = baseVersions.stream()
          .map(v -> {
            checkCancellation();
            return refreshVersion(allVersions, componentBuilder, groupId, artifactId, v, failures);
          })
          .reduce(Boolean::logicalOr)
          .orElse(false);
      Maven2Metadata newMetadata = metadataBuilder.onExitArtifactId();

      boolean isRequestedVersion = StringUtils.equals(this.groupId, groupId) &&
          StringUtils.equals(this.artifactId, artifactId) &&
          StringUtils.equals(baseVersion, null);

      if (isRequestedVersion || rebuiltAtLeastOneVersion || requiresRebuild(metadataPath)) {
        processMetadata(metadataPath, newMetadata, failures);
        return true;
      }
      else {
        log.debug("Skipping {}:{} for rebuild", groupId, artifactId);
        return false;
      }
    }

    private boolean refreshVersion(
        final Collection<String> allVersions,
        final FluentComponentBuilder componentBuilder,
        final String groupId,
        final String artifactId,
        final String version,
        final MultipleFailures failures)
    {
      MavenPath metadataPath = metadataPath(groupId, artifactId, version);

      metadataBuilder.onEnterBaseVersion(baseVersion);
      fetchAssets(allVersions, componentBuilder, version).forEach(this::processAsset);
      Maven2Metadata newMetadata = metadataBuilder.onExitBaseVersion();

      /**
       * The rebuild flag on the requested asset may have been cleared before we were invoked.
       * So we check a special case to always rebuild the metadata for the g:a:v that we were initialized with
       */
      boolean isRequestedVersion = StringUtils.equals(this.groupId, groupId) &&
          StringUtils.equals(this.artifactId, artifactId) &&
          StringUtils.equals(baseVersion, version);

      if (isRequestedVersion || requiresRebuild(metadataPath)) {
        processMetadata(metadataPath, newMetadata, failures);
        return true;
      }
      else {
        log.debug("Skipping {}:{}:{} for rebuild", groupId, artifactId, version);
        return false;
      }
    }

    private boolean requiresRebuild(final MavenPath metadataPath) {
      FluentAssets assets = repository.facet(ContentFacet.class).assets();
      Optional<FluentAsset> existingMetadata = assets.path(metadataPath.getPath()).find();

      return existingMetadata.map(this::getMetadataRebuildFlag).orElse(false);
    }

    private Boolean getMetadataRebuildFlag(FluentAsset asset) {
      return asset.attributes(METADATA_REBUILD).get(METADATA_FORCE_REBUILD, Boolean.class, false);
    }

    private void processAsset(final Pair<FluentComponent, FluentAsset> componentAssetPair) {
      checkCancellation();
      FluentComponent component = componentAssetPair.getLeft();
      FluentAsset asset = componentAssetPair.getRight();
      MavenPath mavenPath = mavenPathParser.parsePath(asset.path());
      metadataBuilder.addArtifactVersion(mavenPath);
      if (rebuildChecksums) {
        mayUpdateChecksum(mavenPath, HashType.SHA1);
        mayUpdateChecksum(mavenPath, HashType.SHA256);
        mayUpdateChecksum(mavenPath, HashType.SHA512);
        mayUpdateChecksum(mavenPath, HashType.MD5);
      }
      final String packaging =
          component.attributes(repository.getFormat().getValue()).get(Attributes.P_PACKAGING, String.class);
      log.debug("POM packaging: {}", packaging);
      if ("maven-plugin".equals(packaging)) {
        metadataBuilder.addPlugin(getPluginPrefix(mavenPath.locateMainArtifact("jar")), component.name(),
            component.attributes(repository.getFormat().getValue()).get(Attributes.P_POM_NAME, String.class));
      }
    }

    @Override
    protected Optional<HashCode> getChecksum(final MavenPath mavenPath, final HashType hashType)
    {
      return repository.facet(ContentFacet.class)
          .assets()
          .path(PATH_PREFIX + mavenPath.getPath())
          .find()
          .flatMap(Asset::blob)
          .map(AssetBlob::checksums)
          .map(checksums -> checksums.get(hashType.name().toLowerCase()))
          .map(HashCode::fromString);
    }
  }

  @Override
  public Set<String> deleteMetadata(final Repository repository, final List<String[]> gavs) {
    checkNotNull(repository);
    checkNotNull(gavs);

    List<String> paths = Lists.newArrayList();
    for (String[] gav : gavs) {
      MavenPath mavenPath = metadataPath(gav[0], gav[1], gav[2]);
      paths.add(prependIfMissing(mavenPath.main().getPath(), PATH_PREFIX));
      for (HashType hashType : HashType.values()) {
        paths.add(prependIfMissing(mavenPath.main().hash(hashType).getPath(), PATH_PREFIX));
      }
    }

    MavenContentFacet mavenContentFacet = repository.facet(MavenContentFacet.class);
    Set<String> deletedPaths = Sets.newHashSet();
    if (mavenContentFacet.delete(paths)) {
      deletedPaths.addAll(paths);
    }
    return deletedPaths;
  }
}
