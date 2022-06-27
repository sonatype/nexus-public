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
package org.sonatype.nexus.testsuite.testsupport.maven;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.time.DateHelper;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.maintenance.ContentMaintenanceFacet;
import org.sonatype.nexus.repository.content.store.InternalIds;
import org.sonatype.nexus.repository.maven.MavenMetadataRebuildFacet;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.HashType;
import org.sonatype.nexus.repository.maven.MavenPathParser;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import com.google.common.collect.ImmutableList;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.common.entity.Continuations.iterableOf;
import static org.sonatype.nexus.common.entity.Continuations.streamOf;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.repository.maven.MavenMetadataRebuildFacet.METADATA_FORCE_REBUILD;
import static org.sonatype.nexus.repository.maven.MavenMetadataRebuildFacet.METADATA_REBUILD;

@Named
@Singleton
public class DataStoreMavenTestHelper
    extends MavenTestHelper
{
  private static final String ASSET_PATH_PREFIX = "/";

  @Override
  public void write(final Repository repository, final String path, final Payload payload) throws IOException
  {
    MavenContentFacet mavenContentFacet = repository.facet(MavenContentFacet.class);
    MavenPath mavenPath = mavenContentFacet.getMavenPathParser().parsePath(path);
    mavenContentFacet.put(mavenPath, payload);
  }

  @Override
  public Payload read(final Repository repository, final String path) throws IOException {
    MavenContentFacet mavenFacet = repository.facet(MavenContentFacet.class);
    Optional<MavenMetadataRebuildFacet> metadataRebuildFacet =
        repository.optionalFacet(MavenMetadataRebuildFacet.class);
    if (metadataRebuildFacet.isPresent()) {
      metadataRebuildFacet.get().maybeRebuildMavenMetadata(prependIfMissing(path, ASSET_PATH_PREFIX), false, true);
    }
    MavenPath mavenPath = mavenFacet.getMavenPathParser().parsePath(path);
    return mavenFacet.get(mavenPath).orElse(null);
  }

  @Override
  public boolean delete(final Repository repository, final String path) throws Exception {
    MavenContentFacet mavenFacet = repository.facet(MavenContentFacet.class);
    MavenPath mavenPath = mavenFacet.getMavenPathParser().parsePath(path);
    return mavenFacet.delete(mavenPath);
  }

  @Override
  public void writeWithoutValidation(final Repository repository, final String path, final Payload payload) {
    MavenContentFacet mavenContentFacet = repository.facet(MavenContentFacet.class);
    MavenPath mavenPath = mavenContentFacet.getMavenPathParser().parsePath(path);

    try (TempBlob blob = mavenContentFacet.blobs().ingest(payload, ImmutableList.of(SHA1, MD5))) {
      mavenContentFacet.assets()
          .path(ASSET_PATH_PREFIX + mavenPath.getPath())
          .blob(blob)
          .save();
    }
  }

  @Override
  public void verifyHashesExistAndCorrect(final Repository repository, final String path) throws Exception {
    MavenContentFacet mavenContentFacet = repository.facet(MavenContentFacet.class);
    MavenPath mavenPath = mavenContentFacet.getMavenPathParser().parsePath(path);

    Optional<Map<String, String>> maybeHashCodes = mavenContentFacet.get(mavenPath).map(this::getExpectedHashCodes);

    assertTrue(maybeHashCodes.isPresent());
    assertExpectedHashContentMatchActual(maybeHashCodes.get(), mavenContentFacet, mavenPath);
  }

  private void assertExpectedHashContentMatchActual(
      final Map<String, String> expectedHashCodes,
      final MavenContentFacet mavenContentFacet,
      final MavenPath mavenPath) throws IOException
  {
    for (HashType hashType : HashType.values()) {
      String expectedHashContent = expectedHashCodes.get(hashType.getHashAlgorithm().name());
      Optional<Content> maybeStoredHashContent = mavenContentFacet.get(mavenPath.hash(hashType));
      // Maven deployer does not create these hashes by default yet but we are storing the calculated values in the asset attributes
      if(!maybeStoredHashContent.isPresent() && (hashType  == HashType.SHA256 ||  hashType  == HashType.SHA512) ) {
        continue;
      }
      assertTrue(maybeStoredHashContent.isPresent());
      try (InputStream inputStream = maybeStoredHashContent.get().openInputStream()) {
        String storedHashContent = IOUtils.toString(new InputStreamReader(inputStream, UTF_8));
        assertThat(storedHashContent, equalTo(expectedHashContent));
      }
    }
  }

  private Map<String, String> getExpectedHashCodes(final Content content) {
    Asset asset = content.getAttributes().get(Asset.class);
    if (asset != null) {
      return asset.blob()
          .map(AssetBlob::checksums)
          .orElse(emptyMap());
    }
    return emptyMap();
  }

  @Override
  public DateTime getLastDownloadedTime(final Repository repository, final String assetPath) throws IOException {
    MavenContentFacet mavenContentFacet = repository.facet(MavenContentFacet.class);
    MavenPath mavenPath = mavenContentFacet.getMavenPathParser().parsePath(assetPath);
    return mavenContentFacet.get(mavenPath)
        .map(Content::getAttributes)
        .map(attributes -> attributes.get(Asset.class))
        .map(Asset::lastDownloaded)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(DateHelper::toDateTime)
        .orElse(null);
  }

  @Override
  public void rebuildMetadata(
      final Repository repository,
      final String groupId,
      final String artifactId,
      final String baseVersion,
      final boolean rebuildChecksums,
      final boolean update)
  {
    repository.facet(MavenMetadataRebuildFacet.class)
        .rebuildMetadata(groupId, artifactId, baseVersion, rebuildChecksums, update);
  }

  @Override
  public void deleteComponents(final Repository repository, final String version, final int expectedNumber) {
    MavenContentFacet mavenContentFacet = repository.facet(MavenContentFacet.class);
    List<FluentComponent> components = findComponents(mavenContentFacet, version).collect(Collectors.toList());
    assertThat(components, hasSize(expectedNumber));
    ContentMaintenanceFacet contentMaintenanceFacet = repository.facet(ContentMaintenanceFacet.class);
    for (FluentComponent component : components) {
      contentMaintenanceFacet.deleteComponent(component);
    }
  }

  @Override
  public void deleteAssets(final Repository repository, final String version, final int expectedNumber) {
    MavenContentFacet mavenContentFacet = repository.facet(MavenContentFacet.class);
    List<FluentAsset> assets = findComponents(mavenContentFacet, version)
        .flatMap(component -> component.assets().stream())
        .collect(Collectors.toList());
    assertThat(assets, hasSize(expectedNumber));
    deleteAll(mavenContentFacet, assets);
  }

  private void deleteAll(final MavenContentFacet mavenContentFacet, final Collection<FluentAsset> assets) {
    MavenPathParser mavenPathParser = mavenContentFacet.getMavenPathParser();
    assets.stream().map(FluentAsset::path).map(mavenPathParser::parsePath).forEach(
        path -> {
          try {
            mavenContentFacet.delete(path);
          }
          catch (IOException e) {
            e.printStackTrace();
          }
        });
  }

  @Nonnull
  private Stream<FluentComponent> findComponents(final MavenContentFacet mavenContentFacet, final String version) {
    return streamOf(mavenContentFacet.components()::browse)
        .filter(component -> component.namespace().equals("org.sonatype.nexus.testsuite"))
        .filter(component -> version.equals(component.attributes("maven2").get("baseVersion")));
  }

  @Override
  public EntityId createComponent(
      final Repository repository,
      final String groupId,
      final String artifactId,
      final String version)
  {
    String path = String.format("/%s/%s/%s/%s-%s.jar", groupId, artifactId, version, artifactId, version);
    FluentAsset asset = repository.facet(ContentFacet.class).assets()
        .path(path)
        .component(repository.facet(ContentFacet.class).components()
            .name(artifactId)
            .namespace(groupId)
            .version(version)
            .getOrCreate()
            .withAttribute("maven2", Collections.singletonMap("baseVersion", version)))
        .save();
    return InternalIds.toExternalId(InternalIds.internalComponentId(asset).getAsInt());
  }

  @Override
  public List<MavenTestComponent> loadComponents(final Repository repository) {
    List<MavenTestComponent> components = new ArrayList<>();
    MavenContentFacet mavenContentFacet = repository.facet(MavenContentFacet.class);
    Iterable<FluentComponent> fluentComponents = iterableOf(mavenContentFacet.components()::browse);
    for (FluentComponent fluentComponent : fluentComponents) {
      components.add(new MavenTestComponent(fluentComponent.name(),
          fluentComponent.attributes().child("maven2").get("baseVersion").toString(), fluentComponent.version(),
          new DateTime(Date.from(fluentComponent.lastUpdated().toInstant()))));
    }
    return components;
  }

  @Override
  public void updateBlobCreated(final Repository repository, final Date date) {
    MavenContentFacet mavenContentFacet = repository.facet(MavenContentFacet.class);
    iterableOf(mavenContentFacet.assets()::browse)
        .forEach(asset -> asset.blobCreated(date.toInstant().atOffset(ZoneOffset.UTC)));
  }

  @Override
  public List<String> findComponents(final Repository repository) {
    MavenContentFacet mavenContentFacet = repository.facet(MavenContentFacet.class);
    return streamOf(mavenContentFacet.components()::browse)
        .map(component -> component.name() + ":" + component.attributes("maven2").get("baseVersion"))
        .collect(Collectors.toList());
  }

  @Override
  public List<String> findAssets(final Repository repository) {
    MavenContentFacet mavenContentFacet = repository.facet(MavenContentFacet.class);
    return streamOf(mavenContentFacet.assets()::browse)
        .map(Asset::path)
        .collect(Collectors.toList());
  }

  @Override
  public List<String> findAssetsExcludingFlaggedForRebuild(final Repository repository) {
    MavenContentFacet mavenContentFacet = repository.facet(MavenContentFacet.class);
    return streamOf(mavenContentFacet.assets()::browse)
        .filter(this::isNotFlaggedForRebuild)
        .map(Asset::path)
        .collect(Collectors.toList());
  }

  @Override
  public void markMetadataForRebuild(final Repository repository, final String path) {
    Optional<FluentAsset> maybeAsset =
        repository.facet(MavenContentFacet.class).assets().path(prependIfMissing(path, ASSET_PATH_PREFIX)).find();
    assertTrue("Could not set forceRebuild flag, because requested path does not exist", maybeAsset.isPresent());
    FluentAsset asset = maybeAsset.get();
    asset.withAttribute(METADATA_REBUILD, Collections.singletonMap(METADATA_FORCE_REBUILD, true));
  }

  private boolean isNotFlaggedForRebuild(final FluentAsset asset) {
    return asset.attributes("metadataRebuild").isEmpty();
  }
}
