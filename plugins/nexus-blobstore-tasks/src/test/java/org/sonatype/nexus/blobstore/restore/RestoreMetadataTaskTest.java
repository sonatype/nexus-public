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
package org.sonatype.nexus.blobstore.restore;

import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.api.BlobStoreUsageChecker;
import org.sonatype.nexus.blobstore.file.FileBlobAttributes;
import org.sonatype.nexus.blobstore.file.FileBlobStore;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.scheduling.TaskConfiguration;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.api.BlobAttributesConstants.HEADER_PREFIX;
import static org.sonatype.nexus.blobstore.restore.DefaultIntegrityCheckStrategy.DEFAULT_NAME;
import static org.sonatype.nexus.blobstore.restore.RestoreMetadataTaskDescriptor.BLOB_STORE_NAME_FIELD_ID;
import static org.sonatype.nexus.blobstore.restore.RestoreMetadataTaskDescriptor.DRY_RUN;
import static org.sonatype.nexus.blobstore.restore.RestoreMetadataTaskDescriptor.INTEGRITY_CHECK;
import static org.sonatype.nexus.blobstore.restore.RestoreMetadataTaskDescriptor.RESTORE_BLOBS;
import static org.sonatype.nexus.blobstore.restore.RestoreMetadataTaskDescriptor.TYPE_ID;
import static org.sonatype.nexus.blobstore.restore.RestoreMetadataTaskDescriptor.UNDELETE_BLOBS;
import static org.sonatype.nexus.repository.storage.Bucket.REPO_NAME_HEADER;

public class RestoreMetadataTaskTest
    extends TestSupport
{

  public static final String BLOBSTORE_NAME = "test";

  RestoreMetadataTask underTest;

  @Mock
  BlobStoreManager blobStoreManager;

  @Mock
  RepositoryManager repositoryManager;

  @Mock
  RestoreBlobStrategy restoreBlobStrategy;

  @Mock
  Repository repository;

  @Mock
  FileBlobStore fileBlobStore;

  @Mock
  Blob blob;

  @Mock
  Format mavenFormat;

  @Mock
  BlobStoreUsageChecker blobstoreUsageChecker;

  @Mock
  DryRunPrefix dryRunPrefix;

  @Mock
  StorageTx storageTx;

  @Mock
  DefaultIntegrityCheckStrategy defaultIntegrityCheckStrategy;

  @Mock
  IntegrityCheckStrategy testIntegrityCheckStrategy;

  Map<String, IntegrityCheckStrategy> integrityCheckStrategies;

  BlobId blobId;

  FileBlobAttributes blobAttributes;

  TaskConfiguration configuration;

  @Before
  public void setup() throws Exception {
    integrityCheckStrategies = spy(new HashMap<>());
    integrityCheckStrategies.put(Maven2Format.NAME, testIntegrityCheckStrategy);
    integrityCheckStrategies.put(DEFAULT_NAME, defaultIntegrityCheckStrategy);

    underTest = new RestoreMetadataTask(blobStoreManager, repositoryManager,
        ImmutableMap.of("maven2", restoreBlobStrategy), blobstoreUsageChecker, dryRunPrefix, integrityCheckStrategies);

    reset(integrityCheckStrategies); // reset this mock so we more easily verify calls

    configuration = new TaskConfiguration();
    configuration.setString(BLOB_STORE_NAME_FIELD_ID, BLOBSTORE_NAME);
    configuration.setId(BLOBSTORE_NAME);
    configuration.setTypeId(TYPE_ID);

    when(repositoryManager.get("maven-central")).thenReturn(repository);
    when(repository.getFormat()).thenReturn(mavenFormat);
    when(mavenFormat.getValue()).thenReturn("maven2");

    URL resource = Resources
        .getResource("test-restore/content/vol-1/chp-1/86e20baa-0bca-4915-a7dc-9a4f34e72321.properties");
    blobAttributes = new FileBlobAttributes(Paths.get(resource.toURI()));
    blobAttributes.load();
    blobId = new BlobId("86e20baa-0bca-4915-a7dc-9a4f34e72321");
    when(fileBlobStore.getBlobIdStream()).thenReturn(Stream.of(blobId));
    when(blobStoreManager.get(BLOBSTORE_NAME)).thenReturn(fileBlobStore);

    when(fileBlobStore.get(blobId, true)).thenReturn(blob);
    when(fileBlobStore.getBlobAttributes(blobId)).thenReturn(blobAttributes);

    when(dryRunPrefix.get()).thenReturn("");
  }

  @Test
  public void testRestoreMetadata() throws Exception {
    configuration.setBoolean(RESTORE_BLOBS, true);
    configuration.setBoolean(UNDELETE_BLOBS, true);
    configuration.setBoolean(INTEGRITY_CHECK, false);
    underTest.configure(configuration);

    underTest.execute();

    ArgumentCaptor<Properties> propertiesArgumentCaptor = ArgumentCaptor.forClass(Properties.class);
    verify(restoreBlobStrategy).restore(propertiesArgumentCaptor.capture(), eq(blob), eq(BLOBSTORE_NAME), eq(false));
    verify(fileBlobStore).undelete(blobstoreUsageChecker, blobId, blobAttributes, false);
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

    verify(restoreBlobStrategy).restore(any(), eq(blob), eq(BLOBSTORE_NAME), eq(false));
    verify(fileBlobStore, never()).undelete(any(), any(), any(), eq(false));
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
    verify(fileBlobStore).undelete(any(), any(), any(), eq(false));
  }

  @Test
  public void testNoRestoreMetadataNoUnDeleteNoIntegrityCheck() throws Exception {
    configuration.setBoolean(RESTORE_BLOBS, false);
    configuration.setBoolean(UNDELETE_BLOBS, false);
    configuration.setBoolean(INTEGRITY_CHECK, false);

    underTest.configure(configuration);

    underTest.execute();

    verifyZeroInteractions(blobStoreManager);
  }

  @Test
  public void testIntegrityCheck_BlobStoreDoesNotExist() throws Exception {
    configuration.setBoolean(RESTORE_BLOBS, false);
    configuration.setBoolean(UNDELETE_BLOBS, false);
    configuration.setBoolean(INTEGRITY_CHECK, true);
    underTest.configure(configuration);

    when(blobStoreManager.get(anyString())).thenReturn(null);

    underTest.execute();

    verifyZeroInteractions(repositoryManager);
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

    verifyZeroInteractions(integrityCheckStrategies);
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

    verify(defaultIntegrityCheckStrategy).check(any(), any(), any());
    verifyZeroInteractions(testIntegrityCheckStrategy);
  }

  @Test
  public void testIntegrityCheck() throws Exception {
    configuration.setBoolean(RESTORE_BLOBS, false);
    configuration.setBoolean(UNDELETE_BLOBS, false);
    configuration.setBoolean(INTEGRITY_CHECK, true);
    underTest.configure(configuration);

    when(repositoryManager.browseForBlobStore(any())).thenReturn(singletonList(repository));

    underTest.execute();

    verifyZeroInteractions(defaultIntegrityCheckStrategy);
    verify(testIntegrityCheckStrategy).check(eq(repository), eq(fileBlobStore), any());
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

    RestoreMetadataTask underTest = new RestoreMetadataTask(blobStoreManager, repositoryManager,
        ImmutableMap.of("maven2", restoreBlobStrategy), blobstoreUsageChecker, dryRunPrefix, integrityCheckStrategies) {
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
    when(fileBlobStore.get(blobId2, true)).thenReturn(blob);
    when(fileBlobStore.getBlobAttributes(blobId2)).thenReturn(blobAttributes2);
    when(blobAttributes2.getProperties()).thenReturn(properties);
    when(properties.getProperty(HEADER_PREFIX + REPO_NAME_HEADER)).thenReturn("maven-central2");
    when(repositoryManager.get("maven-central2")).thenReturn(repository2);
    when(repository2.getFormat()).thenReturn(mavenFormat);

    when(fileBlobStore.getBlobIdStream()).thenReturn(Stream.of(blobId, blobId2));

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
    when(fileBlobStore.get(exceptionBlobId, true)).thenReturn(exceptionBlob);
    when(fileBlobStore.getBlobAttributes(exceptionBlobId)).thenReturn(blobAttributes2);
    BlobStoreConfiguration blobStoreConfiguration = mock(BlobStoreConfiguration.class);
    when(fileBlobStore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);
    when(blobAttributes2.getProperties()).thenReturn(properties2);
    when(properties2.getProperty(HEADER_PREFIX + REPO_NAME_HEADER)).thenReturn("maven-central2");
    when(repositoryManager.get("maven-central2")).thenReturn(repository2);
    when(repository2.getFormat()).thenReturn(mavenFormat);

    doThrow(new RuntimeException())
        .when(restoreBlobStrategy)
        .restore(properties2, exceptionBlob, BLOBSTORE_NAME, false);

    BlobAttributes blobAttributes3 = mock(BlobAttributes.class);
    Properties properties3 = mock(Properties.class);
    Repository repository3 = mock(Repository.class);

    BlobId blobId3 = new BlobId("86e20baa-0bca-4915-a7dc-9a4f34e72324");
    when(fileBlobStore.get(blobId3, true)).thenReturn(blob);
    when(fileBlobStore.getBlobAttributes(blobId3)).thenReturn(blobAttributes3);
    when(blobAttributes3.getProperties()).thenReturn(properties3);
    when(properties3.getProperty(HEADER_PREFIX + REPO_NAME_HEADER)).thenReturn("maven-central2");
    when(repositoryManager.get("maven-central2")).thenReturn(repository3);
    when(repository3.getFormat()).thenReturn(mavenFormat);

    when(fileBlobStore.getBlobIdStream()).thenReturn(Stream.of(blobId, exceptionBlobId, blobId3));

    configuration.setBoolean(RESTORE_BLOBS, true);
    configuration.setBoolean(UNDELETE_BLOBS, true);
    configuration.setBoolean(INTEGRITY_CHECK, false);
    underTest.configure(configuration);

    underTest.execute();

    verify(restoreBlobStrategy).restore(properties2, exceptionBlob, BLOBSTORE_NAME, false);
    verify(restoreBlobStrategy).after(true, repository);
    verify(restoreBlobStrategy, times(0)).after(true, repository2);
    verify(restoreBlobStrategy).after(true, repository3);
  }
}
