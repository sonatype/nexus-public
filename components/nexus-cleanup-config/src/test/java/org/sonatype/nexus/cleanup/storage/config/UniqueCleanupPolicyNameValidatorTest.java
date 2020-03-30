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
package org.sonatype.nexus.cleanup.storage.config;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyStorage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class UniqueCleanupPolicyNameValidatorTest
    extends TestSupport
{
  private UniqueCleanupPolicyNameValidator underTest;

  private static final String TEST_NAME = "test";

  @Mock
  private CleanupPolicyStorage cleanupPolicyStorage;

  @Before
  public void setUp() {
    underTest = new UniqueCleanupPolicyNameValidator(cleanupPolicyStorage);
  }

  @Test
  public void isValidByCleanupPolicyName() {
    when(cleanupPolicyStorage.exists(TEST_NAME)).thenReturn(false);

    assertThat(underTest.isValid(TEST_NAME, null), is(true));
  }

  @Test
  public void isInvalidByCleanupPolicyName() {
    when(cleanupPolicyStorage.exists(TEST_NAME)).thenReturn(true);

    assertThat(underTest.isValid(TEST_NAME, null), is(false));
  }
}
