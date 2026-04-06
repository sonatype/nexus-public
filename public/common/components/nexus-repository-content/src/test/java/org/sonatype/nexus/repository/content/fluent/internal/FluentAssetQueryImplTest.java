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
package org.sonatype.nexus.repository.content.fluent.internal;

import java.util.Collections;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.store.AssetStore;
import org.sonatype.nexus.repository.types.HostedType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FluentAssetQueryImplTest
    extends TestSupport
{
  @Mock
  private ContentFacetSupport facet;

  @Mock
  private AssetStore<?> assetStore;

  @Mock
  private Repository repository;

  private FluentAssetsImpl assets;

  @Before
  public void setUp() {
    Format format = new Format("raw")
    {
    };
    when(repository.getFormat()).thenReturn(format);
    when(repository.getType()).thenReturn(new HostedType());
    when(facet.repository()).thenReturn(repository);
    when(facet.contentRepositoryId()).thenReturn(1);

    assets = new FluentAssetsImpl(facet, assetStore);
  }

  @Test
  public void testCountByKind() {
    when(assetStore.countAssets(eq(1), eq("DOCKER_LAYER"), isNull(), isNull())).thenReturn(12);
    FluentAssetQueryImpl query = new FluentAssetQueryImpl(assets, "DOCKER_LAYER");
    assertThat(query.count(), is(12));
  }

  @Test
  public void testCountByFilter() {
    when(assetStore.countAssets(eq(1), isNull(), eq("path like :path"), any())).thenReturn(7);
    FluentAssetQueryImpl query = new FluentAssetQueryImpl(
        assets, "path like :path", Collections.singletonMap("path", "/test%"));
    assertThat(query.count(), is(7));
  }

  @Test
  public void testBrowseByKind() {
    Continuation continuation = mock(Continuation.class);
    when(assetStore.browseAssets(any(), anyString(), eq("DOCKER_LAYER"), isNull(), isNull(), anyInt()))
        .thenReturn(continuation);

    FluentAssetQueryImpl query = new FluentAssetQueryImpl(assets, "DOCKER_LAYER");
    Continuation<FluentAsset> result = query.browse(10, "token");
    assertThat(result, is(notNullValue()));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testBrowseEagerThrowsUnsupported() {
    FluentAssetQueryImpl query = new FluentAssetQueryImpl(assets, "kind");
    query.browseEager(10, null);
  }
}
