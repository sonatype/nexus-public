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

package org.sonatype.nexus.repository.internal.blobstore;

import java.util.Collections;
import javax.inject.Provider;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.file.FileBlobStore;
import org.sonatype.nexus.blobstore.group.BlobStoreGroup;

import com.codahale.metrics.health.HealthCheck.Result;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BlobStoreStateHealthCheckTest
    extends TestSupport
{
  @Mock
  private BlobStoreManager blobStoreManager;

  private final Provider<BlobStoreManager> blobStoreManagerProvider = () -> blobStoreManager;

  private final BlobStoreStateHealthCheck healthCheck = new BlobStoreStateHealthCheck(blobStoreManagerProvider);

  @Mock
  private BlobStore blobStore;

  @Mock
  private BlobStoreConfiguration blobStoreConfiguration;

  @Before
  public void setUp() throws Exception {
    when(blobStoreManager.browse()).thenReturn(Collections.singletonList(blobStore));
    when(blobStore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);
    when(blobStoreConfiguration.getName()).thenReturn("blob-store-name");
  }

  @Test
  public void shouldBe_healthy_whenNoIssues() {
    when(blobStore.isStarted()).thenReturn(true);
    when(blobStore.isWritable()).thenReturn(true);
    when(blobStore.isStorageAvailable()).thenReturn(true);
    when(blobStoreConfiguration.getType()).thenReturn(FileBlobStore.TYPE);

    Result result = healthCheck.check();
    assertTrue(result.isHealthy());
  }

  @Test
  public void shouldBe_unhealthy_whenStorageNotAvailable() {
    when(blobStore.isStarted()).thenReturn(true);
    when(blobStore.isWritable()).thenReturn(true);
    when(blobStore.isStorageAvailable()).thenReturn(false);
    when(blobStoreConfiguration.getType()).thenReturn(FileBlobStore.TYPE);


    Result result = healthCheck.check();
    assertFalse(result.isHealthy());
    assertThat(result.getMessage(), is("1/1 blob stores report issues<br>Blob store 'blob-store-name' reports as not available"));
  }

  @Test
  public void shouldBe_unhealthy_whenStorageNotWriteable() {
    when(blobStore.isStarted()).thenReturn(true);
    when(blobStore.isWritable()).thenReturn(false);
    when(blobStore.isStorageAvailable()).thenReturn(true);
    when(blobStoreConfiguration.getType()).thenReturn(FileBlobStore.TYPE);

    Result result = healthCheck.check();
    assertFalse(result.isHealthy());
    assertThat(result.getMessage(), is("1/1 blob stores report issues<br>Blob store 'blob-store-name' reports as not writeable"));
  }

  @Test
  public void shouldBe_unhealthy_whenStorageNotStarted() {
    when(blobStore.isStarted()).thenReturn(false);
    when(blobStore.isWritable()).thenReturn(true);
    when(blobStore.isStorageAvailable()).thenReturn(true);
    when(blobStoreConfiguration.getType()).thenReturn(FileBlobStore.TYPE);

    Result result = healthCheck.check();
    assertFalse(result.isHealthy());
    assertThat(result.getMessage(), is("1/1 blob stores report issues<br>Blob store 'blob-store-name' reports as not started"));
  }

  @Test
  public void shouldBe_healthy_whenNoBlobStoresPresent() {
    when(blobStoreManager.browse()).thenReturn(Collections.emptyList());

    Result result = healthCheck.check();
    assertTrue(result.isHealthy());
  }

  @Test
  public void shouldHandleGroupBlobStore() {
    BlobStore blobStore1 = mock(BlobStore.class);
    when(blobStore1.isStarted()).thenReturn(true);
    when(blobStore1.isWritable()).thenReturn(true);
    when(blobStore1.isStorageAvailable()).thenReturn(true);
    when(blobStore1.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);
    when(blobStoreConfiguration.getName()).thenReturn("blob-store-1");
    when(blobStoreConfiguration.getType()).thenReturn(FileBlobStore.TYPE);

    BlobStore blobStore2 = mock(BlobStore.class);
    when(blobStore2.isStarted()).thenReturn(true);
    when(blobStore2.isWritable()).thenReturn(true);
    when(blobStore2.isStorageAvailable()).thenReturn(true);
    BlobStoreConfiguration blobStoreConfiguration2 = mock(BlobStoreConfiguration.class);
    when(blobStore2.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration2);
    when(blobStoreConfiguration2.getName()).thenReturn("blob-store-2");
    when(blobStoreConfiguration2.getType()).thenReturn(FileBlobStore.TYPE);

    BlobStoreGroup blobStore3 = mock(BlobStoreGroup.class);
    when(blobStore3.getMembers()).thenReturn(Lists.newArrayList(blobStore1, blobStore2));
    when(blobStore3.isStarted()).thenReturn(true);
    when(blobStore3.isWritable()).thenReturn(false);
    when(blobStore3.isStorageAvailable()).thenReturn(true);
    BlobStoreConfiguration blobStoreConfiguration3 = mock(BlobStoreConfiguration.class);
    when(blobStore3.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration3);
    when(blobStoreConfiguration3.getName()).thenReturn("group-blob-store");
    when(blobStoreConfiguration3.getType()).thenReturn(BlobStoreGroup.TYPE);

    when(blobStoreManager.browse()).thenReturn(Lists.newArrayList(blobStore1, blobStore2, blobStore3));

    Result result = healthCheck.check();
    assertTrue(result.isHealthy());
  }

  @Test
  public void shouldHandleMultipleBlobStores() {
    when(blobStore.isStarted()).thenReturn(true);
    when(blobStore.isWritable()).thenReturn(true);
    when(blobStore.isStorageAvailable()).thenReturn(true);
    when(blobStore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);
    when(blobStoreConfiguration.getName()).thenReturn("blob-store-1");
    when(blobStoreConfiguration.getType()).thenReturn(FileBlobStore.TYPE);

    BlobStore blobStore2 = mock(BlobStore.class);
    when(blobStore2.isStarted()).thenReturn(false);
    when(blobStore2.isWritable()).thenReturn(true);
    when(blobStore2.isStorageAvailable()).thenReturn(true);
    BlobStoreConfiguration blobStoreConfiguration2 = mock(BlobStoreConfiguration.class);
    when(blobStore2.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration2);
    when(blobStoreConfiguration2.getName()).thenReturn("blob-store-2");
    when(blobStoreConfiguration2.getType()).thenReturn(FileBlobStore.TYPE);

    BlobStore blobStore3 = mock(BlobStore.class);
    when(blobStore3.isStarted()).thenReturn(true);
    when(blobStore3.isWritable()).thenReturn(true);
    when(blobStore3.isStorageAvailable()).thenReturn(false);
    BlobStoreConfiguration blobStoreConfiguration3 = mock(BlobStoreConfiguration.class);
    when(blobStore3.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration3);
    when(blobStoreConfiguration3.getName()).thenReturn("blob-store-3");
    when(blobStoreConfiguration3.getType()).thenReturn(FileBlobStore.TYPE);

    when(blobStoreManager.browse()).thenReturn(Lists.newArrayList(blobStore, blobStore2, blobStore3));

    Result result = healthCheck.check();
    assertFalse(result.isHealthy());
    assertThat(result.getMessage(), is("2/3 blob stores report issues<br>Blob store 'blob-store-2' reports as not started<br>Blob store 'blob-store-3' reports as not available"));
  }
}
