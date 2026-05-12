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
package org.sonatype.nexus.coreui.internal.blobstore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.blobstore.BlobStoreDescriptor;
import org.sonatype.nexus.blobstore.BlobStoreDescriptorProvider;
import org.sonatype.nexus.blobstore.MockBlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.blobstore.file.FileBlobStore;
import org.sonatype.nexus.blobstore.group.BlobStoreGroup;
import org.sonatype.nexus.repository.blobstore.BlobStoreConfigurationStore;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class BlobStoreInternalResourceTest
{
  static final String FILE_TYPE = "File";

  static final String FILE_TYPE_ID = "file";

  static final String S3_TYPE = "S3";

  static final String S3_TYPE_ID = "s3";

  @Mock
  private BlobStoreManager blobStoreManager;

  @Mock
  private BlobStoreConfigurationStore blobStoreConfigurationStore;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private BlobStoreDescriptorProvider blobStoreDescriptorProvider;

  private Map<String, BlobStoreDescriptor> blobStoreDescriptors = new HashMap<>();

  private List<BlobStoreConfiguration> configurations = new ArrayList<>();

  private BlobStoreInternalResource underTest;

  @BeforeEach
  void setup() {
    addDescriptor(FILE_TYPE, FILE_TYPE_ID);
    addDescriptor(S3_TYPE, S3_TYPE_ID);
    addDescriptor(BlobStoreGroup.TYPE, BlobStoreGroup.CONFIG_KEY);

    lenient().when(blobStoreDescriptorProvider.get()).thenReturn(blobStoreDescriptors);
    when(blobStoreConfigurationStore.list()).thenReturn(configurations);

    underTest = new BlobStoreInternalResource(
        blobStoreManager, blobStoreConfigurationStore, blobStoreDescriptorProvider, List.of(), repositoryManager);
  }

  @Test
  void listNoBlobStores() {
    List<BlobStoreUIResponse> responses = underTest.listBlobStores();
    assertThat(responses.isEmpty(), is(true));
  }

  @Test
  void noDataInBlobStoreDescriptorProvider() {
    addBlobStore("fileStore1", FILE_TYPE);
    addBlobStore("s3BlobStore", S3_TYPE);

    assertThat(underTest.listBlobStores().size(), is(2));

    when(blobStoreDescriptorProvider.get()).thenReturn(null);
    assertThat(underTest.listBlobStores().isEmpty(), is(true));

    when(blobStoreDescriptorProvider.get()).thenReturn(Collections.emptyMap());
    assertThat(underTest.listBlobStores().isEmpty(), is(true));
  }

  @Test
  void listOneBlobStore() {
    addBlobStore("fileStore", FILE_TYPE);

    List<BlobStoreUIResponse> responses = underTest.listBlobStores();
    assertThat(responses.size(), is(1));
    BlobStoreUIResponse response = responses.get(0);
    assertThat(response.getName(), is("fileStore"));
    assertThat(response.getPath(), is("my_path"));
    assertThat(response.getBlobCount(), is(1L));
    assertThat(response.getTypeId(), is(FILE_TYPE_ID));
    assertThat(response.getTypeName(), is(FILE_TYPE));
    assertThat(response.getTotalSizeInBytes(), is(100L));
    assertThat(response.getAvailableSpaceInBytes(), is(1000L));
    assertThat(response.isUnavailable(), is(false));
  }

  @Test
  void listMultipleBlobStores() {
    addBlobStore("fileStore1", FILE_TYPE);
    addBlobStore("fileStore2", FILE_TYPE);
    addBlobStore("s3BlobStore", S3_TYPE);

    List<BlobStoreUIResponse> responses = underTest.listBlobStores();
    assertThat(responses.size(), is(3));
    BlobStoreUIResponse response1 = responses.get(0);
    assertThat(response1.getName(), is("fileStore1"));
    assertThat(response1.getPath(), is("my_path"));
    assertThat(response1.getBlobCount(), is(1L));
    assertThat(response1.getTypeId(), is(FILE_TYPE_ID));
    assertThat(response1.getTypeName(), is(FILE_TYPE));
    assertThat(response1.getTotalSizeInBytes(), is(100L));
    assertThat(response1.getAvailableSpaceInBytes(), is(1000L));
    assertThat(response1.isUnavailable(), is(false));

    BlobStoreUIResponse response2 = responses.get(1);
    assertThat(response2.getName(), is("fileStore2"));
    assertThat(response2.getBlobCount(), is(1L));
    assertThat(response2.getTypeId(), is(FILE_TYPE_ID));
    assertThat(response2.getTypeName(), is(FILE_TYPE));
    assertThat(response2.getTotalSizeInBytes(), is(100L));
    assertThat(response2.getAvailableSpaceInBytes(), is(1000L));
    assertThat(response2.isUnavailable(), is(false));

    BlobStoreUIResponse response3 = responses.get(2);
    assertThat(response3.getName(), is("s3BlobStore"));
    assertThat(response3.getPath(), is("my-s3-bucket/my-prefix"));
    assertThat(response3.getBlobCount(), is(1L));
    assertThat(response3.getTypeId(), is(S3_TYPE_ID));
    assertThat(response3.getTypeName(), is(S3_TYPE));
    assertThat(response3.getTotalSizeInBytes(), is(100L));
    assertThat(response3.getAvailableSpaceInBytes(), is(1000L));
    assertThat(response2.isUnavailable(), is(false));
  }

  @Test
  void listNonStartedBlobStore() {
    BlobStore fileBS = addBlobStore("fileStore", FILE_TYPE);
    BlobStore s3BS = addBlobStore("s3BlobStore", S3_TYPE, false);
    addGroupBlobStore("groupBS", BlobStoreGroup.TYPE, true, Arrays.asList(fileBS, s3BS));

    List<BlobStoreUIResponse> responses = underTest.listBlobStores();
    assertThat(responses.size(), is(3));
    BlobStoreUIResponse response1 = responses.get(0);
    assertThat(response1.getName(), is("fileStore"));
    assertThat(response1.getBlobCount(), is(1L));
    assertThat(response1.getTypeId(), is(FILE_TYPE_ID));
    assertThat(response1.getTypeName(), is(FILE_TYPE));
    assertThat(response1.getTotalSizeInBytes(), is(100L));
    assertThat(response1.getAvailableSpaceInBytes(), is(1000L));

    // non-started blobstore should show up but be unavailable
    BlobStoreUIResponse response2 = responses.get(1);
    assertThat(response2.getName(), is("s3BlobStore"));
    assertThat(response2.getBlobCount(), is(0L));
    assertThat(response2.getTypeId(), is(S3_TYPE_ID));
    assertThat(response2.getTypeName(), is(S3_TYPE));
    assertThat(response2.getTotalSizeInBytes(), is(0L));
    assertThat(response2.getAvailableSpaceInBytes(), is(0L));
    assertThat(response2.isUnavailable(), is(true));

    BlobStoreUIResponse response3 = responses.get(2);
    assertThat(response3.getName(), is("groupBS"));
    assertThat(response3.getBlobCount(), is(0L));
    assertThat(response3.getTypeId(), is(BlobStoreGroup.CONFIG_KEY));
    assertThat(response3.getTypeName(), is(BlobStoreGroup.TYPE));
    assertThat(response3.getTotalSizeInBytes(), is(0L));
    assertThat(response3.getAvailableSpaceInBytes(), is(0L));
    assertThat(response3.isUnavailable(), is(true));
  }

  private void addDescriptor(final String type, final String typeId) {
    BlobStoreDescriptor result = mock(BlobStoreDescriptor.class);
    lenient().when(result.getId()).thenReturn(typeId);
    blobStoreDescriptors.put(type, result);
  }

  private BlobStore addBlobStore(final String name, final String type) {
    return addBlobStore(name, type, true);
  }

  private BlobStore addBlobStore(final String name, final String type, final boolean started) {
    // create blobstore and metrics
    BlobStore bs = mock(BlobStore.class);
    BlobStoreMetrics metrics = getBlobStoreMetrics();
    when(bs.isGroupable()).thenReturn(true);
    when(bs.isStarted()).thenReturn(started);
    lenient().when(bs.getMetrics()).thenReturn(metrics);
    // add configuration
    MockBlobStoreConfiguration mockBlobStoreConfiguration = new MockBlobStoreConfiguration(name, type);
    Map<String, Map<String, Object>> attributes = new HashMap<>();

    // Set up attributes based on blob store type
    if (FILE_TYPE.equals(type)) {
      Map<String, Object> fileAttribute = new HashMap<>();
      fileAttribute.put(FileBlobStore.PATH_KEY, "my_path");
      attributes.put(FileBlobStore.CONFIG_KEY, fileAttribute);
    }
    else if (S3_TYPE.equals(type)) {
      Map<String, Object> s3Attribute = new HashMap<>();
      s3Attribute.put("bucket", "my-s3-bucket");
      s3Attribute.put("prefix", "my-prefix");
      attributes.put("s3", s3Attribute);
    }

    mockBlobStoreConfiguration.setAttributes(attributes);
    configurations.add(mockBlobStoreConfiguration);
    // return blobstore from blobStoreManager
    when(blobStoreManager.get(name)).thenReturn(bs);
    return bs;
  }

  private static BlobStoreMetrics getBlobStoreMetrics() {
    BlobStoreMetrics metrics = mock(BlobStoreMetrics.class);
    lenient().when(metrics.getBlobCount()).thenReturn(1L);
    lenient().when(metrics.getTotalSize()).thenReturn(100L);
    lenient().when(metrics.getAvailableSpace()).thenReturn(1000L);
    return metrics;
  }

  private void addGroupBlobStore(
      final String name,
      final String type,
      final boolean started,
      final List<BlobStore> members)
  {
    // create blobstore and metrics
    BlobStoreGroup groupBlobStore = mock(BlobStoreGroup.class);
    BlobStoreMetrics metrics = getBlobStoreMetrics();
    when(groupBlobStore.isGroupable()).thenReturn(false);
    lenient().when(groupBlobStore.isStarted()).thenReturn(started);
    lenient().when(groupBlobStore.getMetrics()).thenReturn(metrics);

    when(groupBlobStore.getMembers()).thenReturn(members);
    // add configuration
    configurations.add(new MockBlobStoreConfiguration(name, type));
    // return blobstore from blobStoreManager
    when(blobStoreManager.get(name)).thenReturn(groupBlobStore);
  }
}
