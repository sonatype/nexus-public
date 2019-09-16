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

package org.sonatype.nexus.repository.rest;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;

import org.apache.shiro.authz.AuthorizationException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class AuthorizingRepositoryManagerTest
    extends TestSupport
{
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private RepositoryPermissionChecker repositoryPermissionChecker;

  @Mock
  private Repository repository;

  @InjectMocks
  private AuthorizingRepositoryManager authorizingRepositoryManager;

  @Before
  public void setUp() {
    when(repository.getName()).thenReturn("repository");
    when(repositoryManager.get(anyString())).thenReturn(repository);
    when(repositoryManager.get(eq("absent"))).thenReturn(null);
  }

  @Test
  public void deleteShouldDeleteRepositoryIfExists() throws Exception {
    authorizingRepositoryManager.delete("repository");

    verify(repositoryManager).get(eq("repository"));
    verify(repositoryPermissionChecker).ensureUserCanAdmin(eq("delete"), eq(repository));
    verify(repositoryManager).delete(eq("repository"));
    verifyNoMoreInteractions(repositoryManager, repositoryPermissionChecker);
  }

  @Test
  public void deleteShouldDoNothingIfRepositoryIsAbsent() throws Exception {
    authorizingRepositoryManager.delete("absent");

    verify(repositoryManager).get(eq("absent"));
    verifyNoMoreInteractions(repositoryManager, repositoryPermissionChecker);
  }

  @Test
  public void deleteShouldThrowExceptionIfInsufficientPermissions() throws Exception {
    doThrow(new AuthorizationException("User is not permitted."))
        .when(repositoryPermissionChecker)
        .ensureUserCanAdmin(any(), any());
    expectedException.expect(AuthorizationException.class);

    authorizingRepositoryManager.delete("repository");
  }
}
