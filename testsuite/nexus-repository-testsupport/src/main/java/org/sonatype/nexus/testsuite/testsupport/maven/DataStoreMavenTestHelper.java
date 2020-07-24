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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.time.DateHelper;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.content.maven.MavenMetadataRebuildFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;

@Named
@Singleton
public class DataStoreMavenTestHelper
    extends MavenTestHelper
{
  private static final char ASSET_PATH_PREFIX = '/';

  @Override
  public void write(final Repository repository, final String path, final Payload payload) throws IOException
  {
    MavenContentFacet mavenContentFacet = repository.facet(MavenContentFacet.class);
    MavenPath mavenPath = mavenContentFacet.getMavenPathParser().parsePath(path);
    mavenContentFacet.put(mavenPath, payload);
  }

  @Override
  public void writeWithoutValidation(final Repository repository, final String path, final Payload payload) {
    MavenContentFacet mavenContentFacet = repository.facet(MavenContentFacet.class);
    MavenPath mavenPath = mavenContentFacet.getMavenPathParser().parsePath(path);

    try (TempBlob blob = mavenContentFacet.blobs().ingest(payload, ImmutableList.of(SHA1, MD5))) {
      mavenContentFacet.assets()
          .path(ASSET_PATH_PREFIX + mavenPath.getPath())
          .getOrCreate()
          .attach(blob);
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
    components.stream().map(FluentComponent::assets).forEach(assets -> deleteAll(mavenContentFacet, assets));
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
    return mavenContentFacet.components()
        .browse(Integer.MAX_VALUE, null)
        .stream()
        .filter(component -> component.namespace().equals("org.sonatype.nexus.testsuite"))
        .filter(component -> version.equals(component.attributes("maven2").get("baseVersion")));
  }
}
