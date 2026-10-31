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
package org.sonatype.nexus.security.user;

import org.sonatype.nexus.security.SecuritySystem;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class UniqueUserIdValidatorTest
{
  @Mock
  private SecuritySystem securitySystem;

  @Mock
  private UserManager userManager;

  private UniqueUserIdValidator underTest;

  @Before
  public void setUp() throws Exception {
    when(securitySystem.getUserManager(UserManager.DEFAULT_SOURCE)).thenReturn(userManager);
    underTest = new UniqueUserIdValidator(securitySystem);
  }

  /*
   * Null value is considered valid (other constraints such as @NotEmpty handle the null/empty case)
   */
  @Test
  public void isValid_nullValue() {
    assertThat(underTest.isValid(null, null), is(true));
  }

  @Test
  public void isValid_userAlreadyExists() throws Exception {
    when(userManager.getUser("existing-user")).thenReturn(new User());
    assertThat(underTest.isValid("existing-user", null), is(false));
  }

  @Test
  public void isValid_userDoesNotExist() throws Exception {
    when(userManager.getUser(any())).thenThrow(new UserNotFoundException("not-found"));
    assertThat(underTest.isValid("new-user", null), is(true));
  }
}
