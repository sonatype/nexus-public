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
package org.sonatype.repository.helm.datastore.internal.createindex;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.repository.helm.internal.AssetKind;
import org.sonatype.repository.helm.datastore.internal.HelmContentFacet;
import org.sonatype.repository.helm.internal.util.YamlParser;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
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
  private YamlParser yamlParser;

  @Mock
  private AssetBlob assetBlob;

  @Mock
  private Iterable<Asset> assets;

  @Mock
  private Iterator<Asset> assetIterator;

  @Mock
  private HelmContentFacet helmFacet;

  @Mock
  private Repository repository;

  @Mock
  private FluentAsset asset;

  @Mock
  private NestedAttributesMap formatAttributes;

  @Before
  public void setUp() {
    when(repository.facet(HelmContentFacet.class)).thenReturn(helmFacet);
    initializeSystemUnderTest();
  }

  @Test
  public void testBuildIndexYaml() {
    List<FluentAsset> list = Arrays.asList(asset);
    when(asset.attributes()).thenReturn(formatAttributes);
    Map<String, String> shaMap = new HashMap<>();
    shaMap.put("sha256", "12345");

    when(assetBlob.checksums()).thenReturn(shaMap);
    when(helmFacet.browseAssetsByKind(AssetKind.HELM_PACKAGE)).thenReturn(list);
    when(yamlParser.getYamlContent(anyObject())).thenReturn("index.yaml");

    Content result = underTest.buildIndexYaml(repository);

    assertThat(result, is(notNullValue()));
  }

  @Test
  public void testIndexYamlBuiltEvenWhenNoAssets() {
    when(assets.iterator()).thenReturn(assetIterator);
    when(assetIterator.next()).thenReturn(asset);
    when(helmFacet.browseAssetsByKind(AssetKind.HELM_PACKAGE)).thenReturn(Collections.emptyList());
    when(yamlParser.getYamlContent(anyObject())).thenReturn("index.yaml");

    Content result = underTest.buildIndexYaml(repository);

    assertThat(result, is(notNullValue()));
  }

  private void initializeSystemUnderTest() {
    underTest = Guice.createInjector(new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(YamlParser.class).toInstance(yamlParser);
      }
    }).getInstance(CreateIndexServiceImpl.class);
  }
}
