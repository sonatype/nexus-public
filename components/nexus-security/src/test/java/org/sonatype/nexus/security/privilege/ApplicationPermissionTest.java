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
package org.sonatype.nexus.security.privilege;

import org.sonatype.goodies.testsupport.TestSupport;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.PermissionResolver;
import org.apache.shiro.authz.permission.WildcardPermissionResolver;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests for {@link ApplicationPermission}.
 */
public class ApplicationPermissionTest
    extends TestSupport
{
  private final PermissionResolver permissionResolver = new WildcardPermissionResolver();

  @Test
  public void simpleApplicationPermissionsMatch() {

    Permission authorizedPermission = new ApplicationPermission("feature", "action", "anotherAction");

    // simulate @RequiresPermissions resolution on UI resources
    Permission requiredPermission = permissionResolver.resolvePermission("nexus:feature:action");

    assertThat(authorizedPermission.implies(requiredPermission), is(true));
  }

  @Test
  public void complexApplicationPermissionsMatch() {

    Permission authorizedPermission = new ApplicationPermission("feature:method", "action", "anotherAction");

    // simulate @RequiresPermissions resolution on UI resources
    Permission requiredPermission = permissionResolver.resolvePermission("nexus:feature:method:action");

    assertThat(authorizedPermission.implies(requiredPermission), is(true));
  }
}
