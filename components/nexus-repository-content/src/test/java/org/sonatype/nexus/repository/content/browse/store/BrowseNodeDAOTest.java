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
import org.junit.Before;
import org.junit.Test;

import static com.google.common.collect.ImmutableList.of;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;
import static org.sonatype.nexus.common.property.SystemPropertiesHelper.getBoolean;
import static org.sonatype.nexus.common.property.SystemPropertiesHelper.getString;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.repository.content.store.InternalIds.internalAssetId;
import static org.sonatype.nexus.repository.content.store.InternalIds.internalComponentId;
import static org.sonatype.nexus.repository.content.store.InternalIds.toExternalId;

/**
 * Test {@link BrowseNodeDAO}.
 */
public class BrowseNodeDAOTest
    extends ExampleContentTestSupport
{
  public BrowseNodeDAOTest() {
    super(TestBrowseNodeDAO.class);
  }

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

  @Before
  public void setupContent() {
    contentRepository = randomContentRepository();

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      ContentRepositoryDAO dao = session.access(TestContentRepositoryDAO.class);
      dao.createContentRepository(contentRepository);
      session.getTransaction().commit();
    }

    generateRandomNamespaces(100);
    generateRandomNames(100);
    generateRandomVersions(100);
    generateRandomPaths(100);

    component1 = randomComponent(1);
    component2 = randomComponent(1);
    component2.setVersion(component1.version() + "-2");

    asset1 = randomAsset(1);
    asset2 = randomAsset(1);
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

  @Test
  public void testPlainBrowsing() {
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

  @Test
  public void testFilteredBrowsing() {
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

  @Test
  public void testComponentAssetDeletesNullify() {
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

  @Test
  public void testRepositoryDeleteCascades() {
    assumeFalse(isPostgreSQL());
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
      session.access(TestAssetDAO.class).deleteAsset(asset1);
      session.access(TestAssetDAO.class).deleteAsset(asset2);
      session.access(TestComponentDAO.class).deleteComponent(component1);
      session.access(TestComponentDAO.class).deleteComponent(component2);
      session.access(TestContentRepositoryDAO.class).deleteContentRepository(contentRepository);
      session.getTransaction().commit();
    }

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      BrowseNodeDAO dao = session.access(TestBrowseNodeDAO.class);

      assertThat(getListing(dao), is(empty()));
    }
  }

  @Test
  public void testFilterClauseIsolation() {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      BrowseNodeDAO dao = session.access(TestBrowseNodeDAO.class);

      List<BrowseNode> listing = dao.getByDisplayPath(1, of("gamma"), 100, "true or true", null);
      assertThat(listing, hasSize(1));
      assertThat(listing.get(0).getPath(), equalTo("/g/1/"));
    }
  }

  @Test
  public void testGetByRequestPath() {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      BrowseNodeDAO dao = session.access(TestBrowseNodeDAO.class);

      List<BrowseNode> nodes = dao.getByRequestPath(1, "/g/1/a");
      assertThat(nodes.isEmpty(), is(false));
      assertThat(nodes.get(0).getPath(), equalTo("/g/1/a"));
    }
  }

  @Test
  public void testDeleteByAssetIdAndPath() {
    assumeTrue(isPostgreSQL());
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      BrowseNodeDAO dao = session.access(TestBrowseNodeDAO.class);
      Long deleted = dao.deleteByAssetIdAndPath(internalAssetId(asset1), "/g/1/a");
      assertThat(deleted, greaterThan(0L));

      List<BrowseNode> nodes = dao.getByRequestPath(1, "/g/1/a");
      assertThat(nodes.isEmpty(), is(true));
    }
  }

  @Test
  public void testGetNodeParents() {
    assumeTrue(isPostgreSQL());
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      BrowseNodeDAO dao = session.access(TestBrowseNodeDAO.class);
      List<BrowseNode> nodes = dao.getByRequestPath(1, "/g/1/a");
      BrowseNodeData nodeData = (BrowseNodeData) nodes.get(0);
      List<BrowseNode> nodeParents = dao.getNodeParents(nodeData.getNodeId());
      assertThat(nodeParents.size(), is(3));
      assertThat(nodeParents.get(0).getPath(), equalTo("/g/1/a"));
      assertThat(nodeParents.get(1).getPath(), equalTo("/g/1/"));
      assertThat(nodeParents.get(2).getPath(), equalTo("/g/"));
    }
  }

  private List<BrowseNode> getListing(final BrowseNodeDAO dao, final String... paths) {
    List<BrowseNode> listing = dao.getByDisplayPath(1, asList(paths), 100, null, null);
    listing.sort(byName);
    return listing;
  }

  private List<BrowseNode> filterListing(final BrowseNodeDAO dao, final String... paths) {
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

  private BrowseNodeData createNode(
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

  private boolean isPostgreSQL() {
    return getBoolean("test.postgres", false) ||
        getString("test.jdbcUrl", "").contains("postgresql");
  }
}
