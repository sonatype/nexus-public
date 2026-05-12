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
package org.sonatype.nexus.repository.apt.internal.upgrade;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.datastore.mybatis.ContinuationArrayList;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.apt.AptFormat;
import org.sonatype.nexus.repository.apt.datastore.AptContentFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.AptContentFacetImpl;
import org.sonatype.nexus.repository.apt.datastore.internal.hosted.AptHostedFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetStores;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.fluent.FluentContinuation;
import org.sonatype.nexus.repository.content.fluent.FluentQuery;
import org.sonatype.nexus.repository.content.fluent.internal.FluentAssetImpl;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.scheduling.TaskConfiguration;

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepairAptMetadataLeadingSlashTaskTest
{
  private Map<String, Repository> repositories = new HashMap<>();

  @Mock
  RepositoryManager repositoryManager;

  @Mock
  GroupType groupType;

  @Mock
  SecurityManager securityManager;

  private RepairAptMetadataLeadingSlashTask underTest;

  @BeforeEach
  void setup() {
    ThreadContext.bind(securityManager);

    underTest = new RepairAptMetadataLeadingSlashTask(5);
    underTest.install(repositoryManager, groupType);
    TaskConfiguration configuration = new TaskConfiguration();
    configuration.setId(UUID.randomUUID().toString());
    configuration.setTypeId(RepairAptMetadataLeadingSlashTaskDescriptor.TYPE_ID);
    configuration.setString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, RepositoryTaskSupport.ALL_REPOSITORIES);
    underTest.configure(configuration);

  }

  @AfterEach
  void tearDown() {
    ThreadContext.unbindSecurityManager();
  }

  @Test
  void testExecute() throws Exception {
    Repository conan = repository("conan", mock(Format.class), groupType);
    Repository aptGroup = repository("apt-group", new AptFormat(), groupType);
    Repository aptHosted = mockAptHostedRepository("apt-hosted", 5, 10);
    Repository aptHosted2 = mockAptHostedRepository("apt-hosted2", 0, 10);

    when(repositoryManager.browse()).thenReturn(repositories.values());

    underTest.call();

    // Repository containing bad assets
    verify(aptHosted).facet(AptHostedFacet.class);
    AptContentFacetImpl content = (AptContentFacetImpl) aptHosted.facet(AptContentFacet.class);
    verify(content.stores().assetStore, times(5)).updateAssetAttributes(any(), any());

    // Repository containing no bad assets
    verify(aptHosted2, never()).facet(AptHostedFacet.class);
    AptContentFacetImpl content2 = (AptContentFacetImpl) aptHosted2.facet(AptContentFacet.class);
    verify(content2.stores().assetStore, never()).updateAssetAttributes(any(), any());

    // Repositories that shouldn't be touched
    verify(conan).getFormat();
    verify(aptGroup).getFormat();
    verify(aptGroup).getType();
    verifyNoMoreInteractions(conan, aptGroup);
  }

  private Repository mockAptHostedRepository(final String name, final int badAssets, final int goodAssets) {
    Repository repository = repository(name, new AptFormat(), new HostedType());
    AptContentFacetImpl content = mock(AptContentFacetImpl.class);
    lenient().when(content.repository()).thenReturn(repository);
    when(repository.facet(AptContentFacet.class)).thenReturn(content);
    ContentFacetStores stores = new ContentFacetStores(mock(), "a-blobstore", mock(Answers.RETURNS_MOCKS), "apt");
    lenient().when(content.stores()).thenReturn(stores);

    FluentAssets fluentAssets = mock(FluentAssets.class);
    when(content.assets()).thenReturn(fluentAssets);

    FluentQuery<FluentAsset> fq = mock(FluentQuery.class);
    when(fluentAssets.byFilter(any(), any())).thenReturn(fq);

    List<AssetData> assets = new ArrayList<>();
    for (int b = 0; b < badAssets; b++) {
      assets.add(mockAsset("Filename: /foo"));
    }

    for (int g = 0; g < goodAssets; g++) {
      assets.add(mockAsset("Filename: foo"));
    }
    when(fq.browse(anyInt(), any())).thenReturn(of(content, assets.toArray(new AssetData[0])));

    AptHostedFacet hosted = mock(AptHostedFacet.class);
    lenient().when(repository.facet(AptHostedFacet.class)).thenReturn(hosted);

    return repository;
  }

  private Repository repository(final String name, final Format format, final Type type) {
    Repository repository = mock(Repository.class);
    lenient().when(repository.getFormat()).thenReturn(format);
    lenient().when(repository.getType()).thenReturn(type);
    lenient().when(repository.getName()).thenReturn(name);
    repositories.put(name, repository);
    return repository;
  }

  private static AssetData mockAsset(final String indexSection) {
    AssetData asset = mock(AssetData.class);
    NestedAttributesMap map = new NestedAttributesMap();
    map.child(AptFormat.NAME).set(RepairAptMetadataLeadingSlashTask.INDEX_SECTION, indexSection);
    when(asset.attributes()).thenReturn(map);
    return asset;
  }

  private static Continuation<FluentAsset> of(final ContentFacetSupport content, final AssetData... assets) {
    ContinuationArrayList<AssetData> continuation = new ContinuationArrayList<>();
    continuation.addAll(Arrays.asList(assets));
    return new FluentContinuation<FluentAsset, AssetData>(continuation, asset -> new FluentAssetImpl(content, asset));
  }
}
