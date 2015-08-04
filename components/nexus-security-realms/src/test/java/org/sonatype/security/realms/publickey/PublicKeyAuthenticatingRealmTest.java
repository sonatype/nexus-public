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
package org.sonatype.security.realms.publickey;

import java.security.PublicKey;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.junit.Test;

public class PublicKeyAuthenticatingRealmTest
{

  @Test
  public void testSuccess() {
    SimplePublicKeyRepository publicKeyRepository = new SimplePublicKeyRepository();
    PublicKey testUsersKey = new MockPublicKey("key1");
    publicKeyRepository.addPublicKey("test-user", testUsersKey);

    PublicKeyAuthenticatingRealm realm = new PublicKeyAuthenticatingRealm(publicKeyRepository);

    AuthenticationInfo authInfo =
        realm.getAuthenticationInfo(new PublicKeyAuthenticationToken("test-user", testUsersKey));
    Assert.assertEquals("test-user", authInfo.getPrincipals().getPrimaryPrincipal());
  }

  @Test
  public void testSuccessWithMultipleKeys() {
    SimplePublicKeyRepository publicKeyRepository = new SimplePublicKeyRepository();
    Set<PublicKey> keySet = new HashSet<PublicKey>();
    keySet.add(new MockPublicKey("key1"));
    keySet.add(new MockPublicKey("key2"));
    keySet.add(new MockPublicKey("key3"));
    publicKeyRepository.addPublicKeys("test-user", keySet);

    PublicKey foosKey = new MockPublicKey("foos-key");
    publicKeyRepository.addPublicKey("foo-bar", foosKey);

    PublicKeyAuthenticatingRealm realm = new PublicKeyAuthenticatingRealm(publicKeyRepository);

    for (PublicKey publicKey : keySet) {
      AuthenticationInfo authInfo =
          realm.getAuthenticationInfo(new PublicKeyAuthenticationToken("test-user", publicKey));
      Assert.assertEquals("test-user", authInfo.getPrincipals().getPrimaryPrincipal());
    }

    try {
      realm.getAuthenticationInfo(new PublicKeyAuthenticationToken("test-user", foosKey));
      Assert.fail("expected AuthenticationException");
    }
    catch (AuthenticationException e) {
      // expected
    }
  }

  @Test
  public void testFailureConditions() {
    SimplePublicKeyRepository publicKeyRepository = new SimplePublicKeyRepository();
    PublicKey testUsersKey = new MockPublicKey("key1");
    publicKeyRepository.addPublicKey("test-user", testUsersKey);
    PublicKey otherKey = new MockPublicKey("key2");
    publicKeyRepository.addPublicKey("foo-bar", otherKey);

    PublicKeyAuthenticatingRealm realm = new PublicKeyAuthenticatingRealm(publicKeyRepository);

    // try valid user with other users key
    try {
      realm.getAuthenticationInfo(new PublicKeyAuthenticationToken("test-user", otherKey));
      Assert.fail("expected AuthenticationException");
    }
    catch (AuthenticationException e) {
      // expected
    }

    // try invalid user with valid key
    Assert.assertNull(realm.getAuthenticationInfo(new PublicKeyAuthenticationToken("Some-Random-Username",
        testUsersKey)));
  }

  @Test
  public void testToken() {
    PublicKeyAuthenticatingRealm realm = new PublicKeyAuthenticatingRealm();
    Assert.assertFalse(realm.supports(new UsernamePasswordToken()));

    // the content is not important, just the class type
    Assert.assertTrue(realm.supports(new PublicKeyAuthenticationToken(null, null)));
  }

  @Test
  public void testAuthorization() {
    PublicKeyAuthenticatingRealm realm = new PublicKeyAuthenticatingRealm();
    Assert.assertFalse(realm.isPermitted(new SimplePrincipalCollection("user", realm.getName()),
        "some:permission"));
  }
}
