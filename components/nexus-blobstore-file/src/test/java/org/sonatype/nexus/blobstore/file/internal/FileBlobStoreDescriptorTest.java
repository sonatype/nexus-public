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
package org.sonatype.nexus.blobstore.file.internal;

import java.nio.file.Files;
import java.nio.file.Path;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.file.FileBlobStore;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaService;
import org.sonatype.nexus.common.app.ApplicationDirectories;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.sonatype.nexus.blobstore.file.FileBlobStore.CONFIG_KEY;
import static org.sonatype.nexus.blobstore.file.FileBlobStore.PATH_KEY;

public class FileBlobStoreDescriptorTest
    extends TestSupport
{
  @Mock
  BlobStoreQuotaService quotaService;

  @Mock
  ApplicationDirectories applicationDirectories;

  FileBlobStoreDescriptor descriptor;

  @Before
  public void setup() {
    descriptor = new FileBlobStoreDescriptor(quotaService, applicationDirectories);
  }

  @Test
  public void descriptorValidationCallQuotaValidation() throws Exception {
    BlobStoreConfiguration config = new BlobStoreConfiguration();
    Path tempDir = Files.createTempDirectory("test");
    config.attributes(CONFIG_KEY).set(PATH_KEY, tempDir.toAbsolutePath().toString());
    descriptor.validateConfig(config);
    verify(quotaService, times(1)).validateSoftQuotaConfig(config);
  }
}
