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
package org.sonatype.nexus.repository.browse.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseResult;
import org.sonatype.nexus.repository.browse.QueryOptions;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.RepositorySelector;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.BucketStore;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.selector.ConstantVariableResolver;
import org.sonatype.nexus.selector.VariableSource;
import org.sonatype.nexus.selector.VariableSourceBuilder;

import com.google.common.base.Supplier;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class BrowseServiceImplTest
    extends TestSupport
{
  private static final String NO_WHERE = null;

  @Mock
  ComponentEntityAdapter componentEntityAdapter;

  @Mock
  Component componentOne;

  @Mock
  ODocument componentOneDoc;

  @Mock
  ORID componentOneORID;

  @Mock
  VariableResolverAdapterManager variableResolverAdapterManager;

  @Mock
  ContentPermissionChecker contentPermissionChecker;

  @Mock
  Repository mavenReleases;

  @Mock
  StorageFacet storageFacet;

  @Mock
  Supplier<StorageTx> txSupplier;

  @Mock
  StorageTx storageTx;

  @Mock
  QueryOptions queryOptions;

  @Mock
  Asset assetOne;

  @Mock
  ORID assetOneORID;

  @Mock
  ODocument assetOneDoc;

  @Mock
  Asset assetTwo;

  @Mock
  AssetEntityAdapter assetEntityAdapter;

  @Mock
  BucketStore bucketStore;

  @Mock
  RepositorySelector repositorySelector;

  @Mock
  GroupFacet groupFacet;

  @Mock
  RepositoryManager repositoryManager;

  BrowseAssetsSqlBuilder browseAssetsSqlBuilder;

  BrowseComponentsSqlBuilder browseComponentsSqlBuilder;

  List<Asset> results;

  private BrowseServiceImpl underTest;

  @Before
  public void setup() {
    results = asList(assetOne, assetTwo);

    when(queryOptions.getContentAuth()).thenReturn(true);

    when(assetOneORID.toString()).thenReturn("assetOne");
    when(componentOneORID.toString()).thenReturn("componentOne");

    when(mavenReleases.facet(StorageFacet.class)).thenReturn(storageFacet);
    when(mavenReleases.getName()).thenReturn("releases");

    when(storageFacet.txSupplier()).thenReturn(txSupplier);
    when(txSupplier.get()).thenReturn(storageTx);

    browseAssetsSqlBuilder = new BrowseAssetsSqlBuilder(assetEntityAdapter);
    browseComponentsSqlBuilder = new BrowseComponentsSqlBuilder(componentEntityAdapter);

    underTest = spy(new BrowseServiceImpl(new GroupType(), componentEntityAdapter, variableResolverAdapterManager,
        contentPermissionChecker, assetEntityAdapter, browseAssetsSqlBuilder,
        browseComponentsSqlBuilder, bucketStore, repositoryManager));
  }

  @Test
  public void testGetRepoToContainedGroupMap() {
    Repository repo1 = mock(Repository.class);
    when(repo1.getName()).thenReturn("repo1");
    when(repo1.optionalFacet(GroupFacet.class)).thenReturn(Optional.empty());
    Repository repo2 = mock(Repository.class);
    when(repo2.getName()).thenReturn("repo2");
    when(repo2.optionalFacet(GroupFacet.class)).thenReturn(Optional.empty());
    Repository group = mock(Repository.class);
    when(group.getName()).thenReturn("group");
    GroupFacet groupFacet = mock(GroupFacet.class);
    when(groupFacet.leafMembers()).thenReturn(Arrays.asList(repo1));
    when(group.optionalFacet(GroupFacet.class)).thenReturn(Optional.of(groupFacet));

    List<Repository> repositories = Arrays.asList(repo1, repo2, group);

    Map<String, List<String>> repoToContainedGroups = underTest.getRepoToContainedGroupMap(repositories);
    assertThat(repoToContainedGroups.size(), is(3));
    assertThat(repoToContainedGroups.get("repo1").size(), is(2));
    assertThat(repoToContainedGroups.get("repo1").get(0), is("repo1"));
    assertThat(repoToContainedGroups.get("repo1").get(1), is("group"));
    assertThat(repoToContainedGroups.get("repo2").size(), is(1));
    assertThat(repoToContainedGroups.get("repo2").get(0), is("repo2"));
    assertThat(repoToContainedGroups.get("group").size(), is(1));
    assertThat(repoToContainedGroups.get("group").get(0), is("group"));
  }

  @Captor
  ArgumentCaptor<Map<String, Object>> sqlParamsCaptor;

  @Test
  public void testBrowseAssets() {
    List<Repository> expectedRepositories = asList(mavenReleases);
    String expectedQuery1 = "SELECT FROM INDEXVALUES:asset_name_ci_idx WHERE (bucket = b4)  SKIP 0 LIMIT 1";
    String expectedQuery2 = "SELECT FROM INDEXVALUES:asset_name_ci_idx WHERE (bucket = b4) AND contentAuth(@this, :browsedRepository) == true  SKIP 0 LIMIT 0";
    List<String> bucketIds = Collections.singletonList("b4");
    Iterable<ODocument> resultDocs = asList(mock(ODocument.class), mock(ODocument.class));

    doReturn(bucketIds).when(underTest).getBucketIds(any(), eq(expectedRepositories));
    doReturn(results).when(underTest).getAssets(resultDocs);
    when(storageTx.browse(eq(expectedQuery1), any())).thenReturn(resultDocs);
    when(storageTx.browse(eq(expectedQuery2), sqlParamsCaptor.capture())).thenReturn(resultDocs);

    BrowseResult<Asset> browseResult = underTest.browseAssets(mavenReleases, queryOptions);
    assertThat(browseResult.getTotal(), is(2L));
    assertThat(browseResult.getResults(), is(results));

    sqlParamsCaptor.getAllValues().stream().forEach(params -> {
      String repoNames = params.get("browsedRepository").toString();
      List<String> splitNames = Arrays.asList(repoNames.split(","));
      assertThat(splitNames, contains("releases"));
    });
  }

  @Test
  public void testPreviewAssets() {
    Repository proxyRepository = mock(Repository.class);
    when(proxyRepository.getType()).thenReturn(new ProxyType());
    when(proxyRepository.getName()).thenReturn("proxy-repository");
    when(proxyRepository.facet(StorageFacet.class)).thenReturn(storageFacet);
    when(proxyRepository.optionalFacet(GroupFacet.class)).thenReturn(Optional.empty());

    Repository hostedRepository = mock(Repository.class);
    when(hostedRepository.getType()).thenReturn(new HostedType());
    when(hostedRepository.getName()).thenReturn("hosted-repository");
    when(hostedRepository.optionalFacet(GroupFacet.class)).thenReturn(Optional.empty());

    Repository groupRepository = mock(Repository.class);
    when(groupRepository.getType()).thenReturn(new GroupType());
    when(groupRepository.getName()).thenReturn("group-repository");
    when(groupRepository.optionalFacet(GroupFacet.class)).thenReturn(Optional.of(groupFacet));

    List<Repository> groupRepositoryLeafMembers = asList(hostedRepository);
    when(groupFacet.leafMembers()).thenReturn(groupRepositoryLeafMembers);

    List<Repository> repositories = asList(proxyRepository, hostedRepository, groupRepository);

    Map<String, List<String>> expectedRepoToContainedGroupMap = new HashMap<>();
    expectedRepoToContainedGroupMap.put("proxy-repository", asList("proxy-repository"));
    expectedRepoToContainedGroupMap.put("hosted-repository", asList("hosted-repository", "group-repository"));
    expectedRepoToContainedGroupMap.put("group-repository", asList("group-repository"));

    Map<String, Object> expectedQueryParams = new HashMap<>();
    expectedQueryParams.put("repositorySelector", "toSelectorResult");
    expectedQueryParams.put("jexlExpression", "myExpression");
    expectedQueryParams.put("repoToContainedGroupMap", expectedRepoToContainedGroupMap);

    String expectedCountQuerySuffix =
        "and (contentExpression(@this, :jexlExpression, :repositorySelector, :repoToContainedGroupMap) == true)";
    String expectedFindQuerySuffix =
        "and (contentExpression(@this, :jexlExpression, :repositorySelector, :repoToContainedGroupMap) == true) " +
        "SKIP 0 LIMIT 0";

    when(repositorySelector.toSelector()).thenReturn("toSelectorResult");
    when(storageTx.countAssets(NO_WHERE, expectedQueryParams, repositories, expectedCountQuerySuffix)).thenReturn(2L);
    when(storageTx.findAssets(NO_WHERE, expectedQueryParams, repositories, expectedFindQuerySuffix)).thenReturn(results);

    BrowseResult<Asset> browseResult =
        underTest.previewAssets(repositorySelector, repositories, "myExpression", queryOptions);
    assertThat(browseResult.getTotal(), is(2L));
    assertThat(browseResult.getResults(), is(results));
  }

  @Test
  public void testPreviewAssetsWithSingleGroupRepository() {
    Repository groupRepositoryLeafMember = mock(Repository.class);
    when(groupRepositoryLeafMember.getType()).thenReturn(new HostedType());
    when(groupRepositoryLeafMember.getName()).thenReturn("group-repository-leaf-member");
    when(groupRepositoryLeafMember.optionalFacet(GroupFacet.class)).thenReturn(Optional.empty());

    Repository groupRepository = mock(Repository.class);
    when(groupRepository.getType()).thenReturn(new GroupType());
    when(groupRepository.getName()).thenReturn("group-repository");
    when(groupRepository.facet(StorageFacet.class)).thenReturn(storageFacet);
    when(groupRepository.facet(GroupFacet.class)).thenReturn(groupFacet);
    when(groupRepository.optionalFacet(GroupFacet.class)).thenReturn(Optional.of(groupFacet));

    List<Repository> groupRepositoryLeafMembers = asList(groupRepositoryLeafMember);
    when(groupFacet.leafMembers()).thenReturn(groupRepositoryLeafMembers);

    List<Repository> repositories = asList(groupRepository);

    Map<String, List<String>> expectedRepoToContainedGroupMap = new HashMap<>();
    expectedRepoToContainedGroupMap.put("group-repository", asList("group-repository"));

    Map<String, Object> expectedQueryParams = new HashMap<>();
    expectedQueryParams.put("repositorySelector", "toSelectorResult");
    expectedQueryParams.put("jexlExpression", "myExpression");
    expectedQueryParams.put("repoToContainedGroupMap", expectedRepoToContainedGroupMap);

    String expectedCountQuerySuffix =
        "and (contentExpression(@this, :jexlExpression, :repositorySelector, :repoToContainedGroupMap) == true)";
    String expectedFindQuerySuffix =
        "and (contentExpression(@this, :jexlExpression, :repositorySelector, :repoToContainedGroupMap) == true) " +
            "SKIP 0 LIMIT 0";

    when(repositorySelector.toSelector()).thenReturn("toSelectorResult");
    when(storageTx.countAssets(NO_WHERE, expectedQueryParams, groupRepositoryLeafMembers, expectedCountQuerySuffix))
        .thenReturn(57L);
    when(storageTx.findAssets(NO_WHERE, expectedQueryParams, groupRepositoryLeafMembers, expectedFindQuerySuffix))
        .thenReturn(results);

    BrowseResult<Asset> browseResult =
        underTest.previewAssets(repositorySelector, repositories, "myExpression", queryOptions);
    assertThat(browseResult.getTotal(), is(57L));
    assertThat(browseResult.getResults(), is(results));
  }

  @Captor
  ArgumentCaptor<Map<String, Object>> paramsCaptor;

  @Test
  public void testGetAssetById() {
    String expectedSql = "SELECT * FROM asset WHERE contentAuth(@this, :browsedRepository) == true AND @RID == :rid";

    when(storageTx.browse(eq(expectedSql), paramsCaptor.capture())).thenReturn(Collections.singletonList(assetOneDoc));

    when(assetEntityAdapter.readEntity(assetOneDoc)).thenReturn(assetOne);

    assertThat(underTest.getAssetById(assetOneORID, mavenReleases), is(assetOne));

    Map<String, Object> params = paramsCaptor.getValue();

    assertThat(params.get("browsedRepository"), is("releases"));
    assertThat(params.get("rid"), is("assetOne"));
  }

  @Test
  public void testGetAssetById_withEntityId() {
    EntityId assetId = new DetachedEntityId(assetOneORID.toString());
    EntityId bucketId = mock(EntityId.class);
    Bucket bucket = mock(Bucket.class);

    when(storageTx.findAsset(assetId)).thenReturn(assetOne);
    when(assetOne.bucketId()).thenReturn(bucketId);
    when(bucketStore.getById(bucketId)).thenReturn(bucket);
    when(bucket.getRepositoryName()).thenReturn("releases");

    when(assetEntityAdapter.readEntity(assetOneDoc)).thenReturn(assetOne);

    assertThat(underTest.getAssetById(assetId, mavenReleases), is(assetOne));
  }

  @Test
  public void testGetAssetById_withEntityId_groupRepository() {
    EntityId assetId = new DetachedEntityId(assetOneORID.toString());
    EntityId bucketId = mock(EntityId.class);
    Bucket bucket = mock(Bucket.class);

    when(storageTx.findAsset(assetId)).thenReturn(assetOne);
    when(assetOne.bucketId()).thenReturn(bucketId);
    when(bucketStore.getById(bucketId)).thenReturn(bucket);
    when(bucket.getRepositoryName()).thenReturn("releases");

    Repository groupRepository = mock(Repository.class);
    when(groupRepository.getType()).thenReturn(new GroupType());
    when(groupRepository.getName()).thenReturn("group-repository");
    when(groupRepository.facet(StorageFacet.class)).thenReturn(storageFacet);
    GroupFacet groupFacet = mock(GroupFacet.class);
    when(groupFacet.allMembers()).thenReturn(Arrays.asList(groupRepository, mavenReleases));
    when(groupRepository.facet(GroupFacet.class)).thenReturn(groupFacet);

    when(assetEntityAdapter.readEntity(assetOneDoc)).thenReturn(assetOne);

    assertThat(underTest.getAssetById(assetId, groupRepository), is(assetOne));
    verify(groupFacet).allMembers();
  }

  @Test
  public void testGetAssetById_withEntityId_wrongRepository() {
    EntityId assetId = new DetachedEntityId(assetOneORID.toString());
    EntityId bucketId = mock(EntityId.class);
    Bucket bucket = mock(Bucket.class);

    when(storageTx.findAsset(assetId)).thenReturn(assetOne);
    when(assetOne.bucketId()).thenReturn(bucketId);
    when(bucketStore.getById(bucketId)).thenReturn(bucket);
    when(bucket.getRepositoryName()).thenReturn("some-other-repository");

    when(assetEntityAdapter.readEntity(assetOneDoc)).thenReturn(assetOne);

    assertNull(underTest.getAssetById(assetId, mavenReleases));
  }

  @Test
  public void testGetAssetById_NoResultsIsNull() {
    String expectedSql = "SELECT * FROM asset WHERE contentAuth(@this, :browsedRepository) == true AND @RID == :rid";

    when(storageTx.browse(eq(expectedSql), paramsCaptor.capture())).thenReturn(Collections.emptyList());

    assertThat(underTest.getAssetById(assetOneORID, mavenReleases), nullValue());

    Map<String, Object> params = paramsCaptor.getValue();

    assertThat(params.get("browsedRepository"), is("releases"));
    assertThat(params.get("rid"), is("assetOne"));
  }

  @Test
  public void testGetComponentById() {
    String expectedSql = "SELECT * FROM component WHERE contentAuth(@this, :browsedRepository) == true AND @RID == :rid";

    when(storageTx.browse(eq(expectedSql), paramsCaptor.capture()))
        .thenReturn(Collections.singletonList(componentOneDoc));

    when(componentEntityAdapter.readEntity(componentOneDoc)).thenReturn(componentOne);

    assertThat(underTest.getComponentById(componentOneORID, mavenReleases), is(componentOne));

    Map<String, Object> params = paramsCaptor.getValue();

    assertThat(params.get("browsedRepository"), is("releases"));
    assertThat(params.get("rid"), is("componentOne"));
  }

  @Test
  public void testGetComponentById_NoResultsIsNull() {
    String expectedSql = "SELECT * FROM component WHERE contentAuth(@this, :browsedRepository) == true AND @RID == :rid";

    when(storageTx.browse(eq(expectedSql), paramsCaptor.capture())).thenReturn(Collections.emptyList());

    assertThat(underTest.getComponentById(componentOneORID, mavenReleases), nullValue());

    Map<String, Object> params = paramsCaptor.getValue();

    assertThat(params.get("browsedRepository"), is("releases"));
    assertThat(params.get("rid"), is("componentOne"));
  }

  @Test
  public void testBrowseComponentAssets_all_authorized() {
    setupMocksForBrowserComponentAssets(true, true);
    BrowseResult<Asset> results = underTest.browseComponentAssets(mavenReleases, "componentOne");

    assertThat(results.getTotal(), is(2l));
    assertThat(results.getResults().get(0).name(), is(assetOne.name()));
    assertThat(results.getResults().get(1).name(), is(assetTwo.name()));
  }

  @Test
  public void testBrowseComponentAssets_not_all_authorized() {
    setupMocksForBrowserComponentAssets(false, true);
    BrowseResult<Asset> results = underTest.browseComponentAssets(mavenReleases, "componentOne");

    assertThat(results.getTotal(), is(1l));
    assertThat(results.getResults().get(0).name(), is(assetTwo.name()));
  }

  @Test
  public void testBrowseComponentAssets_none_authorized() {
    setupMocksForBrowserComponentAssets(false, false);
    BrowseResult<Asset> results = underTest.browseComponentAssets(mavenReleases, "componentOne");

    assertThat(results.getTotal(), is(0l));
  }

  private void setupMocksForBrowserComponentAssets(boolean allowAssetOne, boolean allowAssetTwo) {
    Repository groupRepository = mock(Repository.class);
    when(groupRepository.getType()).thenReturn(new GroupType());
    when(groupRepository.getName()).thenReturn("group-repository");
    when(groupRepository.facet(StorageFacet.class)).thenReturn(storageFacet);
    GroupFacet groupFacet = mock(GroupFacet.class);
    when(groupFacet.allMembers()).thenReturn(Arrays.asList(groupRepository, mavenReleases));
    when(groupRepository.facet(GroupFacet.class)).thenReturn(groupFacet);
    when(storageTx.findComponent(new DetachedEntityId(componentOneORID.toString()))).thenReturn(componentOne);
    when(repositoryManager.findContainingGroups(mavenReleases.getName())).thenReturn(Collections.singletonList("group-repository"));
    VariableResolverAdapter variableResolverAdapter = mock(VariableResolverAdapter.class);
    when(variableResolverAdapterManager.get(componentOne.format())).thenReturn(variableResolverAdapter);
    VariableSource variableSourceOne = createVariableSource(assetOne);
    VariableSource variableSourceTwo = createVariableSource(assetTwo);
    when(variableResolverAdapter.fromAsset(assetOne)).thenReturn(variableSourceOne);
    when(variableResolverAdapter.fromAsset(assetTwo)).thenReturn(variableSourceTwo);
    when(storageTx.browseAssets(componentOne)).thenReturn(Arrays.asList(assetOne, assetTwo));
    when(contentPermissionChecker
        .isPermitted(eq(Stream.of(mavenReleases.getName(), "group-repository").collect(Collectors.toSet())), any(),
            any(), eq(variableSourceOne))).thenReturn(allowAssetOne);
    when(contentPermissionChecker
        .isPermitted(eq(Stream.of(mavenReleases.getName(), "group-repository").collect(Collectors.toSet())), any(),
            any(), eq(variableSourceTwo))).thenReturn(allowAssetTwo);
  }

  private VariableSource createVariableSource(Asset asset) {
    return new VariableSourceBuilder().addResolver(new ConstantVariableResolver('/' + asset.name(), "path"))
        .addResolver(new ConstantVariableResolver(asset.format(), "format")).build();
  }
}
