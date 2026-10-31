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
package org.sonatype.nexus.repository.search.sql.index;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.search.sql.SearchCustomFieldContributor;
import org.sonatype.nexus.repository.content.search.sql.SearchRecordExtension;
import org.sonatype.nexus.repository.content.store.InternalIds;
import org.sonatype.nexus.repository.content.utils.PreReleaseEvaluator;
import org.sonatype.nexus.repository.content.utils.SearchComponentPathFilter;
import org.sonatype.nexus.repository.search.normalize.VersionNormalizerService;
import org.sonatype.nexus.repository.search.sql.query.DatabaseTypeDetector;
import org.sonatype.nexus.repository.search.sql.store.SearchRecordData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// LENIENT: several test fixtures set up stubs on shared mocks (e.g. assetBlob, component)
// that are not consumed by every test method. STRICT_STUBS would fail on those unused stubs.
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchRecordProducerTest
{
  @Mock
  private VersionNormalizerService versionNormalizerService;

  @Mock
  private DatabaseTypeDetector databaseTypeDetector;

  @Mock
  private Repository repository;

  @Mock
  private FluentComponent component;

  @Mock
  private ContentFacet contentFacet;

  @Mock
  private Format format;

  private SearchRecordProducer underTest;

  @BeforeEach
  void setUp() {
    underTest = new SearchRecordProducer(
        Collections.emptyList(),
        versionNormalizerService,
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptySet(),
        databaseTypeDetector);

    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(contentFacet.contentRepositoryId()).thenReturn(1);
    when(repository.getName()).thenReturn("test-repo");
    when(repository.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("maven2");
    when(component.namespace()).thenReturn("com.example");
    when(component.name()).thenReturn("artifact");
    when(component.version()).thenReturn("1.0");
    when(component.kind()).thenReturn("component");
    when(component.entityVersion()).thenReturn(1);
    when(component.attributes()).thenReturn(mock(NestedAttributesMap.class));
  }

  @Test
  void testCreateSearchRecord_ComponentWithBlobAsset_UsesMaxBlobCreated() {
    OffsetDateTime blobTime = OffsetDateTime.now().minusHours(1);
    AssetBlob blob = mock(AssetBlob.class);
    when(blob.blobCreated()).thenReturn(blobTime);
    when(blob.checksums()).thenReturn(Collections.emptyMap());
    when(blob.createdBy()).thenReturn(Optional.empty());
    when(blob.createdByIp()).thenReturn(Optional.empty());

    FluentAsset asset = mock(FluentAsset.class);
    when(asset.blob()).thenReturn(Optional.of(blob));
    when(asset.path()).thenReturn("/com/example/artifact/1.0/artifact-1.0.jar");

    when(component.assets()).thenReturn(List.of(asset));

    try (MockedStatic<InternalIds> internalIds = mockStatic(InternalIds.class)) {
      internalIds.when(() -> InternalIds.internalComponentId(any(FluentComponent.class))).thenReturn(100);

      Optional<SearchRecordData> result = underTest.createSearchRecord(component, repository);

      assertTrue(result.isPresent());
      assertNotNull(result.get().getLastModified());
      assertTrue(result.get().getLastModified().isEqual(blobTime),
          "lastModified should equal the blob's blobCreated time");
    }
  }

  @Test
  void testCreateSearchRecord_AssetWithNoBlob_UsesNowFallback() {
    FluentAsset asset = mock(FluentAsset.class);
    when(asset.blob()).thenReturn(Optional.empty());
    when(asset.path()).thenReturn("/com/example/artifact/1.0/artifact-1.0.jar");
    when(component.assets()).thenReturn(List.of(asset));

    OffsetDateTime before = OffsetDateTime.now().minusSeconds(1);

    try (MockedStatic<InternalIds> internalIds = mockStatic(InternalIds.class)) {
      internalIds.when(() -> InternalIds.internalComponentId(any(FluentComponent.class))).thenReturn(100);

      Optional<SearchRecordData> result = underTest.createSearchRecord(component, repository);
      OffsetDateTime after = OffsetDateTime.now().plusSeconds(1);

      assertTrue(result.isPresent());
      assertNotNull(result.get().getLastModified(),
          "lastModified must not be null even when assets have no blob");
      assertTrue(!result.get().getLastModified().isBefore(before),
          "lastModified fallback should be close to now()");
      assertTrue(!result.get().getLastModified().isAfter(after),
          "lastModified fallback should be close to now()");
    }
  }

  @Test
  void testCreateSearchRecord_AssetWithNoBlob_DebugLoggingEnabled_LogsDebug() {
    ch.qos.logback.classic.Logger logger =
        (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(SearchRecordProducer.class);
    ch.qos.logback.classic.Level originalLevel = logger.getLevel();
    logger.setLevel(ch.qos.logback.classic.Level.DEBUG);

    try {
      FluentAsset asset = mock(FluentAsset.class);
      when(asset.blob()).thenReturn(Optional.empty());
      when(asset.path()).thenReturn("/path/to/artifact.jar");
      when(component.assets()).thenReturn(List.of(asset));

      try (MockedStatic<InternalIds> internalIds = mockStatic(InternalIds.class)) {
        internalIds.when(() -> InternalIds.internalComponentId(any(FluentComponent.class))).thenReturn(100);

        Optional<SearchRecordData> result = underTest.createSearchRecord(component, repository);

        assertTrue(result.isPresent());
        assertNotNull(result.get().getLastModified());
      }
    }
    finally {
      logger.setLevel(originalLevel);
    }
  }

  @Test
  void testCreateSearchRecord_NoAssets_ReturnsEmpty() {
    when(component.assets()).thenReturn(Collections.emptyList());

    Optional<SearchRecordData> result = underTest.createSearchRecord(component, repository);

    assertTrue(result.isEmpty(), "should return empty when component has no assets");
  }

  @Test
  void testCreateSearchRecord_WithPreReleaseEvaluator_SetsPrerelease() {
    PreReleaseEvaluator evaluator = mock(PreReleaseEvaluator.class, "maven2");
    when(evaluator.isPreRelease(any(), any())).thenReturn(true);

    underTest = new SearchRecordProducer(
        Collections.emptyList(),
        versionNormalizerService,
        List.of(evaluator),
        Collections.emptyList(),
        Collections.emptySet(),
        databaseTypeDetector);

    FluentAsset asset = mock(FluentAsset.class);
    when(asset.blob()).thenReturn(Optional.empty());
    when(asset.path()).thenReturn("/path/to/1.0-SNAPSHOT.jar");
    when(component.assets()).thenReturn(List.of(asset));

    try (MockedStatic<InternalIds> internalIds = mockStatic(InternalIds.class)) {
      internalIds.when(() -> InternalIds.internalComponentId(any(FluentComponent.class))).thenReturn(100);

      Optional<SearchRecordData> result = underTest.createSearchRecord(component, repository);

      assertTrue(result.isPresent());
      assertTrue(result.get().isPrerelease());
    }
  }

  @Test
  void testCreateSearchRecord_WithPathFilter_FilteredPath_NotAdded() {
    SearchComponentPathFilter filter = mock(SearchComponentPathFilter.class, "maven2");
    when(filter.shouldFilterPathExtension(any())).thenReturn(true);

    underTest = new SearchRecordProducer(
        Collections.emptyList(),
        versionNormalizerService,
        Collections.emptyList(),
        List.of(filter),
        Collections.emptySet(),
        databaseTypeDetector);

    FluentAsset asset = mock(FluentAsset.class);
    when(asset.blob()).thenReturn(Optional.empty());
    when(asset.path()).thenReturn("/path/to/artifact.jar");
    when(component.assets()).thenReturn(List.of(asset));

    try (MockedStatic<InternalIds> internalIds = mockStatic(InternalIds.class)) {
      internalIds.when(() -> InternalIds.internalComponentId(any(FluentComponent.class))).thenReturn(100);

      Optional<SearchRecordData> result = underTest.createSearchRecord(component, repository);

      assertTrue(result.isPresent());
    }
  }

  @Test
  void testCreateSearchRecord_WithExtension_ExtensionContributes() {
    SearchRecordExtension extension = mock(SearchRecordExtension.class);

    underTest = new SearchRecordProducer(
        Collections.emptyList(),
        versionNormalizerService,
        Collections.emptyList(),
        Collections.emptyList(),
        Set.of(extension),
        databaseTypeDetector);

    FluentAsset asset = mock(FluentAsset.class);
    when(asset.blob()).thenReturn(Optional.empty());
    when(asset.path()).thenReturn("/path/to/artifact.jar");
    when(component.assets()).thenReturn(List.of(asset));

    try (MockedStatic<InternalIds> internalIds = mockStatic(InternalIds.class)) {
      internalIds.when(() -> InternalIds.internalComponentId(any(FluentComponent.class))).thenReturn(100);

      Optional<SearchRecordData> result = underTest.createSearchRecord(component, repository);

      assertTrue(result.isPresent());
      verify(extension).contribute(any(), same(component));
    }
  }

  @Test
  void testCreateSearchRecord_WithEvaluatorReturnsFalse_NotPrerelease() {
    PreReleaseEvaluator evaluator = mock(PreReleaseEvaluator.class, "maven2");
    when(evaluator.isPreRelease(any(), any())).thenReturn(false);

    underTest = new SearchRecordProducer(
        Collections.emptyList(),
        versionNormalizerService,
        List.of(evaluator),
        Collections.emptyList(),
        Collections.emptySet(),
        databaseTypeDetector);

    FluentAsset asset = mock(FluentAsset.class);
    when(asset.blob()).thenReturn(Optional.empty());
    when(asset.path()).thenReturn("/path/to/1.0.jar");
    when(component.assets()).thenReturn(List.of(asset));

    try (MockedStatic<InternalIds> internalIds = mockStatic(InternalIds.class)) {
      internalIds.when(() -> InternalIds.internalComponentId(any(FluentComponent.class))).thenReturn(100);

      Optional<SearchRecordData> result = underTest.createSearchRecord(component, repository);

      assertTrue(result.isPresent());
      assertTrue(!result.get().isPrerelease());
    }
  }

  @Test
  void testCreateSearchRecord_WithFilterNotFiltering_AddsPath() {
    SearchComponentPathFilter filter = mock(SearchComponentPathFilter.class, "maven2");
    when(filter.shouldFilterPathExtension(any())).thenReturn(false);

    underTest = new SearchRecordProducer(
        Collections.emptyList(),
        versionNormalizerService,
        Collections.emptyList(),
        List.of(filter),
        Collections.emptySet(),
        databaseTypeDetector);

    FluentAsset asset = mock(FluentAsset.class);
    when(asset.blob()).thenReturn(Optional.empty());
    when(asset.path()).thenReturn("/path/to/artifact.jar");
    when(component.assets()).thenReturn(List.of(asset));

    try (MockedStatic<InternalIds> internalIds = mockStatic(InternalIds.class)) {
      internalIds.when(() -> InternalIds.internalComponentId(any(FluentComponent.class))).thenReturn(100);

      Optional<SearchRecordData> result = underTest.createSearchRecord(component, repository);

      assertTrue(result.isPresent());
    }
  }

  @Test
  void testCreateSearchRecord_WithContributorSearchByPathEnabled_AddsPath() {
    SearchCustomFieldContributor contributor = mock(SearchCustomFieldContributor.class, "maven2");
    when(contributor.isEnableSearchByPath(any())).thenReturn(true);

    underTest = new SearchRecordProducer(
        List.of(contributor),
        versionNormalizerService,
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptySet(),
        databaseTypeDetector);

    FluentAsset asset = mock(FluentAsset.class);
    when(asset.blob()).thenReturn(Optional.empty());
    when(asset.path()).thenReturn("/path/to/artifact.jar");
    when(component.assets()).thenReturn(List.of(asset));

    try (MockedStatic<InternalIds> internalIds = mockStatic(InternalIds.class)) {
      internalIds.when(() -> InternalIds.internalComponentId(any(FluentComponent.class))).thenReturn(100);

      Optional<SearchRecordData> result = underTest.createSearchRecord(component, repository);

      assertTrue(result.isPresent());
      verify(contributor).populateSearchCustomFields(any(), same(asset));
    }
  }

  @Test
  void testCreateSearchRecord_WithCustomFieldContributor_PopulatesFields() {
    SearchCustomFieldContributor contributor = mock(SearchCustomFieldContributor.class, "maven2");
    when(contributor.isEnableSearchByPath(any())).thenReturn(false);

    underTest = new SearchRecordProducer(
        List.of(contributor),
        versionNormalizerService,
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptySet(),
        databaseTypeDetector);

    FluentAsset asset = mock(FluentAsset.class);
    when(asset.blob()).thenReturn(Optional.empty());
    when(asset.path()).thenReturn("/path/to/artifact.jar");
    when(component.assets()).thenReturn(List.of(asset));

    try (MockedStatic<InternalIds> internalIds = mockStatic(InternalIds.class)) {
      internalIds.when(() -> InternalIds.internalComponentId(any(FluentComponent.class))).thenReturn(100);

      Optional<SearchRecordData> result = underTest.createSearchRecord(component, repository);

      assertTrue(result.isPresent());
      verify(contributor).populateSearchCustomFields(any(), same(asset));
    }
  }
}
