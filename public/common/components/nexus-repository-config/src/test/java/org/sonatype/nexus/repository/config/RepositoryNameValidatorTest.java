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

import org.sonatype.nexus.repository.manager.RepositoryManager;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class RepositoryNameValidatorTest
{
  @Mock
  private RepositoryManager repositoryManager;

  @InjectMocks
  private RepositoryNameValidator underTest;

  /*
   * Null value is considered valid (other constraints such as @NotEmpty handle the null/empty case)
   */
  @Test
  public void isValid_nullValue() {
    assertTrue(underTest.isValid(null, null));
  }

  @Test
  public void isValid_repositoryExists() {
    when(repositoryManager.exists("existing-repo")).thenReturn(true);
    assertTrue(underTest.isValid("existing-repo", null));
  }

  @Test
  public void isValid_repositoryDoesNotExist() {
    when(repositoryManager.exists("missing-repo")).thenReturn(false);
    assertFalse(underTest.isValid("missing-repo", null));
  }
}
