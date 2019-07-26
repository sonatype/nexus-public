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
import java.util.Collections;
import java.util.HashMap;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.orient.HexRecordIdObfuscator;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.browse.BrowseTestSupport;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.BrowseNodeEntityAdapter;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.BucketEntityAdapter;
import org.sonatype.nexus.repository.storage.ComponentEntityAdapter;
import org.sonatype.nexus.repository.storage.ComponentFactory;
import org.sonatype.nexus.repository.storage.MetadataNode;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.TaskInfo.CurrentState;
import org.sonatype.nexus.scheduling.TaskInfo.State;
import org.sonatype.nexus.scheduling.TaskScheduler;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.browse.internal.RebuildBrowseNodesTaskDescriptor.REPOSITORY_NAME_FIELD_ID;
import static org.sonatype.nexus.repository.storage.BucketEntityAdapter.P_PENDING_DELETION;

public class RebuildBrowseNodesManagerTest
    extends BrowseTestSupport
{
  private static final String REPOSITORY_NAME = "repo";
  @Rule
  public DatabaseInstanceRule databaseInstanceRule = DatabaseInstanceRule.inMemory("test");

  private RebuildBrowseNodesManager underTest;

  private BucketEntityAdapter bucketEntityAdapter;

  private AssetEntityAdapter assetEntityAdapter;

  private ComponentEntityAdapter componentEntityAdapter;

  private Bucket bucket;

  private BrowseNodeEntityAdapter browseNodeEntityAdapter;

  @Mock
  private ComponentFactory componentFactory;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private Repository repository;

  @Mock
  private TaskScheduler taskScheduler;

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

  @Before
  public void configure() throws Exception {
    when(configuration.isAutomaticRebuildEnabled()).thenReturn(true);
    when(repositoryManager.browse()).thenReturn(Collections.singleton(repository));
    when(repository.getName()).thenReturn(REPOSITORY_NAME);

    initializeDatabase();

    bucket = createBucket(REPOSITORY_NAME);

    bucketEntityAdapter.addEntity(databaseInstanceRule.getInstance().acquire(), bucket);
  }

  private void initializeDatabase() throws Exception {
    bucketEntityAdapter = new BucketEntityAdapter();
    componentEntityAdapter = new ComponentEntityAdapter(bucketEntityAdapter, componentFactory, emptySet());
    assetEntityAdapter = new AssetEntityAdapter(bucketEntityAdapter, componentEntityAdapter);
    browseNodeEntityAdapter = new BrowseNodeEntityAdapter(componentEntityAdapter, assetEntityAdapter);
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
    setupExistingTask(false);

    assetEntityAdapter.addEntity(databaseInstanceRule.getInstance().acquire(), createAsset("asset", "maven2", bucket));

    underTest.doStart();
    //doesn't match the repo, so shouldn't be called
    verify(taskInfo, never()).runNow();
    //matches the repo so should be called
    verify(taskInfo2).runNow();
    //only called when no match found, so should not be called
    verify(taskScheduler, never()).createTaskConfigurationInstance(RebuildBrowseNodesTaskDescriptor.TYPE_ID);
  }

  @Test
  public void doStartDoesntRunExistingRunningTask() throws Exception {
    setupExistingTask(true);

    assetEntityAdapter.addEntity(databaseInstanceRule.getInstance().acquire(), createAsset("asset", "maven2", bucket));

    underTest.doStart();
    //as the task should already be running, we shouldn't call runNow on either of these tasks
    verify(taskInfo, never()).runNow();
    verify(taskInfo2, never()).runNow();
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

  private void setupExistingTask(final boolean running) {
    TaskConfiguration matchConfiguration = new TaskConfiguration();
    matchConfiguration.setTypeId(RebuildBrowseNodesTaskDescriptor.TYPE_ID);
    matchConfiguration.setString(RebuildBrowseNodesTaskDescriptor.REPOSITORY_NAME_FIELD_ID, REPOSITORY_NAME);
    TaskConfiguration noMatchConfiguration = new TaskConfiguration();
    noMatchConfiguration.setTypeId(RebuildBrowseNodesTaskDescriptor.TYPE_ID);
    noMatchConfiguration.setString(RebuildBrowseNodesTaskDescriptor.REPOSITORY_NAME_FIELD_ID, "badreponame");
    when(taskInfo.getConfiguration()).thenReturn(noMatchConfiguration);
    when(taskInfo.getCurrentState()).thenReturn(currentState);
    when(taskInfo2.getConfiguration()).thenReturn(matchConfiguration);
    when(taskInfo2.getCurrentState()).thenReturn(currentState2);
    when(currentState.getState()).thenReturn(State.WAITING);
    when(currentState2.getState()).thenReturn(running ? State.RUNNING : State.WAITING);
    when(taskScheduler.listsTasks()).thenReturn(Arrays.asList(taskInfo, taskInfo2));
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
