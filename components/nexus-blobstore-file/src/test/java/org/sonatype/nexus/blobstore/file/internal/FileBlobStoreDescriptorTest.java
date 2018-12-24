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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class FileBlobStoreDescriptorTest
    extends TestSupport
{
  @Mock
  BlobStoreQuotaService quotaService;

  FileBlobStoreDescriptor descriptor;

  @Before
  public void setup() {
    descriptor = new FileBlobStoreDescriptor(quotaService);
  }

  @Test
  public void descriptorValidationCallQuotaValidation() {
    BlobStoreConfiguration config = new BlobStoreConfiguration();
    descriptor.validateConfig(config);
    verify(quotaService, times(1)).validateSoftQuotaConfig(config);
  }
}
