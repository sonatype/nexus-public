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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.ContentRepository;
import org.sonatype.nexus.repository.content.store.ExampleContentTestSupport;
import org.sonatype.nexus.repository.content.store.InternalIds;

import com.google.common.collect.Iterables;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.repository.content.browse.internal.BrowseNodeDAOQueryBuilder.WHERE_PARAMS;

public class BrowseNodeDAOTest
    extends ExampleContentTestSupport
{
  private ContentRepository contentRepository;

  private String databaseId;

  public BrowseNodeDAOTest() {
    super(TestBrowseNodeDAO.class);
  }

  @Before
  public void setup() throws Exception {
    generateRandomRepositories(1);
    generateRandomNamespaces(1);
    generateRandomNames(1);
    generateRandomPaths(2);
    generateRandomVersions(1);
    generateRandomContent(1, 2);

    contentRepository = generatedRepositories().get(0);

    try (Connection connection = sessionRule.openConnection("content")) {
      databaseId = connection.getMetaData().getDatabaseProductName();
    }
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

  @Test
  public void testAssetNodeExists() {
    Asset asset1 = generatedAssets().get(0);
    createNode("foo", "foo", asset1, null);

    assertTrue(call(dao -> dao.assetNodeExists(asset1)));

    Asset asset2 = generatedAssets().get(1);
    assertFalse(call(dao -> dao.assetNodeExists(asset2)));
  }

  @Test
  public void testcreateNode() {
    createNode(createNode(null, "foo", "foo"), "foo/bar", "bar");

    run(dao -> {
      Optional<DatastoreBrowseNode> node = dao.findPath(contentRepository, "foo");
      assertTrue(node.isPresent());
      assertThat(node.get(), isBrowseNodeWith(contentRepository, null, "foo", "foo", null, null));

      int parentId = node.get().getId();
      node = dao.findPath(contentRepository, "foo/bar");
      assertTrue(node.isPresent());
      assertThat(node.get(), isBrowseNodeWith(contentRepository, parentId, "foo/bar", "bar", null, null));
    });
  }

  @Test
  public void testDeleteBrowseNode() {
    final int browseNode = createNode("foo", "foo", null, null);

    run(dao -> dao.deleteBrowseNode(browseNode));

    assertFalse(call(dao -> dao.findPath(contentRepository, "foo")).isPresent());
  }

  @Test
  public void testDeleteRepository() {
    createNode(createNode(null, "foo", "foo"), "foo/bar", "bar");
    int deletedNodes = call(dao -> dao.deleteRepository(contentRepository, 1));
    assertThat(deletedNodes, is(1));

    deletedNodes = call(dao -> dao.deleteRepository(contentRepository, 1));
    assertThat(deletedNodes, is(1));

    deletedNodes = call(dao -> dao.deleteRepository(contentRepository, 1));
    assertThat(deletedNodes, is(0));
  }

  @Test
  public void testFindChildren() {
    final int parentId = createNode(null, "foo", "foo");
    createNode(parentId, "foo/bar", "bar");
    createNode(parentId, "foo/foo", "foo");

    DatastoreBrowseNode[] children =
        Iterables.toArray(call(dao -> dao.findChildren(contentRepository, "foo", 100, null, null)), DatastoreBrowseNode.class);
    assertThat(children, arrayWithSize(2));

    assertThat(children,
        arrayContainingInAnyOrder(
            Arrays.asList(isBrowseNodeWith(contentRepository, parentId, "foo/foo", "foo", null, null),
                isBrowseNodeWith(contentRepository, parentId, "foo/bar", "bar", null, null))));
  }

  @Test
  public void testFindChildren_atRoot() {
    createNode(null, "bar", "bar");
    createNode(null, "foo", "foo");

    DatastoreBrowseNode[] children =
        Iterables.toArray(call(dao -> dao.findChildren(contentRepository, null, 100, null, null)), DatastoreBrowseNode.class);
    assertThat(children, arrayWithSize(2));

    assertThat(children,
        arrayContainingInAnyOrder(
            Arrays.asList(isBrowseNodeWith(contentRepository, null, "foo", "foo", null, null),
                isBrowseNodeWith(contentRepository, null, "bar", "bar", null, null))));
  }


  @Test
  public void testFindChildren_withWhereClause() {
    final int dogParent = createNode(null, "dog", "woof");
    createNode(dogParent, "dog/woof", "woof");
    createNode(dogParent, "dog/bark", "bark");
    createNode(dogParent, "dog/ruff", "ruff");

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("regex", "'.*f$'");

    List<DatastoreBrowseNode> children = stream(dao -> dao
        .findChildren(contentRepository, "dog", 100,
            asList(" path " + getRegexOperator() + " ${" + WHERE_PARAMS + ".regex}"),
            parameters))
        .collect(toList());

    Matcher<DatastoreBrowseNode> woofDog = isBrowseNodeWith(contentRepository, dogParent, "dog/woof", "woof", null, null);
    Matcher<DatastoreBrowseNode> ruffDog = isBrowseNodeWith(contentRepository, dogParent, "dog/ruff", "ruff", null, null);

    assertThat(children.size(), equalTo(2));
    assertThat(children, containsInAnyOrder(woofDog, ruffDog));
  }

  @Test
  public void testFindDeepestNode() {
    createNode(null, "foo", "foo");
    createNode(null, "foo/1", "1");
    createNode(null, "foo/1/2", "2");
    createNode(null, "foo/1/2/3", "3");
    createNode(null, "foo/1/2/3/4", "4");

    Optional<DatastoreBrowseNode> node = call(dao -> dao.findDeepestNode(contentRepository,
        "foo", "foo/1", "foo/1/2", "foo/1/2/3", "foo/1/2/3/4"));

    assertTrue(node.isPresent());
    assertThat(node.get(), isBrowseNodeWith(contentRepository, null, "foo/1/2/3/4", "4", null, null));
  }

  @Test
  public void testFindPath() {
    createNode(null, "foo", "foo");
    createNode(null, "foo/1", "1");

    Optional<DatastoreBrowseNode> node = call(dao -> dao.findPath(contentRepository, "foo/1"));

    assertTrue(node.isPresent());
    assertThat(node.get(), isBrowseNodeWith(contentRepository, null, "foo/1", "1", null, null));
  }

  @Test
  public void testFindBrowseNodeIdByAssetId() {
    Asset asset = generatedAssets().get(0);
    createNode("foo", "foo", asset, null);

    Optional<DatastoreBrowseNode> node = call(dao -> dao.findBrowseNodeByAssetId(asset));

    assertTrue(node.isPresent());
    assertThat(node.get(), isBrowseNodeWith(contentRepository, null, "foo", "foo", asset, null));
  }

  @Test
  public void testFindBrowseNodeIdByComponentId() {
    Component component = generatedComponents().get(0);
    createNode("foo", "foo", null, component);

    Optional<DatastoreBrowseNode> node = call(dao -> dao.findBrowseNodeByComponentId(component));

    assertTrue(node.isPresent());
    assertThat(node.get(), isBrowseNodeWith(contentRepository, null, "foo", "foo", null, component));
  }

  @Test
  public void testGetParentBrowseNodeId() {
    int actualParentId = createNode(null, "foo", "foo");
    int actualChildId = createNode(actualParentId, "foo/bar", "bar");

    Optional<Integer> parentId = call(dao -> dao.getParentBrowseNodeId(actualParentId));
    assertFalse(parentId.isPresent());

    parentId = call(dao -> dao.getParentBrowseNodeId(actualChildId));
    assertTrue(parentId.isPresent());
    assertThat(parentId.get(), is(actualParentId));
  }

  @Test
  public void testLinkAsset() {
    final Asset asset = generatedAssets().get(0);
    final int nodeId = createNode("foo", "foo", null, null);

    run(dao -> dao.linkAsset(nodeId, asset));

    DatastoreBrowseNode node = call(dao -> dao.findPath(contentRepository, "foo")).get();
    assertThat(node, isBrowseNodeWith(contentRepository, null, "foo", "foo", asset, null));
  }

  @Test
  public void testLinkComponent() {
    Component component = generatedComponents().get(0);
    final int nodeId = createNode("foo", "foo", null, null);

    run(dao -> dao.linkComponent(nodeId, component));

    DatastoreBrowseNode node = call(dao -> dao.findPath(contentRepository, "foo")).get();
    assertThat(node, isBrowseNodeWith(contentRepository, null, "foo", "foo", null, component));
  }

  @Test
  public void testMaybeDeleteAssetNode() {
    Asset asset = generatedAssets().get(0);
    Component component = generatedComponents().get(0);

    createNode("foo", "foo", asset, null);
    assertTrue(call(dao -> dao.maybeDeleteAssetNode(asset)));
    assertFalse(call(dao -> dao.findPath(contentRepository, "foo")).isPresent());

    createNode("foo", "foo", asset, component);
    assertFalse(call(dao -> dao.maybeDeleteAssetNode(asset)));
    assertTrue(call(dao -> dao.findPath(contentRepository, "foo")).isPresent());
  }

  @Test
  public void testMaybeDeleteComponentNode() {
    final Asset asset = generatedAssets().get(0);
    Component component = generatedComponents().get(0);

    createNode("foo", "foo", null, component);
    assertTrue(call(dao -> dao.maybeDeleteComponentNode(component)));
    assertFalse(call(dao -> dao.findPath(contentRepository, "foo")).isPresent());

    createNode("foo", "foo", asset, component);
    assertFalse(call(dao -> dao.maybeDeleteComponentNode(component)));
    assertTrue(call(dao -> dao.findPath(contentRepository, "foo")).isPresent());
  }

  @Test
  public void testMaybeDeleteNode() {
    int parentId = createNode(null, "foo", "foo");
    createNode(parentId, "foo/bar", "bar");

    // Can't delete a node with a child
    assertFalse(call(dao -> dao.maybeDeleteNode(contentRepository, "foo")));
    // Can delete a leaf
    assertTrue(call(dao -> dao.maybeDeleteNode(contentRepository, "foo/bar")));
  }

  @Test
  public void testUnlinkAsset_withChildren() {
    Asset asset = generatedAssets().get(0);
    int nodeId = createNode("foo", "foo", asset, null);
    createNode(nodeId, "foo/bar", "bar");

    assertTrue(call(dao -> dao.unlinkAsset(asset)));
  }

  @Test
  public void testUnlinkAsset_withComponent() {
    Asset asset = generatedAssets().get(0);
    Component component = generatedComponents().get(0);
    createNode("foo", "foo", asset, component);

    assertTrue(call(dao -> dao.unlinkAsset(asset)));
  }

  @Test
  public void testUnlinkAsset_withoutComponentOrChildren() {
    Asset asset = generatedAssets().get(0);
    createNode("foo", "foo", asset, null);

    assertFalse(call(dao -> dao.unlinkAsset(asset)));
  }

  @Test
  public void testUnlinkComponent_withAsset() {
    Asset asset = generatedAssets().get(0);
    Component component = generatedComponents().get(0);
    createNode("foo", "foo", asset, component);

    assertTrue(call(dao -> dao.unlinkComponent(component)));
  }

  @Test
  public void testUnlinkComponent_withChildren() {
    Component component = generatedComponents().get(0);
    int nodeId = createNode("foo", "foo", null, component);
    createNode(nodeId, "foo/bar", "bar");

    assertTrue(call(dao -> dao.unlinkComponent(component)));
  }

  @Test
  public void testUnlinkComponent_withoutAssetOrChildren() {
    Component component = generatedComponents().get(0);
    createNode("foo", "foo", null, component);

    assertFalse(call(dao -> dao.unlinkComponent(component)));
  }

  private int createNode(final String path, final String name, final Asset asset, final Component component) {
    final int browseNodeId = createNode(null, path, name);
    if (asset != null) {
      run(dao -> dao.linkAsset(browseNodeId, asset));
    }
    if (component != null) {
      run(dao -> dao.linkComponent(browseNodeId, component));
    }
    return browseNodeId;
  }

  private int createNode(final Integer parentId, final String path, final String name) {
    DatastoreBrowseNode node = new DatastoreBrowseNode("maven2", parentId, path, name);
    run(dao -> dao.createNode(contentRepository, node));
    return node.getId();
  }

  private <T> Stream<T> stream(final Function<BrowseNodeDAO, Iterable<T>> fn) {
    try (DataSession<?> session = sessionRule.openSession("content")) {
      BrowseNodeDAO dao = session.access(TestBrowseNodeDAO.class);
      Iterable<T> retVal = fn.apply(dao);
      session.getTransaction().commit();
      return StreamSupport.stream(retVal.spliterator(), false);
    }
  }

  private <T> T call(final Function<BrowseNodeDAO, T> fn) {
    try (DataSession<?> session = sessionRule.openSession("content")) {
      BrowseNodeDAO dao = session.access(TestBrowseNodeDAO.class);
      T retVal = fn.apply(dao);
      session.getTransaction().commit();
      return retVal;
    }
  }

  private void run(final Consumer<BrowseNodeDAO> fn) {
    try (DataSession<?> session = sessionRule.openSession("content")) {
      BrowseNodeDAO dao = session.access(TestBrowseNodeDAO.class);
      fn.accept(dao);
      session.getTransaction().commit();
    }
  }

  private static Matcher<DatastoreBrowseNode> isBrowseNodeWith(
      final ContentRepository contentRepository,
      final Integer parentId,
      final String path,
      final String name,
      final Asset asset,
      final Component component)
  {
    Integer assetId = ofNullable(asset).map(InternalIds::internalAssetId).orElse(null);
    Integer componentId = ofNullable(component).map(InternalIds::internalComponentId).orElse(null);
    Integer repositoryId = contentRepository.contentRepositoryId();

    return new TypeSafeMatcher<DatastoreBrowseNode>()
    {
      @Override
      public void describeTo(final Description description) {
        description.appendText("repositoryId=" + repositoryId + " parentId=" + parentId + " path=" + path + " name="
            + name + " assetId=" + assetId + " componentId=" + componentId);
      }

      @Override
      protected boolean matchesSafely(final DatastoreBrowseNode item) {
        return Objects.equals(parentId, item.getParentId())
            && Objects.equals(repositoryId, item.getRepositoryId())
            && Objects.equals(assetId, item.getAssetId())
            && Objects.equals(componentId, item.getComponentId())
            && Objects.equals(path, item.getPath())
            && Objects.equals(name, item.getName());
      }
    };
  }
}
