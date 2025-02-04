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
package org.sonatype.nexus.security.authz;

import org.sonatype.goodies.testsupport.TestSupport;

import org.apache.shiro.authz.permission.WildcardPermission;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Permission trials.
 */
public class PermissionTrialTest
    extends TestSupport
{
  @Test
  public void testImpliedWildcard() {
    WildcardPermission perm = new WildcardPermission("nexus:something:special:read");
    WildcardPermission granted = new WildcardPermission("nexus:*");
    assertTrue(granted.implies(perm));
  }

  @Test
  public void testImpliedWildcard_withoutAsterick() {
    WildcardPermission perm = new WildcardPermission("nexus:something:special:read");
    WildcardPermission granted = new WildcardPermission("nexus");
    assertTrue(granted.implies(perm));
  }
}
