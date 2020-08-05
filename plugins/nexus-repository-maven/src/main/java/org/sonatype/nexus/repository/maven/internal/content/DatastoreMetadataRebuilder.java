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

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.MultipleFailures;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponentBuilder;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.HashType;
import org.sonatype.nexus.repository.maven.internal.Attributes;
import org.sonatype.nexus.repository.maven.internal.hosted.metadata.AbstractMetadataRebuilder;
import org.sonatype.nexus.repository.maven.internal.hosted.metadata.AbstractMetadataUpdater;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.transaction.Transactional;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;

import static com.google.common.base.Preconditions.checkNotNull;
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
    return new Worker(repository, update, rebuildChecksums, groupId, artifactId, baseVersion, bufferSize,
        timeoutSeconds, new DatastoreMetadataUpdater(update, repository)).rebuildMetadata();
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

  protected static class Worker
      extends AbstractMetadataRebuilder.Worker
  {
    public Worker(
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

        List<FluentComponent> filteredComponents = allVersions.stream()
            .map(version -> componentBuilder.version(version).find())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(component -> baseVersion.equals(component.attributes("maven2").get("baseVersion", String.class)))
            .collect(Collectors.toList());
        List<FluentAsset> assets = filteredComponents.stream()
            .map(FluentComponent::assets)
            .flatMap(Collection::stream)
            .filter(asset -> !mavenPathParser.parsePath(asset.path()).isSubordinate())
            .collect(Collectors.toList());

        for (FluentAsset asset : assets) {
          processAsset(asset);
        }

        processMetadata(metadataPath(groupId, artifactId, baseVersion), metadataBuilder.onExitBaseVersion(), failures);
      }

      processMetadata(metadataPath(groupId, artifactId, null), metadataBuilder.onExitArtifactId(), failures);
    }

    private void processAsset(final FluentAsset asset) {
      checkCancellation();
      Component component = asset.component().get();
      MavenPath mavenPath = mavenPathParser.parsePath(asset.path());
      metadataBuilder.addArtifactVersion(mavenPath);
      if (rebuildChecksums) {
        mayUpdateChecksum(mavenPath, HashType.SHA1);
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
          .path("/" + mavenPath.getPath())
          .find()
          .flatMap(Asset::blob)
          .map(AssetBlob::checksums)
          .map(checksums -> checksums.get(hashType.name()))
          .map(HashCode::fromString);
    }
  }

  @Override
  public void deleteMetadata(final Repository repository, final List<String[]> gavs) {
    checkNotNull(repository);
    checkNotNull(gavs);

    List<MavenPath> mavenPaths = Lists.newArrayList();
    for (String[] gav : gavs) {
      MavenPath mavenPath = metadataPath(gav[0], gav[1], gav[2]);
      mavenPaths.add(mavenPath.main());
      for (HashType hashType : HashType.values()) {
        mavenPaths.add(mavenPath.main().hash(hashType));
      }
    }

    MavenContentFacet mavenContentFacet = repository.facet(MavenContentFacet.class);
    try {
      for (MavenPath mavenPath : mavenPaths) {
        mavenContentFacet.delete(mavenPath);
      }
    }
    catch (IOException e) {
      log.warn("Error encountered when deleting metadata: repository={}", repository);
      throw new RuntimeException(e);
    }
  }
}
