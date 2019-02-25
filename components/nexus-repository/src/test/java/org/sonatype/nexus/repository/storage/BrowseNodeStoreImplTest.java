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
package org.sonatype.nexus.repository.storage;

import java.util.Arrays;
import java.util.List;
import java.util.stream.StreamSupport;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.VersionComparator;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.security.RepositoryViewPermission;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.selector.CselSelector;
import org.sonatype.nexus.selector.JexlSelector;
import org.sonatype.nexus.selector.Selector;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorFactory;
import org.sonatype.nexus.selector.SelectorManager;
import org.sonatype.nexus.selector.SelectorSqlBuilder;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.goodies.common.Time.seconds;

public class BrowseNodeStoreImplTest
    extends TestSupport
{
  private static final String REPOSITORY_NAME = "test-repo";

  private static final String MEMBER_A = "test-repo-a";

  private static final String MEMBER_B = "test-repo-b";

  private static final String MEMBER_C = "test-repo-c";

  private static final String FORMAT_NAME = "test-format";

  private static final int MAX_NODES = 100;

  private static final int DELETE_PAGE_SIZE = 80;

  @Mock
  private DatabaseInstance databaseInstance;

  @Mock
  private ODatabaseDocumentTx db;

  @Mock
  private BrowseNodeEntityAdapter browseNodeEntityAdapter;

  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private SelectorManager selectorManager;

  @Mock
  private SelectorConfiguration byGroup;

  @Mock
  private SelectorConfiguration byVersion;

  @Mock
  private SelectorConfiguration jexl;

  @Mock
  private Repository repository;

  @Mock
  private Repository memberA;

  @Mock
  private Repository memberB;

  @Mock
  private Repository memberC;

  @Mock
  private GroupFacet groupFacet;

  @Mock
  private Format format;

  @Mock
  private Component component;

  @Mock
  private EntityId componentId;

  @Mock
  private Asset asset;

  @Mock
  private EntityId assetId;

  @Mock
  private BrowseNodeFilter browseNodeFilter;

  private BrowseNodeStoreImpl underTest;

  @Before
  public void setUp() throws Exception {
    when(databaseInstance.acquire()).thenReturn(db);
    when(databaseInstance.connect()).thenReturn(db);

    when(format.getValue()).thenReturn(FORMAT_NAME);
    when(repository.getName()).thenReturn(REPOSITORY_NAME);
    when(repository.getFormat()).thenReturn(format);
    when(memberA.getName()).thenReturn(MEMBER_A);
    when(memberA.getFormat()).thenReturn(format);
    when(memberB.getName()).thenReturn(MEMBER_B);
    when(memberB.getFormat()).thenReturn(format);
    when(memberC.getName()).thenReturn(MEMBER_C);
    when(memberC.getFormat()).thenReturn(format);

    when(byGroup.getType()).thenReturn(CselSelector.TYPE);
    when(byGroup.getAttributes()).thenReturn(ImmutableMap.of("expression", "coordinate.groupId == \"org.sonatype\""));
    when(byVersion.getType()).thenReturn(CselSelector.TYPE);
    when(byVersion.getAttributes()).thenReturn(ImmutableMap.of("expression", "coordinate.version == \"2.1\""));
    when(jexl.getType()).thenReturn(JexlSelector.TYPE);

    ConstraintViolationFactory violationFactory = mock(ConstraintViolationFactory.class);
    SelectorFactory selectorFactory = new SelectorFactory(violationFactory);

    doAnswer(invocation -> {
      SelectorConfiguration config = (SelectorConfiguration) invocation.getArguments()[0];
      String type = config.getType();
      String expression = config.getAttributes().get("expression");
      Selector selector = selectorFactory.createSelector(type, expression);
      selector.toSql((SelectorSqlBuilder) invocation.getArguments()[1]);
      return null;
    }).when(selectorManager).toSql(any(), any());

    when(browseNodeFilter.test(any(), any())).thenReturn(true);

    underTest = new BrowseNodeStoreImpl(
        () -> databaseInstance,
        browseNodeEntityAdapter,
        securityHelper,
        selectorManager,
        new BrowseNodeConfiguration(true, 1000, DELETE_PAGE_SIZE, 10_000, 10_000, seconds(0)),
        ImmutableMap.of(FORMAT_NAME, browseNodeFilter),
        ImmutableMap.of(DefaultBrowseNodeComparator.NAME, new DefaultBrowseNodeComparator(new VersionComparator())));

    underTest.start();

    verify(browseNodeEntityAdapter).register(db);
  }

  @After
  public void tearDown() throws Exception {
    underTest.stop();
  }

  @Test
  public void storeOperations() throws Exception {
    List<String> componentPath = asList("org", "foo", "1.0");
    List<String> assetPath = asList("org", "foo", "1.0", "foo-1.0.jar");

    underTest.createComponentNode(REPOSITORY_NAME, componentPath, component);
    underTest.createAssetNode(REPOSITORY_NAME, assetPath, asset);
    underTest.deleteAssetNode(assetId);
    underTest.deleteComponentNode(componentId);

    verify(browseNodeEntityAdapter).createComponentNode(db, REPOSITORY_NAME, componentPath, component);
    verify(browseNodeEntityAdapter).createAssetNode(db, REPOSITORY_NAME, assetPath, asset);
    verify(browseNodeEntityAdapter).deleteAssetNode(db, assetId);
    verify(browseNodeEntityAdapter).deleteComponentNode(db, componentId);

    // check truncation stops when results drop the below the expected page size
    when(browseNodeEntityAdapter.deleteByRepository(db, REPOSITORY_NAME, DELETE_PAGE_SIZE)).thenReturn(
        DELETE_PAGE_SIZE, DELETE_PAGE_SIZE, DELETE_PAGE_SIZE - 1);

    underTest.deleteByRepository(REPOSITORY_NAME);

    verify(browseNodeEntityAdapter, times(3)).deleteByRepository(db, REPOSITORY_NAME, DELETE_PAGE_SIZE);

    verifyNoMoreInteractions(browseNodeEntityAdapter, securityHelper, selectorManager);
  }

  @Test
  public void simpleQueryWithBrowsePermission() throws Exception {
    List<String> queryPath = asList("org", "foo");

    when(securityHelper.anyPermitted(any())).thenReturn(true);

    underTest.getByPath(repository, queryPath, MAX_NODES, null);

    verify(securityHelper).anyPermitted(any(RepositoryViewPermission.class));
    verify(browseNodeEntityAdapter).getByPath(db, REPOSITORY_NAME, queryPath, MAX_NODES, "", emptyMap());
    verifyNoMoreInteractions(browseNodeEntityAdapter, securityHelper, selectorManager);
  }

  @Test
  public void keywordQueryWithBrowsePermission() throws Exception {
    List<String> queryPath = asList("org", "foo");

    when(securityHelper.anyPermitted(any())).thenReturn(true);

    underTest.getByPath(repository, queryPath, MAX_NODES, "wibble");

    verify(securityHelper).anyPermitted(any(RepositoryViewPermission.class));
    verify(browseNodeEntityAdapter).getByPath(db, REPOSITORY_NAME, queryPath, MAX_NODES,
        "asset_name_lowercase like :keyword_filter", ImmutableMap.of("keyword_filter", "%wibble%"));
    verifyNoMoreInteractions(browseNodeEntityAdapter, securityHelper, selectorManager);
  }

  @Test
  public void simpleQueryWithoutBrowsePermissionOrSelectors() throws Exception {
    List<String> queryPath = asList("org", "foo");

    when(securityHelper.anyPermitted(any())).thenReturn(false);
    when(selectorManager.browseActive(asList(REPOSITORY_NAME), asList(FORMAT_NAME))).thenReturn(emptyList());

    underTest.getByPath(repository, queryPath, MAX_NODES, null);

    verify(securityHelper).anyPermitted(any(RepositoryViewPermission.class));
    verify(selectorManager).browseActive(asList(REPOSITORY_NAME), asList(FORMAT_NAME));
    verifyNoMoreInteractions(browseNodeEntityAdapter, securityHelper, selectorManager);
  }

  @Test
  public void keywordQueryWithoutBrowsePermissionOrSelectors() throws Exception {
    List<String> queryPath = asList("org", "foo");

    when(securityHelper.anyPermitted(any())).thenReturn(false);
    when(selectorManager.browseActive(asList(REPOSITORY_NAME), asList(FORMAT_NAME))).thenReturn(emptyList());

    underTest.getByPath(repository, queryPath, MAX_NODES, "wibble");

    verify(securityHelper).anyPermitted(any(RepositoryViewPermission.class));
    verify(selectorManager).browseActive(asList(REPOSITORY_NAME), asList(FORMAT_NAME));
    verifyNoMoreInteractions(browseNodeEntityAdapter, securityHelper, selectorManager);
  }

  @Test
  public void simpleQueryWithContentSelector() throws Exception {
    List<String> queryPath = asList("org", "foo");

    when(securityHelper.anyPermitted(any())).thenReturn(false);
    when(selectorManager.browseActive(asList(REPOSITORY_NAME), asList(FORMAT_NAME))).thenReturn(asList(byGroup));

    underTest.getByPath(repository, queryPath, MAX_NODES, null);

    verify(securityHelper).anyPermitted(any(RepositoryViewPermission.class));
    verify(selectorManager).browseActive(asList(REPOSITORY_NAME), asList(FORMAT_NAME));
    verify(selectorManager).toSql(eq(byGroup), any());
    verify(browseNodeEntityAdapter).getByPath(db, REPOSITORY_NAME, queryPath, MAX_NODES,
        "(asset_id.attributes.test-format.groupId = :s0p0)", ImmutableMap.of("s0p0", "org.sonatype"));
    verifyNoMoreInteractions(browseNodeEntityAdapter, securityHelper, selectorManager);
  }

  @Test
  public void keywordQueryWithContentSelector() throws Exception {
    List<String> queryPath = asList("org", "foo");

    when(securityHelper.anyPermitted(any())).thenReturn(false);
    when(selectorManager.browseActive(asList(REPOSITORY_NAME), asList(FORMAT_NAME))).thenReturn(asList(byGroup));

    underTest.getByPath(repository, queryPath, MAX_NODES, "wibble");

    verify(securityHelper).anyPermitted(any(RepositoryViewPermission.class));
    verify(selectorManager).browseActive(asList(REPOSITORY_NAME), asList(FORMAT_NAME));
    verify(selectorManager).toSql(eq(byGroup), any());
    verify(browseNodeEntityAdapter).getByPath(db, REPOSITORY_NAME, queryPath, MAX_NODES,
        "asset_name_lowercase like :keyword_filter and (asset_id.attributes.test-format.groupId = :s0p0)",
        ImmutableMap.of("keyword_filter", "%wibble%", "s0p0", "org.sonatype"));
    verifyNoMoreInteractions(browseNodeEntityAdapter, securityHelper, selectorManager);
  }

  @Test
  public void simpleQueryWithContentSelectors() throws Exception {
    List<String> queryPath = asList("org", "foo");

    when(securityHelper.anyPermitted(any())).thenReturn(false);
    when(selectorManager.browseActive(asList(REPOSITORY_NAME), asList(FORMAT_NAME)))
        .thenReturn(asList(byGroup, byVersion));

    underTest.getByPath(repository, queryPath, MAX_NODES, null);

    verify(securityHelper).anyPermitted(any(RepositoryViewPermission.class));
    verify(selectorManager).browseActive(asList(REPOSITORY_NAME), asList(FORMAT_NAME));
    verify(selectorManager).toSql(eq(byGroup), any());
    verify(selectorManager).toSql(eq(byVersion), any());
    verify(browseNodeEntityAdapter).getByPath(db, REPOSITORY_NAME, queryPath, MAX_NODES,
        "((asset_id.attributes.test-format.groupId = :s0p0) or (asset_id.attributes.test-format.version = :s1p0))",
        ImmutableMap.of("s0p0", "org.sonatype", "s1p0", "2.1"));
    verifyNoMoreInteractions(browseNodeEntityAdapter, securityHelper, selectorManager);
  }

  @Test
  public void keywordQueryWithContentSelectors() throws Exception {
    List<String> queryPath = asList("org", "foo");

    when(securityHelper.anyPermitted(any())).thenReturn(false);
    when(selectorManager.browseActive(asList(REPOSITORY_NAME), asList(FORMAT_NAME)))
        .thenReturn(asList(byGroup, byVersion));

    underTest.getByPath(repository, queryPath, MAX_NODES, "wibble");

    verify(securityHelper).anyPermitted(any(RepositoryViewPermission.class));
    verify(selectorManager).browseActive(asList(REPOSITORY_NAME), asList(FORMAT_NAME));
    verify(selectorManager).toSql(eq(byGroup), any());
    verify(selectorManager).toSql(eq(byVersion), any());
    verify(browseNodeEntityAdapter).getByPath(db, REPOSITORY_NAME, queryPath, MAX_NODES,
        "asset_name_lowercase like :keyword_filter and"
            + " ((asset_id.attributes.test-format.groupId = :s0p0) or (asset_id.attributes.test-format.version = :s1p0))",
        ImmutableMap.of("keyword_filter", "%wibble%", "s0p0", "org.sonatype", "s1p0", "2.1"));
    verifyNoMoreInteractions(browseNodeEntityAdapter, securityHelper, selectorManager);
  }

  @Test
  public void simpleQueryWithContentSelectorMix() throws Exception {
    List<String> queryPath = asList("org", "foo");

    when(securityHelper.anyPermitted(any())).thenReturn(false);
    when(selectorManager.browseActive(asList(REPOSITORY_NAME), asList(FORMAT_NAME)))
        .thenReturn(asList(byGroup, jexl, byVersion));

    underTest.getByPath(repository, queryPath, MAX_NODES, null);

    verify(securityHelper).anyPermitted(any(RepositoryViewPermission.class));
    verify(selectorManager).browseActive(asList(REPOSITORY_NAME), asList(FORMAT_NAME));
    verify(selectorManager).toSql(eq(byGroup), any());
    verify(selectorManager).toSql(eq(byVersion), any());
    verify(browseNodeEntityAdapter).getByPath(db, REPOSITORY_NAME, queryPath, MAX_NODES,
        "((asset_id.attributes.test-format.groupId = :s0p0) or (asset_id.attributes.test-format.version = :s1p0)"
            + " or contentAuth(@this.asset_id, :authz_repository_name, true) = true)",
        ImmutableMap.of("s0p0", "org.sonatype", "s1p0", "2.1", "authz_repository_name", "test-repo"));
    verifyNoMoreInteractions(browseNodeEntityAdapter, securityHelper, selectorManager);
  }

  @Test
  public void keywordQueryWithContentSelectorMix() throws Exception {
    List<String> queryPath = asList("org", "foo");

    when(securityHelper.anyPermitted(any())).thenReturn(false);
    when(selectorManager.browseActive(asList(REPOSITORY_NAME), asList(FORMAT_NAME)))
        .thenReturn(asList(byGroup, jexl, byVersion));

    underTest.getByPath(repository, queryPath, MAX_NODES, "wibble");

    verify(securityHelper).anyPermitted(any(RepositoryViewPermission.class));
    verify(selectorManager).browseActive(asList(REPOSITORY_NAME), asList(FORMAT_NAME));
    verify(selectorManager).toSql(eq(byGroup), any());
    verify(selectorManager).toSql(eq(byVersion), any());
    verify(browseNodeEntityAdapter).getByPath(db, REPOSITORY_NAME, queryPath, MAX_NODES,
        "asset_name_lowercase like :keyword_filter and"
            + " ((asset_id.attributes.test-format.groupId = :s0p0) or (asset_id.attributes.test-format.version = :s1p0)"
            + " or contentAuth(@this.asset_id, :authz_repository_name, true) = true)",
        ImmutableMap.of("keyword_filter", "%wibble%", "s0p0", "org.sonatype", "s1p0", "2.1", "authz_repository_name", "test-repo"));
    verifyNoMoreInteractions(browseNodeEntityAdapter, securityHelper, selectorManager);
  }

  @Test
  public void simpleQueryWithJEXL() throws Exception {
    List<String> queryPath = asList("org", "foo");

    when(securityHelper.anyPermitted(any())).thenReturn(false);
    when(selectorManager.browseActive(asList(REPOSITORY_NAME), asList(FORMAT_NAME))).thenReturn(asList(jexl));

    underTest.getByPath(repository, queryPath, MAX_NODES, null);

    verify(securityHelper).anyPermitted(any(RepositoryViewPermission.class));
    verify(selectorManager).browseActive(asList(REPOSITORY_NAME), asList(FORMAT_NAME));
    verify(browseNodeEntityAdapter).getByPath(db, REPOSITORY_NAME, queryPath, MAX_NODES,
        "contentAuth(@this.asset_id, :authz_repository_name, true) = true",
        ImmutableMap.of("authz_repository_name", REPOSITORY_NAME));
    verifyNoMoreInteractions(browseNodeEntityAdapter, securityHelper, selectorManager);
  }

  @Test
  public void keywordQueryWithJEXL() throws Exception {
    List<String> queryPath = asList("org", "foo");

    when(securityHelper.anyPermitted(any())).thenReturn(false);
    when(selectorManager.browseActive(asList(REPOSITORY_NAME), asList(FORMAT_NAME))).thenReturn(asList(jexl));

    underTest.getByPath(repository, queryPath, MAX_NODES, "wibble");

    verify(securityHelper).anyPermitted(any(RepositoryViewPermission.class));
    verify(selectorManager).browseActive(asList(REPOSITORY_NAME), asList(FORMAT_NAME));
    verify(browseNodeEntityAdapter).getByPath(db, REPOSITORY_NAME, queryPath, MAX_NODES,
        "asset_name_lowercase like :keyword_filter and contentAuth(@this.asset_id, :authz_repository_name, true) = true",
        ImmutableMap.of("keyword_filter", "%wibble%", "authz_repository_name", REPOSITORY_NAME));
    verifyNoMoreInteractions(browseNodeEntityAdapter, securityHelper, selectorManager);
  }

  @Test
  public void groupQuery() throws Exception {
    List<String> queryPath = asList("org", "foo");

    when(securityHelper.anyPermitted(any())).thenReturn(true);
    when(repository.getType()).thenReturn(new GroupType());
    when(repository.facet(GroupFacet.class)).thenReturn(groupFacet);
    when(groupFacet.leafMembers()).thenReturn(asList(memberA, memberB, memberC));

    when(browseNodeEntityAdapter.getByPath(db, MEMBER_A, queryPath, MAX_NODES, "", emptyMap()))
        .thenReturn(asList(node(MEMBER_A, "com"), node(MEMBER_A, "org")));
    when(browseNodeEntityAdapter.getByPath(db, MEMBER_B, queryPath, MAX_NODES, "", emptyMap()))
        .thenReturn(asList(node(MEMBER_B, "biz"), node(MEMBER_B, "org")));
    when(browseNodeEntityAdapter.getByPath(db, MEMBER_C, queryPath, MAX_NODES, "", emptyMap()))
        .thenReturn(asList(node(MEMBER_C, "com"), node(MEMBER_C, "javax")));

    Iterable<BrowseNode> nodes = underTest.getByPath(repository, queryPath, MAX_NODES, null);

    // check that duplicate nodes were removed, should follow a 'first-one-wins' approach
    assertThat(nodes, containsInAnyOrder(
        allOf(hasProperty("repositoryName", is(MEMBER_A)), hasProperty("name", is("com"))),
        allOf(hasProperty("repositoryName", is(MEMBER_A)), hasProperty("name", is("org"))),
        allOf(hasProperty("repositoryName", is(MEMBER_B)), hasProperty("name", is("biz"))),
        allOf(hasProperty("repositoryName", is(MEMBER_C)), hasProperty("name", is("javax")))));

    verify(securityHelper).anyPermitted(any(RepositoryViewPermission.class));
    verify(browseNodeEntityAdapter).getByPath(db, MEMBER_A, queryPath, MAX_NODES, "", emptyMap());
    verify(browseNodeEntityAdapter).getByPath(db, MEMBER_B, queryPath, MAX_NODES, "", emptyMap());
    verify(browseNodeEntityAdapter).getByPath(db, MEMBER_C, queryPath, MAX_NODES, "", emptyMap());
    verifyNoMoreInteractions(browseNodeEntityAdapter, securityHelper, selectorManager);
  }

  @Test
  public void groupQueryWithLimit() throws Exception {
    List<String> queryPath = asList("org", "foo");

    when(securityHelper.anyPermitted(any())).thenReturn(true);
    when(repository.getType()).thenReturn(new GroupType());
    when(repository.facet(GroupFacet.class)).thenReturn(groupFacet);
    when(groupFacet.leafMembers()).thenReturn(asList(memberA, memberB, memberC));

    when(browseNodeEntityAdapter.getByPath(db, MEMBER_A, queryPath, 1, "", emptyMap()))
        .thenReturn(asList(node(MEMBER_A, "com")));
    when(browseNodeEntityAdapter.getByPath(db, MEMBER_B, queryPath, 1, "", emptyMap()))
        .thenReturn(asList(node(MEMBER_B, "com")));
    when(browseNodeEntityAdapter.getByPath(db, MEMBER_C, queryPath, 1, "", emptyMap()))
        .thenReturn(asList(node(MEMBER_C, "com")));

    Iterable<BrowseNode> nodes = underTest.getByPath(repository, queryPath, 1, null);

    // check that the limit was correctly applied to the merged results
    assertThat(nodes, containsInAnyOrder(
        allOf(hasProperty("repositoryName", is(MEMBER_A)), hasProperty("name", is("com")))));

    verify(securityHelper).anyPermitted(any(RepositoryViewPermission.class));
    // merging of results should be lazy: only the first member should have been consulted
    verify(browseNodeEntityAdapter).getByPath(db, MEMBER_A, queryPath, 1, "", emptyMap());
    verifyNoMoreInteractions(browseNodeEntityAdapter, securityHelper, selectorManager);
  }


  @Test
  public void keywordQueryWithSingleQuote() throws Exception {
    List<String> queryPath = asList("org", "foo");

    when(securityHelper.anyPermitted(any())).thenReturn(true);

    underTest.getByPath(repository, queryPath, MAX_NODES, "'");

    verify(securityHelper).anyPermitted(any(RepositoryViewPermission.class));
    verify(browseNodeEntityAdapter).getByPath(db, REPOSITORY_NAME, queryPath, MAX_NODES,
        "asset_name_lowercase like :keyword_filter", ImmutableMap.of("keyword_filter", "%'%"));
    verifyNoMoreInteractions(browseNodeEntityAdapter, securityHelper, selectorManager);
  }

  @Test
  public void filterResponses() throws Exception {
    List<String> queryPath = asList("org", "foo");

    when(securityHelper.anyPermitted(any())).thenReturn(true);
    when(browseNodeEntityAdapter.getByPath(any(), any(), any(), anyInt(), any(), anyMap()))
        .thenReturn(ImmutableList.of(new BrowseNode()));

    underTest.getByPath(repository, queryPath, MAX_NODES, null);

    verify(browseNodeFilter).test(any(), eq(REPOSITORY_NAME));
  }

  @Test
  public void defaultSortingOfComponents() throws Exception {
    when(securityHelper.anyPermitted(any())).thenReturn(true);
    when(browseNodeEntityAdapter.getByPath(any(), any(), any(), anyInt(), any(), anyMap())).thenReturn(Arrays
        .asList(
            node("1.0", false, true),
            node("2.0", false, true),
            node("1.1", false, true),
            node("1.10", false, true),
            node("1.2", false, true)));

    assertThat(versions(asList("org", "foo")), contains("1.0", "1.1", "1.2", "1.10", "2.0"));
  }

  @Test
  public void defaultSortingOfAssets() throws Exception {
    when(securityHelper.anyPermitted(any())).thenReturn(true);
    //this test is for asset sorting, but i've left version numbers here so we validate version sorter is _not_ used
    when(browseNodeEntityAdapter.getByPath(any(), any(), any(), anyInt(), any(), anyMap())).thenReturn(Arrays
        .asList(
            node("1.0", true, false),
            node("2.0", true, false),
            node("1.1", true, false),
            node("1.10", true, false),
            node("1.2", true, false)));

    assertThat(versions(asList("org", "foo")), contains("1.0", "1.1", "1.10", "1.2", "2.0"));
  }

  @Test
  public void defaultSortingOfFolders() throws Exception {
    when(securityHelper.anyPermitted(any())).thenReturn(true);
    //this test is for folder sorting, but i've left version numbers here so we validate version sorter is _not_ used
    when(browseNodeEntityAdapter.getByPath(any(), any(), any(), anyInt(), any(), anyMap())).thenReturn(Arrays
        .asList(
            node("1.0", false, false),
            node("2.0", false, false),
            node("1.1", true, false),
            node("1.10", false, true),
            node("1.2", false, false)));

    assertThat(versions(asList("org", "foo")), contains("1.10", "1.0", "1.2", "2.0", "1.1"));
  }

  @Test
  public void alternateSorting() throws Exception {
    underTest = new BrowseNodeStoreImpl(
        () -> databaseInstance,
        browseNodeEntityAdapter,
        securityHelper,
        selectorManager,
        new BrowseNodeConfiguration(true, 1000, DELETE_PAGE_SIZE, 10_000, 10_000, seconds(0)),
        ImmutableMap.of(FORMAT_NAME, browseNodeFilter),
        ImmutableMap.of(DefaultBrowseNodeComparator.NAME, new DefaultBrowseNodeComparator(new VersionComparator()), FORMAT_NAME, new TestComparator()));

    when(securityHelper.anyPermitted(any())).thenReturn(true);
    when(browseNodeEntityAdapter.getByPath(any(), any(), any(), anyInt(), any(), anyMap())).thenReturn(Arrays
        .asList(
            node("1.0", false, false),
            node("2.0", true, false),
            node("1.1", false, false),
            node("1.10", false, true),
            node("1.2", false, false)));

    assertThat(versions(asList("org", "foo")), contains("2.0", "1.2", "1.10", "1.1", "1.0"));
  }

  private List<String> versions(List<String> queryPath) {
    return StreamSupport.stream(underTest.getByPath(repository, queryPath, MAX_NODES, null).spliterator(), false)
        .map(BrowseNode::getName).collect(toList());
  }

  private static BrowseNode node(final String repositoryName, final String name) {
    BrowseNode node = new BrowseNode();
    node.setRepositoryName(repositoryName);
    node.setParentPath("/");
    node.setName(name);
    return node;
  }

  private static BrowseNode node(final String name, final boolean isAsset, final boolean isComponent ) {
    BrowseNode node = new BrowseNode();
    node.setName(name);
    if (isAsset) {
      node.setAssetId(new DetachedEntityId(name));
    }
    if (isComponent) {
      node.setComponentId(new DetachedEntityId(name + "c"));
    }
    return node;
  }

  private final class TestComparator implements BrowseNodeComparator {
    @Override
    public int compare(BrowseNode o1, BrowseNode o2) {
      return 0 - o1.getName().compareToIgnoreCase(o2.getName());
    }
  }
}
