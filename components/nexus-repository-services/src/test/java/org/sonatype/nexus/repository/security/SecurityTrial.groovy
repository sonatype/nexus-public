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
package org.sonatype.nexus.repository.security

import org.sonatype.goodies.testsupport.TestSupport

import org.apache.shiro.authz.permission.DomainPermission
import org.apache.shiro.authz.permission.WildcardPermission
import org.junit.Test

/**
 * Security trials.
 */
class SecurityTrial
  extends TestSupport
{
  @Test
  void 'wildcard permission string'() {
    def p = new WildcardPermission('foo:bar:*:baz')
    log p
  }

  @Test
  void 'domain permission string'() {
    def p = new DomainPermission('foo,bar', 'read,wrote')
    log p
  }

  private static class CustomPermission
    extends DomainPermission
  {
    CustomPermission(final String actions, final String targets) {
      super(actions, targets)
    }
  }

  @Test
  void 'custom domain permission string'() {
    def p = new CustomPermission('foo,bar', 'read,wrote')
    log p
  }

  @Test
  void 'implied permission'() {
    def granted = new WildcardPermission('test:*')
    def permission = new WildcardPermission('test:foo')
    log granted.implies(permission)
  }
}
