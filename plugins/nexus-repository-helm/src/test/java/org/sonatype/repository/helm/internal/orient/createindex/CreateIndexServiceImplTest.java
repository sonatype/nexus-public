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
package org.sonatype.repository.helm.internal.orient.createindex;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.transaction.TransactionModule;
import org.sonatype.nexus.transaction.UnitOfWork;
import org.sonatype.repository.helm.internal.AssetKind;
import org.sonatype.repository.helm.internal.orient.HelmFacet;
import org.sonatype.repository.helm.internal.orient.metadata.IndexYamlBuilder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.powermock.api.mockito.PowerMockito.when;

public class CreateIndexServiceImplTest
    extends TestSupport
{
  private CreateIndexServiceImpl underTest;

  @Mock
  private StorageFacet storageFacet;

  @Mock
  private StorageTx storageTx;

  @Mock
  private IndexYamlBuilder indexYamlBuilder;

  @Mock
  private HelmFacet helmFacet;

  @Mock
  private Repository repository;

  @Mock
  private Bucket bucket;

  @Mock
  private Asset asset;

  @Mock
  private NestedAttributesMap formatAttributes;

  @Mock
  private NestedAttributesMap assetAttributes;

  @Mock
  TempBlob tempBlob;

  @Mock
  EntityId entityId;

  @Mock
  Iterable<Asset> assets;

  @Mock
  Iterator<Asset> assetIterator;

  @Before
  public void setUp() throws Exception {
    initializeSystemUnderTest();
    setupMocks();
    UnitOfWork.begin(() -> storageTx);
  }

  @After
  public void tearDown() throws Exception {
    UnitOfWork.end();
  }

  @Test
  public void testBuildIndexYaml() throws Exception {
    List<Asset> list = Arrays.asList(asset);
    when(asset.formatAttributes()).thenReturn(formatAttributes);
    when(asset.attributes()).thenReturn(assetAttributes);
    when(asset.componentId()).thenReturn(entityId);
    Map<String, String> shaMap = new HashMap<>();
    shaMap.put("sha256", "12345");

    when(assetAttributes.get("checksum", Map.class)).thenReturn(shaMap);
    when(helmFacet.browseComponentAssets(storageTx, AssetKind.HELM_PACKAGE)).thenReturn(list);
    when(indexYamlBuilder.build(anyObject(), anyObject())).thenReturn(tempBlob);

    TempBlob result = underTest.buildIndexYaml(repository);

    assertThat(result, is(notNullValue()));
  }

  @Test
  public void testIndexYamlBuiltEvenWhenNoAssets() throws Exception {
    when(assets.iterator()).thenReturn(assetIterator);
    when(assetIterator.next()).thenReturn(asset);
    when(asset.componentId()).thenReturn(null);
    when(helmFacet.browseComponentAssets(storageTx, AssetKind.HELM_PACKAGE)).thenReturn(assets);
    when(indexYamlBuilder.build(anyObject(), anyObject())).thenReturn(tempBlob);

    TempBlob result = underTest.buildIndexYaml(repository);

    assertThat(result, is(notNullValue()));
  }

  private void initializeSystemUnderTest() {
    underTest = Guice.createInjector(new TransactionModule(), new AbstractModule() {
      @Override
      protected void configure() {
        bind(IndexYamlBuilder.class).toInstance(indexYamlBuilder);
      }
    }).getInstance(CreateIndexServiceImpl.class);
  }

  private void setupMocks() {
    when(storageTx.findBucket(repository)).thenReturn(bucket);
    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
    when(repository.facet(HelmFacet.class)).thenReturn(helmFacet);
  }
}
