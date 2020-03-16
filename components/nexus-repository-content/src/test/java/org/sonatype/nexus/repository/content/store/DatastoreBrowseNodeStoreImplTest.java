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
package org.sonatype.nexus.repository.content.store;

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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.sonatype.nexus.common.app.VersionComparator;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.common.entity.Entity;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.browse.BrowsePaths;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.ContentRepository;
import org.sonatype.nexus.repository.content.browse.internal.DatastoreBrowseNode;
import org.sonatype.nexus.repository.content.browse.internal.TestBrowseNodeDAO;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.BrowseNode;
import org.sonatype.nexus.repository.storage.BrowseNodeComparator;
import org.sonatype.nexus.repository.storage.BrowseNodeFacet;
import org.sonatype.nexus.repository.storage.BrowseNodeFilter;
import org.sonatype.nexus.repository.storage.DefaultBrowseNodeComparator;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.selector.SelectorManager;
import org.sonatype.nexus.transaction.Operation;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.VoidOperation;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class DatastoreBrowseNodeStoreImplTest
    extends RepositoryContentTestSupport
{
  private static final String FORMAT_NAME = "maven2";

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
  private ContentRepositoryStore<? extends ContentRepositoryDAO> contentRepositoryStores;

  private DatastoreBrowseNodeStoreImpl<TestBrowseNodeDAO> underTest;

  public DatastoreBrowseNodeStoreImplTest() {
    super(TestBrowseNodeDAO.class);
  }

  @Before
  public void setup() throws Exception {
    underTest = new TestDatastoreBrowseNodeStoreImpl(sessionRule, securityHelper, selectorManager,
        new BrowseNodeConfiguration(true, 1000, DELETE_PAGE_SIZE, 10_000, 10_000), repositoryManager,
        ImmutableMap.of(FORMAT_NAME, contentRepositoryStores), ImmutableMap.of(FORMAT_NAME, browseNodeFilter),
        ImmutableMap.of(DefaultBrowseNodeComparator.NAME, new DefaultBrowseNodeComparator(new VersionComparator())));

    generateRandomRepositories(2);
    generateRandomNamespaces(100);
    generateRandomNames(100);
    generateRandomPaths(100);
    generateRandomVersions(100);
    generateRandomContent(50, 100);

    mockRepository(repository, REPOSITORY_NAME, FORMAT_NAME, 0);

    mockRepository(repositoryGroup, GROUP_REPOSITORY_NAME, FORMAT_NAME, 1);
    when(repositoryGroup.optionalFacet(BrowseNodeFacet.class)).thenReturn(Optional.empty());
    when(repositoryGroup.getType()).thenReturn(new GroupType());
    GroupFacet groupFacet = mock(GroupFacet.class);
    when(groupFacet.allMembers()).thenReturn(Arrays.asList(repositoryGroup, repository));
    when(repositoryGroup.facet(GroupFacet.class)).thenReturn(groupFacet);

    underTest.start();

    when(securityHelper.anyPermitted(any())).thenReturn(true);
    when(browseNodeFilter.test(any(), any())).thenReturn(true);
  }

  @After
  public void teardown() throws Exception {
    underTest.stop();
  }

  @Test
  public void testAssetNodeExists() {
    Asset asset = generatedAssets().get(0);
    assertFalse(call(() -> underTest.assetNodeExists(asset)));

    run(() -> underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"), asset));

    assertTrue(call(() -> underTest.assetNodeExists(asset)));
  }

  @Test
  public void testCreateAssetNode() {
    Asset asset = generatedAssets().get(0);
    run(() -> underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"),
        asset));

    assertTrue(call(() -> underTest.assetNodeExists(asset)));
  }

  @Test
  public void testCreateComponentNode() {
    Component component = generatedComponents().get(0);
    run(() -> underTest.createComponentNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"),
        component));

    Iterable<BrowseNode<Integer>> nodes =
        call(() -> underTest.getByPath(REPOSITORY_NAME, Collections.singletonList("org"), 1));
    BrowseNode<Integer> node = Iterables.getFirst(nodes, null);

    assertThat(node, isBrowseNodeWith(generatedRepositories().get(0), 1, "org/foo", "foo", null, component, true));
  }

  @Test
  public void testDeleteAssetNode() {
    Asset asset = generatedAssets().get(0);
    run(() -> underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"), asset));

    run(() -> underTest.deleteAssetNode(asset));
    assertFalse(call(() -> underTest.assetNodeExists(asset)));
  }

  @Test
  public void testDeleteAssetNode_whenNotExist() {
    Asset asset = generatedAssets().get(0);
    run(() -> underTest.deleteAssetNode(asset));
  }

  @Test
  public void testDeleteAssetNode_whenComponentAlsoLinked() {
    Asset asset = generatedAssets().get(0);
    Component component = generatedComponents().get(0);
    run(() -> {
      underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"), asset);
      underTest.createComponentNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"), component);
    });

    run(() -> underTest.deleteAssetNode(asset));
    assertFalse(call(() -> underTest.assetNodeExists(asset)));

    Iterator<BrowseNode<Integer>> iter =
        call(() -> underTest.getByPath(REPOSITORY_NAME, Arrays.asList("org"), 1)).iterator();
    assertTrue(iter.hasNext());

    DatastoreBrowseNode node = (DatastoreBrowseNode) iter.next();
    assertThat(node.getAssetId(), nullValue());
    assertThat(node.getComponentId(), is(((ComponentData) component).componentId));
  }

  @Test
  public void testDeleteComponentNode() {
    Component component = generatedComponents().get(0);
    run(() -> underTest.createComponentNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"), component));

    run(() -> underTest.deleteComponentNode(component));
    assertFalse(componentNodeExists(component, createBrowsePaths("org", "foo")));
  }

  @Test
  public void testDeleteComponentNode_whenNotExist() {
    Component component = generatedComponents().get(0);
    run(() -> underTest.deleteComponentNode(component));
    // basically this shouldn't error
  }

  @Test
  public void testDeleteComponentNode_whenAssetAlsoLinked() {
    Asset asset = generatedAssets().get(0);
    Component component = generatedComponents().get(0);
    run(() -> {
      underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"), asset);
      underTest.createComponentNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"), component);
    });

    run(() -> underTest.deleteComponentNode(component));
    assertFalse(componentNodeExists(component, createBrowsePaths("org", "foo")));

    Iterator<BrowseNode<Integer>> iter =
        call(() -> underTest.getByPath(REPOSITORY_NAME, Arrays.asList("org"), 1)).iterator();
    assertTrue(iter.hasNext());

    DatastoreBrowseNode node = (DatastoreBrowseNode) iter.next();
    assertThat(node.getAssetId(), is(((AssetData) asset).assetId));
    assertThat(node.getComponentId(), nullValue());
  }

  @Ignore("NEXUS-23076")
  @Test
  public void testDeleteNodeConcurrency() {
    populateFullRepository();

    ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());

    pool.submit(() -> generatedAssets().stream().forEach(asset -> pool.submit(() -> delete(asset))));
    pool.submit(() -> generatedComponents().stream().forEach(component -> pool.submit(() -> delete(component))));

    pool.awaitQuiescence(10, TimeUnit.SECONDS);
    dumpDb("C:/tmp/concurrency.sql");
    assertRepositoryEmpty(generatedRepositories().get(0).repositoryId);
  }

  private void delete(final Component component) {
    try {
      Transactional.operation.withDb(() -> sessionRule.openSession("content")).retryOn(RuntimeException.class)
          .run(() -> underTest.deleteComponentNode(component));
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void delete(final Asset asset) {
    try {
      Transactional.operation.withDb(() -> sessionRule.openSession("content")).retryOn(RuntimeException.class)
          .run(() -> underTest.deleteAssetNode(asset));
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testDeleteByRepository() {
    populateFullRepository();
    run(() -> underTest.deleteByRepository(REPOSITORY_NAME));
    assertRepositoryEmpty(generatedRepositories().get(0).repositoryId);
  }

  @Test
  public void testGetByPath() {
    Asset asset = generatedAssets().get(0);
    Component component = generatedComponents().get(0);
    Asset asset2 = generatedAssets().get(1);
    run(() -> {
      underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"), asset);
      underTest.createComponentNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "bar"), component);
      underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "bar", "foo"), asset2);
    });

    Iterable<BrowseNode<Integer>> it =
        call(() -> underTest.getByPath(REPOSITORY_NAME, Arrays.asList("org"), 1000));

    List<BrowseNode<Integer>> nodes = StreamSupport.stream(it.spliterator(), false).collect(Collectors.toList());

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
    run(() -> {
      underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"), asset);
      underTest.createAssetNode(GROUP_REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "bar", "foo"), asset2);
      underTest.createComponentNode(GROUP_REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "bar"), component);
    });

    Iterable<BrowseNode<Integer>> it =
        call(() -> underTest.getByPath(GROUP_REPOSITORY_NAME, Arrays.asList("org"), 1000));

    List<BrowseNode<Integer>> nodes = StreamSupport.stream(it.spliterator(), false).collect(Collectors.toList());

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
    run(() -> {
      underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"), asset);
      underTest.createAssetNode(GROUP_REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"), asset2);
    });

    BrowseNodeFacet browseNodeFacet = mock(BrowseNodeFacet.class);
    Function<BrowseNode<?>, String> fn = mock(Function.class);
    when(browseNodeFacet.browseNodeIdentity()).thenReturn(fn);
    when(repositoryGroup.optionalFacet(BrowseNodeFacet.class)).thenReturn(Optional.of(browseNodeFacet));

    Iterable<BrowseNode<Integer>> it =
        call(() -> underTest.getByPath(GROUP_REPOSITORY_NAME, Arrays.asList("org"), 1000));
    List<BrowseNode<Integer>> nodes = StreamSupport.stream(it.spliterator(), false).collect(Collectors.toList());

    verify(fn, times(5)).apply(any(BrowseNode.class));

    ContentRepository groupRepository = generatedRepositories().get(1);

    assertThat(nodes, hasSize(1));
    assertThat(nodes.get(0),
        isBrowseNodeWith(groupRepository, getNodeId(groupRepository, "org"), "org/foo", "foo", asset2, null, true));
  }

  @Test
  public void testGetByPath_queryLimit() {
    Asset asset = generatedAssets().get(0);
    run(() -> underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"), asset));
    Component component = generatedComponents().get(0);
    run(() -> underTest.createComponentNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "bar"), component));

    Iterable<BrowseNode<Integer>> it = call(() -> underTest.getByPath(REPOSITORY_NAME, Arrays.asList("org"), 1));

    List<BrowseNode<Integer>> nodes = StreamSupport.stream(it.spliterator(), false).collect(Collectors.toList());

    ContentRepository repository = generatedRepositories().get(0);
    assertThat(nodes, hasSize(1));
    assertThat(nodes.get(0),
        isBrowseNodeWith(repository, 1, "org/foo", "foo", asset, null, true));
  }

  @Test
  public void testGetByPath_atRoot() {
    Asset asset = generatedAssets().get(0);
    run(() -> underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org"), asset));
    Component component = generatedComponents().get(0);
    run(() -> underTest.createComponentNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("com"), component));

    Iterable<BrowseNode<Integer>> it = call(() -> underTest.getByPath(REPOSITORY_NAME, Arrays.asList(), 1000));

    List<BrowseNode<Integer>> nodes = StreamSupport.stream(it.spliterator(), false).collect(Collectors.toList());

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
    run(() -> underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "foo"), asset));
    Component component = generatedComponents().get(0);
    run(() -> underTest.createComponentNode(REPOSITORY_NAME, FORMAT_NAME, createBrowsePaths("org", "bar"), component));

    Iterable<BrowseNode<Integer>> it = call(() -> underTest.getByPath(REPOSITORY_NAME, Arrays.asList("org"), 1000));

    List<BrowseNode<Integer>> nodes = StreamSupport.stream(it.spliterator(), false).collect(Collectors.toList());

    assertThat(nodes, hasSize(1));
    assertThat(nodes.get(0), isBrowseNodeWith(generatedRepositories().get(0), 1, "org/foo", "foo", asset, null, true));
  }

  private void populateFullRepository() {
    Iterator<Asset> assets = generatedAssets().iterator();
    Iterator<Component> components = generatedComponents().iterator();

    while (assets.hasNext() && components.hasNext()) {
      Asset asset = assets.next();
      Component component = components.next();
      List<BrowsePaths> paths = createBrowsePaths(asset.path().split("/"));

      run(() -> {
        underTest.createAssetNode(REPOSITORY_NAME, FORMAT_NAME, paths, asset);
        underTest.createComponentNode(REPOSITORY_NAME, FORMAT_NAME, paths, component);
      });
    }
  }

  private List<BrowsePaths> createBrowsePaths(final String... paths) {
    List<BrowsePaths> browsePaths = new ArrayList<>();
    List<String> pathsList = Arrays.asList(paths);
    for (int i = 0; i < paths.length; i++) {
      browsePaths.add(new BrowsePaths(paths[i], Joiner.on('/').join(pathsList.subList(0, i + 1))));
    }
    return browsePaths;
  }

  private <E> void run(final VoidOperation<RuntimeException> runnable) {
    Transactional.operation.withDb(() -> sessionRule.openSession("content")).run(runnable);
  }

  private <T> T call(final Operation<T, RuntimeException> supplier) {
    return Transactional.operation.withDb(() -> sessionRule.openSession("content")).call(supplier);
  }

  private boolean componentNodeExists(final Component component, final List<BrowsePaths> browsePaths) {
    String requestPath = browsePaths.get(browsePaths.size() - 2).getRequestPath();
    Iterable<BrowseNode<Integer>> iter =
        call(() -> underTest.getByPath(REPOSITORY_NAME, Arrays.asList(requestPath.split("/")), 100));

    Integer componentId = ((ComponentData) component).componentId;
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
          conn.prepareStatement("SELECT browse_node_id FROM test_browse_node WHERE repository_id = ? AND path = ?")) {
        statement.setInt(1, ((ContentRepositoryData) repository).repositoryId);
        statement.setString(2, path);
        try (ResultSet rs = statement.executeQuery()) {
          if (!rs.first()) {
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
          if (!rs.first()) {
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

  private static class TestDatastoreBrowseNodeStoreImpl
      extends DatastoreBrowseNodeStoreImpl<TestBrowseNodeDAO>
  {

    public TestDatastoreBrowseNodeStoreImpl(
        final DataSessionSupplier sessionSupplier,
        final SecurityHelper securityHelper,
        final SelectorManager selectorManager,
        final BrowseNodeConfiguration configuration,
        final RepositoryManager repositoryManager,
        final Map<String, ContentRepositoryStore<? extends ContentRepositoryDAO>> contentRepositoryStores,
        final Map<String, BrowseNodeFilter> browseNodeFilters,
        final Map<String, BrowseNodeComparator> browseNodeComparators)
    {
      super(sessionSupplier, securityHelper, selectorManager, configuration, repositoryManager, contentRepositoryStores,
          browseNodeFilters, browseNodeComparators);
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
    Integer assetId = Optional.ofNullable((AssetData) asset).map(a -> a.assetId).orElse(null);
    Integer componentId = Optional.ofNullable((ComponentData) component).map(c -> c.componentId).orElse(null);
    Integer repositoryId = ((ContentRepositoryData) contentRepository).repositoryId;

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
