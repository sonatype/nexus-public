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
package org.sonatype.nexus.content.maven.internal.recipe;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.content.maven.store.Maven2ComponentStore;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacetStores;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssetBuilder;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponentBuilder;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPathParser;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.maven.internal.Maven2MavenPathParser;
import org.sonatype.nexus.repository.maven.internal.hosted.metadata.MetadataRebuilder;
import org.sonatype.nexus.repository.maven.internal.validation.MavenMetadataContentValidator;
import org.sonatype.nexus.repository.search.normalize.VersionNormalizerService;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class MavenContentFacetImplTest

{
  private MavenContentFacetImpl underTest;

  @Mock
  private FormatStoreManager formatStoreManager;

  @Mock
  private MetadataRebuilder metadataRebuilder;

  @Mock
  private MavenMetadataContentValidator metadataValidator;

  @Mock
  private EventManager eventManager;

  @Mock
  private Repository repository;

  @Mock
  private FluentAssets fluentAssets;

  @Mock
  private FluentAssetBuilder fluentAssetBuilder;

  @Mock
  private FluentAsset fluentAsset;

  @Mock
  private Content content;

  @Mock
  private FluentComponents fluentComponents;

  @Mock
  private FluentComponentBuilder fluentComponentBuilder;

  @Mock
  private FluentComponent fluentComponent;

  @Mock
  private NestedAttributesMap componentAttributes;

  @Mock
  private Maven2ComponentStore maven2ComponentStore;

  @Mock
  private ContentFacetStores contentFacetStores;

  @Mock
  private VersionNormalizerService versionNormalizerService;

  @Mock
  private TempBlob tempBlob;

  private MavenPathParser mavenPathParser;

  @Before
  public void setup() throws Exception {
    mavenPathParser = new Maven2MavenPathParser();
    List<MavenPathParser> mavenPathParsers = Collections.singletonList(mavenPathParser);

    underTest = spy(new MavenContentFacetImpl(
        formatStoreManager,
        mavenPathParsers,
        metadataRebuilder,
        metadataValidator,
        eventManager,
        true));

    when(repository.getFormat()).thenReturn(new Maven2Format());

    underTest.attach(repository);

    when(underTest.assets()).thenReturn(fluentAssets);
  }

  @Test
  public void shouldReturnContentWhenAssetExists() {
    MavenPath mavenPath = mavenPathParser.parsePath("com/example/artifact/1.0/artifact-1.0.jar");

    when(fluentAssets.path("/com/example/artifact/1.0/artifact-1.0.jar")).thenReturn(fluentAssetBuilder);
    when(fluentAssetBuilder.find()).thenReturn(Optional.of(fluentAsset));
    when(fluentAsset.download()).thenReturn(content);

    Optional<Content> result = underTest.get(mavenPath);

    assertTrue(result.isPresent());
    assertThat(result.get(), is(content));
  }

  @Test
  public void shouldReturnEmptyWhenAssetDoesNotExist() {
    MavenPath mavenPath = mavenPathParser.parsePath("com/example/artifact/1.0/artifact-1.0.jar");

    when(fluentAssets.path("/com/example/artifact/1.0/artifact-1.0.jar")).thenReturn(fluentAssetBuilder);
    when(fluentAssetBuilder.find()).thenReturn(Optional.empty());

    Optional<Content> result = underTest.get(mavenPath);

    assertFalse(result.isPresent());
  }

  @Test
  public void shouldReturnTrueWhenAssetExists() {
    MavenPath mavenPath = mavenPathParser.parsePath("com/example/artifact/1.0/artifact-1.0.jar");

    when(fluentAssets.path("/com/example/artifact/1.0/artifact-1.0.jar")).thenReturn(fluentAssetBuilder);
    when(fluentAssetBuilder.find()).thenReturn(Optional.of(fluentAsset));

    boolean result = underTest.exists(mavenPath);

    assertTrue(result);
  }

  @Test
  public void shouldReturnFalseWhenAssetDoesNotExist() {
    MavenPath mavenPath = mavenPathParser.parsePath("com/example/artifact/1.0/artifact-1.0.jar");

    when(fluentAssets.path("/com/example/artifact/1.0/artifact-1.0.jar")).thenReturn(fluentAssetBuilder);
    when(fluentAssetBuilder.find()).thenReturn(Optional.empty());

    boolean result = underTest.exists(mavenPath);

    assertFalse(result);
  }

  @Test
  public void shouldDeleteAssetWhenItExists() {
    MavenPath mavenPath = mavenPathParser.parsePath("com/example/maven-metadata.xml");

    when(fluentAssets.path("/com/example/maven-metadata.xml")).thenReturn(fluentAssetBuilder);
    when(fluentAssetBuilder.find()).thenReturn(Optional.of(fluentAsset));
    when(fluentAsset.delete()).thenReturn(true);

    boolean result = underTest.delete(mavenPath);

    assertTrue(result);
    verify(fluentAsset).delete();
  }

  @Test
  public void shouldReturnFalseWhenDeletingNonExistentAsset() {
    MavenPath mavenPath = mavenPathParser.parsePath("com/example/maven-metadata.xml");

    when(fluentAssets.path("/com/example/maven-metadata.xml")).thenReturn(fluentAssetBuilder);
    when(fluentAssetBuilder.find()).thenReturn(Optional.empty());

    boolean result = underTest.delete(mavenPath);

    assertFalse(result);
  }
}
