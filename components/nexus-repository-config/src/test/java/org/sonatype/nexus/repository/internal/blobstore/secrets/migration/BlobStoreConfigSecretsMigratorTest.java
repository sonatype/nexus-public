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
package org.sonatype.nexus.repository.internal.blobstore.secrets.migration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.crypto.secrets.SecretsService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.internal.blobstore.secrets.migration.BlobStoreConfigSecretsMigrator.S3_TYPE;
import static org.sonatype.nexus.repository.internal.blobstore.secrets.migration.BlobStoreConfigSecretsMigrator.secretKeys;

public class BlobStoreConfigSecretsMigratorTest
    extends TestSupport
{
  @Mock
  private BlobStoreManager blobStoreManager;

  @Mock
  private SecretsService secretsService;

  private BlobStoreConfigSecretsMigrator underTest;

  @Before
  public void setUp() {
    underTest = new BlobStoreConfigSecretsMigrator(blobStoreManager, secretsService);
    when(secretsService.from(anyString())).thenAnswer(invocation -> {
      Secret secret = mock(Secret.class);
      when(secret.getId()).thenReturn(invocation.getArgument(0));
      when(secret.decrypt()).thenReturn(Strings2.EMPTY.toCharArray());
      return secret;
    });
  }

  @Test
  public void testMigrateSecrets() throws Exception {
    mockBrowseCall(10, 4);

    underTest.migrate();

    verify(blobStoreManager).browse();
    verify(blobStoreManager, times(4)).update(any(BlobStoreConfiguration.class));
    verify(secretsService, times(8)).from(anyString());
  }

  @Test
  public void testMigrateNoSecrets() throws Exception {
    mockBrowseCall(5, 0);

    underTest.migrate();

    verify(blobStoreManager).browse();
    verify(blobStoreManager, never()).update(any(BlobStoreConfiguration.class));
    verify(secretsService, never()).from(anyString());
  }

  private void mockBrowseCall(final int totalSize, final int s3BlobStores) {
    List<BlobStore> results = new ArrayList<>();

    for (int i = 0; i < totalSize; i++) {
      BlobStore blobStore = mock(BlobStore.class);
      BlobStoreConfiguration blobStoreConfiguration = mock(BlobStoreConfiguration.class);
      when(blobStore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);
      when(blobStoreConfiguration.getName()).thenReturn("blobstore-" + i);
      when(blobStoreConfiguration.copy(anyString())).thenReturn(blobStoreConfiguration);

      if (i < s3BlobStores) {
        when(blobStoreConfiguration.getType()).thenReturn(S3_TYPE);
        Map<String, Map<String, Object>> attributes = new HashMap<>();
        Map<String, Object> config = new HashMap<>();
        config.put(secretKeys.get(0), "legacy-secret-" + i);
        config.put(secretKeys.get(1), "legacy-token" + i);
        attributes.put(S3_TYPE, config);

        when(blobStoreConfiguration.getAttributes()).thenReturn(attributes);
      }
      else {
        when(blobStoreConfiguration.getType()).thenReturn("other");
      }
      results.add(blobStore);
    }

    when(blobStoreManager.browse()).thenReturn(results);
  }
}
