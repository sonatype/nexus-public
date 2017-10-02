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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.orient.entity.AttachedEntityId;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;
import org.sonatype.nexus.orient.transaction.OrientOperations;
import org.sonatype.nexus.repository.browse.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.security.RepositoryViewPermission;
import org.sonatype.nexus.repository.selector.internal.ContentAuth;
import org.sonatype.nexus.repository.storage.internal.BrowseNodeSqlBuilder;
import org.sonatype.nexus.security.BreadActions;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.selector.CselAssetSqlBuilder;
import org.sonatype.nexus.selector.CselSelector;
import org.sonatype.nexus.selector.JexlSelector;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorManager;

import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.OSQLEngine;
import com.orientechnologies.orient.core.sql.query.OConcurrentResultSet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.orient.transaction.OrientTransactional.inTx;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES;
import static org.sonatype.nexus.repository.storage.StorageTestUtil.createAsset;
import static org.sonatype.nexus.repository.storage.StorageTestUtil.createComponent;

public class BrowseNodeEntityAdapterTest
    extends TestSupport
{
  private static final String MAVEN_2 = "maven2";

  @Mock
  private ContentAuth contentAuth;

  @Mock
  private SecurityHelper securityHelper;

  private BrowseNodeSqlBuilder browseNodeSqlBuilder;

  @Mock
  private SelectorManager selectorManager;

  private static final String REPOSITORY_NAME = "someRepository";

  private static final String PERM_REPOSITORY_NAME = "someRepositoryWithPermission";

  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory("test");

  private ComponentEntityAdapter componentEntityAdapter;

  private AssetEntityAdapter assetEntityAdapter;

  private BrowseNodeEntityAdapter underTest;

  private Bucket bucket;

  private EntityId componentId;

  private EntityId assetIdWithPerm;

  private EntityId assetIdWithoutPerm;

  private EntityId assetIdFoo;

  private EntityId assetIdhiddenWithPerm;

  private EntityId assetIdhiddenWithoutPerm;

  private EntityId assetIdRootWithPerm;

  private EntityId assetIdRootWithoutPerm;

  List<SelectorConfiguration> configs = emptyList();

  @Before
  public void setUp() {
    initializeDatabase();

    ArgumentMatcher<Object[]> matcher = new ArgumentMatcher<Object[]>()
    {
      @Override

      public boolean matches(final Object argument) {
        Object[] args = (Object[]) argument;
        ORID orid = ((OIdentifiable) args[0]).getIdentity();
        if (!(orid.equals(assetEntityAdapter.recordIdentity(assetIdWithPerm)) || orid
            .equals(assetEntityAdapter.recordIdentity(assetIdFoo)) || orid
            .equals(assetEntityAdapter.recordIdentity(assetIdhiddenWithPerm)) || orid
            .equals(assetEntityAdapter.recordIdentity(assetIdRootWithPerm)))) {
          return false;
        }

        if (args.length == 3 && !configs.isEmpty() && args[2].equals(true)) {
          return false;
        }

        return args[1].equals(PERM_REPOSITORY_NAME);
      }
    };

    when(contentAuth.execute(any(), any(), any(), argThat(matcher), any())).thenReturn(true);

    createTree();
  }

  private void initializeDatabase() {
    BucketEntityAdapter bucketEntityAdapter = new BucketEntityAdapter();
    componentEntityAdapter = new ComponentEntityAdapter(bucketEntityAdapter);
    assetEntityAdapter = new AssetEntityAdapter(bucketEntityAdapter, componentEntityAdapter);
    browseNodeSqlBuilder = new BrowseNodeSqlBuilder(selectorManager, new CselAssetSqlBuilder(),
        new BrowseNodeConfiguration());
    underTest = new BrowseNodeEntityAdapter(componentEntityAdapter, assetEntityAdapter, securityHelper,
        browseNodeSqlBuilder, new BrowseNodeConfiguration());

    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      bucketEntityAdapter.register(db);
      bucket = new Bucket();
      bucket.attributes(new NestedAttributesMap(P_ATTRIBUTES, new HashMap<>()));
      bucket.setRepositoryName(REPOSITORY_NAME);
      bucketEntityAdapter.addEntity(db, bucket);

      componentEntityAdapter.register(db);
      assetEntityAdapter.register(db);
      underTest.register(db);

      Component component = createComponent(bucket, "tomcat", "catalina", "5.0.28");
      componentId = EntityHelper
          .id(componentEntityAdapter.readEntity(
              componentEntityAdapter.addEntity(db, component)));

      assetIdWithPerm = EntityHelper.id(assetEntityAdapter.readEntity(assetEntityAdapter
          .addEntity(db, createAsset(bucket, "com/example/leaf-with-perm", component))));
      assetIdWithoutPerm = EntityHelper.id(assetEntityAdapter.readEntity(assetEntityAdapter
          .addEntity(db, createAsset(bucket, "com/example/leaf-without-perm", component))));
      assetIdFoo = EntityHelper.id(assetEntityAdapter.readEntity(assetEntityAdapter
          .addEntity(db, createAsset(bucket, "com/example/foo", component))));
      assetIdhiddenWithPerm = EntityHelper.id(assetEntityAdapter.readEntity(assetEntityAdapter
          .addEntity(db, createAsset(bucket, "com/example/node-with-perm/hidden-leaf-with-perm", component))));
      assetIdhiddenWithoutPerm = EntityHelper.id(assetEntityAdapter.readEntity(assetEntityAdapter
          .addEntity(db, createAsset(bucket, "com/example/node-without-perm/hidden-leaf-without-perm", component))));
      assetIdRootWithPerm = EntityHelper.id(assetEntityAdapter.readEntity(assetEntityAdapter
          .addEntity(db, createAsset(bucket, "root-leaf-with-perm", component))));
      assetIdRootWithoutPerm = EntityHelper.id(assetEntityAdapter.readEntity(assetEntityAdapter
          .addEntity(db, createAsset(bucket, "root-leaf-without-perm", component))));
    }

    OSQLEngine.getInstance().registerFunction(ContentAuth.NAME, contentAuth);
  }

  private void createTree() {
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      ORID orid = createBrowseNode(db, null, "com", REPOSITORY_NAME, null);
      orid = createBrowseNode(db, orid, "example", REPOSITORY_NAME, null);

      createBrowseNode(db, orid, "leaf-with-perm", REPOSITORY_NAME, assetIdWithPerm);
      createBrowseNode(db, orid, "leaf-without-perm", REPOSITORY_NAME, assetIdWithoutPerm);
      createBrowseNode(db, orid, "foo", REPOSITORY_NAME, assetIdFoo);

      ORID parentId = createBrowseNode(db, orid, "node-with-perm", REPOSITORY_NAME, null);
      createBrowseNode(db, parentId, "hidden-leaf-with-perm", REPOSITORY_NAME, assetIdhiddenWithPerm);

      parentId = createBrowseNode(db, orid, "node-without-perm", REPOSITORY_NAME, null);
      createBrowseNode(db, parentId, "hidden-leaf-without-perm", REPOSITORY_NAME, assetIdhiddenWithoutPerm);

      createBrowseNode(db, null, "root-leaf", REPOSITORY_NAME, assetIdRootWithPerm);
      createBrowseNode(db, null, "hidden-root-leaf", REPOSITORY_NAME, assetIdRootWithoutPerm);
    }
  }

  @Test
  public void testRegister() {
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      OSchema schema = db.getMetadata().getSchema();
      assertThat(schema.getClass(underTest.getTypeName()), is(notNullValue()));
    }
  }

  @Test
  public void testUpsert() {
    EntityId rootNodeId = EntityHelper.id(dbTx().call(db -> underTest.upsert(db, REPOSITORY_NAME, null, "bar", true)));

    BrowseNode node1 = dbTx().call(db -> underTest.upsert(db, REPOSITORY_NAME, rootNodeId, "foo", true));
    // add a component
    BrowseNode node2 = dbTx()
        .call(db -> underTest.upsert(db, REPOSITORY_NAME, rootNodeId, "foo", null, componentId, true));
    // add an asset
    BrowseNode node3 = dbTx()
        .call(db -> underTest.upsert(db, REPOSITORY_NAME, rootNodeId, "foo", assetIdFoo, null, true));

    EntityId nodeId = EntityHelper.id(node1);
    assertThat(nodeId, is(EntityHelper.id(node2)));
    assertThat(nodeId, is(EntityHelper.id(node3)));

    BrowseNode actualNode = dbTx().call(db -> underTest.read(db, nodeId));
    assertThat(actualNode.getParentId(), is(rootNodeId));
    assertThat(actualNode.getPath(), is("foo"));
    assertThat(actualNode.getRepositoryName(), is(REPOSITORY_NAME));
    assertThat(actualNode.getAssetId(), is(assetIdFoo));
    assertThat(actualNode.getComponentId(), is(componentId));
  }

  private OrientOperations<RuntimeException, ?> dbTx() {
    return inTx(database.getInstanceProvider());
  }

  @Test
  public void testUpsertWithoutCreatingChildLinks() {
    BrowseNode parent = inTx(database.getInstanceProvider()).call(db -> {
      return newArrayList(underTest.getByPath(db, asList("com"), REPOSITORY_NAME)).get(0);
    });

    BrowseNode child = inTx(database.getInstanceProvider()).call(db -> {
      return underTest.upsert(db, REPOSITORY_NAME, EntityHelper.id(parent), "child", false);
    });

    inTx(database.getInstanceProvider()).run(db -> {
      Set<ODocument> children = underTest.document(db, EntityHelper.id(parent)).field("children_ids", OType.LINKSET);
      Set<ORID> childIds = children.stream().map(ODocument::getIdentity).collect(toSet());
      assertThat(childIds, not(hasItems(underTest.recordIdentity(child))));
    });
  }

  @Test
  public void testSave() {
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      BrowseNode node = underTest
          .save(db, new BrowseNode().withPath("foo").withRepositoryName(REPOSITORY_NAME).withComponentId(componentId),
              true);

      EntityId entityId = EntityHelper.id(node);
      assertNotNull(entityId); // not really necessary, EntityHelper.id ensures it

      // change a field & update
      node.setPath("bar");
      underTest.save(db, node, true);

      assertThat(EntityHelper.id(node), is(entityId));

      // our change was actually persisted
      node = underTest.read(db, entityId);
      assertThat(node.getPath(), is("bar"));
      assertThat(node.getComponentId(), is(componentId));
    }
  }

  @Test
  public void testGetChildrenByPath_contentAuth() {
    setupTestGetChildrenByPath();
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      List<BrowseNode> nodes = Lists.newArrayList(underTest.getChildrenByPath(db, asList("com", "example"),
          REPOSITORY_NAME, PERM_REPOSITORY_NAME, MAVEN_2, null));
      List<String> names = new LinkedList<>();
      nodes.forEach((a) -> names.add(a.getPath()));
      assertThat(names, containsInAnyOrder("leaf-with-perm", "node-with-perm", "foo"));
    }
  }

  @Test
  public void testGetChildrenByPath_contentAuth_noSelectors_root_nodes() {
    reset(contentAuth);
    when(contentAuth.execute(any(), any(), any(), any(), any())).thenReturn(false);
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      assertThat(underTest.getChildrenByPath(db, emptyList(),
          REPOSITORY_NAME, PERM_REPOSITORY_NAME, MAVEN_2, null), nullValue());
    }
  }

  @Test
  public void testGetChildrenByPath_contentAuth_onlyCselSelectors_root_nodes() {
    setupTestGetChildrenByPath();
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      List<BrowseNode> nodes = Lists.newArrayList(underTest.getChildrenByPath(db, emptyList(),
          REPOSITORY_NAME, PERM_REPOSITORY_NAME, MAVEN_2, null));
      List<String> names = new LinkedList<>();
      nodes.forEach((a) -> names.add(a.getPath()));
      assertThat(names, contains("com", "root-leaf"));
    }
    verify(contentAuth, never()).execute(any(), any(), any(), any(), any());
  }

  @Test
  public void testGetChildrenByPath_contentAuth_cselAndJexlSelectors_root_nodes() {
    setupTestGetChildrenByPath(true);
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      List<BrowseNode> nodes = Lists.newArrayList(underTest.getChildrenByPath(db, emptyList(),
          REPOSITORY_NAME, PERM_REPOSITORY_NAME, MAVEN_2, null));
      List<String> names = new LinkedList<>();
      nodes.forEach((a) -> names.add(a.getPath()));
      assertThat(names, containsInAnyOrder("com", "root-leaf"));
    }
    verify(contentAuth, times(2)).execute(any(), any(), any(), any(), any());
  }

  @Test
  public void testGetChildrenByPath_repoViewPerms_root_nodes() {
    setupTestGetChildrenByPath(true);
    when(securityHelper.anyPermitted(new RepositoryViewPermission("*", PERM_REPOSITORY_NAME, BreadActions.BROWSE)))
        .thenReturn(true);
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      List<BrowseNode> nodes = Lists.newArrayList(underTest.getChildrenByPath(db, emptyList(),
          REPOSITORY_NAME, PERM_REPOSITORY_NAME, MAVEN_2, null));
      List<String> names = new LinkedList<>();
      nodes.forEach((a) -> names.add(a.getPath()));
      assertThat(names, containsInAnyOrder("com", "root-leaf", "hidden-root-leaf"));
    }
    verify(contentAuth, never()).execute(any(), any(), any(), any(), any());
  }

  @Test
  public void testGetChildrenByPath_repoViewPerms_root_nodes_filtering() {
    setupTestGetChildrenByPath(false);
    when(securityHelper.anyPermitted(new RepositoryViewPermission("*", PERM_REPOSITORY_NAME, BreadActions.BROWSE)))
        .thenReturn(true);
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      List<BrowseNode> nodes = Lists.newArrayList(underTest.getChildrenByPath(db, emptyList(),
          REPOSITORY_NAME, PERM_REPOSITORY_NAME, MAVEN_2, "com"));
      List<String> names = new LinkedList<>();
      nodes.forEach((a) -> names.add(a.getPath()));
      assertThat(names, contains("com"));
    }
    verify(contentAuth, never()).execute(any(), any(), any(), any(), any());
  }

  @Test
  public void testGetChildrenByPath_repoViewPerms_root_nodes_filtering_case_insensitive() {
    setupTestGetChildrenByPath(false);
    when(securityHelper.anyPermitted(new RepositoryViewPermission("*", PERM_REPOSITORY_NAME, BreadActions.BROWSE)))
        .thenReturn(true);
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      List<BrowseNode> nodes = Lists.newArrayList(underTest.getChildrenByPath(db, emptyList(),
          REPOSITORY_NAME, PERM_REPOSITORY_NAME, MAVEN_2, "COM"));
      List<String> names = new LinkedList<>();
      nodes.forEach((a) -> names.add(a.getPath()));
      assertThat(names, contains("com"));
    }
    verify(contentAuth, never()).execute(any(), any(), any(), any(), any());
  }

  @Test
  public void testGetChildrenByPath_repoViewPerms_root_nodes_filtering_children() {
    setupTestGetChildrenByPath(false);
    when(securityHelper.anyPermitted(new RepositoryViewPermission("*", PERM_REPOSITORY_NAME, BreadActions.BROWSE)))
        .thenReturn(true);
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      List<BrowseNode> nodes = Lists.newArrayList(underTest.getChildrenByPath(db, emptyList(),
          REPOSITORY_NAME, PERM_REPOSITORY_NAME, MAVEN_2, "foo"));
      List<String> names = new LinkedList<>();
      nodes.forEach((a) -> names.add(a.getPath()));
      assertThat(names, contains("com"));
    }
    verify(contentAuth, never()).execute(any(), any(), any(), any(), any());
  }

  @Test
  public void testGetChildrenByPath_repoViewPerms_root_nodes_filtering_children_case_insensitive() {
    setupTestGetChildrenByPath(false);
    when(securityHelper.anyPermitted(new RepositoryViewPermission("*", PERM_REPOSITORY_NAME, BreadActions.BROWSE)))
        .thenReturn(true);
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      List<BrowseNode> nodes = Lists.newArrayList(underTest.getChildrenByPath(db, emptyList(),
          REPOSITORY_NAME, PERM_REPOSITORY_NAME, MAVEN_2, "FOO"));
      List<String> names = new LinkedList<>();
      nodes.forEach((a) -> names.add(a.getPath()));
      assertThat(names, contains("com"));
    }
    verify(contentAuth, never()).execute(any(), any(), any(), any(), any());
  }

  @Test
  public void testGetChildrenByPath_repoViewPerms_root_nodes_filtering_all_children() {
    setupTestGetChildrenByPath(false);
    when(securityHelper.anyPermitted(new RepositoryViewPermission("*", PERM_REPOSITORY_NAME, BreadActions.BROWSE)))
        .thenReturn(true);
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      assertThat(underTest
          .getChildrenByPath(db, emptyList(), REPOSITORY_NAME, PERM_REPOSITORY_NAME, MAVEN_2,
              "nothingmatchesme"), nullValue());
    }
    verify(contentAuth, never()).execute(any(), any(), any(), any(), any());
  }

  @Test
  public void testGetChildrenByPath_contentAuth_noSelectors() {
    reset(contentAuth);
    when(contentAuth.execute(any(), any(), any(), any(), any())).thenReturn(false);
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      assertThat(underTest.getChildrenByPath(db, Arrays.asList("com", "example"),
          REPOSITORY_NAME, PERM_REPOSITORY_NAME, MAVEN_2, null), nullValue());
    }
  }

  @Test
  public void testGetChildrenByPath_contentAuth_onlyCselSelectors() {
    setupTestGetChildrenByPath();
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      List<BrowseNode> nodes = Lists.newArrayList(underTest
          .getChildrenByPath(db, Arrays.asList("com", "example"), REPOSITORY_NAME, PERM_REPOSITORY_NAME,
              MAVEN_2, null));
      List<String> names = new LinkedList<>();
      nodes.forEach((a) -> names.add(a.getPath()));
      assertThat(names, contains("foo", "leaf-with-perm", "node-with-perm"));
    }
    verify(contentAuth, never()).execute(any(), any(), any(), any(), any());
  }

  @Test
  public void testGetChildrenByPath_contentAuth_cselAndJexlSelectors() {
    setupTestGetChildrenByPath(true);
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      List<BrowseNode> nodes = Lists.newArrayList(underTest
          .getChildrenByPath(db, Arrays.asList("com", "example"), REPOSITORY_NAME, PERM_REPOSITORY_NAME, MAVEN_2,
              null));
      List<String> names = new LinkedList<>();
      nodes.forEach((a) -> names.add(a.getPath()));
      assertThat(names, containsInAnyOrder("foo", "leaf-with-perm", "node-with-perm"));
    }
    verify(contentAuth, times(2)).execute(any(), any(), any(), any(), any());
  }

  @Test
  public void testGetChildrenByPath_repoViewPerms() {
    setupTestGetChildrenByPath(true);
    when(securityHelper.anyPermitted(new RepositoryViewPermission("*", PERM_REPOSITORY_NAME, BreadActions.BROWSE)))
        .thenReturn(true);
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      List<BrowseNode> nodes = Lists.newArrayList(underTest
          .getChildrenByPath(db, Arrays.asList("com", "example"), REPOSITORY_NAME, PERM_REPOSITORY_NAME, MAVEN_2,
              null));
      List<String> names = new LinkedList<>();
      nodes.forEach((a) -> names.add(a.getPath()));
      assertThat(names,
          containsInAnyOrder("foo", "leaf-with-perm", "leaf-without-perm", "node-with-perm", "node-without-perm"));
    }
    verify(contentAuth, never()).execute(any(), any(), any(), any(), any());
  }

  @Test
  public void testGetChildrenByPath_repoViewPerms_filtering() {
    setupTestGetChildrenByPath(false);
    when(securityHelper.anyPermitted(new RepositoryViewPermission("*", PERM_REPOSITORY_NAME, BreadActions.BROWSE)))
        .thenReturn(true);
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      List<BrowseNode> nodes = Lists.newArrayList(underTest
          .getChildrenByPath(db, Arrays.asList("com", "example"), REPOSITORY_NAME, PERM_REPOSITORY_NAME, MAVEN_2,
              "node-with"));
      List<String> names = new LinkedList<>();
      nodes.forEach((a) -> names.add(a.getPath()));
      assertThat(names, contains("node-with-perm", "node-without-perm"));
    }
    verify(contentAuth, never()).execute(any(), any(), any(), any(), any());
  }

  @Test
  public void testGetChildrenByPath_repoViewPerms_filtering_case_insensitive() {
    setupTestGetChildrenByPath(false);
    when(securityHelper.anyPermitted(new RepositoryViewPermission("*", PERM_REPOSITORY_NAME, BreadActions.BROWSE)))
        .thenReturn(true);
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      List<BrowseNode> nodes = Lists.newArrayList(underTest
          .getChildrenByPath(db, Arrays.asList("com", "example"), REPOSITORY_NAME, PERM_REPOSITORY_NAME, MAVEN_2,
              "NODE-WITH"));
      List<String> names = new LinkedList<>();
      nodes.forEach((a) -> names.add(a.getPath()));
      assertThat(names, contains("node-with-perm", "node-without-perm"));
    }
    verify(contentAuth, never()).execute(any(), any(), any(), any(), any());
  }

  @Test
  public void testGetChildrenByPath_repoViewPerms_filtering_children() {
    setupTestGetChildrenByPath(false);
    when(securityHelper.anyPermitted(new RepositoryViewPermission("*", PERM_REPOSITORY_NAME, BreadActions.BROWSE)))
        .thenReturn(true);
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      List<BrowseNode> nodes = Lists.newArrayList(underTest
          .getChildrenByPath(db, Arrays.asList("com", "example"), REPOSITORY_NAME, PERM_REPOSITORY_NAME, MAVEN_2,
              "foo"));
      List<String> names = new LinkedList<>();
      nodes.forEach((a) -> names.add(a.getPath()));
      assertThat(names, contains("foo"));
    }
    verify(contentAuth, never()).execute(any(), any(), any(), any(), any());
  }

  @Test
  public void testGetChildrenByPath_repoViewPerms_filtering_children_case_insensitive() {
    setupTestGetChildrenByPath(false);
    when(securityHelper.anyPermitted(new RepositoryViewPermission("*", PERM_REPOSITORY_NAME, BreadActions.BROWSE)))
        .thenReturn(true);
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      List<BrowseNode> nodes = Lists.newArrayList(underTest
          .getChildrenByPath(db, Arrays.asList("com", "example"), REPOSITORY_NAME, PERM_REPOSITORY_NAME, MAVEN_2,
              "FOO"));
      List<String> names = new LinkedList<>();
      nodes.forEach((a) -> names.add(a.getPath()));
      assertThat(names, contains("foo"));
    }
    verify(contentAuth, never()).execute(any(), any(), any(), any(), any());
  }

  @Test
  public void testGetChildrenByPath_repoViewPerms_filtering_all_children() {
    setupTestGetChildrenByPath(false);
    when(securityHelper.anyPermitted(new RepositoryViewPermission("*", PERM_REPOSITORY_NAME, BreadActions.BROWSE)))
        .thenReturn(true);
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      assertThat(underTest
          .getChildrenByPath(db, Arrays.asList("com", "example"), REPOSITORY_NAME, PERM_REPOSITORY_NAME, MAVEN_2,
              "nothingmatchesme"), nullValue());
    }
    verify(contentAuth, never()).execute(any(), any(), any(), any(), any());
  }

  @Test
  public void testGetChildrenByPath_repositoryAuth() {
    when(securityHelper.anyPermitted(new RepositoryViewPermission("*", PERM_REPOSITORY_NAME, BreadActions.BROWSE)))
        .thenReturn(true);
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      List<BrowseNode> nodes = Lists.newArrayList(underTest.getChildrenByPath(db, asList("com", "example"),
          REPOSITORY_NAME, PERM_REPOSITORY_NAME, MAVEN_2, null));
      List<String> names = new LinkedList<>();
      nodes.forEach((a) -> names.add(a.getPath()));
      assertThat(names,
          containsInAnyOrder("leaf-with-perm", "leaf-without-perm", "node-with-perm", "node-without-perm", "foo"));
    }
  }

  @Test
  public void testGetChildrenByPath_rootPath() {
    setupTestGetChildrenByPath();
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      List<BrowseNode> nodes = Lists.newArrayList(underTest.getChildrenByPath(db, emptyList(),
          REPOSITORY_NAME, PERM_REPOSITORY_NAME, MAVEN_2, null));
      List<String> names = new LinkedList<>();
      nodes.forEach((a) -> names.add(a.getPath()));
      assertThat(names, containsInAnyOrder("com", "root-leaf"));
    }
  }

  @Test
  public void testGetChildrenByPath_filtered() {
    setupTestGetChildrenByPath();
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      List<BrowseNode> nodes = Lists.newArrayList(underTest.getChildrenByPath(db, Arrays.asList("com", "example"),
          REPOSITORY_NAME, PERM_REPOSITORY_NAME, MAVEN_2, "foo"));
      List<String> names = new LinkedList<>();
      nodes.forEach((a) -> names.add(a.getPath()));
      assertThat(names, containsInAnyOrder("foo"));
    }
  }

  @Test
  public void testGetChildrenByPath_rootContentSelector() {
    SelectorConfiguration config = new SelectorConfiguration();
    config.setAttributes(Collections.singletonMap("expression", "path == \"with-perm\""));
    when(selectorManager
        .browseActive((List<String>) argThat(containsInAnyOrder(is(REPOSITORY_NAME), is(PERM_REPOSITORY_NAME))),
            (List<String>) argThat(containsInAnyOrder(MAVEN_2))))
        .thenReturn(asList(config));

    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      Iterable<BrowseNode> nodes = underTest.getChildrenByPath(db, emptyList(), REPOSITORY_NAME, PERM_REPOSITORY_NAME, MAVEN_2, null);
      assertThat(nodes, containsInAnyOrder(
          hasProperty("path", is("com")),
          hasProperty("path", is("root-leaf"))));
    }
  }

  @Test
  public void testGetChildrenByPath_rootContentSelectorWithFilter() {
    SelectorConfiguration config = new SelectorConfiguration();
    config.setAttributes(Collections.singletonMap("expression", "path == \"with-perm\""));
    when(selectorManager
        .browseActive((List<String>) argThat(containsInAnyOrder(is(REPOSITORY_NAME), is(PERM_REPOSITORY_NAME))),
            (List<String>) argThat(containsInAnyOrder(MAVEN_2))))
        .thenReturn(asList(config));

    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      Iterable<BrowseNode> nodes = underTest
          .getChildrenByPath(db, asList("com"), REPOSITORY_NAME, PERM_REPOSITORY_NAME, MAVEN_2, "with-perm");
      assertThat(nodes, contains(hasProperty("path", is("example"))));
    }
  }

  @Test
  public void testGetChildrenByPath_rootContentSelectorDoesNotMatch() {
    SelectorConfiguration config = new SelectorConfiguration();
    config.setAttributes(Collections.singletonMap("expression", "path == \"unknown\""));

    configs = asList(config);

    when(selectorManager
        .browseActive((List<String>) argThat(containsInAnyOrder(is(REPOSITORY_NAME), is(PERM_REPOSITORY_NAME))),
            (List<String>) argThat(containsInAnyOrder(MAVEN_2))))
        .thenReturn(configs);

    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      Iterable<BrowseNode> nodes = underTest.getChildrenByPath(db, emptyList(), REPOSITORY_NAME, PERM_REPOSITORY_NAME, MAVEN_2, null);
      assertThat(nodes, nullValue());
    }
  }

  @Test
  public void getByPath() {
    List<String> pathSegments = Arrays.asList("com", "sonatype", "example");

    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      insertTestNodes(db, pathSegments, REPOSITORY_NAME);

      List<BrowseNode> nodes = newArrayList(underTest.getByPath(db, pathSegments, REPOSITORY_NAME));

      assertThat(nodes.size(), is(pathSegments.size()));
      for (int i = 0; i < nodes.size(); i++) {
        BrowseNode node = nodes.get(i);
        assertThat(node.getPath(), is(pathSegments.get(i)));
      }
    }
  }

  @Test
  public void getByPath_notFound() {
    String repositoryName = "repository";
    List<String> pathSegments = Arrays.asList("com", "sonatype", "example");

    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      underTest.register(db);
      List<BrowseNode> nodes = newArrayList(underTest.getByPath(db, pathSegments, repositoryName));

      assertThat(nodes.size(), is(0));
    }
  }

  @Test
  public void testDeleteEntity() {
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      List<BrowseNode> nodes = newArrayList(underTest.getByPath(db, newArrayList("com", "example", "leaf-with-perm"),
          REPOSITORY_NAME));
      BrowseNode node = nodes.get(nodes.size() - 1);

      underTest.deleteEntity(db, node);
    }
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      assertThat(newArrayList(underTest.getByPath(db, newArrayList("com", "example"), REPOSITORY_NAME)).size(), is(2));
      assertThat(newArrayList(underTest.getByPath(db, newArrayList("com", "example", "leaf-with-perm"), REPOSITORY_NAME)).size(), is(0));
    }
  }

  @Test
  public void testDeleteEntity_recursive() {
    BrowseNode node;
    EntityId id;
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      id = EntityHelper.id(underTest.upsert(db, REPOSITORY_NAME, null, "org", true));
      node = underTest.upsert(db, REPOSITORY_NAME, id, "cruft", true);
    }

    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      underTest.deleteEntity(db, node);
    }
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      assertNull(underTest.read(db, id));
    }
  }

  @Test
  public void testAddEntity() {
    EntityId id;
    EntityId parentId;
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      parentId = new AttachedEntityId(underTest, underTest.addEntity(db,
          new BrowseNode().withPath("org").withRepositoryName(REPOSITORY_NAME)).getIdentity());
      id = new AttachedEntityId(underTest, underTest.addEntity(db,
          new BrowseNode().withParentId(parentId).withPath("example").withRepositoryName(REPOSITORY_NAME))
          .getIdentity());
    }

    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      ODocument doc = underTest.document(db, parentId);
      Set<OIdentifiable> set = doc.field("children_ids");
      assertThat(set, contains(underTest.recordIdentity(id)));
    }
  }

  @Test
  public void testTruncateRepository() {
    EntityId parentId;
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      parentId = new AttachedEntityId(underTest, underTest.addEntity(db,
          new BrowseNode().withPath("org").withRepositoryName(REPOSITORY_NAME)).getIdentity());
      new AttachedEntityId(underTest, underTest.addEntity(db,
          new BrowseNode().withParentId(parentId).withPath("example").withRepositoryName(REPOSITORY_NAME))
          .getIdentity());
    }
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      assertThat(underTest.truncateRepository(db, REPOSITORY_NAME), is(13));
    }
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      assertThat(newArrayList(underTest.browseDocuments(db)), empty());
    }
  }

  @Test
  public void updateChildNodes() {
    inTx(database.getInstanceProvider()).run(db -> {
      OConcurrentResultSet<ODocument> browseNodesBefore = db.command(new OCommandSQL("select from browse_node"))
          .execute();
      assertTrue("all children_ids should be empty sets",
          browseNodesBefore.stream().anyMatch(document -> {
            Set<ORID> childIds = document.field("children_ids", OType.LINKSET);
            return !childIds.isEmpty();
          })
      );
    });

    inTx(database.getInstanceProvider()).run(db -> {
      db.command(new OCommandSQL("update browse_node set children_ids = []")).execute();

      OConcurrentResultSet<ODocument> browseNodesBefore = db.command(new OCommandSQL("select from browse_node"))
          .execute();
      assertFalse("all children_ids should be empty sets",
          browseNodesBefore.stream().anyMatch(document -> {
            Set<ORID> childIds = document.field("children_ids", OType.LINKSET);
            return !childIds.isEmpty();
          })
      );
    });

    inTx(database.getInstanceProvider()).run(db -> {
      OConcurrentResultSet<ODocument> nodes = db.command(new OCommandSQL("select @rid from browse_node")).execute();
      nodes.forEach(node -> underTest.updateChildren(db, ((OIdentifiable) node.field("rid")).getIdentity()));
    });

    inTx(database.getInstanceProvider()).run(db -> {
      OConcurrentResultSet<ODocument> browseNodesAfter = db.command(new OCommandSQL("select from browse_node")).execute();
      Map<ORID, ODocument> nodesById = browseNodesAfter.stream().collect(
          Collectors.toMap(document -> document.getIdentity(), document -> document));
      for (ODocument browseNode : browseNodesAfter) {
        ORID parentId = browseNode.field("parent_id", ORID.class);
        if (parentId != null) {
          ODocument parent = nodesById.get(parentId);
          Set<ODocument> children = parent.field("children_ids", OType.LINKSET);
          Set<ORID> ids = children.stream().map(ODocument::getIdentity).collect(toSet());
          assertThat(ids, not(hasSize(0)));
          assertThat(ids, hasItems(browseNode.getIdentity()));
        }
      }
    });
  }

  private void insertTestNodes(final ODatabaseDocumentTx db, final Iterable<String> pathSegments, final String repositoryName) {
    EntityId parentId = null;
    for (String pathSegment : pathSegments) {
      parentId = EntityHelper.id(underTest.upsert(db, REPOSITORY_NAME, parentId, pathSegment, true));
    }
  }

  private ORID createBrowseNode(final ODatabaseDocumentTx db,
                                final ORID parentId,
                                final String path,
                                final String repositoryName,
                                final EntityId assetId)
  {
    return underTest.recordIdentity(underTest.upsert(
        db,
        repositoryName,
        parentId == null ? null : new AttachedEntityId(underTest, parentId),
        path,
        assetId,
        null,
        true));
  }

  private void setupTestGetChildrenByPath() {
    setupTestGetChildrenByPath(false);
  }

  private void setupTestGetChildrenByPath(boolean withJexl) {
    SelectorConfiguration config = new SelectorConfiguration();
    config.setType(CselSelector.TYPE);
    config.setAttributes(Collections.singletonMap("expression", "path =~ \".*with-perm.*\" || path =~ \".*foo.*\""));

    SelectorConfiguration jexlConfig = new SelectorConfiguration();
    jexlConfig.setType(JexlSelector.TYPE);
    jexlConfig.setAttributes(Collections.singletonMap("expression", "path =~ \".*with-perm.*\" || path =~ \".*foo.*\""));

    when(selectorManager
        .browseActive((List<String>) argThat(contains(is(PERM_REPOSITORY_NAME))),
            (List<String>) argThat(contains(MAVEN_2))))
        .thenReturn(withJexl ? asList(config, jexlConfig) : asList(config));
  }
}
