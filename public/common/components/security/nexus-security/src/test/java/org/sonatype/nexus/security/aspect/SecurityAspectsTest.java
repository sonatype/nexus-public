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
package org.sonatype.nexus.security.aspect;

import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;

import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(AuthenticationExtension.class)
class SecurityAspectsTest
{
  @Test
  @WithUser
  void testMethodRequiresAuthentication() {
    assertDoesNotThrow(
        this::methodRequiringAuthentication,
        "Method should not throw an exception when the subject is authenticated");
  }

  @Test
  @WithUser(isAuthenticated = false)
  void testMethodThrowsExceptionWhenAUserIsNotAuthenticated() {
    assertThrows(UnauthenticatedException.class, this::methodRequiringAuthentication);
  }

  @Test
  @WithUser(permissions = "nexus:other:*")
  void testExceptionRequiresPermissions() {
    assertThrows(AuthorizationException.class,
        this::methodRequiresPermission,
        "Method should throw an exception when the user doesn't have the required permissions");
  }

  @Test
  @WithUser(permissions = "nexus:some:*")
  void testRequiresPermissions() {
    assertDoesNotThrow(
        this::methodRequiresPermission,
        "Method should not throw an exception when the user has the required permissions");
  }

  @Test
  @WithUser(permissions = {"nexus:capabilities:view", "nexus:capabilities:read"})
  void testRequiresMultiplePermissionsOnMethod() {
    assertThrows(UnauthorizedException.class,
        this::methodRequiresMultiplePermissions,
        "Method should throw an exception when the user doesn't have all the required permissions");
  }

  @Test
  @WithUser(permissions = {"nexus:capabilities:view"})
  void testMethodAllowsUserWithAtLeastOnePermission() {
    assertDoesNotThrow(
        this::methodAllowsUserWithAtLeastOnePermission,
        "Method should throw an exception when the user doesn't have all the required permissions");
  }

  @RequiresAuthentication
  public String methodRequiringAuthentication() {
    return "authenticated";
  }

  @RequiresPermissions("nexus:some:permission")
  public String methodRequiresPermission() {
    return "requires some permissions";
  }

  @RequiresPermissions(value = {"nexus:capabilities:write", "nexus:capabilities:read"}, logical = Logical.AND)
  public String methodRequiresMultiplePermissions() {
    return "requires multiple permissions";
  }

  @RequiresPermissions(value = {"nexus:capabilities:view", "nexus:capabilities:read"}, logical = Logical.OR)
  public String methodAllowsUserWithAtLeastOnePermission() {
    return "requires multiple permissions";
  }
}
