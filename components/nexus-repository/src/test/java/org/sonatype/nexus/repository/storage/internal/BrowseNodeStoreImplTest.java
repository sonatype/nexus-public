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
package org.sonatype.nexus.repository.storage.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.orient.HexRecordIdObfuscator;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.security.RepositoryViewPermission;
import org.sonatype.nexus.repository.selector.internal.ContentAuth;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.BrowseNode;
import org.sonatype.nexus.repository.storage.BrowseNodeEntityAdapter;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.BucketEntityAdapter;
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.security.BreadActions;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.selector.CselAssetSqlBuilder;
import org.sonatype.nexus.selector.SelectorManager;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.sql.OSQLEngine;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES;
import static org.sonatype.nexus.repository.storage.internal.BrowseNodeSqlBuilder.P_CHILDREN_IDS;
import static org.sonatype.nexus.repository.storage.internal.BrowseNodeSqlBuilder.P_PATH;

public class BrowseNodeStoreImplTest
    extends TestSupport
{
  private static final String REPOSITORY_NAME = "test-repo";

  private static final String PERM_REPOSITORY_NAME = "test-repo-with-perm";

  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory("test");

  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private Repository repository;

  @Mock
  private ContentAuth contentAuth;

  @Mock
  private SelectorManager selectorManager;

  @Mock
  private Format format;

  private AssetEntityAdapter assetEntityAdapter;

  private ComponentEntityAdapter componentEntityAdapter;

  private BrowseNodeStoreImpl underTest;

  private BrowseNodeEntityAdapter browseNodeEntityAdapter;

  @Before
  public void setUp() throws Exception {
    underTest = createBrowseNodeStoreImpl();
    underTest.start();

    when(repository.getName()).thenReturn(REPOSITORY_NAME);
    when(repository.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("maven2");

    OSQLEngine.getInstance().registerFunction(ContentAuth.NAME, contentAuth);

    ArgumentMatcher<Object[]> matcher = new ArgumentMatcher<Object[]>()
    {
      @Override
      public boolean matches(final Object argument) {
        Object[] args = (Object[]) argument;
        return args[1].equals(PERM_REPOSITORY_NAME);
      }
    };
    when(contentAuth.execute(any(), any(), any(), argThat(matcher), any())).thenReturn(true);
  }

  private BrowseNodeStoreImpl createBrowseNodeStoreImpl() {
    BrowseNodeConfiguration configuration = new BrowseNodeConfiguration(true, true, 1000, 1000, 1000, 10, 2);
    BucketEntityAdapter bucketEntityAdapter = new BucketEntityAdapter();
    componentEntityAdapter = new ComponentEntityAdapter(bucketEntityAdapter);
    assetEntityAdapter = new AssetEntityAdapter(bucketEntityAdapter, componentEntityAdapter);
    browseNodeEntityAdapter = new BrowseNodeEntityAdapter(componentEntityAdapter, assetEntityAdapter, securityHelper,
        new BrowseNodeSqlBuilder(selectorManager, new CselAssetSqlBuilder()), configuration);

    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      bucketEntityAdapter.register(db);
      Bucket bucket = mock(Bucket.class);
      when(bucket.attributes()).thenReturn(new NestedAttributesMap(P_ATTRIBUTES, new HashMap<>()));
      when(bucket.getRepositoryName()).thenReturn(REPOSITORY_NAME);
      bucketEntityAdapter.addEntity(db, bucket);

      componentEntityAdapter.register(db);
      assetEntityAdapter.register(db);
    }
    browseNodeEntityAdapter.enableObfuscation(new HexRecordIdObfuscator());
    return new BrowseNodeStoreImpl(database.getInstanceProvider(), browseNodeEntityAdapter, configuration);
  }

  @After
  public void cleanup() {
    ThreadContext.unbindSubject();
  }

  @Test
  public void testCreateNodes_noneExisting() {
    BrowseNode node = underTest.createNodes(REPOSITORY_NAME, Arrays.asList("one", "two"));

    assertNotNull(node);
    assertNotNull(node.getParentId());
    assertThat(node.getPath(), is("two"));

    node = getByEntityId(node.getParentId());

    assertNotNull(node);
    assertNull(node.getParentId());
    assertThat(node.getPath(), is("one"));
  }

  @Test
  public void testCreateNodes_partial() {
    BrowseNode rootNode;
    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      rootNode = new BrowseNode().withPath("one").withRepositoryName(REPOSITORY_NAME);
      browseNodeEntityAdapter.addEntity(db, rootNode);
    }

    BrowseNode node = underTest.createNodes(REPOSITORY_NAME, Arrays.asList("one", "two", "three"));

    assertNotNull(node);
    assertNotNull(node.getParentId());
    assertThat(node.getPath(), is("three"));

    node = getByEntityId(node.getParentId());

    assertNotNull(node);
    assertNotNull(node.getParentId());
    assertThat(node.getPath(), is("two"));

    node = getByEntityId(node.getParentId());

    assertNotNull(node);
    assertNull(node.getParentId());
    assertThat(node.getPath(), is("one"));
    assertThat(node.getEntityMetadata().getId(), is(rootNode.getEntityMetadata().getId()));
  }

  @Test
  public void testGetChildrenByPath() {
    when(securityHelper.anyPermitted(new RepositoryViewPermission("*", REPOSITORY_NAME, BreadActions.BROWSE)))
        .thenReturn(true);

    BrowseNode rootNode;
    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      rootNode = new BrowseNode().withPath("one").withRepositoryName(REPOSITORY_NAME);
      browseNodeEntityAdapter.addEntity(db, rootNode);
    }

    List<BrowseNode> nodes = Lists.newArrayList(underTest.getChildrenByPath(repository, Collections.emptyList(), 100, null));
    assertThat(nodes, hasSize(1));
    assertThat(EntityHelper.id(nodes.get(0)), is(EntityHelper.id(rootNode)));
  }

  @Test
  public void testGetChildrenByPath_group() {
    when(securityHelper.anyPermitted(new RepositoryViewPermission("*", "bar", BreadActions.BROWSE)))
        .thenReturn(true);

    // we're using the same repository twice to show de-duplication works
    Repository groupRepository = createGroupRepository(Lists.newArrayList(repository, repository), "bar");

    BrowseNode rootNode, groupMetadataNode;
    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      rootNode = new BrowseNode().withPath("one").withRepositoryName(REPOSITORY_NAME);
      browseNodeEntityAdapter.addEntity(db, rootNode);

      groupMetadataNode = new BrowseNode().withPath("group-metadata").withRepositoryName("bar");
      browseNodeEntityAdapter.addEntity(db, groupMetadataNode);
    }

    List<EntityId> ids = new ArrayList<>();
    underTest.getChildrenByPath(groupRepository, Collections.emptyList(), 100, null).forEach(
        node -> ids.add(EntityHelper.id(node)));
    assertThat(ids, hasSize(2));
    assertThat(ids, contains(EntityHelper.id(groupMetadataNode), EntityHelper.id(rootNode)));
  }

  @Test
  public void testGetChildrenByPath_groupEmpty() {
    when(securityHelper.anyPermitted(new RepositoryViewPermission("*", "bar", BreadActions.BROWSE)))
        .thenReturn(true);

    BrowseNode rootNode;
    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      rootNode = new BrowseNode().withPath("one").withRepositoryName(REPOSITORY_NAME);
      browseNodeEntityAdapter.addEntity(db, rootNode);
    }

    when(repository.getName()).thenReturn("foo");
    Repository groupRepository = createGroupRepository(Collections.singletonList(repository), "bar");

    List<BrowseNode> nodes = Lists.newArrayList(underTest.getChildrenByPath(groupRepository, Collections.emptyList(),
        100, null));
    assertThat(nodes, empty());
  }

  @Test
  public void testGetChildrenByPath_groupNoPerm() {
    when(repository.getName()).thenReturn("foo");
    Repository groupRepository = createGroupRepository(Collections.singletonList(repository), "bar");

    assertNull(underTest.getChildrenByPath(groupRepository, Collections.emptyList(), 100, null));
  }

  private Repository createGroupRepository(final List<Repository> repositories, final String name) {
    Repository groupRepository = mock(Repository.class);
    when(groupRepository.getType()).thenReturn(new GroupType());
    when(groupRepository.getName()).thenReturn(name);

    when(groupRepository.getFormat()).thenReturn(format);

    GroupFacet facet = mock(GroupFacet.class);
    when(facet.leafMembers()).thenReturn(repositories);
    when(groupRepository.facet(GroupFacet.class)).thenReturn(facet);

    return groupRepository;
  }

  @Test
  public void testSave() {
    BrowseNode node = underTest.save(new BrowseNode().withPath("foo").withRepositoryName(REPOSITORY_NAME));

    EntityId entityId = EntityHelper.id(node);
    assertNotNull(entityId); // not really necessary, EntityHelper.id ensures it

    // change a field & update
    node.setPath("bar");
    underTest.save(node);

    assertThat(EntityHelper.id(node), is(entityId));

    // our change was actually persisted
    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      node = browseNodeEntityAdapter.read(db, entityId);
    }
    assertThat(node.getPath(), is("bar"));
  }

  @Test
  public void testDeleteByAssetId() {
    BrowseNodeEntityAdapter entityAdapter = mock(BrowseNodeEntityAdapter.class);
    EntityId id = mock(EntityId.class);
    BrowseNode node = new BrowseNode();
    node.setLeaf(true);
    BrowseNode node2 = new BrowseNode();
    node2.setLeaf(true);

    BrowseNodeConfiguration configuration = new BrowseNodeConfiguration(true, true, 1000, 10000, 10000, 10, 10);
    underTest = new BrowseNodeStoreImpl(database.getInstanceProvider(), entityAdapter, configuration);

    when(entityAdapter.getByAssetId(any(), eq(id))).thenReturn(Arrays.asList(node, node2));
    underTest.deleteNodeByAssetId(id);
    verify(entityAdapter).deleteEntity(any(), eq(node));
    verify(entityAdapter).deleteEntity(any(), eq(node2));
  }

  @Test
  public void testDeleteByAssetIdFolderAsset() {
    BrowseNodeEntityAdapter entityAdapter = mock(BrowseNodeEntityAdapter.class);
    BrowseNodeConfiguration configuration = new BrowseNodeConfiguration(true, true, 1000, 10000, 10000, 10, 10);
    underTest = new BrowseNodeStoreImpl(database.getInstanceProvider(), entityAdapter, configuration);

    EntityId id = mock(EntityId.class);
    BrowseNode node = new BrowseNode().withAssetId(mock(EntityId.class));
    when(entityAdapter.getByAssetId(any(), eq(id))).thenReturn(Arrays.asList(node));

    underTest.deleteNodeByAssetId(id);

    verify(entityAdapter).save(any(), eq(node), eq(false));
    assertThat(node.getAssetId(), is(nullValue()));
  }

  @Test
  public void testDeleteByComponentId() {
    BrowseNodeEntityAdapter entityAdapter = mock(BrowseNodeEntityAdapter.class);
    BrowseNodeConfiguration configuration = new BrowseNodeConfiguration(true, true, 1000, 10000, 10000, 10, 10);
    underTest = new BrowseNodeStoreImpl(database.getInstanceProvider(), entityAdapter, configuration);

    EntityId id = mock(EntityId.class);
    BrowseNode node = new BrowseNode().withComponentId(mock(EntityId.class));
    when(entityAdapter.getByComponentId(any(), eq(id))).thenReturn(Arrays.asList(node));

    underTest.deleteNodeByComponentId(id);

    verify(entityAdapter).save(any(), eq(node), eq(false));
    assertThat(node.getComponentId(), is(nullValue()));
  }

  @Test
  public void testDeleteNode_concurrency() throws InterruptedException, ExecutionException {
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      long startTime = System.currentTimeMillis();
      while (System.currentTimeMillis() - startTime < 5000) {
        BrowseNode a = underTest.createNodes(PERM_REPOSITORY_NAME, Arrays.asList("rnd", "a"));
        BrowseNode b = underTest.createNodes(PERM_REPOSITORY_NAME, Arrays.asList("rnd", "b"));
        List<Future<Object>> futures = executor.invokeAll(Arrays.asList(delete(a), delete(b)));
        futures.get(0).get();
        futures.get(1).get();
        assertThat(Iterables.size(underTest.getByPath(PERM_REPOSITORY_NAME, Collections.singletonList("rnd"))), is(0));
      }
    }
    finally {
      executor.shutdownNow();
    }
  }

  @Test
  public void testTruncateRepository_multipleIterations() {
    when(securityHelper.anyPermitted(any())).thenReturn(true);
    for (int i = 0; i < 20; i++) {
      underTest.createNodes(REPOSITORY_NAME, Arrays.asList("asset" + i));
    }

    assertThat(Iterables.size(underTest.getChildrenByPath(repository, Collections.emptyList(), 100, null)), is(20));

    underTest.truncateRepository(REPOSITORY_NAME);

    assertThat(Iterables.size(underTest.getChildrenByPath(repository, Collections.emptyList(), 100, null)), is(0));
  }

  @Test
  public void testUpdateChildren() {
    when(securityHelper.anyPermitted(any())).thenReturn(true);
    for (int i = 0; i < 5; i++) {
      underTest.createNodes(REPOSITORY_NAME, Arrays.asList("com", "asset" + i), false);
    }

    //add 5 from another repo that should not be updated
    for (int i = 0; i < 5; i++) {
      underTest.createNodes("junk", Arrays.asList("org", "asset" + i), false);
    }

    browseNodeEntityAdapter.browseDocuments(database.getInstance().acquire()).forEach(document -> {
      String path = document.field(P_PATH);
      Set<OIdentifiable> childrenIds = document.field(P_CHILDREN_IDS);
      assertThat(childrenIds.size(), is(0));
    });

    underTest.updateChildNodes(REPOSITORY_NAME);

    browseNodeEntityAdapter.browseDocuments(database.getInstance().acquire()).forEach(document -> {
      String path = document.field(P_PATH);
      Set<OIdentifiable> childrenIds = document.field(P_CHILDREN_IDS);
      if ("com".equals(path)) {
        assertThat(childrenIds.size(), is(5));
      }
      else {
        assertThat(childrenIds.size(), is(0));
      }
    });
  }

  private Callable<Object> delete(final BrowseNode x) {
    return new Callable<Object>()
    {

      @Override
      public Object call() throws Exception {
        underTest.deleteNode(x);
        return null;
      }
    };
  }

  private BrowseNode getByEntityId(final EntityId entityId) {
    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      return browseNodeEntityAdapter.read(db, entityId);
    }
  }
}
