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

import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;

import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.goodies.testsupport.hamcrest.DiffMatchers.equalTo;
import static org.sonatype.nexus.repository.internal.blobstore.BlobStoreOverrideImpl.NEXUS_BLOB_STORE_OVERRIDE;

/**
 * Test {@code BlobStoreOverrideImpl}.
 */
public class BlobStoreOverrideImplTest
    extends BlobStoreOverrideImplTestSupport
{
  @Test
  public void testNoBlobStoresNoOverride() {
    when(configStore.list()).thenReturn(Collections.emptyList());
    underTest.apply();
    verifyNoMoreInteractions(configStore);
  }

  @Test
  public void testNoOverride() {
    BlobStoreConfiguration config = defaultConfig();
    when(configStore.list()).thenReturn(singletonList(config));
    underTest.apply();
    verify(configStore, never()).update(any());
  }

  @Test
  public void testWithOverride() {
    environmentVariables.set(NEXUS_BLOB_STORE_OVERRIDE, "{\"default\":{\"file\":{\"path\":\"other_path\"}}}");
    BlobStoreConfiguration config = defaultConfig();
    when(configStore.list()).thenReturn(singletonList(config));
    underTest.apply();
    verify(configStore).update(config);
    assertThat((String)config.getAttributes().get("file").get("path"), equalTo("other_path"));
  }

  @Test(expected = IllegalStateException.class)
  public void testWithParsingError() {
    environmentVariables.set(NEXUS_BLOB_STORE_OVERRIDE, "invalid json");
    BlobStoreConfiguration config = defaultConfig();
    when(configStore.list()).thenReturn(singletonList(config));
    underTest.apply();
  }

  @Test
  public void testS3BucketOverride() {
    environmentVariables.set(NEXUS_BLOB_STORE_OVERRIDE,
        "{\"some s3 blob store\":{\"s3\":{\"bucket\":\"some-other-s3-bucket\"}}}");
    BlobStoreConfiguration defaultConfig = defaultConfig();
    BlobStoreConfiguration config = createConfig("some s3 blob store", "S3");
    config.getAttributes().get("s3").put("bucket", "some-s3-bucket");
    when(configStore.list()).thenReturn(asList(defaultConfig, config));
    underTest.apply();
    verify(configStore).update(config);
    verify(configStore, never()).update(defaultConfig);
    assertThat((String)config.getAttributes().get("s3").get("bucket"), equalTo("some-other-s3-bucket"));
  }

  @Test
  public void testS3BucketAndFilePathOverride() {
    environmentVariables.set(NEXUS_BLOB_STORE_OVERRIDE,
        "{\"some s3 blob store\":{\"s3\":{\"bucket\":\"some-other-s3-bucket\"}},\"default\":{\"file\":{\"path\":\"other_path\"}}}");
    BlobStoreConfiguration defaultConfig = defaultConfig();
    BlobStoreConfiguration config = createConfig("some s3 blob store", "S3");
    config.getAttributes().get("s3").put("bucket", "some-s3-bucket");
    when(configStore.list()).thenReturn(asList(defaultConfig, config));
    underTest.apply();
    verify(configStore).update(config);
    verify(configStore).update(defaultConfig);
    assertThat((String)config.getAttributes().get("s3").get("bucket"), equalTo("some-other-s3-bucket"));
    assertThat((String)defaultConfig.getAttributes().get("file").get("path"), equalTo("other_path"));
  }
}
