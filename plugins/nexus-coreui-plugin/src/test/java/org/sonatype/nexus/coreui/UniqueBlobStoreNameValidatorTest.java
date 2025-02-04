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
package org.sonatype.nexus.coreui;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Tests {@link UniqueBlobStoreNameValidator}
 */
public class UniqueBlobStoreNameValidatorTest
    extends TestSupport
{
  @Mock
  private BlobStoreManager blobStoreManager;

  private UniqueBlobStoreNameValidator underTest;

  @Before
  public void setup() {
    underTest = new UniqueBlobStoreNameValidator(blobStoreManager);
  }

  @Test
  public void testValidation() {
    when(blobStoreManager.exists("test")).thenReturn(true);
    when(blobStoreManager.exists("DEFAULT")).thenReturn(false);

    assertFalse(underTest.isValid("test", null));
    assertTrue(underTest.isValid("DEFAULT", null));
  }
}
