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
package org.sonatype.nexus.self.hosted.blobstore.s3.upgrade;

import java.util.HashMap;
import java.util.List;

import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.s3.S3BlobStoreConfigurationHelper;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.blobstore.s3.internal.S3BlobStore;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleS3CompactTasksTaskTest
{
  @Captor
  ArgumentCaptor<TaskConfiguration> captor;

  @Mock
  BlobStoreManager blobstoreManager;

  @Mock(answer = Answers.RETURNS_MOCKS)
  TaskScheduler taskScheduler;

  @InjectMocks
  ScheduleS3CompactTasksTask undertest;

  @Test
  void testExecute() throws Exception {
    List<BlobStore> blobstores = List.of(mockBlobStore("not-s3", "File"), mockS3BlobStore("s3-no-expiration", null),
        mockS3BlobStore("s3-expiration-disabled", -1), mockS3BlobStore("s3-expiration", 1));
    when(blobstoreManager.browse()).thenReturn(blobstores);

    assertThat(undertest.execute(), is(2));
    verify(taskScheduler, times(2)).createTaskConfigurationInstance("blobstore.compact");
    verify(taskScheduler, times(2)).scheduleTask(captor.capture(), any());
    verify(taskScheduler, times(2)).getScheduleFactory();
    verifyNoMoreInteractions(taskScheduler);

    List<TaskConfiguration> taskConfigurations = captor.getAllValues();
    verify(taskConfigurations.get(0)).setString("blobstoreName", "s3-no-expiration");
    verify(taskConfigurations.get(0)).setInteger("blobsOlderThan", 3);
    verify(taskConfigurations.get(1)).setString("blobstoreName", "s3-expiration");
    verify(taskConfigurations.get(1)).setInteger("blobsOlderThan", 1);
  }

  private static BlobStore mockS3BlobStore(final String name, final Integer expiration) {
    BlobStore blobstore = mockBlobStore(name, S3BlobStore.TYPE);

    BlobStoreConfiguration config = blobstore.getBlobStoreConfiguration();

    NestedAttributesMap attributes = new NestedAttributesMap("", new HashMap<>());

    if (expiration != null) {
      attributes.set("expiration", expiration);
    }

    when(config.attributes(S3BlobStoreConfigurationHelper.CONFIG_KEY)).thenReturn(attributes);

    return blobstore;
  }

  private static BlobStore mockBlobStore(final String name, final String type) {
    BlobStoreConfiguration config = mock(BlobStoreConfiguration.class);
    lenient().when(config.getName()).thenReturn(name);
    when(config.getType()).thenReturn(type);
    BlobStore blobstore = mock(BlobStore.class);
    when(blobstore.getBlobStoreConfiguration()).thenReturn(config);
    return blobstore;
  }
}
