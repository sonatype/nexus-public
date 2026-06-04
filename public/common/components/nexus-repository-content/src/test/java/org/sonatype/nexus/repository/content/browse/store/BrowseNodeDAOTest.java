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
package org.sonatype.nexus.repository.content.browse.store;

import java.util.List;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.app.VersionComparator;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.repository.browse.node.BrowseNode;
import org.sonatype.nexus.repository.browse.node.BrowseNodeComparator;
import org.sonatype.nexus.repository.browse.node.DefaultBrowseNodeComparator;
import org.sonatype.nexus.repository.content.browse.store.example.TestBrowseNodeDAO;
import org.sonatype.nexus.repository.content.store.AssetDAO;
import org.sonatype.nexus.repository.content.store.AssetData;
import org.sonatype.nexus.repository.content.store.ComponentDAO;
import org.sonatype.nexus.repository.content.store.ComponentData;
import org.sonatype.nexus.repository.content.store.ContentRepositoryDAO;
import org.sonatype.nexus.repository.content.store.ContentRepositoryData;
import org.sonatype.nexus.repository.content.store.ExampleContentTestSupport;
import org.sonatype.nexus.repository.content.store.example.TestAssetDAO;
import org.sonatype.nexus.repository.content.store.example.TestComponentDAO;
import org.sonatype.nexus.repository.content.store.example.TestContentRepositoryDAO;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.sonatype.nexus.testdb.DatabaseTest;

import static com.google.common.collect.ImmutableList.of;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.sonatype.nexus.common.property.SystemPropertiesHelper.getBoolean;
import static org.sonatype.nexus.common.property.SystemPropertiesHelper.getString;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.repository.content.store.InternalIds.internalAssetId;
import static org.sonatype.nexus.repository.content.store.InternalIds.internalComponentId;
import static org.sonatype.nexus.repository.content.store.InternalIds.toExternalId;

/**
 * Test {@link BrowseNodeDAO}.
 */
class BrowseNodeDAOTest
    extends ExampleContentTestSupport
{
  private static final BrowseNodeComparator byName = new DefaultBrowseNodeComparator(new VersionComparator());

  private ContentRepositoryData contentRepository;

  private ComponentData component1;

  private ComponentData component2;

  private AssetData asset1;

  private AssetData asset2;

  private BrowseNodeData alpha;

  private BrowseNodeData beta;

  private BrowseNodeData betaTwo;

  private BrowseNodeData betaThree;

  private BrowseNodeData gamma;

  private BrowseNodeData gammaOne;

  private BrowseNodeData gammaOneAlpha;

  @BeforeEach
  void setupContent() {
    sessionRule.register(TestBrowseNodeDAO.class);
    contentRepository = generateContentRepository();

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ContentRepositoryDAO dao = session.access(TestContentRepositoryDAO.class);
      dao.createContentRepository(contentRepository);
      session.getTransaction().commit();
    }

    generateNamespaces(100);
    generateNames(100);
    generateVersions(100);
    generatePaths(100);

    component1 = generateComponent(1, "namespace1", "name1", "1.0.0");
    component2 = generateComponent(1, "namespace2", "name2", "2.0.0");
    component2.setVersion(component1.version() + "-2");

    asset1 = generateAsset(1, "/asset1/asset1.jar");
    asset2 = generateAsset(1, "/asset2/asset2.jar");
    asset2.setPath(asset1.path() + "/2");

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ComponentDAO componentDao = session.access(TestComponentDAO.class);
      AssetDAO assetDao = session.access(TestAssetDAO.class);

      componentDao.createComponent(component1, false);
      componentDao.createComponent(component2, false);

      assetDao.createAsset(asset1, false);
      assetDao.createAsset(asset2, false);

      session.getTransaction().commit();
    }

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      BrowseNodeDAO dao = session.access(TestBrowseNodeDAO.class);

      alpha = createNode(null, "alpha", "/a");
      dao.mergeBrowseNode(alpha);

      gamma = createNode(null, "gamma", "/g/");
      dao.mergeBrowseNode(gamma);

      beta = createNode(null, "beta", "/b/");
      dao.mergeBrowseNode(beta);

      gammaOne = createNode(gamma, "one", "/g/1/");
      gammaOne.dbComponentId = internalComponentId(component1);
      dao.mergeBrowseNode(gammaOne);

      betaTwo = createNode(beta, "two", "/b/2");
      betaTwo.dbComponentId = internalComponentId(component2);
      dao.mergeBrowseNode(betaTwo);

      gammaOneAlpha = createNode(gammaOne, "alpha", "/g/1/a");
      gammaOneAlpha.dbAssetId = internalAssetId(asset1);
      dao.mergeBrowseNode(gammaOneAlpha);

      betaThree = createNode(beta, "three", "/b/3");
      betaThree.dbAssetId = internalAssetId(asset2);
      dao.mergeBrowseNode(betaThree);

      session.getTransaction().commit();
    }
  }

  @DatabaseTest
  void testPlainBrowsing() {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      BrowseNodeDAO dao = session.access(TestBrowseNodeDAO.class);

      List<BrowseNode> listing;

      listing = getListing(dao);
      assertThat(listing.size(), is(3));
      assertThat(listing.get(0).isLeaf(), is(true));
      assertThat(listing.get(1).isLeaf(), is(false));
      assertThat(listing.get(2).isLeaf(), is(false));
      assertThat(listing.get(0), sameNode(alpha));
      assertThat(listing.get(1), sameNode(beta));
      assertThat(listing.get(2), sameNode(gamma));

      listing = getListing(dao, "alpha");
      assertThat(listing.size(), is(0));

      listing = getListing(dao, "beta");
      assertThat(listing.size(), is(2));
      assertThat(listing.get(0).isLeaf(), is(true));
      assertThat(listing.get(1).isLeaf(), is(true));
      assertThat(listing.get(0), sameNode(betaTwo));
      assertThat(listing.get(1), sameNode(betaThree));

      listing = getListing(dao, "beta", "two");
      assertThat(listing.size(), is(0));

      listing = getListing(dao, "beta", "three");
      assertThat(listing.size(), is(0));

      listing = getListing(dao, "gamma");
      assertThat(listing.size(), is(1));
      assertThat(listing.get(0).isLeaf(), is(false));
      assertThat(listing.get(0), sameNode(gammaOne));

      listing = getListing(dao, "gamma", "one");
      assertThat(listing.size(), is(1));
      assertThat(listing.get(0).isLeaf(), is(true));
      assertThat(listing.get(0), sameNode(gammaOneAlpha));

      listing = getListing(dao, "gamma", "one", "alpha");
      assertThat(listing.size(), is(0));
    }
  }

  @DatabaseTest
  void testFilteredBrowsing() {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      BrowseNodeDAO dao = session.access(TestBrowseNodeDAO.class);

      List<BrowseNode> listing;

      listing = filterListing(dao);
      assertThat(listing.size(), is(2));
      assertThat(listing.get(0).isLeaf(), is(false));
      assertThat(listing.get(1).isLeaf(), is(false));
      assertThat(listing.get(0), sameNode(beta));
      assertThat(listing.get(1), sameNode(gamma));

      listing = filterListing(dao, "alpha");
      assertThat(listing.size(), is(0));

      listing = filterListing(dao, "beta");
      assertThat(listing.size(), is(0));

      listing = filterListing(dao, "beta", "two");
      assertThat(listing.size(), is(0));

      listing = filterListing(dao, "beta", "three");
      assertThat(listing.size(), is(0));

      listing = filterListing(dao, "gamma");
      assertThat(listing.size(), is(1));
      assertThat(listing.get(0).isLeaf(), is(false));
      assertThat(listing.get(0), sameNode(gammaOne));

      listing = filterListing(dao, "gamma", "one");
      assertThat(listing.size(), is(0));

      listing = filterListing(dao, "gamma", "one", "alpha");
      assertThat(listing.size(), is(0));
    }
  }

  @DatabaseTest
  void testComponentAssetDeletesNullify() {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      BrowseNodeDAO dao = session.access(TestBrowseNodeDAO.class);

      List<BrowseNode> listing;

      listing = getListing(dao, "beta");
      assertThat(listing.get(0).getComponentId(), is(toExternalId(internalComponentId(component2))));
      assertThat(listing.get(1).getAssetId(), is(toExternalId(internalAssetId(asset2))));

      listing = getListing(dao, "gamma");
      assertThat(listing.get(0).getComponentId(), is(toExternalId(internalComponentId(component1))));

      listing = getListing(dao, "gamma", "one");
      assertThat(listing.get(0).getAssetId(), is(toExternalId(internalAssetId(asset1))));
    }

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      session.access(TestAssetDAO.class).deleteAsset(asset2);
      session.getTransaction().commit();
    }

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      BrowseNodeDAO dao = session.access(TestBrowseNodeDAO.class);

      List<BrowseNode> listing;

      listing = getListing(dao, "beta");
      assertThat(listing.get(0).getComponentId(), is(toExternalId(internalComponentId(component2))));
      assertThat(listing.get(1).getAssetId(), is(nullValue()));

      listing = getListing(dao, "gamma");
      assertThat(listing.get(0).getComponentId(), is(toExternalId(internalComponentId(component1))));

      listing = getListing(dao, "gamma", "one");
      assertThat(listing.get(0).getAssetId(), is(toExternalId(internalAssetId(asset1))));
    }

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      session.access(TestComponentDAO.class).deleteComponent(component2);
      session.getTransaction().commit();
    }

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      BrowseNodeDAO dao = session.access(TestBrowseNodeDAO.class);

      List<BrowseNode> listing;

      listing = getListing(dao, "beta");
      assertThat(listing.get(0).getComponentId(), is(nullValue()));
      assertThat(listing.get(1).getAssetId(), is(nullValue()));

      listing = getListing(dao, "gamma");
      assertThat(listing.get(0).getComponentId(), is(toExternalId(internalComponentId(component1))));

      listing = getListing(dao, "gamma", "one");
      assertThat(listing.get(0).getAssetId(), is(toExternalId(internalAssetId(asset1))));
    }
  }

  @DatabaseTest
  void testFilterClauseIsolation() {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      BrowseNodeDAO dao = session.access(TestBrowseNodeDAO.class);

      List<BrowseNode> listing = dao.getByDisplayPath(1, of("gamma"), 100, "true or true", null);
      assertThat(listing, hasSize(1));
      assertThat(listing.get(0).getPath(), equalTo("/g/1/"));
    }
  }

  @DatabaseTest
  void testGetByRequestPath() {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      BrowseNodeDAO dao = session.access(TestBrowseNodeDAO.class);

      List<BrowseNode> nodes = dao.getByRequestPath(1, "/g/1/a");
      assertThat(nodes.isEmpty(), is(false));
      assertThat(nodes.get(0).getPath(), equalTo("/g/1/a"));
    }
  }

  @DatabaseTest
  void testGetMaxNodeId() {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      BrowseNodeDAO dao = session.access(TestBrowseNodeDAO.class);

      // Test with existing repository
      Long maxNodeId = dao.getMaxNodeId(1);
      assertThat(maxNodeId, is(greaterThan(0L)));

      // Test with non-existent repository
      Long emptyMaxNodeId = dao.getMaxNodeId(999);
      assertThat(emptyMaxNodeId, is(nullValue()));
    }
  }

  @DatabaseTest
  void testDeleteBrowseNodesByIdRange() {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      BrowseNodeDAO dao = session.access(TestBrowseNodeDAO.class);

      // Get the current max node ID to determine our range
      Long maxNodeId = dao.getMaxNodeId(1);
      assertThat(maxNodeId, is(greaterThan(0L)));

      // Verify we have nodes before deletion
      List<BrowseNode> listingBefore = getListing(dao);
      assertThat(listingBefore.size(), is(greaterThan(0)));

      // Delete a range that includes some nodes
      int deletedCount = dao.deleteBrowseNodesByIdRange(1, 1, maxNodeId);
      assertThat(deletedCount, is(greaterThan(0)));

      // Verify nodes are deleted
      List<BrowseNode> listingAfter = getListing(dao);
      assertThat(listingAfter.size(), is(0));

      session.getTransaction().commit();
    }
  }

  @DatabaseTest
  void testDeleteBrowseNodesByIdRangeEmptyRange() {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      BrowseNodeDAO dao = session.access(TestBrowseNodeDAO.class);

      // Delete a range with no matching nodes
      int deletedCount = dao.deleteBrowseNodesByIdRange(1, 999999, 999999);
      assertThat(deletedCount, is(0));

      // Verify original nodes are still there
      List<BrowseNode> listing = getListing(dao);
      assertThat(listing.size(), is(3));

      session.getTransaction().commit();
    }
  }

  @DatabaseTest
  void testDeleteBrowseNodesByIdRangeNonExistentRepository() {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      BrowseNodeDAO dao = session.access(TestBrowseNodeDAO.class);

      // Delete from non-existent repository
      int deletedCount = dao.deleteBrowseNodesByIdRange(999, 1, 1000);
      assertThat(deletedCount, is(0));

      session.getTransaction().commit();
    }
  }

  @DatabaseTest
  void testTrimBrowseNodes_removesOrphanedDirectoriesWithComponentId() {
    // NEXUS-45497: Test that trimBrowseNodes removes orphaned directory nodes even when they have component_id set
    // This simulates the scenario where duplicate RPMs are uploaded to different paths and one is deleted

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      BrowseNodeDAO dao = session.access(TestBrowseNodeDAO.class);

      // Create a directory node that has component_id set but no asset_id and no children
      // This represents an orphaned directory after an asset deletion
      BrowseNodeData orphanedDir = createNode(null, "orphaned", "/orphaned/");
      orphanedDir.dbComponentId = internalComponentId(component1); // Set component_id
      dao.mergeBrowseNode(orphanedDir);

      // Create another directory node without component_id or asset_id (standard orphaned directory)
      BrowseNodeData orphanedDir2 = createNode(null, "orphaned2", "/orphaned2/");
      dao.mergeBrowseNode(orphanedDir2);

      session.getTransaction().commit();
    }

    // Verify both orphaned directories exist
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      BrowseNodeDAO dao = session.access(TestBrowseNodeDAO.class);

      List<BrowseNode> listing = getListing(dao);
      assertThat(listing.size(), is(5)); // original 3 + 2 new orphaned directories

      // Find the orphaned directories in the listing
      boolean foundOrphaned1 = listing.stream().anyMatch(node -> node.getName().equals("orphaned"));
      boolean foundOrphaned2 = listing.stream().anyMatch(node -> node.getName().equals("orphaned2"));
      assertThat(foundOrphaned1, is(true));
      assertThat(foundOrphaned2, is(true));
    }

    // Run trimBrowseNodes to clean up orphaned directories
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      BrowseNodeDAO dao = session.access(TestBrowseNodeDAO.class);

      // trimBrowseNodes returns true if any nodes were deleted, false otherwise
      // Run it in a loop until no more orphaned nodes are found
      int trimCount = 0;
      while (dao.trimBrowseNodes(1)) {
        trimCount++;
      }

      // Should have run at least once (to delete the orphaned directories)
      assertThat(trimCount, is(greaterThan(0)));

      session.getTransaction().commit();
    }

    // Verify orphaned directories are removed but other nodes remain
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      BrowseNodeDAO dao = session.access(TestBrowseNodeDAO.class);

      List<BrowseNode> listing = getListing(dao);

      // Note: "alpha" is also an orphaned node (no asset_id, no component_id, no children)
      // so it will be cleaned up along with the two orphaned directories we created
      // Only beta and gamma directories should remain at the root level
      assertThat(listing.size(), is(2)); // beta and gamma

      // Verify orphaned directories are gone
      boolean foundOrphaned1 = listing.stream().anyMatch(node -> node.getName().equals("orphaned"));
      boolean foundOrphaned2 = listing.stream().anyMatch(node -> node.getName().equals("orphaned2"));
      boolean foundAlpha = listing.stream().anyMatch(node -> node.getName().equals("alpha"));
      assertThat(foundOrphaned1, is(false));
      assertThat(foundOrphaned2, is(false));
      assertThat(foundAlpha, is(false)); // alpha was also orphaned

      // Verify beta and gamma still exist
      assertThat(listing.get(0), sameNode(beta));
      assertThat(listing.get(1), sameNode(gamma));
    }
  }

  @DatabaseTest
  void testTrimBrowseNodes_preservesDirectoriesWithChildren() {
    // Test that trimBrowseNodes does NOT remove directory nodes that have children
    // but DOES remove orphaned nodes like alpha (no asset, no children) and betaTwo (component-only node)

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      BrowseNodeDAO dao = session.access(TestBrowseNodeDAO.class);

      // Run trimBrowseNodes - will delete orphaned nodes
      boolean deletedAny = dao.trimBrowseNodes(1);

      // Should delete orphaned nodes (alpha has no asset/children, betaTwo has component but no asset/children)
      assertThat(deletedAny, is(true));

      session.getTransaction().commit();
    }

    // Verify directories with children are preserved, but orphaned nodes are gone
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      BrowseNodeDAO dao = session.access(TestBrowseNodeDAO.class);

      List<BrowseNode> listing = getListing(dao);
      // beta and gamma should remain (they have children)
      assertThat(listing.size(), is(2));
      assertThat(listing.get(0), sameNode(beta));
      assertThat(listing.get(1), sameNode(gamma));

      // Verify beta still has its child with asset
      List<BrowseNode> betaChildren = getListing(dao, "beta");
      assertThat(betaChildren.size(), is(1)); // only betaThree (with asset) should remain
      assertThat(betaChildren.get(0), sameNode(betaThree));
    }
  }

  @DatabaseTest
  void testTrimBrowseNodes_preservesNodesWithAssets() {
    // Test that trimBrowseNodes does NOT remove nodes that have asset_id set
    // We'll use the existing betaThree node which already has an asset

    // First, let's clean up any orphaned nodes from the initial setup
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      BrowseNodeDAO dao = session.access(TestBrowseNodeDAO.class);
      // Run trim to clean up orphaned nodes (alpha and betaTwo)
      while (dao.trimBrowseNodes(1)) {
        // Continue until no more nodes are deleted
      }
      session.getTransaction().commit();
    }

    // Verify that nodes with assets are preserved
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      BrowseNodeDAO dao = session.access(TestBrowseNodeDAO.class);

      // Check that betaThree (which has asset2) still exists
      List<BrowseNode> betaChildren = getListing(dao, "beta");
      assertThat(betaChildren.size(), is(1)); // only betaThree should remain (betaTwo was trimmed)
      assertThat(betaChildren.get(0), sameNode(betaThree));

      // Check that gammaOneAlpha (which has asset1) still exists
      List<BrowseNode> gammaOneChildren = getListing(dao, "gamma", "one");
      assertThat(gammaOneChildren.size(), is(1));
      assertThat(gammaOneChildren.get(0), sameNode(gammaOneAlpha));
    }

    // Run trimBrowseNodes again - should not delete anything since all remaining nodes
    // either have assets or have children
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      BrowseNodeDAO dao = session.access(TestBrowseNodeDAO.class);
      boolean deletedAny = dao.trimBrowseNodes(1);

      // Should not delete any more nodes
      assertThat(deletedAny, is(false));

      session.getTransaction().commit();
    }
  }

  private static List<BrowseNode> getListing(final BrowseNodeDAO dao, final String... paths) {
    List<BrowseNode> listing = dao.getByDisplayPath(1, asList(paths), 100, null, null);
    listing.sort(byName);
    return listing;
  }

  private static List<BrowseNode> filterListing(final BrowseNodeDAO dao, final String... paths) {
    // select any nodes whose request path ends in a slash
    List<BrowseNode> listing = dao.getByDisplayPath(1, asList(paths), 100,
        "B.request_path ~ #{filterParams.regex}", ImmutableMap.of("regex", "^(.*/)$"));
    listing.sort(byName);
    return listing;
  }

  private static Matcher<BrowseNode> sameNode(final BrowseNode expected) {
    return allOf(new FieldMatcher<BrowseNode>(expected, BrowseNode::getName),
        new FieldMatcher<BrowseNode>(expected, BrowseNode::getPath),
        new FieldMatcher<BrowseNode>(expected, BrowseNode::getComponentId),
        new FieldMatcher<BrowseNode>(expected, BrowseNode::getAssetId));
  }

  private static BrowseNodeData createNode(
      @Nullable final BrowseNodeData parent,
      final String displayName,
      final String requestPath)
  {
    BrowseNodeData node = new BrowseNodeData();
    node.setRepositoryId(1);
    node.setDisplayName(displayName);
    node.setRequestPath(requestPath);
    if (parent != null) {
      node.setParentId(parent.nodeId);
    }
    return node;
  }

  private static boolean isPostgreSQL() {
    return getBoolean("test.postgres", false) ||
        getString("test.jdbcUrl", "").contains("postgresql");
  }
}
