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
package org.sonatype.nexus.blobstore.restore.orient;

import java.net.URL;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import org.sonatype.nexus.repository.move.ChangeRepositoryBlobStoreStore;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.api.BlobStoreUsageChecker;
import org.sonatype.nexus.blobstore.file.FileBlobAttributes;
import org.sonatype.nexus.blobstore.restore.RestoreBlobStrategy;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.maintenance.MaintenanceService;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.BucketStore;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.HEADER_PREFIX;
import static org.sonatype.nexus.blobstore.api.BlobStore.REPO_NAME_HEADER;
import static org.sonatype.nexus.blobstore.restore.BaseRestoreMetadataTaskDescriptor.BLOB_STORE_NAME_FIELD_ID;
import static org.sonatype.nexus.blobstore.restore.BaseRestoreMetadataTaskDescriptor.DRY_RUN;
import static org.sonatype.nexus.blobstore.restore.BaseRestoreMetadataTaskDescriptor.INTEGRITY_CHECK;
import static org.sonatype.nexus.blobstore.restore.BaseRestoreMetadataTaskDescriptor.RESTORE_BLOBS;
import static org.sonatype.nexus.blobstore.restore.BaseRestoreMetadataTaskDescriptor.SINCE_DAYS;
import static org.sonatype.nexus.blobstore.restore.BaseRestoreMetadataTaskDescriptor.TYPE_ID;
import static org.sonatype.nexus.blobstore.restore.BaseRestoreMetadataTaskDescriptor.UNDELETE_BLOBS;
import static org.sonatype.nexus.blobstore.restore.orient.DefaultOrientIntegrityCheckStrategy.DEFAULT_NAME;

public class OrientRestoreMetadataTaskTest
    extends TestSupport
{

  public static final String BLOBSTORE_NAME = "test";

  @Mock
  BlobStoreManager blobStoreManager;

  @Mock
  ChangeRepositoryBlobStoreStore changeBlobstoreStore;

  @Mock
  RepositoryManager repositoryManager;

  @Mock
  RestoreBlobStrategy restoreBlobStrategy;

  @Mock
  Repository repository;

  @Mock
  BlobStore blobStore;

  @Mock
  Blob blob;

  @Mock
  Format mavenFormat;

  @Mock
  BlobStoreUsageChecker blobstoreUsageChecker;

  @Mock
  DryRunPrefix dryRunPrefix;

  @Mock
  DefaultOrientIntegrityCheckStrategy orientDefaultIntegrityCheckStrategy;

  @Mock
  OrientIntegrityCheckStrategy testOrientIntegrityCheckStrategy;

  @Mock
  BucketStore bucketStore;

  @Mock
  MaintenanceService maintenanceService;

  @Mock
  TaskUtils taskUtils;

  OrientRestoreMetadataTask underTest;

  Map<String, OrientIntegrityCheckStrategy> integrityCheckStrategies;

  BlobId blobId;

  FileBlobAttributes blobAttributes;

  TaskConfiguration configuration;

  @Before
  public void setup() throws Exception {
    integrityCheckStrategies = spy(new HashMap<>());
    integrityCheckStrategies.put("maven2", testOrientIntegrityCheckStrategy);
    integrityCheckStrategies.put(DEFAULT_NAME, orientDefaultIntegrityCheckStrategy);

    underTest =
        new OrientRestoreMetadataTask(blobStoreManager,changeBlobstoreStore, repositoryManager, ImmutableMap.of("maven2", restoreBlobStrategy),
            blobstoreUsageChecker, dryRunPrefix, integrityCheckStrategies, bucketStore, maintenanceService,taskUtils);

    reset(integrityCheckStrategies); // reset this mock so we more easily verify calls

    configuration = new TaskConfiguration();
    configuration.setString(BLOB_STORE_NAME_FIELD_ID, BLOBSTORE_NAME);
    configuration.setString(".name", "test");
    configuration.setId(BLOBSTORE_NAME);
    configuration.setTypeId(TYPE_ID);

    when(repositoryManager.get("maven-central")).thenReturn(repository);
    when(repository.isStarted()).thenReturn(true);
    when(repository.getFormat()).thenReturn(mavenFormat);
    when(mavenFormat.getValue()).thenReturn("maven2");

    URL resource = Resources
        .getResource("test-restore/content/vol-1/chp-1/86e20baa-0bca-4915-a7dc-9a4f34e72321.properties");
    blobAttributes = new FileBlobAttributes(Paths.get(resource.toURI()));
    blobAttributes.load();
    blobId = new BlobId("86e20baa-0bca-4915-a7dc-9a4f34e72321");
    when(blobStore.getBlobIdStream()).thenReturn(Stream.of(blobId));
    when(blobStore.getBlobIdUpdatedSinceStream(anyInt())).thenReturn(Stream.of(blobId));
    when(blobStoreManager.get(BLOBSTORE_NAME)).thenReturn(blobStore);

    when(blobStore.get(blobId, true)).thenReturn(blob);
    when(blobStore.getBlobAttributes(blobId)).thenReturn(blobAttributes);

    when(dryRunPrefix.get()).thenReturn("");
  }

  @Test
  public void checkForConflictsRunsIfNoChangeStoreProvider() {
    OrientRestoreMetadataTask underTest = new OrientRestoreMetadataTask(blobStoreManager,null, repositoryManager, ImmutableMap.of("maven2", restoreBlobStrategy),
        blobstoreUsageChecker, dryRunPrefix, integrityCheckStrategies, bucketStore, maintenanceService,taskUtils);

    underTest.configure(configuration);

    doNothing().when(taskUtils).checkForConflictingTasks(anyString(), anyString(), any(List.class), any(Map.class));
    underTest.checkForConflicts();

    verify(taskUtils, times(1)).checkForConflictingTasks(anyString(), anyString(), any(List.class), any(Map.class));
  }

  @Test
  public void testRestoreMetadata() throws Exception {
    configuration.setBoolean(RESTORE_BLOBS, true);
    configuration.setBoolean(UNDELETE_BLOBS, true);
    configuration.setBoolean(INTEGRITY_CHECK, false);
    underTest.configure(configuration);

    underTest.execute();

    ArgumentCaptor<Properties> propertiesArgumentCaptor = ArgumentCaptor.forClass(Properties.class);
    verify(restoreBlobStrategy).restore(propertiesArgumentCaptor.capture(), eq(blob), eq(blobStore), eq(false));
    verify(blobStore).undelete(blobstoreUsageChecker, blobId, blobAttributes, false);
    Properties properties = propertiesArgumentCaptor.getValue();

    assertThat(properties.getProperty("@BlobStore.blob-name"), is("org/codehaus/plexus/plexus/3.1/plexus-3.1.pom"));
  }

  @Test
  public void testRestoreMetadataNoUnDelete() throws Exception {
    configuration.setBoolean(RESTORE_BLOBS, true);
    configuration.setBoolean(UNDELETE_BLOBS, false);
    configuration.setBoolean(INTEGRITY_CHECK, false);
    underTest.configure(configuration);

    underTest.execute();

    verify(restoreBlobStrategy).restore(any(), eq(blob), eq(blobStore), eq(false));
    verify(blobStore, never()).undelete(any(), any(), any(), eq(false));
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testRestoreMetadata_BlobIsMarkedAsDeleted() throws Exception {
    configuration.setBoolean(RESTORE_BLOBS, true);
    configuration.setBoolean(UNDELETE_BLOBS, true);
    configuration.setBoolean(INTEGRITY_CHECK, false);
    underTest.configure(configuration);

    blobAttributes.setDeleted(true);

    underTest.execute();

    verify(restoreBlobStrategy, never()).restore(any(), any(), any());
    verify(blobStore).undelete(any(), any(), any(), eq(false));
  }

  @Test
  public void testNoRestoreMetadataNoUnDeleteNoIntegrityCheck() throws Exception {
    configuration.setBoolean(RESTORE_BLOBS, false);
    configuration.setBoolean(UNDELETE_BLOBS, false);
    configuration.setBoolean(INTEGRITY_CHECK, false);

    underTest.configure(configuration);

    underTest.execute();

    verifyNoInteractions(blobStoreManager);
  }

  @Test
  public void testIntegrityCheck_BlobStoreDoesNotExist() throws Exception {
    configuration.setBoolean(RESTORE_BLOBS, false);
    configuration.setBoolean(UNDELETE_BLOBS, false);
    configuration.setBoolean(INTEGRITY_CHECK, true);
    underTest.configure(configuration);

    when(blobStoreManager.get(anyString())).thenReturn(null);

    underTest.execute();

    verifyNoInteractions(repositoryManager);
  }

  @Test
  public void testIntegrityCheckNoRepositories() throws Exception {
    configuration.setBoolean(RESTORE_BLOBS, false);
    configuration.setBoolean(UNDELETE_BLOBS, false);
    configuration.setBoolean(INTEGRITY_CHECK, true);
    underTest.configure(configuration);

    when(repositoryManager.browseForBlobStore(any())).thenReturn(Collections.emptyList());

    underTest.execute();

    verifyNoInteractions(integrityCheckStrategies);
  }

  @Test
  public void testIntegrityCheckNullRepository() throws Exception {
    configuration.setBoolean(RESTORE_BLOBS, false);
    configuration.setBoolean(UNDELETE_BLOBS, false);
    configuration.setBoolean(INTEGRITY_CHECK, true);
    underTest.configure(configuration);

    List<Repository> repositories = singletonList(null);
    when(repositoryManager.browseForBlobStore(any())).thenReturn(repositories);

    underTest.execute();

    verifyNoInteractions(integrityCheckStrategies);
  }

  @Test
  public void testIntegrityCheck_SkipGroupRepositories() throws Exception {
    configuration.setBoolean(RESTORE_BLOBS, false);
    configuration.setBoolean(UNDELETE_BLOBS, false);
    configuration.setBoolean(INTEGRITY_CHECK, true);
    underTest.configure(configuration);

    when(repository.getType()).thenReturn(new GroupType());
    when(repositoryManager.browseForBlobStore(any())).thenReturn(singletonList(repository));

    underTest.execute();

    verifyNoInteractions(integrityCheckStrategies);
  }

  @Test
  public void testIntegrityCheck_SkipNotStartedRepositories() throws Exception {
    configuration.setBoolean(RESTORE_BLOBS, false);
    configuration.setBoolean(UNDELETE_BLOBS, false);
    configuration.setBoolean(INTEGRITY_CHECK, true);
    underTest.configure(configuration);

    when(repository.isStarted()).thenReturn(false);
    when(repositoryManager.browseForBlobStore(any())).thenReturn(singletonList(repository));

    underTest.execute();

    verifyNoInteractions(integrityCheckStrategies);
  }

  @Test
  public void testIntegrityCheck_DefaultStrategy() throws Exception {
    configuration.setBoolean(RESTORE_BLOBS, false);
    configuration.setBoolean(UNDELETE_BLOBS, false);
    configuration.setBoolean(INTEGRITY_CHECK, true);
    underTest.configure(configuration);

    when(repositoryManager.browseForBlobStore(any())).thenReturn(singletonList(repository));

    // this should return the DefaultIntegrityCheckStrategy
    when(mavenFormat.getValue()).thenReturn("foo");

    underTest.execute();

    verify(orientDefaultIntegrityCheckStrategy).check(any(), any(), any(), any());
    verifyNoInteractions(testOrientIntegrityCheckStrategy);
  }

  @Test
  public void testIntegrityCheck() throws Exception {
    configuration.setBoolean(RESTORE_BLOBS, false);
    configuration.setBoolean(UNDELETE_BLOBS, false);
    configuration.setBoolean(INTEGRITY_CHECK, true);
    underTest.configure(configuration);

    when(repositoryManager.browseForBlobStore(any())).thenReturn(singletonList(repository));

    underTest.execute();

    verifyNoInteractions(orientDefaultIntegrityCheckStrategy);
    verify(testOrientIntegrityCheckStrategy).check(eq(repository), eq(blobStore), any(), any());
  }

  @Test
  public void updateAfterAssetsWhenCallAfter() throws Exception {
    configuration.setBoolean(RESTORE_BLOBS, true);
    configuration.setBoolean(UNDELETE_BLOBS, true);
    configuration.setBoolean(INTEGRITY_CHECK, false);
    underTest.configure(configuration);

    underTest.execute();

    verify(restoreBlobStrategy).after(true, repository);
  }

  @Test
  public void doNotUpdateAfterAssetsWhenDryRun() throws Exception {
    configuration.setBoolean(RESTORE_BLOBS, true);
    configuration.setBoolean(UNDELETE_BLOBS, true);
    configuration.setBoolean(INTEGRITY_CHECK, false);
    configuration.setBoolean(DRY_RUN, true);
    underTest.configure(configuration);

    underTest.execute();

    verify(restoreBlobStrategy, never()).after(false, repository);
  }

  @Test
  public void doNotUpdateAfterAssetsWhenRestoreFalse() throws Exception {
    configuration.setBoolean(RESTORE_BLOBS, false);
    configuration.setBoolean(UNDELETE_BLOBS, true);
    configuration.setBoolean(INTEGRITY_CHECK, false);
    configuration.setBoolean(DRY_RUN, true);
    underTest.configure(configuration);

    underTest.execute();

    verify(restoreBlobStrategy, never()).after(false, repository);
  }

  @Test
  public void whenAfterCallRunningShouldBeCancelable() throws Exception {
    configuration.setBoolean(RESTORE_BLOBS, true);
    configuration.setBoolean(UNDELETE_BLOBS, true);
    configuration.setBoolean(INTEGRITY_CHECK, false);

    OrientRestoreMetadataTask underTest =
        new OrientRestoreMetadataTask(blobStoreManager, changeBlobstoreStore, repositoryManager,
            ImmutableMap.of("maven2", restoreBlobStrategy),
            blobstoreUsageChecker, dryRunPrefix, integrityCheckStrategies, bucketStore, maintenanceService, taskUtils)
        {
      @Override
      public boolean isCanceled() {
        return true;
      }
    };

    underTest.configure(configuration);

    underTest.execute();

    verify(restoreBlobStrategy, never()).after(true, repository);
  }

  @Test
  public void whenUnknownFormatAfterCallWillNotRun() throws Exception {
    when(mavenFormat.getValue()).thenReturn("unknownFormat");

    configuration.setBoolean(RESTORE_BLOBS, true);
    configuration.setBoolean(UNDELETE_BLOBS, true);
    configuration.setBoolean(INTEGRITY_CHECK, false);
    underTest.configure(configuration);

    underTest.execute();

    verify(restoreBlobStrategy, never()).after(true, repository);
  }

  @Test
  public void whenBlobsFromDifferentRepositoriesNeedUpdatingAfterIsCalledForEachRepository() throws Exception {
    BlobAttributes blobAttributes2 = mock(BlobAttributes.class);
    Properties properties = mock(Properties.class);
    Repository repository2 = mock(Repository.class);

    BlobId blobId2 = new BlobId("86e20baa-0bca-4915-a7dc-9a4f34e72322");
    when(blobStore.get(blobId2, true)).thenReturn(blob);
    when(blobStore.getBlobAttributes(blobId2)).thenReturn(blobAttributes2);
    when(blobAttributes2.getProperties()).thenReturn(properties);
    when(properties.getProperty(HEADER_PREFIX + REPO_NAME_HEADER)).thenReturn("maven-central2");
    when(repositoryManager.get("maven-central2")).thenReturn(repository2);
    when(repository2.getFormat()).thenReturn(mavenFormat);

    when(blobStore.getBlobIdStream()).thenReturn(Stream.of(blobId, blobId2));

    configuration.setBoolean(RESTORE_BLOBS, true);
    configuration.setBoolean(UNDELETE_BLOBS, true);
    configuration.setBoolean(INTEGRITY_CHECK, false);
    underTest.configure(configuration);

    underTest.execute();

    verify(restoreBlobStrategy).after(true, repository);
    verify(restoreBlobStrategy).after(true, repository2);
  }

  @Test
  public void taskWillNotFailIfOneBlobThrowsException() throws Exception {
    BlobAttributes blobAttributes2 = mock(BlobAttributes.class);
    Properties properties2 = mock(Properties.class);
    Repository repository2 = mock(Repository.class);

    BlobId exceptionBlobId = new BlobId("86e20baa-0bca-4915-a7dc-9a4f34e72322");
    Blob exceptionBlob = mock(Blob.class);
    when(blobStore.get(exceptionBlobId, true)).thenReturn(exceptionBlob);
    when(blobStore.getBlobAttributes(exceptionBlobId)).thenReturn(blobAttributes2);
    BlobStoreConfiguration blobStoreConfiguration = mock(BlobStoreConfiguration.class);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);
    when(blobAttributes2.getProperties()).thenReturn(properties2);
    when(properties2.getProperty(HEADER_PREFIX + REPO_NAME_HEADER)).thenReturn("maven-central2");
    when(repositoryManager.get("maven-central2")).thenReturn(repository2);
    when(repository2.getFormat()).thenReturn(mavenFormat);

    doThrow(new RuntimeException())
        .when(restoreBlobStrategy)
        .restore(properties2, exceptionBlob, blobStore, false);

    BlobAttributes blobAttributes3 = mock(BlobAttributes.class);
    Properties properties3 = mock(Properties.class);
    Repository repository3 = mock(Repository.class);

    BlobId blobId3 = new BlobId("86e20baa-0bca-4915-a7dc-9a4f34e72324");
    when(blobStore.get(blobId3, true)).thenReturn(blob);
    when(blobStore.getBlobAttributes(blobId3)).thenReturn(blobAttributes3);
    when(blobAttributes3.getProperties()).thenReturn(properties3);
    when(properties3.getProperty(HEADER_PREFIX + REPO_NAME_HEADER)).thenReturn("maven-central2");
    when(repositoryManager.get("maven-central2")).thenReturn(repository3);
    when(repository3.getFormat()).thenReturn(mavenFormat);

    when(blobStore.getBlobIdStream()).thenReturn(Stream.of(blobId, exceptionBlobId, blobId3));

    configuration.setBoolean(RESTORE_BLOBS, true);
    configuration.setBoolean(UNDELETE_BLOBS, true);
    configuration.setBoolean(INTEGRITY_CHECK, false);
    underTest.configure(configuration);

    underTest.execute();

    verify(restoreBlobStrategy).restore(properties2, exceptionBlob, blobStore, false);
    verify(restoreBlobStrategy).after(true, repository);
    verify(restoreBlobStrategy, times(0)).after(true, repository2);
    verify(restoreBlobStrategy).after(true, repository3);
  }

  @Test
  public void shouldGetAllBlobsToRestoreWhenSinceDaysSetToNegativeNumber() throws Exception {
    configuration.setBoolean(RESTORE_BLOBS, true);
    configuration.setBoolean(UNDELETE_BLOBS, true);
    configuration.setBoolean(INTEGRITY_CHECK, false);
    configuration.setInteger(SINCE_DAYS, -4);
    underTest.configure(configuration);

    underTest.execute();

    verify(blobStore, never()).getBlobIdUpdatedSinceStream(anyInt());
    verify(blobStore).getBlobIdStream();
  }

  @Test
  public void shouldGetAllBlobsToRestoreWhenSinceDaysNotSet() throws Exception {
    configuration.setBoolean(RESTORE_BLOBS, true);
    configuration.setBoolean(UNDELETE_BLOBS, true);
    configuration.setBoolean(INTEGRITY_CHECK, false);
    underTest.configure(configuration);

    underTest.execute();

    verify(blobStore, never()).getBlobIdUpdatedSinceStream(anyInt());
    verify(blobStore).getBlobIdStream();
  }

  @Test
  public void shouldGetRecentBlobsWhenSinceDaysConfigured() throws Exception {
    configuration.setBoolean(RESTORE_BLOBS, true);
    configuration.setBoolean(UNDELETE_BLOBS, true);
    configuration.setBoolean(INTEGRITY_CHECK, false);
    configuration.setInteger(SINCE_DAYS, 2);
    underTest.configure(configuration);
    when(blobStore.getBlobIdUpdatedSinceStream(2)).thenReturn(Stream.empty());

    underTest.execute();

    verify(blobStore).getBlobIdUpdatedSinceStream(2);
    verify(blobStore, never()).getBlobIdStream();
  }
}
