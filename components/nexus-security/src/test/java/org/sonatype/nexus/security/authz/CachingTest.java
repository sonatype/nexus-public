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

import org.sonatype.nexus.security.AbstractSecurityTest;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.realm.MockRealmB;
import org.sonatype.nexus.security.user.User;

import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.junit.Assert;
import org.junit.Test;

public class CachingTest
    extends AbstractSecurityTest
{
  @Test
  public void testCacheClearing() throws Exception {
    SecuritySystem securitySystem = this.lookup(SecuritySystem.class);

    MockRealmB mockRealmB = (MockRealmB) this.lookup(Realm.class, "MockRealmB");

    // cache should be empty to start
    Assert.assertTrue(mockRealmB.getAuthorizationCache().keys().isEmpty());

    Assert.assertTrue(securitySystem.isPermitted(
        new SimplePrincipalCollection("jcool", mockRealmB.getName()), "test:heHasIt"));

    // now something will be in the cache, just make sure
    Assert.assertFalse(mockRealmB.getAuthorizationCache().keys().isEmpty());

    // now if we update a user the cache should be cleared
    User user = securitySystem.getUser("bburton", "MockUserManagerB");
    // different user, doesn't matter, in the future we should get a little more fine grained
    securitySystem.updateUser(user);

    // empty again
    Assert.assertTrue(mockRealmB.getAuthorizationCache().keys().isEmpty());
  }
}
