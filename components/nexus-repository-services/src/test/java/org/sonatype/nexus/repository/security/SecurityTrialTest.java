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
package org.sonatype.nexus.repository.security;

import org.sonatype.goodies.testsupport.TestSupport;

import org.apache.shiro.authz.permission.DomainPermission;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class SecurityTrialTest
    extends TestSupport
{
  @Test
  public void wildcardPermissionString() {
    assertDoesNotThrowException(() -> new WildcardPermission("foo:bar:*:baz"));
  }

  @Test
  public void domainPermissionString() {
    assertDoesNotThrowException(() -> new DomainPermission("foo,bar", "read,write"));
  }

  private static class CustomPermission
      extends DomainPermission
  {
    CustomPermission(final String actions, final String targets) {
      super(actions, targets);
    }
  }

  @Test
  public void customDomainPermissionString() {
    assertDoesNotThrowException(() -> new CustomPermission("foo,bar", "read,write"));
  }

  @Test
  public void impliedPermission() {
    WildcardPermission granted = new WildcardPermission("test:*");
    WildcardPermission permission = new WildcardPermission("test:foo");
    assertTrue(granted.implies(permission));
  }

  private void assertDoesNotThrowException(Runnable r) {
    try {
      r.run();
    }
    catch (Exception e) {
      throw new AssertionError("Expected no exception, but got: " + e, e);
    }
  }
}
