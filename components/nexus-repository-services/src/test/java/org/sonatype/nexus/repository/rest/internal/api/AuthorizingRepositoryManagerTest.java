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

package org.sonatype.nexus.repository.rest.internal.api;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.NegativeCacheFacet;
import org.sonatype.nexus.repository.cache.RepositoryCacheInvalidationService;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.rest.api.IncompatibleRepositoryException;
import org.sonatype.nexus.repository.rest.api.RepositoryNotFoundException;
import org.sonatype.nexus.repository.search.index.RebuildIndexTaskDescriptor;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskScheduler;

import org.apache.shiro.authz.AuthorizationException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.security.BreadActions.EDIT;

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
  private TaskScheduler taskScheduler;

  @Mock
  private Repository repository;

  @Mock
  private EventManager eventManager;

  private AuthorizingRepositoryManagerImpl authorizingRepositoryManager;

  @Before
  public void setUp() {
    when(repository.getName()).thenReturn("repository");
    when(repositoryManager.get(anyString())).thenReturn(repository);
    when(repositoryManager.get(eq("absent"))).thenReturn(null);

    RepositoryCacheInvalidationService repositoryCacheInvalidationService =
        new RepositoryCacheInvalidationService(repositoryManager, eventManager);
    authorizingRepositoryManager = new AuthorizingRepositoryManagerImpl(
        repositoryManager, repositoryPermissionChecker, taskScheduler, repositoryCacheInvalidationService);
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

  @Test
  public void rebuildIndexShouldThrowExceptionIfRepositoryDoesNotExist() throws Exception {
    expectedException.expect(RepositoryNotFoundException.class);

    authorizingRepositoryManager.rebuildSearchIndex("absent");

    verify(repositoryManager).get(eq("repository"));
    verifyNoMoreInteractions(repositoryManager, repositoryPermissionChecker, taskScheduler);
  }

  @Test
  public void rebuildIndexShouldThrowExceptionIfRepositoryTypeIsNotHostedOrProxy() throws Exception {
    when(repository.getType()).thenReturn(new GroupType());
    expectedException.expect(IncompatibleRepositoryException.class);

    authorizingRepositoryManager.rebuildSearchIndex("repository");

    verify(repositoryManager).get(eq("repository"));
    verifyNoMoreInteractions(repositoryManager, repositoryPermissionChecker, taskScheduler);
  }

  @Test
  public void rebuildIndexShouldThrowExceptionIfInsufficientPermissions() throws Exception {
    when(repository.getType()).thenReturn(new HostedType());
    doThrow(new AuthorizationException("User is not permitted."))
        .when(repositoryPermissionChecker)
        .ensureUserCanAdmin(any(), any());
    expectedException.expect(AuthorizationException.class);

    authorizingRepositoryManager.rebuildSearchIndex("repository");

    verify(repositoryManager).get(eq("repository"));
    verify(repositoryPermissionChecker).ensureUserCanAdmin(eq(EDIT), eq(repository));
    verifyNoMoreInteractions(repositoryManager, repositoryPermissionChecker, taskScheduler);
  }

  @Test
  public void rebuildIndexShouldTriggerTask() throws Exception {
    TaskConfiguration taskConfiguration = mock(TaskConfiguration.class);
    when(taskScheduler.createTaskConfigurationInstance(any())).thenReturn(taskConfiguration);
    when(repository.getType()).thenReturn(new HostedType());

    authorizingRepositoryManager.rebuildSearchIndex("repository");

    verify(repositoryManager).get(eq("repository"));
    verify(repositoryPermissionChecker).ensureUserCanAdmin(eq(EDIT), eq(repository));
    verify(taskScheduler).createTaskConfigurationInstance(RebuildIndexTaskDescriptor.TYPE_ID);
    verify(taskScheduler).submit(any());
    verifyNoMoreInteractions(repositoryManager, repositoryPermissionChecker, taskScheduler);
  }

  @Test
  public void invalidateCacheShouldThrowExceptionIfRepositoryDoesNotExist() throws Exception {
    expectedException.expect(RepositoryNotFoundException.class);

    authorizingRepositoryManager.invalidateCache("absent");

    verify(repositoryManager).get(eq("repository"));
    verifyNoMoreInteractions(repositoryManager, repositoryPermissionChecker, taskScheduler);
  }

  @Test
  public void invalidateCacheShouldThrowExceptionIfRepositoryTypeIsNotProxyOrGroup() throws Exception {
    when(repository.getType()).thenReturn(new HostedType());
    expectedException.expect(IncompatibleRepositoryException.class);

    authorizingRepositoryManager.invalidateCache("repository");

    verify(repositoryManager).get(eq("repository"));
    verifyNoMoreInteractions(repositoryManager, repositoryPermissionChecker, taskScheduler);
  }

  @Test
  public void invalidateCacheShouldThrowExceptionIfInsufficientPermissions() throws Exception {
    when(repository.getType()).thenReturn(new GroupType());
    doThrow(new AuthorizationException("User is not permitted."))
        .when(repositoryPermissionChecker)
        .ensureUserCanAdmin(any(), any());
    expectedException.expect(AuthorizationException.class);

    authorizingRepositoryManager.invalidateCache("repository");

    verify(repositoryManager).get(eq("repository"));
    verify(repositoryPermissionChecker).ensureUserCanAdmin(eq(EDIT), eq(repository));
    verifyNoMoreInteractions(repositoryManager, repositoryPermissionChecker, taskScheduler);
  }

  @Test
  public void invalidateCacheProxyRepository() throws Exception {
    when(repository.getType()).thenReturn(new ProxyType());
    ProxyFacet proxyFacet = mock(ProxyFacet.class);
    when(repository.facet(ProxyFacet.class)).thenReturn(proxyFacet);
    NegativeCacheFacet negativeCacheFacet = mock(NegativeCacheFacet.class);
    when(repository.facet(NegativeCacheFacet.class)).thenReturn(negativeCacheFacet);

    authorizingRepositoryManager.invalidateCache("repository");

    verify(repositoryManager).get(eq("repository"));
    verify(repositoryPermissionChecker).ensureUserCanAdmin(eq(EDIT), eq(repository));
    verify(repository).facet(ProxyFacet.class);
    verify(proxyFacet).invalidateProxyCaches();
    verifyNoMoreInteractions(repositoryManager, repositoryPermissionChecker, proxyFacet);
  }

  @Test
  public void invalidateCacheGroupRepository() throws Exception {
    when(repository.getType()).thenReturn(new GroupType());
    GroupFacet groupFacet = mock(GroupFacet.class);
    when(repository.facet(GroupFacet.class)).thenReturn(groupFacet);

    authorizingRepositoryManager.invalidateCache("repository");

    verify(repositoryManager).get(eq("repository"));
    verify(repositoryPermissionChecker).ensureUserCanAdmin(eq(EDIT), eq(repository));
    verify(repository).facet(GroupFacet.class);
    verify(groupFacet).invalidateGroupCaches();
    verifyNoMoreInteractions(repositoryManager, repositoryPermissionChecker, groupFacet);
  }
}
