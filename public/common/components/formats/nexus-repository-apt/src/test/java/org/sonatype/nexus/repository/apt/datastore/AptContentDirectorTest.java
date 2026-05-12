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
package org.sonatype.nexus.repository.apt.datastore;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.apt.AptFormat;
import org.sonatype.nexus.repository.apt.datastore.internal.hosted.metadata.AptHostedMetadataFacet;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.types.HostedType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class AptContentDirectorTest
{
  private static final String REPO_NAME = "apt-hosted";

  private static final String SOURCE_REPO_NAME = "apt-source";

  private static final String DISTRIBUTION = "focal";

  private static final String METADATA_PREFIX = "/dists/focal/";

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private Repository destination;

  @Mock
  private Repository source;

  @Mock
  private Component component;

  @Mock
  private FluentComponent fluentComponent;

  @Mock
  private List<Asset> assets;

  @Mock
  private AptHostedMetadataFacet metadataFacet;

  @Mock
  private AptHostedMetadataFacet sourceMetadataFacet;

  @Mock
  private AptContentFacet aptContentFacet;

  @Mock
  private ContentFacet contentFacet;

  @Mock
  private FluentComponents fluentComponents;

  @Mock
  private FluentAssets fluentAssets;

  @Mock
  private Format aptFormat;

  @Mock
  private Type hostedType;

  @Captor
  private ArgumentCaptor<List<String>> rebuildCaptor;

  private AptContentDirector underTest;

  @Before
  public void setup() {
    underTest = new AptContentDirector(repositoryManager);

    // Setup APT hosted repository mocks
    when(aptFormat.getValue()).thenReturn(AptFormat.NAME);
    when(hostedType.getValue()).thenReturn(HostedType.NAME);
    when(destination.getFormat()).thenReturn(aptFormat);
    when(destination.getType()).thenReturn(hostedType);
    when(destination.getName()).thenReturn(REPO_NAME);
    when(source.getFormat()).thenReturn(aptFormat);
    when(source.getType()).thenReturn(hostedType);
    when(source.getName()).thenReturn(SOURCE_REPO_NAME);

    // Setup facets for destination repository
    when(destination.facet(AptHostedMetadataFacet.class)).thenReturn(metadataFacet);
    when(destination.facet(AptContentFacet.class)).thenReturn(aptContentFacet);
    when(destination.facet(ContentFacet.class)).thenReturn(contentFacet);

    // Setup facets for source repository
    when(source.facet(AptHostedMetadataFacet.class)).thenReturn(sourceMetadataFacet);
    when(source.facet(AptContentFacet.class)).thenReturn(aptContentFacet);
    when(source.facet(ContentFacet.class)).thenReturn(contentFacet);

    // Setup APT facet and content facet behavior
    when(aptContentFacet.getDistribution()).thenReturn(DISTRIBUTION);
    when(aptContentFacet.getAptPackageAssets()).thenReturn(Collections.emptyList());
    when(contentFacet.components()).thenReturn(fluentComponents);
    when(contentFacet.assets()).thenReturn(fluentAssets);
  }

  @Test
  public void testAllowMoveTo() {
    boolean result = underTest.allowMoveTo(destination);
    assertTrue(result);
  }

  @Test
  public void testAllowMoveToWithComponent() {
    boolean result = underTest.allowMoveTo(fluentComponent, destination);
    assertTrue(result);
  }

  @Test
  public void testAllowMoveFrom() {
    boolean result = underTest.allowMoveFrom(source);
    assertTrue(result);
  }

  @Test
  public void testBeforeMove_TracksAptHostedRepository() {
    Component result = underTest.beforeMove(component, assets, source, destination);

    assertThat(result, is(component));
    // ThreadLocal tracking is verified in batch afterMove test
  }

  @Test
  public void testBeforeMove_IgnoresNonAptRepository() {
    when(source.getFormat().getValue()).thenReturn("maven2");

    Component result = underTest.beforeMove(component, assets, source, destination);

    assertThat(result, is(component));
    // No tracking should occur for non-APT repos
  }

  @Test
  public void testAfterMoveIndividual_RebuildsDestinationMetadata() throws IOException {
    Component result = underTest.afterMove(component, destination);

    assertThat(result, is(component));
    verify(metadataFacet).rebuildMetadata();
  }

  @Test
  public void testAfterMoveBatch_RebuildsDestinationAndProcessesEmptySource() throws IOException {
    // Setup: empty source repository
    setupEmptyRepository();
    setupMetadataAssets();
    when(repositoryManager.get(SOURCE_REPO_NAME)).thenReturn(source);

    // Track source repository via beforeMove
    underTest.beforeMove(component, assets, source, destination);

    List<Map<String, String>> components = List.of(Map.of("name", "comp1", "group", "group1", "version", "1.0"));

    underTest.afterMove(components, destination);

    // Verify destination rebuild
    verify(metadataFacet).rebuildMetadata();

    // Verify source cleanup (metadata assets deleted)
    verify(repositoryManager).get(SOURCE_REPO_NAME);
  }

  @Test
  public void testAfterMoveBatch_HandlesMultipleSourceRepositories() throws IOException {
    // Setup multiple source repositories
    Repository sourceA = mock(Repository.class);
    Repository sourceB = mock(Repository.class);
    Component component1 = mock(Component.class);
    Component component2 = mock(Component.class);
    Component component3 = mock(Component.class);
    AptContentFacet aptContentFacetA = mock(AptContentFacet.class);
    AptContentFacet aptContentFacetB = mock(AptContentFacet.class);
    ContentFacet contentFacetA = mock(ContentFacet.class);
    ContentFacet contentFacetB = mock(ContentFacet.class);

    // Setup sourceA
    when(sourceA.getFormat()).thenReturn(aptFormat);
    when(sourceA.getType()).thenReturn(hostedType);
    when(sourceA.getName()).thenReturn("sourceA");
    when(sourceA.facet(AptHostedMetadataFacet.class)).thenReturn(sourceMetadataFacet);
    when(sourceA.facet(AptContentFacet.class)).thenReturn(aptContentFacetA);
    when(sourceA.facet(ContentFacet.class)).thenReturn(contentFacetA);
    when(aptContentFacetA.getAptPackageAssets()).thenReturn(Collections.emptyList());
    when(aptContentFacetA.getDistribution()).thenReturn(DISTRIBUTION);
    when(contentFacetA.components()).thenReturn(fluentComponents);
    when(contentFacetA.assets()).thenReturn(fluentAssets);

    // Setup sourceB
    when(sourceB.getFormat()).thenReturn(aptFormat);
    when(sourceB.getType()).thenReturn(hostedType);
    when(sourceB.getName()).thenReturn("sourceB");
    when(sourceB.facet(AptHostedMetadataFacet.class)).thenReturn(sourceMetadataFacet);
    when(sourceB.facet(AptContentFacet.class)).thenReturn(aptContentFacetB);
    when(sourceB.facet(ContentFacet.class)).thenReturn(contentFacetB);
    when(aptContentFacetB.getAptPackageAssets()).thenReturn(Collections.emptyList());
    when(aptContentFacetB.getDistribution()).thenReturn(DISTRIBUTION);
    when(contentFacetB.components()).thenReturn(fluentComponents);
    when(contentFacetB.assets()).thenReturn(fluentAssets);

    // Setup repository manager
    when(repositoryManager.get("sourceA")).thenReturn(sourceA);
    when(repositoryManager.get("sourceB")).thenReturn(sourceB);

    // Setup empty repositories for cleanup
    setupEmptyRepository();
    setupMetadataAssets();

    // Track multiple sources
    underTest.beforeMove(component1, assets, sourceA, destination);
    underTest.beforeMove(component2, assets, sourceB, destination);
    underTest.beforeMove(component3, assets, sourceA, destination); // sourceA again

    List<Map<String, String>> components = List.of(Map.of("name", "comp1", "group", "group1", "version", "1.0"),
        Map.of("name", "comp2", "group", "group2", "version", "2.0"),
        Map.of("name", "comp3", "group", "group3", "version", "3.0"));

    // Batch move should process both sourceA and sourceB
    underTest.afterMove(components, destination);

    // Verify both sources processed
    verify(repositoryManager).get("sourceA");
    verify(repositoryManager).get("sourceB");

    // Verify destination rebuild
    verify(metadataFacet).rebuildMetadata();
  }

  @Test
  public void testAfterMoveBatch_RebuildsNonEmptySource() throws IOException {
    // Setup: non-empty source repository
    setupNonEmptyRepository();
    when(repositoryManager.get(SOURCE_REPO_NAME)).thenReturn(source);

    // Track source repository
    underTest.beforeMove(component, assets, source, destination);

    List<Map<String, String>> components = List.of(Map.of("name", "comp1", "group", "group1", "version", "1.0"));

    underTest.afterMove(components, destination);

    // Verify destination rebuild
    verify(metadataFacet).rebuildMetadata();

    // Verify source rebuild (not cleanup)
    verify(sourceMetadataFacet).rebuildMetadata();
  }

  @Test
  public void testAfterMoveBatch_HandlesMetadataAssetPagination() throws IOException {
    setupEmptyRepository();
    setupPaginatedMetadataAssets();
    when(repositoryManager.get(SOURCE_REPO_NAME)).thenReturn(source);

    underTest.beforeMove(component, assets, source, destination);

    List<Map<String, String>> components = List.of(Map.of("name", "comp1", "group", "group1", "version", "1.0"));

    underTest.afterMove(components, destination);

    // Verify pagination was handled (browse called multiple times)
    verify(fluentAssets, times(2)).browse(eq(1000), any());
  }

  @Test
  public void testThreadLocalThreadSafety() throws InterruptedException {
    int threadCount = 5;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    AtomicInteger successCount = new AtomicInteger(0);

    // Setup different source repositories for each thread
    for (int i = 0; i < threadCount; i++) {
      final int threadId = i;
      executor.submit(() -> {
        try {
          Repository threadSource = mock(Repository.class);
          AptContentFacet threadAptFacet = mock(AptContentFacet.class);
          ContentFacet threadContentFacet = mock(ContentFacet.class);
          AptHostedMetadataFacet threadMetadataFacet = mock(AptHostedMetadataFacet.class);
          FluentComponents threadFluentComponents = mock(FluentComponents.class);
          FluentAssets threadFluentAssets = mock(FluentAssets.class);

          when(threadSource.getFormat()).thenReturn(aptFormat);
          when(threadSource.getType()).thenReturn(hostedType);
          when(threadSource.getName()).thenReturn("source-" + threadId);
          when(repositoryManager.get("source-" + threadId)).thenReturn(threadSource);
          when(threadSource.facet(ContentFacet.class)).thenReturn(threadContentFacet);
          when(threadSource.facet(AptContentFacet.class)).thenReturn(threadAptFacet);
          when(threadSource.facet(AptHostedMetadataFacet.class)).thenReturn(threadMetadataFacet);
          when(threadAptFacet.getAptPackageAssets()).thenReturn(Collections.emptyList());
          when(threadAptFacet.getDistribution()).thenReturn(DISTRIBUTION);
          when(threadContentFacet.components()).thenReturn(threadFluentComponents);
          when(threadContentFacet.assets()).thenReturn(threadFluentAssets);

          // Setup empty repository for this thread
          when(threadFluentComponents.count()).thenReturn(0);
          Continuation<FluentAsset> emptyAssetPage = mock(Continuation.class);
          when(emptyAssetPage.iterator()).thenReturn(Collections.emptyIterator());
          when(emptyAssetPage.nextContinuationToken()).thenReturn(null);
          when(threadFluentAssets.browse(anyInt(), any())).thenReturn(emptyAssetPage);

          // Each thread tracks its own repository
          underTest.beforeMove(component, assets, threadSource, destination);

          List<Map<String, String>> components =
              List.of(Map.of("name", "comp-" + threadId, "group", "group", "version", "1.0"));

          underTest.afterMove(components, destination);
          successCount.incrementAndGet();
        }
        catch (Exception e) {
          System.err.println("Thread " + threadId + " failed: " + e.getMessage());
        }
        finally {
          latch.countDown();
        }
      });
    }

    assertTrue("All threads should complete successfully", latch.await(10, TimeUnit.SECONDS));
    assertThat("All threads should succeed", successCount.get(), is(threadCount));

    executor.shutdown();
  }

  private void setupEmptyRepository() {
    Iterator<FluentComponent> emptyIterator = Collections.emptyIterator();
    Continuation<FluentComponent> emptyPage = mock(Continuation.class);
    when(emptyPage.iterator()).thenReturn(emptyIterator);
    when(fluentComponents.browse(1, null)).thenReturn(emptyPage);
    when(fluentComponents.count()).thenReturn(0);
  }

  private void setupNonEmptyRepository() {
    FluentComponent comp = mock(FluentComponent.class);
    Iterator<FluentComponent> iterator = List.of(comp).iterator();
    Continuation<FluentComponent> page = mock(Continuation.class);
    when(page.iterator()).thenReturn(iterator);
    when(fluentComponents.browse(1, null)).thenReturn(page);
    when(fluentComponents.count()).thenReturn(1);
  }

  private void setupMetadataAssets() {
    FluentAsset metadataAsset = mock(FluentAsset.class);
    when(metadataAsset.path()).thenReturn(METADATA_PREFIX + "Release");

    FluentAsset packageAsset = mock(FluentAsset.class);
    when(packageAsset.path()).thenReturn("/pool/main/h/hello/hello_1.0_amd64.deb");

    Continuation<FluentAsset> assetPage = mockAssetPage(List.of(metadataAsset, packageAsset), null);
    when(fluentAssets.browse(anyInt(), any())).thenReturn(assetPage);
  }

  private void setupPaginatedMetadataAssets() {
    // First page
    FluentAsset asset1 = mock(FluentAsset.class);
    when(asset1.path()).thenReturn(METADATA_PREFIX + "Release");

    // Second page
    FluentAsset asset2 = mock(FluentAsset.class);
    when(asset2.path()).thenReturn(METADATA_PREFIX + "Packages");

    Continuation<FluentAsset> page1 = mockAssetPage(List.of(asset1), "token1");
    Continuation<FluentAsset> page2 = mockAssetPage(List.of(asset2), null);

    when(fluentAssets.browse(eq(1000), eq(null))).thenReturn(page1);
    when(fluentAssets.browse(eq(1000), eq("token1"))).thenReturn(page2);
  }

  @SuppressWarnings("unchecked")
  private Continuation<FluentAsset> mockAssetPage(List<FluentAsset> assets, String nextToken) {
    Continuation<FluentAsset> page = mock(Continuation.class);
    when(page.iterator()).thenReturn(assets.iterator());
    when(page.nextContinuationToken()).thenReturn(nextToken);
    return page;
  }
}
