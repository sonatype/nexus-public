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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.orient.HexRecordIdObfuscator;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseTestSupport;
import org.sonatype.nexus.repository.browse.internal.orient.BrowseNodeEntityAdapter;
import org.sonatype.nexus.repository.browse.node.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.browse.node.RebuildBrowseNodesTaskDescriptor;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.ossindex.PackageUrlService;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.BucketEntityAdapter;
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter;
import org.sonatype.nexus.repository.storage.ComponentFactory;
import org.sonatype.nexus.repository.storage.MetadataNode;
import org.sonatype.nexus.scheduling.CurrentState;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskSchedulerImpl;

import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.browse.node.RebuildBrowseNodesTaskDescriptor.REPOSITORY_NAME_FIELD_ID;
import static org.sonatype.nexus.repository.storage.BucketEntityAdapter.P_PENDING_DELETION;

public class RebuildBrowseNodesManagerTest
    extends BrowseTestSupport
{
  private static final String REPOSITORY_NAME = "repo";

  private static final String REPOSITORY2_NAME = "repo2";

  @Rule
  public DatabaseInstanceRule databaseInstanceRule = DatabaseInstanceRule.inMemory("test");

  private RebuildBrowseNodesManager underTest;

  private BucketEntityAdapter bucketEntityAdapter;

  private AssetEntityAdapter assetEntityAdapter;

  private ComponentEntityAdapter componentEntityAdapter;

  private Bucket bucket;

  private Bucket bucket2;

  private BrowseNodeEntityAdapter browseNodeEntityAdapter;

  @Mock
  private ComponentFactory componentFactory;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private Repository repository;

  @Mock
  private Repository repository2;

  @Mock
  private TaskSchedulerImpl taskScheduler;

  @Mock
  private TaskInfo taskInfo;

  @Mock
  private TaskInfo taskInfo2;

  @Mock
  private CurrentState currentState;

  @Mock
  private CurrentState currentState2;

  @Mock
  private BrowseNodeConfiguration configuration;

  @Mock
  private PackageUrlService packageUrlService;

  @Before
  public void configure() throws Exception {
    when(configuration.isAutomaticRebuildEnabled()).thenReturn(true);
    when(repositoryManager.browse()).thenReturn(Arrays.asList(repository, repository2));
    when(repository.getName()).thenReturn(REPOSITORY_NAME);
    when(repository2.getName()).thenReturn(REPOSITORY2_NAME);

    initializeDatabase();

    bucket = createBucket(REPOSITORY_NAME);
    bucket2 = createBucket(REPOSITORY2_NAME);

    bucketEntityAdapter.addEntity(databaseInstanceRule.getInstance().acquire(), bucket);
    bucketEntityAdapter.addEntity(databaseInstanceRule.getInstance().acquire(), bucket2);
  }

  private void initializeDatabase() throws Exception {
    bucketEntityAdapter = new BucketEntityAdapter();
    componentEntityAdapter = new ComponentEntityAdapter(bucketEntityAdapter, componentFactory, emptySet());
    assetEntityAdapter = new AssetEntityAdapter(bucketEntityAdapter, componentEntityAdapter);
    when(packageUrlService.getPackageUrl(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Optional.empty());
    browseNodeEntityAdapter = new BrowseNodeEntityAdapter(componentEntityAdapter, assetEntityAdapter, packageUrlService);
    underTest = new RebuildBrowseNodesManager(databaseInstanceRule.getInstanceProvider(), taskScheduler, configuration,
        bucketEntityAdapter);

    bucketEntityAdapter.register(databaseInstanceRule.getInstance().acquire());
    componentEntityAdapter.register(databaseInstanceRule.getInstance().acquire());
    assetEntityAdapter.register(databaseInstanceRule.getInstance().acquire());
    browseNodeEntityAdapter.register(databaseInstanceRule.getInstance().acquire());

    bucketEntityAdapter.enableObfuscation(new HexRecordIdObfuscator());
    componentEntityAdapter.enableObfuscation(new HexRecordIdObfuscator());
    assetEntityAdapter.enableObfuscation(new HexRecordIdObfuscator());
    browseNodeEntityAdapter.enableObfuscation(new HexRecordIdObfuscator());
  }

  @Test
  public void doStartRunsExistingTask() throws Exception {
    setupExistingTask();

    assetEntityAdapter.addEntity(databaseInstanceRule.getInstance().acquire(), createAsset("asset", "maven2", bucket));

    underTest.doStart();
    //only called when no match found, so should not be called
    verify(taskScheduler, never()).createTaskConfigurationInstance(RebuildBrowseNodesTaskDescriptor.TYPE_ID);
  }

  @Test
  public void doStartSkipsTaskSchedulingForNoAssets() {
    underTest.doStart();

    verifyNoMoreInteractions(taskScheduler);
  }

  @Test
  public void doStartSchedulesTaskIfThereAreNoBrowseNodes() throws Exception {
    TaskConfiguration taskConfiguration = new TaskConfiguration();
    assetEntityAdapter.addEntity(databaseInstanceRule.getInstance().acquire(), createAsset("asset", "maven2", bucket));
    when(taskScheduler.createTaskConfigurationInstance(RebuildBrowseNodesTaskDescriptor.TYPE_ID))
        .thenReturn(taskConfiguration);

    underTest.doStart();
    assertThat(taskConfiguration.getString(REPOSITORY_NAME_FIELD_ID), is(REPOSITORY_NAME));
    verify(taskScheduler).createTaskConfigurationInstance(RebuildBrowseNodesTaskDescriptor.TYPE_ID);
    verify(taskScheduler).submit(taskConfiguration);
  }

  @Test
  public void doStartSkipsTaskSchedulingIfThereAreBrowseNodes() throws Exception {
    TaskConfiguration taskConfiguration = new TaskConfiguration();
    Asset asset = createAsset("asset", "maven2", bucket);

    try (ODatabaseDocumentTx db = databaseInstanceRule.getInstance().acquire()) {
      assetEntityAdapter.addEntity(db, asset);
    }
    try (ODatabaseDocumentTx db = databaseInstanceRule.getInstance().acquire()) {
      browseNodeEntityAdapter
          .createAssetNode(db, REPOSITORY_NAME, "maven2", toBrowsePaths(singletonList(asset.name())), asset);
    }

    when(taskScheduler.createTaskConfigurationInstance(RebuildBrowseNodesTaskDescriptor.TYPE_ID))
        .thenReturn(taskConfiguration);

    underTest.doStart();

    verifyNoMoreInteractions(taskScheduler);
  }

  @Test
  public void doStartSkipsTaskSchedulingIfBucketIsPendingDeletion() throws Exception {
    try (ODatabaseDocumentTx db = databaseInstanceRule.getInstance().acquire()) {
      Bucket bucket = bucketEntityAdapter.read(db, REPOSITORY_NAME);
      bucket.attributes().set(P_PENDING_DELETION, true);
      bucketEntityAdapter.editEntity(db, bucket);
    }
    try (ODatabaseDocumentTx db = databaseInstanceRule.getInstance().acquire()) {
      assetEntityAdapter.addEntity(db, createAsset("asset", "maven2", bucket));
    }

    underTest.doStart();

    verifyNoMoreInteractions(taskScheduler);
  }

  @Test
  public void doStartIncludesRepositoriesWithZeroAssets() throws Exception {
    TaskConfiguration taskConfiguration = new TaskConfiguration();
    Asset asset = createAsset("asset", "maven2", bucket);

    try (ODatabaseDocumentTx db = databaseInstanceRule.getInstance().acquire()) {
      assetEntityAdapter.addEntity(db, asset);
    }
    try (ODatabaseDocumentTx db = databaseInstanceRule.getInstance().acquire()) {
      browseNodeEntityAdapter
          .createAssetNode(db, REPOSITORY_NAME, "maven2", toBrowsePaths(singletonList(asset.name())), asset);
    }
    try (ODatabaseDocumentTx db = databaseInstanceRule.getInstance().acquire()) {
      assetEntityAdapter.deleteEntity(db, asset);
    }

    when(taskScheduler.createTaskConfigurationInstance(RebuildBrowseNodesTaskDescriptor.TYPE_ID))
        .thenReturn(taskConfiguration);

    underTest.doStart();
    assertThat(taskConfiguration.getString(REPOSITORY_NAME_FIELD_ID), is(REPOSITORY_NAME));
    verify(taskScheduler).createTaskConfigurationInstance(RebuildBrowseNodesTaskDescriptor.TYPE_ID);
    verify(taskScheduler).submit(taskConfiguration);
  }

  @Test
  public void doStartIncludesMultipleRepositories() throws Exception {
    TaskConfiguration taskConfiguration = new TaskConfiguration();
    Asset asset = createAsset("asset", "maven2", bucket);
    Asset asset2 = createAsset("asset", "maven2", bucket2);

    try (ODatabaseDocumentTx db = databaseInstanceRule.getInstance().acquire()) {
      assetEntityAdapter.addEntity(db, asset);
      assetEntityAdapter.addEntity(db, asset2);
    }

    when(taskScheduler.createTaskConfigurationInstance(RebuildBrowseNodesTaskDescriptor.TYPE_ID))
        .thenReturn(taskConfiguration);

    underTest.doStart();
    assertThat(taskConfiguration.getString(REPOSITORY_NAME_FIELD_ID), is(REPOSITORY_NAME + "," + REPOSITORY2_NAME));
    verify(taskScheduler).createTaskConfigurationInstance(RebuildBrowseNodesTaskDescriptor.TYPE_ID);
    verify(taskScheduler).submit(taskConfiguration);
  }

  @Test
  public void doStart_rebuildDisabled() throws Exception {
    when(configuration.isAutomaticRebuildEnabled()).thenReturn(false);
    underTest = new RebuildBrowseNodesManager(databaseInstanceRule.getInstanceProvider(), taskScheduler, configuration,
        bucketEntityAdapter);
    TaskConfiguration taskConfiguration = new TaskConfiguration();
    assetEntityAdapter.addEntity(databaseInstanceRule.getInstance().acquire(), createAsset("asset", "maven2", bucket));
    when(taskScheduler.createTaskConfigurationInstance(RebuildBrowseNodesTaskDescriptor.TYPE_ID)).thenReturn(
        taskConfiguration);

    underTest.doStart();
    assertNull(taskConfiguration.getString(REPOSITORY_NAME_FIELD_ID));
  }

  private void setupExistingTask() {
    when(taskScheduler.findAndSubmit(RebuildBrowseNodesTaskDescriptor.TYPE_ID,
        ImmutableMap.of(RebuildBrowseNodesTaskDescriptor.REPOSITORY_NAME_FIELD_ID, REPOSITORY_NAME)))
        .thenReturn(true);
    when(taskScheduler.findAndSubmit(RebuildBrowseNodesTaskDescriptor.TYPE_ID,
        ImmutableMap.of(RebuildBrowseNodesTaskDescriptor.REPOSITORY_NAME_FIELD_ID, "badreponame")))
        .thenReturn(true);
  }

  private Asset createAsset(final String name, final String format, final Bucket bucket) throws Exception {
    Asset asset = new Asset();
    asset.name(name);

    Method m = MetadataNode.class.getDeclaredMethod("format", String.class);
    m.setAccessible(true);
    m.invoke(asset, format);

    m = MetadataNode.class.getDeclaredMethod("bucketId", EntityId.class);
    m.setAccessible(true);
    m.invoke(asset, EntityHelper.id(bucket));

    m = MetadataNode.class.getDeclaredMethod("attributes", NestedAttributesMap.class);
    m.setAccessible(true);
    m.invoke(asset, new NestedAttributesMap("attributes", new HashMap<>()));

    return asset;
  }

  private Bucket createBucket(final String repositoryName) throws Exception {
    Bucket bucket = new Bucket();
    bucket.setRepositoryName(repositoryName);

    Method m = Bucket.class.getDeclaredMethod("attributes", NestedAttributesMap.class);
    m.setAccessible(true);
    m.invoke(bucket, new NestedAttributesMap("attributes", new HashMap<>()));

    return bucket;
  }
}
