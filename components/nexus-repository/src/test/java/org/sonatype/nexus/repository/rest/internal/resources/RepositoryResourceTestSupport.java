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
package org.sonatype.nexus.repository.rest.internal.resources;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseService;
import org.sonatype.nexus.repository.rest.SearchMapping;
import org.sonatype.nexus.repository.rest.SearchMappings;
import org.sonatype.nexus.repository.rest.SearchUtils;
import org.sonatype.nexus.repository.search.DefaultSearchContribution;
import org.sonatype.nexus.repository.search.KeywordSearchContribution;
import org.sonatype.nexus.repository.search.SearchContribution;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;

import com.google.common.base.Supplier;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;

public abstract class RepositoryResourceTestSupport
    extends TestSupport
{
  @Mock
  RepositoryManagerRESTAdapter repositoryManagerRESTAdapter;

  @Mock
  BrowseService browseService;

  @Mock
  Format format;

  @Mock
  Repository mavenReleases;

  @Mock
  StorageFacet storageFacet;

  @Mock
  StorageTx storageTx;

  Supplier<StorageTx> storageTxSupplier;

  String mavenReleasesId = "maven-releases";

  Map<String, SearchMappings> searchMappings = ImmutableMap.of(
      "default", () -> ImmutableList.of(
          new SearchMapping("sha1", "assets.attributes.checksum.sha1", ""),
          new SearchMapping("sha256", "assets.attributes.checksum.sha256", ""),
          new SearchMapping("maven.extension", "assets.attributes.maven2.extension", ""),
          new SearchMapping("maven.classifier", "assets.attributes.maven2.classifier", ""),
          new SearchMapping("mvn.extension", "assets.attributes.maven2.extension", ""),
          new SearchMapping("q", "keyword", "")
      )
  );

  SearchUtils searchUtils;

  AssetMapUtils assetMapUtils;

  @Before
  public void init() {
    configureMockedRepository(mavenReleases, mavenReleasesId, "http://localhost:8081/repository/maven-releases");

    when(format.toString()).thenReturn("maven2");
    when(format.getValue()).thenReturn("maven2");

    storageTxSupplier = () -> storageTx;
    when(storageFacet.txSupplier()).thenReturn(storageTxSupplier);

    Map<String, SearchContribution> searchContributions = new HashMap<>();
    searchContributions.put(DefaultSearchContribution.NAME, new DefaultSearchContribution());
    searchContributions.put(KeywordSearchContribution.NAME, new KeywordSearchContribution());
    searchUtils = new SearchUtils(repositoryManagerRESTAdapter, searchMappings, searchContributions);

    assetMapUtils = new AssetMapUtils(searchUtils);
  }

  protected void configureMockedRepository(Repository repository,
                                           String name,
                                           String url)
  {
    when(repositoryManagerRESTAdapter.getRepository(name)).thenReturn(repository);
    when(repository.getUrl()).thenReturn(url);
    when(repository.getName()).thenReturn(name);
    when(repository.getFormat()).thenReturn(format);
    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
  }

  Asset getMockedAsset(String name, String idValue) {
    Map<String,Object> checksum = Maps.newHashMap(ImmutableMap.of(HashAlgorithm.SHA1.name(), "87acec17cd9dcd20a716cc2cf67417b71c8a7016"));

    AssetMocks assetMocks = new AssetMocks();
    MockitoAnnotations.initMocks(assetMocks);

    when(assetMocks.asset.name()).thenReturn(name);
    when(assetMocks.asset.getEntityMetadata()).thenReturn(assetMocks.assetEntityMetadata);
    when(assetMocks.asset.attributes()).thenReturn(new NestedAttributesMap("attributes", ImmutableMap.of(Asset.CHECKSUM, checksum)));

    when(assetMocks.assetEntityMetadata.getId()).thenReturn(assetMocks.assetEntityId);
    when(assetMocks.assetEntityId.getValue()).thenReturn(idValue);

    return assetMocks.asset;
  }

  class AssetMocks
  {
    @Mock
    Asset asset;
    
    @Mock
    EntityMetadata assetEntityMetadata;

    @Mock
    EntityId assetEntityId;
  }
}
