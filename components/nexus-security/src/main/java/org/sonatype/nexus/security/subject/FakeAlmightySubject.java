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
package org.sonatype.nexus.security.subject;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.ExecutionException;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectCallable;
import org.apache.shiro.subject.support.SubjectRunnable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

// FIXME: Rename to TaskSubject or drop task concept and change to SystemSubject?

/**
 * An "almighty" subject, that has all permissions, all roles, has all. Mostly to be used in thread pools and executor
 * services, as security subject for task execution.
 *
 * @since 2.6
 */
public class FakeAlmightySubject
    implements Subject
{
  public static final String TASK_USERID = "*TASK";

  public static final Subject TASK_SUBJECT = forUserId(TASK_USERID);

  /**
   * Creates an "almighty" Subject with given String userId as principal. Should be used with care, as usually you
   * don't need to tackle anything with this class, as Nexus uses this class only for scheduled tasks.
   */
  public static Subject forUserId(final String fakeUserId) {
    return new FakeAlmightySubject(fakeUserId);
  }

  // ==

  private final String fakeUserId;

  private final PrincipalCollection principalCollection;

  private FakeAlmightySubject(final String fakeUserId) {
    this.fakeUserId = checkNotNull(fakeUserId);
    this.principalCollection = new SimplePrincipalCollection(fakeUserId, getClass().getName());
  }

  @Override
  public Object getPrincipal() {
    return fakeUserId;
  }

  @Override
  public PrincipalCollection getPrincipals() {
    return principalCollection;
  }

  @Override
  public boolean isPermitted(final String permission) {
    return true;
  }

  @Override
  public boolean isPermitted(final Permission permission) {
    return true;
  }

  @Override
  public boolean[] isPermitted(final String... permissions) {
    return repeat(true, permissions.length);
  }

  @Override
  public boolean[] isPermitted(final List<Permission> permissions) {
    return repeat(true, permissions.size());
  }

  @Override
  public boolean isPermittedAll(final String... permissions) {
    return true;
  }

  @Override
  public boolean isPermittedAll(final Collection<Permission> permissions) {
    return true;
  }

  @Override
  public void checkPermission(final String permission) throws AuthorizationException {
    // do nothing
  }

  @Override
  public void checkPermission(final Permission permission) throws AuthorizationException {
    // do nothing
  }

  @Override
  public void checkPermissions(final String... permissions) throws AuthorizationException {
    // do nothing
  }

  @Override
  public void checkPermissions(final Collection<Permission> permissions) throws AuthorizationException {
    // do nothing
  }

  @Override
  public boolean hasRole(final String roleIdentifier) {
    return true;
  }

  @Override
  public boolean[] hasRoles(final List<String> roleIdentifiers) {
    return repeat(true, roleIdentifiers.size());
  }

  @Override
  public boolean hasAllRoles(final Collection<String> roleIdentifiers) {
    return true;
  }

  @Override
  public void checkRole(final String roleIdentifier) throws AuthorizationException {
    // do nothing
  }

  @Override
  public void checkRoles(final Collection<String> roleIdentifiers) throws AuthorizationException {
    // do nothing
  }

  @Override
  public void checkRoles(final String... roleIdentifiers) throws AuthorizationException {
    // do nothing
  }

  @Override
  public void login(final AuthenticationToken token) throws AuthenticationException {
    // do nothing
  }

  @Override
  public void logout() {
    // do nothing
  }

  @Override
  public boolean isAuthenticated() {
    return true;
  }

  @Override
  public boolean isRemembered() {
    return false;
  }

  @Override
  public Session getSession() {
    return null;
  }

  @Override
  public Session getSession(final boolean create) {
    return getSession();
  }

  @Override
  public <V> V execute(final Callable<V> callable) throws ExecutionException {
    try {
      return associateWith(callable).call();
    }
    catch (final Throwable t) {
      throw new ExecutionException(t);
    }
  }

  @Override
  public void execute(final Runnable runnable) {
    associateWith(runnable).run();
  }

  @Override
  public <V> Callable<V> associateWith(final Callable<V> callable) {
    return new SubjectCallable<V>(this, callable);
  }

  @Override
  public Runnable associateWith(final Runnable runnable) {
    return new SubjectRunnable(this, runnable);
  }

  @Override
  public void runAs(final PrincipalCollection principals) {
    throw new IllegalStateException("The " + getClass().getName() + " subject does not support runAs");
  }

  @Override
  public boolean isRunAs() {
    return false;
  }

  @Override
  public PrincipalCollection getPreviousPrincipals() {
    return null;
  }

  @Override
  public PrincipalCollection releaseRunAs() {
    return null;
  }

  private boolean[] repeat(final boolean val, final int count) {
    checkArgument(count > -1);
    final boolean[] result = new boolean[count];
    Arrays.fill(result, val);
    return result;
  }
}
