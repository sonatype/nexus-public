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
package org.sonatype.nexus.security;

import java.util.Arrays;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

// NOTE: Specifically not using SecuritySystem here as that is a legacy api and has a lot of cruft

/**
 * Security helper.
 *
 * Exposed to add additional support around security as well as to promote testability via mocks.
 *
 * @since 3.0
 */
@Named
@Singleton
public class SecurityHelper
    extends ComponentSupport
{
  /**
   * Returns current security manager.
   */
  public SecurityManager getSecurityManager() {
    return SecurityUtils.getSecurityManager();
  }

  /**
   * Returns current subject.
   */
  public Subject subject() {
    return SecurityUtils.getSubject();
  }

  /**
   * Ensure subject has given permissions.
   *
   * @throws AuthorizationException
   */
  public void ensurePermitted(final Subject subject, final Permission... permissions) {
    checkNotNull(subject);
    checkNotNull(permissions);
    checkArgument(permissions.length != 0);

    if (log.isTraceEnabled()) {
      log.trace("Ensuring subject '{}' has permissions: {}", subject.getPrincipal(), Arrays.toString(permissions));
    }
    subject.checkPermissions(Arrays.asList(permissions));
  }

  /**
   * Ensure subject has any of the given permissions.
   *
   * @throws AuthorizationException
   */
  public void ensureAnyPermitted(final Subject subject, final Permission... permissions) {
    checkNotNull(subject);
    checkNotNull(permissions);
    checkArgument(permissions.length != 0);

    if (log.isTraceEnabled()) {
      log.trace("Ensuring subject '{}' has any of the following permissions: {}", subject.getPrincipal(), Arrays.toString(permissions));
    }

    if (!anyPermitted(subject, permissions)) {
      throw new AuthorizationException("User is not permitted.");
    }
  }

  /**
   * Ensure current subject has given permissions.
   *
   * @throws AuthorizationException
   */
  public void ensurePermitted(final Permission... permissions) {
    ensurePermitted(subject(), permissions);
  }

  /**
   * Check if subject has ANY of the given permissions.
   */
  public boolean anyPermitted(final Subject subject, final Permission... permissions) {
    checkNotNull(subject);
    checkNotNull(permissions);
    checkArgument(permissions.length != 0);

    boolean trace = log.isTraceEnabled();
    if (trace) {
      log.trace("Checking if subject '{}' has ANY of these permissions: {}",
          subject.getPrincipal(), Arrays.toString(permissions));
    }
    for (Permission permission : permissions) {
      if (subject.isPermitted(permission)) {
        if (trace) {
          log.trace("Subject '{}' has permission: {}", subject.getPrincipal(), permission);
        }
        return true;
      }
    }
    if (trace) {
      log.trace("Subject '{}' missing required permissions: {}",
          subject.getPrincipal(), Arrays.toString(permissions));
    }
    return false;
  }

  /**
   * Check if current subject has ANY of the given permissions.
   */
  public boolean anyPermitted(final Permission... permissions) {
    return anyPermitted(subject(), permissions);
  }

  /**
   * Check if subject has ALL of the given permissions.
   */
  public boolean allPermitted(final Subject subject, final Permission... permissions) {
    checkNotNull(subject);
    checkNotNull(permissions);
    checkArgument(permissions.length != 0);

    boolean trace = log.isTraceEnabled();
    if (trace) {
      log.trace("Checking if subject '{}' has ALL of these permissions: {}",
          subject.getPrincipal(), Arrays.toString(permissions));
    }
    for (Permission permission : permissions) {
      if (!subject.isPermitted(permission)) {
        if (trace) {
          log.trace("Subject '{}' missing permission: {}", subject.getPrincipal(), permission);
        }
        return false;
      }
    }

    if (trace) {
      log.trace("Subject '{}' has required permissions: {}",
          subject.getPrincipal(), Arrays.toString(permissions));
    }
    return true;
  }

  /**
   * Check if current subject has ALL of the given permissions.
   */
  public boolean allPermitted(final Permission... permissions) {
    return allPermitted(subject(), permissions);
  }

  /**
   * Check which permissions the subject has.
   *
   * @since 3.13
   */
  public boolean[] isPermitted(final Subject subject, final Permission... permissions) {
    checkNotNull(subject);
    checkNotNull(permissions);
    checkArgument(permissions.length != 0);

    boolean trace = log.isTraceEnabled();
    if (trace) {
      log.trace("Checking which permissions subject '{}' has in: {}", subject.getPrincipal(),
          Arrays.toString(permissions));
    }
    boolean[] results = subject.isPermitted(Arrays.asList(permissions));
    if (trace) {
      log.trace("Subject '{}' has permissions: [{}] results {}", subject.getPrincipal(), Arrays.toString(permissions),
          results);
    }
    return results;
  }

  /**
   * Check which permissions the current subject has.
   *
   * @since 3.13
   */
  public boolean[] isPermitted(final Permission... permissions) {
    return isPermitted(subject(), permissions);
  }
}
