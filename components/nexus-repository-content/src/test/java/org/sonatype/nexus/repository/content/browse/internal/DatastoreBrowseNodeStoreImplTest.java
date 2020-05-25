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
package org.sonatype.nexus.repository.content.browse.internal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import org.sonatype.nexus.common.app.VersionComparator;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.common.entity.Entity;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.node.BrowseNode;
import org.sonatype.nexus.repository.browse.node.BrowseNodeComparator;
import org.sonatype.nexus.repository.browse.node.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.browse.node.BrowseNodeFacet;
import org.sonatype.nexus.repository.browse.node.BrowseNodeFilter;
import org.sonatype.nexus.repository.browse.node.BrowsePath;
import org.sonatype.nexus.repository.browse.node.DefaultBrowseNodeComparator;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.ContentRepository;
import org.sonatype.nexus.repository.content.store.ContentRepositoryDAO;
import org.sonatype.nexus.repository.content.store.ContentRepositoryData;
import org.sonatype.nexus.repository.content.store.ContentRepositoryStore;
import org.sonatype.nexus.repository.content.store.ExampleContentTestSupport;
import org.sonatype.nexus.repository.content.store.InternalIds;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.selector.DatastoreContentAuthHelper;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.selector.CselSelector;
import org.sonatype.nexus.selector.JexlSelector;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorEvaluationException;
import org.sonatype.nexus.selector.SelectorManager;
import org.sonatype.nexus.selector.SelectorSqlBuilder;
import org.sonatype.nexus.transaction.TransactionModule;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.TypeLiteral;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.sonatype.nexus.repository.content.store.InternalIds.internalAssetId;
import static org.sonatype.nexus.repository.content.store.InternalIds.internalComponentId;

public class DatastoreBrowseNodeStoreImplTest
    extends ExampleContentTestSupport
{
  private static final String FORMAT_NAME = "test";

  private static final String REPOSITORY_NAME = "maven-central";

  private static final String GROUP_REPOSITORY_NAME = "maven-public";

  private static final int DELETE_PAGE_SIZE = 10;

  @Mock
  private BrowseNodeFilter browseNodeFilter;

  @Mock
  private Repository repository;

  @Mock
  private Repository repositoryGroup;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private SelectorManager selectorManager;

  @Mock
  private DatastoreContentAuthHelper contentAuthHelper;

  @Mock
  private ContentRepositoryStore<? extends ContentRepositoryDAO> contentRepositoryStores;

  private DatastoreBrowseNodeStoreImpl<TestBrowseNodeDAO> underTest;

  private String databaseId;

  public DatastoreBrowseNodeStoreImplTest() {
    super(TestBrowseNodeDAO.class);
  }

  @Before
  public void setup() throws Exception {
    generateRandomRepositories(2);
    generateRandomNamespaces(100);
    generateRandomNames(100);
    generateRandomPaths(100);
    generateRandomVersions(100);
    generateRandomContent(50, 100);

    BrowseNodeConfiguration browseNodeConfiguration =
        new BrowseNodeConfiguration(true, 1000, DELETE_PAGE_SIZE, 10_000, 10_000);

    mockRepository(repository, REPOSITORY_NAME, FORMAT_NAME, 0);
    mockRepository(repositoryGroup, GROUP_REPOSITORY_NAME, FORMAT_NAME, 1);
    when(repositoryGroup.optionalFacet(BrowseNodeFacet.class)).thenReturn(Optional.empty());
    when(repositoryGroup.getType()).thenReturn(new GroupType());
    GroupFacet groupFacet = mock(GroupFacet.class);
    when(groupFacet.allMembers()).thenReturn(Arrays.asList(repositoryGroup, repository));
    when(repositoryGroup.facet(GroupFacet.class)).thenReturn(groupFacet);

    when(contentAuthHelper.checkPathPermissionsJexlOnly(any(), any(), any())).thenReturn(true);

    when(securityHelper.anyPermitted(any())).thenReturn(true);
    when(browseNodeFilter.test(any(), any())).thenReturn(true);

    underTest = Guice.createInjector(new TransactionModule(), new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(DataSessionSupplier.class).toInstance(sessionRule);
        bind(SecurityHelper.class).toInstance(securityHelper);
        bind(SelectorManager.class).toInstance(selectorManager);
        bind(BrowseNodeConfiguration.class).toInstance(browseNodeConfiguration);
        bind(RepositoryManager.class).toInstance(repositoryManager);
        bind(DatastoreContentAuthHelper.class).toInstance(contentAuthHelper);
        bind(new TypeLiteral<Map<String, ContentRepositoryStore<? extends ContentRepositoryDAO>>>() { })
            .toInstance(ImmutableMap.of(FORMAT_NAME, contentRepositoryStores));
        bind(new TypeLiteral<Map<String, BrowseNodeFilter>>() { })
            .toInstance(ImmutableMap.of(FORMAT_NAME, browseNodeFilter));
        bind(new TypeLiteral<Map<String, BrowseNodeComparator>>() { })
            .toInstance(ImmutableMap
                .of(DefaultBrowseNodeComparator.NAME, new DefaultBrowseNodeComparator(new VersionComparator())));
      }
    }).getInstance(TestDatastoreBrowseNodeStoreImpl.class);

    underTest.start();

    try (Connection connection = sessionRule.openConnection("content")) {
      databaseId = connection.getMetaData().getDatabaseProductName();
    }
  }

  @After
  public void teardown() throws Exception {
    underTest.stop();
  }

  @Test
  public void testAssetNodeExists() {
    Asset asset = generatedAssets().get(0);
    assertFalse(underTest.assetNodeExists(asset));

    underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"), asset);

    assertTrue(underTest.assetNodeExists(asset));
  }

  @Test
  public void testCreateAssetNode() {
    Asset asset = generatedAssets().get(0);
    underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"), asset);

    assertTrue(underTest.assetNodeExists(asset));
  }

  @Test
  public void testCreateComponentNode() {
    Component component = generatedComponents().get(0);
    underTest.createComponentNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"), component);

    Iterable<BrowseNode<Integer>> nodes = underTest.getByPath(REPOSITORY_NAME, Collections.singletonList("org"), 1);
    BrowseNode<Integer> node = Iterables.getFirst(nodes, null);

    assertThat(node, isBrowseNodeWith(generatedRepositories().get(0), 1, "org/foo", "foo", null, component, true));
  }

  @Test
  public void testDeleteAssetNode() {
    Asset asset = generatedAssets().get(0);
    underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"), asset);

    underTest.deleteAssetNode(asset);
    assertFalse(underTest.assetNodeExists(asset));
  }

  @Test
  public void testDeleteAssetNode_whenNotExist() {
    Asset asset = generatedAssets().get(0);
    underTest.deleteAssetNode(asset);
  }

  @Test
  public void testDeleteAssetNode_whenComponentAlsoLinked() {
    Asset asset = generatedAssets().get(0);
    Component component = generatedComponents().get(0);

    underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"), asset);
    underTest.createComponentNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"), component);

    underTest.deleteAssetNode(asset);
    assertFalse(underTest.assetNodeExists(asset));

    Iterator<BrowseNode<Integer>> iter = underTest.getByPath(REPOSITORY_NAME, Arrays.asList("org"), 1).iterator();
    assertTrue(iter.hasNext());

    DatastoreBrowseNode node = (DatastoreBrowseNode) iter.next();
    assertThat(node.getAssetId(), nullValue());
    assertThat(node.getComponentId(), is(internalComponentId(component)));
  }

  @Test
  public void testDeleteComponentNode() {
    Component component = generatedComponents().get(0);
    underTest.createComponentNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"), component);

    underTest.deleteComponentNode(component);
    assertFalse(componentNodeExists(component, createBrowsePaths("org", "foo")));
  }

  @Test
  public void testDeleteComponentNode_whenNotExist() {
    Component component = generatedComponents().get(0);
    underTest.deleteComponentNode(component);
    // basically this shouldn't error
  }

  @Test
  public void testDeleteComponentNode_whenAssetAlsoLinked() {
    Asset asset = generatedAssets().get(0);
    Component component = generatedComponents().get(0);

    underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"), asset);
    underTest.createComponentNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"), component);

    underTest.deleteComponentNode(component);
    assertFalse(componentNodeExists(component, createBrowsePaths("org", "foo")));

    Iterator<BrowseNode<Integer>> iter =
        underTest.getByPath(REPOSITORY_NAME, Collections.singletonList("org"), 1).iterator();
    assertTrue(iter.hasNext());

    DatastoreBrowseNode node = (DatastoreBrowseNode) iter.next();
    assertThat(node.getAssetId(), is(internalAssetId(asset)));
    assertThat(node.getComponentId(), nullValue());
  }

  @Test
  public void testDeleteNodeConcurrency() throws Exception {
    populateFullRepository();

    ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());

    pool.submit(() -> generatedAssets().forEach(asset -> pool.submit(() -> delete(asset))));
    pool.submit(() -> generatedComponents().forEach(component -> pool.submit(() -> delete(component))));

    pool.awaitQuiescence(10, TimeUnit.SECONDS);

    assertRepositoryEmpty(
        generatedRepositories()
            .stream()
            .filter(repo -> repo.contentRepositoryId() == 1)
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("Repository with id 1 does not exist"))
            .contentRepositoryId()
    );
  }

  @Test
  public void testDeleteByRepository() {
    populateFullRepository();
    underTest.deleteByRepository(REPOSITORY_NAME);
    assertRepositoryEmpty(generatedRepositories().get(0).contentRepositoryId());
  }

  @Test
  public void testGetByPath() {
    Asset asset = generatedAssets().get(0);
    Component component = generatedComponents().get(0);
    Asset asset2 = generatedAssets().get(1);

    underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"), asset);
    underTest.createComponentNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "bar"), component);
    underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "bar", "foo"), asset2);

    Iterable<BrowseNode<Integer>> it = underTest.getByPath(REPOSITORY_NAME, Collections.singletonList("org"), 1000);

    List<BrowseNode<Integer>> nodes = StreamSupport.stream(it.spliterator(), false).collect(toList());

    assertThat(nodes, hasSize(2));
    assertThat(nodes.get(0),
        isBrowseNodeWith(generatedRepositories().get(0), 1, "org/bar", "bar", null, component, false));
    assertThat(nodes.get(1),
        isBrowseNodeWith(generatedRepositories().get(0), 1, "org/foo", "foo", asset, null, true));
  }

  @Test
  public void testGetByPath_groupRepository() {
    Asset asset = generatedAssets().get(0);
    Component component = generatedComponents().get(0);
    Asset asset2 = generatedAssets().get(1);

    underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"), asset);
    underTest.createAssetNode(GROUP_REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "bar", "foo"), asset2);
    underTest.createComponentNode(GROUP_REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "bar"), component);

    Iterable<BrowseNode<Integer>> it =
        underTest.getByPath(GROUP_REPOSITORY_NAME, Collections.singletonList("org"), 1000);

    List<BrowseNode<Integer>> nodes = StreamSupport.stream(it.spliterator(), false).collect(toList());

    ContentRepository repository = generatedRepositories().get(0);
    ContentRepository groupRepository = generatedRepositories().get(1);

    assertThat(nodes, hasSize(2));
    assertThat(nodes.get(0),
        isBrowseNodeWith(groupRepository, getNodeId(groupRepository, "org"), "org/bar", "bar", null,
            component, false));
    assertThat(nodes.get(1),
        isBrowseNodeWith(repository, getNodeId(repository, "org"), "org/foo", "foo", asset,
            null, true));
  }

  @Test
  public void testGetByPath_groupRepository_identity() {
    Asset asset = generatedAssets().get(0);
    Asset asset2 = generatedAssets().get(1);

    underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"), asset);
    underTest.createAssetNode(GROUP_REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"), asset2);

    BrowseNodeFacet browseNodeFacet = mock(BrowseNodeFacet.class);
    Function<BrowseNode<?>, String> fn = mock(Function.class);
    when(browseNodeFacet.browseNodeIdentity()).thenReturn(fn);
    when(repositoryGroup.optionalFacet(BrowseNodeFacet.class)).thenReturn(Optional.of(browseNodeFacet));

    Iterable<BrowseNode<Integer>> it = underTest.getByPath(GROUP_REPOSITORY_NAME, Collections.singletonList("org"), 1000);
    List<BrowseNode<Integer>> nodes = StreamSupport.stream(it.spliterator(), false).collect(toList());

    verify(fn, times(5)).apply(any(BrowseNode.class));

    ContentRepository groupRepository = generatedRepositories().get(1);

    assertThat(nodes, hasSize(1));
    assertThat(nodes.get(0),
        isBrowseNodeWith(groupRepository, getNodeId(groupRepository, "org"), "org/foo", "foo", asset2, null, true));
  }

  @Test
  public void testGetByPath_queryLimit() {
    Asset asset = generatedAssets().get(0);
    underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"), asset);
    Component component = generatedComponents().get(0);
    underTest.createComponentNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "bar"), component);

    Iterable<BrowseNode<Integer>> it = underTest.getByPath(REPOSITORY_NAME, Collections.singletonList("org"), 1);

    List<BrowseNode<Integer>> nodes = StreamSupport.stream(it.spliterator(), false).collect(toList());

    ContentRepository repository = generatedRepositories().get(0);
    assertThat(nodes, hasSize(1));
    assertThat(nodes.get(0),
        isBrowseNodeWith(repository, 1, "org/foo", "foo", asset, null, true));
  }

  @Test
  public void testGetByPath_atRoot() {
    Asset asset = generatedAssets().get(0);
    underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org"), asset);
    Component component = generatedComponents().get(0);
    underTest.createComponentNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("com"), component);

    Iterable<BrowseNode<Integer>> it = underTest.getByPath(REPOSITORY_NAME, Collections.emptyList(), 1000);

    List<BrowseNode<Integer>> nodes = StreamSupport.stream(it.spliterator(), false).collect(toList());

    ContentRepository repository = generatedRepositories().get(0);
    assertThat(nodes, hasSize(2));
    assertThat(nodes.get(0),
        isBrowseNodeWith(repository, null, "com", "com", null, component, true));
    assertThat(nodes.get(1),
        isBrowseNodeWith(repository, null, "org", "org", asset, null, true));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetByPath_browseNodeFilter() {
    when(browseNodeFilter.test(any(), any())).thenAnswer(invok -> {
      return "foo".equals(((BrowseNode<Integer>) invok.getArguments()[0]).getName());
    });

    Asset asset = generatedAssets().get(0);
    underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"), asset);
    Component component = generatedComponents().get(0);
    underTest.createComponentNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "bar"), component);

    Iterable<BrowseNode<Integer>> it = underTest.getByPath(REPOSITORY_NAME, Collections.singletonList("org"), 1000);

    List<BrowseNode<Integer>> nodes = StreamSupport.stream(it.spliterator(), false).collect(toList());

    assertThat(nodes, hasSize(1));
    assertThat(nodes.get(0), isBrowseNodeWith(generatedRepositories().get(0), 1, "org/foo", "foo", asset, null, true));
  }

  private void delete(final Component component) {
    underTest.deleteComponentNode(component);
  }

  private void delete(final Asset asset) {
    underTest.deleteAssetNode(asset);
  }

  @Test
  public void testGetByPath_cselContentSelector() throws SelectorEvaluationException {
    when(securityHelper.anyPermitted(any())).thenReturn(false);

    Asset orgAsset = generatedAssets().get(0);
    underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"), orgAsset);
    Component orgComponent = generatedComponents().get(0);
    underTest.createComponentNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "bar"), orgComponent);

    Asset comAsset = generatedAssets().get(1);
    underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("com", "foo"), comAsset);
    Component comComponent = generatedComponents().get(1);
    underTest.createComponentNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("com", "bar"), comComponent);

    Asset sunAsset = generatedAssets().get(2);
    underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("sun", "foo"), sunAsset);
    Component sunComponent = generatedComponents().get(2);
    underTest.createComponentNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("sun", "bar"), sunComponent);

    SelectorConfiguration fooSelector = mock(SelectorConfiguration.class);
    when(fooSelector.getType()).thenReturn(CselSelector.TYPE);

    SelectorConfiguration sunSelector = mock(SelectorConfiguration.class);
    when(sunSelector.getType()).thenReturn(CselSelector.TYPE);

    when(selectorManager.browseActive(eq(asList(REPOSITORY_NAME)), eq(asList(FORMAT_NAME)))).thenReturn(Arrays.asList(sunSelector, fooSelector));

    doAnswer(invocationOnMock -> {
      SelectorSqlBuilder builder = (SelectorSqlBuilder) invocationOnMock.getArguments()[1];
      builder.appendProperty("path");
      builder.appendOperator(getRegexOperator());
      builder.appendLiteral(".*foo.*");
      return null;
    }).when(selectorManager).toSql(eq(fooSelector), any());

    doAnswer(invocationOnMock -> {
      SelectorSqlBuilder builder = (SelectorSqlBuilder) invocationOnMock.getArguments()[1];
      builder.appendProperty("path");
      builder.appendOperator(getRegexOperator());
      builder.appendLiteral(".*sun.*");
      return null;
    }).when(selectorManager).toSql(eq(sunSelector), any());

    Iterable<BrowseNode<Integer>> it = underTest.getByPath(REPOSITORY_NAME, asList("org"), 1000);

    List<BrowseNode<Integer>> nodes = StreamSupport.stream(it.spliterator(), false).collect(toList());

    assertThat(nodes, hasSize(1));
    assertThat(nodes.get(0), isBrowseNodeWith(generatedRepositories().get(0), 1, "org/foo", "foo", orgAsset, null, true));
  }

  @Test
  public void testGetByPath_jexl_and_Csel_ContentSelector() throws SelectorEvaluationException {
    when(securityHelper.anyPermitted(any())).thenReturn(false);

    Asset fooAsset = generatedAssets().get(0);
    underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"), fooAsset);
    Component fooComponent = generatedComponents().get(0);
    underTest.createComponentNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"), fooComponent);

    Asset barAsset = generatedAssets().get(1);
    underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "bar"), barAsset);
    Component barComponent = generatedComponents().get(1);
    underTest.createComponentNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "bar"), barComponent);

    Asset biffAsset = generatedAssets().get(2);
    underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "biff"), biffAsset);
    Component biffComponent = generatedComponents().get(2);
    underTest.createComponentNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "biff"), biffComponent);

    Asset bazAsset = generatedAssets().get(3);
    underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "baz"), bazAsset);
    Component bazComponent = generatedComponents().get(3);
    underTest.createComponentNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "baz"), bazComponent);

    SelectorConfiguration cselSelector = mock(SelectorConfiguration.class);
    when(cselSelector.getType()).thenReturn(CselSelector.TYPE);

    SelectorConfiguration jexlSelector = mock(SelectorConfiguration.class);
    when(jexlSelector.getType()).thenReturn(JexlSelector.TYPE);

    when(contentAuthHelper.checkPathPermissions(any(), any(), any())).thenReturn(false);
    when(contentAuthHelper.checkPathPermissions(eq("org/bar"), any(), any())).thenReturn(true);
    when(contentAuthHelper.checkPathPermissions(eq("org/baz"), any(), any())).thenReturn(true);
    when(contentAuthHelper.checkPathPermissions(eq("org/foo"), any(), any())).thenReturn(true);

    when(selectorManager.browseActive(eq(asList(REPOSITORY_NAME)), eq(asList(FORMAT_NAME)))).thenReturn(Arrays.asList(cselSelector, jexlSelector));


    Iterable<BrowseNode<Integer>> it = underTest.getByPath(REPOSITORY_NAME, asList("org"), 2);

    List<BrowseNode<Integer>> nodes = StreamSupport.stream(it.spliterator(), false).collect(toList());

    verify(selectorManager, never()).toSql(any(), any());

    assertThat(nodes, hasSize(2));
    assertThat(nodes.get(0), isBrowseNodeWith(generatedRepositories().get(0), 1, "org/bar", "bar", barAsset, barComponent, true));
    assertThat(nodes.get(1), isBrowseNodeWith(generatedRepositories().get(0), 1, "org/foo", "foo", fooAsset, fooComponent, true));

  }

  private String getRegexOperator() {
    switch (databaseId) {
      case "H2":
        return "regexp";
      case "PostgreSQL":
        return "~";
      default:
        throw new IllegalStateException("Failed to handle databaseId");
    }
  }

  private void populateFullRepository() {
    Iterator<Asset> assets = generatedAssets().iterator();
    Iterator<Component> components = generatedComponents().iterator();

    while (assets.hasNext() && components.hasNext()) {
      Asset asset = assets.next();
      Component component = components.next();
      List<BrowsePath> paths = createBrowsePaths(asset.path().split("/"));

      underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, paths, asset);
      underTest.createComponentNode(REPOSITORY_NAME, FORMAT_NAME, paths, component);
    }
  }

  private List<BrowsePath> createBrowsePaths(final String... paths) {
    List<BrowsePath> browsePaths = new ArrayList<>();
    List<String> pathsList = Arrays.asList(paths);
    for (int i = 0; i < paths.length; i++) {
      browsePaths.add(new BrowsePath(paths[i], Joiner.on('/').join(pathsList.subList(0, i + 1))));
    }
    return browsePaths;
  }

  private boolean componentNodeExists(final Component component, final List<BrowsePath> browsePaths) {
    String requestPath = browsePaths.get(browsePaths.size() - 2).getRequestPath();
    Iterable<BrowseNode<Integer>> iter =
        underTest.getByPath(REPOSITORY_NAME, Arrays.asList(requestPath.split("/")), 100);

    int componentId = internalComponentId(component);
    for (BrowseNode<Integer> node : iter) {
      if (Objects.equals(node.getComponentId(), componentId)) {
        return true;
      }
    }

    return false;
  }

  private void mockRepository(
      final Repository repository,
      final String name,
      final String formatName,
      final int repositoryIndex)
  {
    when(repositoryManager.get(name)).thenReturn(repository);

    when(repository.optionalFacet(BrowseNodeFacet.class)).thenReturn(Optional.empty());

    Format format = mock(Format.class);
    when(format.getValue()).thenReturn(formatName);

    when(repository.getName()).thenReturn(name);
    when(repository.getFormat()).thenReturn(format);

    Configuration config = mock(Configuration.class, withSettings().extraInterfaces(Entity.class));
    when(repository.getConfiguration()).thenReturn(config);

    EntityMetadata metadata = mock(EntityMetadata.class);
    when(((Entity) config).getEntityMetadata()).thenReturn(metadata);

    when(metadata.getId()).thenReturn(new DetachedEntityId(name));

    ContentRepositoryData data = generatedRepositories().get(repositoryIndex);
    when(contentRepositoryStores.readContentRepository(new DetachedEntityId(name))).thenReturn(Optional.of(data));
  }

  private int getNodeId(final ContentRepository repository, final String path) {
    try (Connection conn = sessionRule.openConnection("content")) {
      try (PreparedStatement statement =
               conn.prepareStatement(
                   "SELECT browse_node_id FROM test_browse_node WHERE repository_id = ? AND path = ?")) {
        statement.setInt(1, repository.contentRepositoryId());
        statement.setString(2, path);
        try (ResultSet rs = statement.executeQuery()) {
          if (!rs.next()) {
            fail("No results found for path: " + path + " in repository=" + repository);
          }
          return rs.getInt(1);
        }
      }
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private void assertRepositoryEmpty(final int repositoryId) {
    try (Connection conn = sessionRule.openConnection("content")) {
      int rowCount;
      try (PreparedStatement statement =
               conn.prepareStatement("SELECT count(*) as row_count FROM test_browse_node WHERE repository_id = ?")) {
        statement.setInt(1, repositoryId);
        try (ResultSet rs = statement.executeQuery()) {
          if (!rs.next()) {
            fail("Query failed for repository=" + repositoryId);
          }
          rowCount = rs.getInt("row_count");
        }
      }
      assertThat("Expected no browse nodes for the repository", rowCount, is(0));
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private void dumpDb(final String location) {
    try (Connection conn = sessionRule.openConnection("content")) {
      try (PreparedStatement statement = conn.prepareStatement("SCRIPT SIMPLE TO ?")) {
        statement.setString(1, location);
        statement.execute();
      }
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private static Matcher<BrowseNode<Integer>> isBrowseNodeWith(
      final ContentRepository contentRepository,
      final Integer parentId,
      final String path,
      final String name,
      final Asset asset,
      final Component component,
      final boolean isLeaf)
  {
    Integer assetId = ofNullable(asset).map(InternalIds::internalAssetId).orElse(null);
    Integer componentId = ofNullable(component).map(InternalIds::internalComponentId).orElse(null);
    Integer repositoryId = contentRepository.contentRepositoryId();

    return new TypeSafeMatcher<BrowseNode<Integer>>()
    {
      @Override
      public void describeTo(final Description description) {
        description.appendText("repositoryId=" + repositoryId + " parentId=" + parentId + " path=" + path + " name="
            + name + " assetId=" + assetId + " componentId=" + componentId + " isLeaf=" + isLeaf);
      }

      @Override
      protected boolean matchesSafely(final BrowseNode<Integer> item) {
        return Objects.equals(parentId, ((DatastoreBrowseNode) item).getParentId())
            && Objects.equals(repositoryId, ((DatastoreBrowseNode) item).getRepositoryId())
            && Objects.equals(assetId, item.getAssetId())
            && Objects.equals(componentId, item.getComponentId())
            && Objects.equals(path, item.getPath())
            && Objects.equals(name, item.getName()) && Objects.equals(isLeaf, item.isLeaf());
      }
    };
  }
}
