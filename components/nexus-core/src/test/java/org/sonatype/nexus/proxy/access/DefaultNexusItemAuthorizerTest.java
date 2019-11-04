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
package org.sonatype.nexus.proxy.access;

import java.util.Collections;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.targets.Target;
import org.sonatype.nexus.proxy.targets.TargetRegistry;
import org.sonatype.nexus.threads.FakeAlmightySubject;
import org.sonatype.security.SecuritySystem;
import org.sonatype.security.authorization.AuthorizationManager;
import org.sonatype.security.authorization.NoSuchPrivilegeException;
import org.sonatype.security.authorization.NoSuchRoleException;
import org.sonatype.security.authorization.Privilege;
import org.sonatype.security.authorization.Role;
import org.sonatype.security.usermanagement.RoleIdentifier;
import org.sonatype.security.usermanagement.User;
import org.sonatype.security.usermanagement.UserNotFoundException;

import org.apache.shiro.subject.Subject;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.proxy.access.DefaultNexusItemAuthorizer.ADMIN_PRIVILEGE_ID;

public class DefaultNexusItemAuthorizerTest
{
  private DefaultNexusItemAuthorizer underTest;

  private SecuritySystem securitySystem;

  private RepositoryRegistry repositoryRegistry;

  private TargetRegistry targetRegistry;

  private AuthorizationManager authorizationManager;

  private Repository repository;

  private Subject subject;

  private User user;

  private Role role;

  private Privilege privilege;

  private Target target;

  private ContentClass contentClass;

  @Before
  public void setup() throws Exception {
    securitySystem = mock(SecuritySystem.class);
    repositoryRegistry = mock(RepositoryRegistry.class);
    targetRegistry = mock(TargetRegistry.class);
    authorizationManager = mock(AuthorizationManager.class);
    repository = mock(Repository.class);
    subject = mock(Subject.class);
    user = mock(User.class);
    role = mock(Role.class);
    privilege = mock(Privilege.class);
    target = mock(Target.class);
    contentClass = mock(ContentClass.class);

    when(securitySystem.getSubject()).thenReturn(subject);
    when(subject.getPrincipal()).thenReturn("subject");
    when(subject.isPermitted("nexus:target:target:repository:read")).thenReturn(true);

    when(securitySystem.getUser("subject")).thenReturn(user);
    when(user.getRoles()).thenReturn(Collections.singleton(new RoleIdentifier("default", "role")));

    when(authorizationManager.getRole("role")).thenReturn(role);
    when(role.getPrivileges()).thenReturn(Collections.singleton("privilege"));

    when(authorizationManager.getPrivilege("privilege")).thenReturn(privilege);
    when(privilege.getType()).thenReturn("target");
    when(privilege.getPrivilegeProperty("repositoryTargetId")).thenReturn("target");

    when(targetRegistry.getRepositoryTarget("target")).thenReturn(target);
    when(target.isPathContained(contentClass, "/test.jar")).thenReturn(true);
    when(target.getId()).thenReturn("target");

    when(repository.getRepositoryContentClass()).thenReturn(contentClass);
    when(repository.getId()).thenReturn("repository");

    underTest =
        new DefaultNexusItemAuthorizer(securitySystem, repositoryRegistry, targetRegistry, authorizationManager, true);
  }

  @Test
  public void testAuthorizePath_byUserTargets() {
    boolean result = underTest.authorizePath(repository, new ResourceStoreRequest("/test.jar"), Action.read);
    assertThat(result, is(true));
  }

  @Test
  public void testAuthorizePath_byUserTargets_almightySubject() {
    when(securitySystem.getSubject()).thenReturn(FakeAlmightySubject.TASK_SUBJECT);

    boolean result = underTest.authorizePath(repository, new ResourceStoreRequest("/test.jar"), Action.read);
    assertThat(result, is(true));
  }

  @Test
  public void testAuthorizePath_byUserTargets_invalidSubject() {
    when(securitySystem.getSubject()).thenReturn(null);
    boolean result = underTest.authorizePath(repository, new ResourceStoreRequest("/test.jar"), Action.read);
    assertThat(result, is(false));
  }

  @Test
  public void testAuthorizePath_byUserTargets_invalidUser() throws Exception {
    when(securitySystem.getUser("subject")).thenThrow(UserNotFoundException.class);
    boolean result = underTest.authorizePath(repository, new ResourceStoreRequest("/test.jar"), Action.read);
    assertThat(result, is(false));
  }

  @Test
  public void testAuthorizePath_byUserTargets_noRoles() {
    when(user.getRoles()).thenReturn(Collections.emptySet());
    boolean result = underTest.authorizePath(repository, new ResourceStoreRequest("/test.jar"), Action.read);
    assertThat(result, is(false));
  }

  @Test
  public void testAuthorizePath_byUserTargets_invalidRole() throws Exception {
    when(authorizationManager.getRole("role")).thenThrow(NoSuchRoleException.class);
    boolean result = underTest.authorizePath(repository, new ResourceStoreRequest("/test.jar"), Action.read);
    assertThat(result, is(false));
  }

  @Test
  public void testAuthorizePath_byUserTargets_roleWithNoPrivileges() throws Exception {
    when(role.getPrivileges()).thenReturn(Collections.emptySet());
    boolean result = underTest.authorizePath(repository, new ResourceStoreRequest("/test.jar"), Action.read);
    assertThat(result, is(false));
  }

  @Test
  public void testAuthorizePath_byUserTargets_roleWithInvalidPrivilege() throws Exception {
    when(authorizationManager.getPrivilege("privilege")).thenThrow(NoSuchPrivilegeException.class);
    boolean result = underTest.authorizePath(repository, new ResourceStoreRequest("/test.jar"), Action.read);
    assertThat(result, is(false));
  }

  @Test
  public void testAuthorizePath_byUserTargets_roleWithAdminPrivilege() {
    when(role.getPrivileges()).thenReturn(Collections.singleton(ADMIN_PRIVILEGE_ID));
    boolean result = underTest.authorizePath(repository, new ResourceStoreRequest("/test.jar"), Action.read);
    assertThat(result, is(true));
  }

  @Test
  public void testAuthorizePath_byUserTargets_roleWithNonRepoTargetPrivilege() {
    when(privilege.getType()).thenReturn("OTHER");
    boolean result = underTest.authorizePath(repository, new ResourceStoreRequest("/test.jar"), Action.read);
    assertThat(result, is(false));
  }

  @Test
  public void testAuthorizePath_byUserTargets_roleWithInvalidRepoTargetPrivilege() {
    when(privilege.getPrivilegeProperty("repositoryTargetId")).thenReturn("invalid");
    boolean result = underTest.authorizePath(repository, new ResourceStoreRequest("/test.jar"), Action.read);
    assertThat(result, is(false));
  }

  @Test
  public void testAuthorizePath_byUserTargets_roleWithNonMatchingRepoTargetPrivilege() {
    when(target.isPathContained(contentClass, "/test.jar")).thenReturn(false);
    boolean result = underTest.authorizePath(repository, new ResourceStoreRequest("/test.jar"), Action.read);
    assertThat(result, is(false));
  }

  @Test
  public void testAuthorizePath_byUserTargets_roleWithNonPrivilegedRepoTargetPrivilege() {
    when(subject.isPermitted("nexus:target:target:repository:read")).thenReturn(false);
    boolean result = underTest.authorizePath(repository, new ResourceStoreRequest("/test.jar"), Action.read);
    assertThat(result, is(false));
  }
}
