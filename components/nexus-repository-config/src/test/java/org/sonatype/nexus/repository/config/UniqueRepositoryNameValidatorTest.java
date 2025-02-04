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
package org.sonatype.nexus.repository.config;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Tests validity of Repository "foo"s validated by {@link UniqueRepositoryNameValidator}
 *
 * @since 3.0
 */
public class UniqueRepositoryNameValidatorTest
    extends TestSupport
{
  @Mock
  private RepositoryManager repositoryManager;

  @InjectMocks
  private UniqueRepositoryNameValidator validator;

  /*
   * Name is valid when the RepositoryManager says it does not exist
   */
  @Test
  public void testIsValid() {
    when(repositoryManager.exists("foo")).thenReturn(true, false);

    assertFalse(validator.isValid("foo", null));
    assertTrue(validator.isValid("foo", null));
  }
}
