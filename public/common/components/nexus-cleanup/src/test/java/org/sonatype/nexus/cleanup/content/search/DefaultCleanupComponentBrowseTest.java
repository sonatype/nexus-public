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
package org.sonatype.nexus.cleanup.content.search;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.sonatype.nexus.cleanup.datastore.search.criteria.AssetCleanupEvaluator;
import org.sonatype.nexus.cleanup.datastore.search.criteria.ComponentCleanupEvaluator;
import org.sonatype.nexus.cleanup.internal.storage.CleanupPolicyData;
import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.extdirect.model.PagedResponse;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.query.QueryOptions;

import com.google.common.collect.ForwardingCollection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultCleanupComponentBrowseTest

{
  @Mock
  private Repository repository;

  @Mock
  private ContentFacet contentFacet;

  @Mock
  private FluentComponents fluentComponents;

  @Mock
  private ComponentCleanupEvaluator componentCleanupEvaluator;

  @Mock
  private AssetCleanupEvaluator assetCleanupEvaluator;

  @Mock
  private FluentComponent fluentComponent;

  @Mock
  private FluentComponent fluentComponent2;

  @Mock
  private FluentAsset fluentAsset;

  @Mock
  private FluentAsset fluentAsset2;

  private DefaultCleanupComponentBrowse underTest;

  private MockedStatic<QualifierUtil> mockedStatic;

  @BeforeEach
  void setUp() {
    mockedStatic = Mockito.mockStatic(QualifierUtil.class);
    List<ComponentCleanupEvaluator> componentEvaluators = List.of(componentCleanupEvaluator);
    List<AssetCleanupEvaluator> assetCleanupEvaluators = List.of(assetCleanupEvaluator);
    Map<String, ComponentCleanupEvaluator> componentCriteria = Map.of("componentKey", componentCleanupEvaluator);
    Map<String, AssetCleanupEvaluator> assetCriteria = Map.of("componentKey", assetCleanupEvaluator);
    when(QualifierUtil.buildQualifierBeanMap(componentEvaluators)).thenReturn(componentCriteria);
    when(QualifierUtil.buildQualifierBeanMap(assetCleanupEvaluators)).thenReturn(assetCriteria);
    underTest = new DefaultCleanupComponentBrowse(componentEvaluators, assetCleanupEvaluators);

    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(contentFacet.components()).thenReturn(fluentComponents);
  }

  @AfterEach
  void tearDown() {
    mockedStatic.close();
  }

  @Test
  void testBrowseWithFilter() {
    setupMocks(false);

    CleanupPolicy policy = createPolicy();

    Stream<FluentComponent> result = underTest.browse(policy, repository);
    validateResult(result);
  }

  @Test
  void testBrowseEagerWithFilterIncludingAssets() {
    setupMocks(true);
    CleanupPolicy policy = createPolicy();

    Stream<FluentComponent> result = underTest.browseIncludingAssets(policy, repository);
    validateResult(result);
  }

  @Test
  void testBrowseByPage() {
    setupMocks(false);
    CleanupPolicy policy = createPolicy();
    QueryOptions queryOptions = new QueryOptions("my-component-1", "name", "asc", 0, 100);

    PagedResponse<Component> result = underTest.browseByPage(policy, repository, queryOptions);

    assertThat(result.getTotal(), is(-1L));
    assertThat(result.getData(), hasSize(1));
    assertThat(result.getData(), contains(fluentComponent));
  }

  private void setupMocks(final boolean useBrowseEager) {
    when(fluentComponent.name()).thenReturn("my-component-1");
    when(fluentComponent2.name()).thenReturn("my-other-component-1");

    if (useBrowseEager) {
      when(fluentComponents.browseEager(anyInt(), any())).thenReturn(
          new TestContinuation<>(List.of(fluentComponent, fluentComponent2), null));
    }
    else {
      when(fluentComponents.browse(anyInt(), any())).thenReturn(
          new TestContinuation<>(List.of(fluentComponent, fluentComponent2), null));
    }

    when(componentCleanupEvaluator.getPredicate(repository, "componentValue"))
        .thenReturn((component, assets) -> component.name().endsWith("-1"));
    when(assetCleanupEvaluator.getPredicate(repository, "componentValue")).thenReturn(Asset::hasBlob);

    when(fluentAsset.hasBlob()).thenReturn(true);
    when(fluentAsset2.hasBlob()).thenReturn(false);

    when(fluentComponent.assets()).thenReturn(List.of(fluentAsset));
    when(fluentComponent2.assets()).thenReturn(List.of(fluentAsset2));
  }

  private void validateResult(final Stream<FluentComponent> result) {
    List<FluentComponent> components = result.toList();
    assertThat(components, hasSize(1));
    assertThat(components, contains(fluentComponent));
  }

  private static CleanupPolicyData createPolicy() {
    CleanupPolicyData policyData = new CleanupPolicyData();
    policyData.setCriteria(Map.of("componentKey", "componentValue"));
    return policyData;
  }

  private static class TestContinuation<E>
      extends ForwardingCollection<E>
      implements Continuation<E>
  {
    private final Collection<E> collection;

    private final String continuationToken;

    public TestContinuation(final Collection<E> collection, final String continuationToken) {
      this.collection = collection;
      this.continuationToken = continuationToken;
    }

    @Override
    protected Collection<E> delegate() {
      return collection;
    }

    @Override
    public String nextContinuationToken() {
      return continuationToken;
    }
  }
}
