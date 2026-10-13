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
package org.sonatype.nexus.security.role;

import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.security.authz.NoSuchAuthorizationManagerException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class UniqueRoleIdValidatorTest
{
  @Mock
  private SecuritySystem securitySystem;

  @Mock
  private AuthorizationManager authorizationManager;

  private UniqueRoleIdValidator underTest;

  @Before
  public void setUp() throws NoSuchAuthorizationManagerException {
    when(securitySystem.getAuthorizationManager(any())).thenReturn(authorizationManager);
    underTest = new UniqueRoleIdValidator(securitySystem);
  }

  /*
   * Null value is considered valid (other constraints such as @NotEmpty handle the null/empty case)
   */
  @Test
  public void isValid_nullValue() {
    assertThat(underTest.isValid(null, null), is(true));
  }

  @Test
  public void isValid_roleAlreadyExists() throws NoSuchRoleException {
    when(authorizationManager.getRole("existing-role")).thenReturn(mock(Role.class));
    assertThat(underTest.isValid("existing-role", null), is(false));
  }

  @Test
  public void isValid_roleDoesNotExist() throws NoSuchRoleException {
    when(authorizationManager.getRole(any())).thenThrow(new NoSuchRoleException("not-found"));
    assertThat(underTest.isValid("new-role", null), is(true));
  }
}
