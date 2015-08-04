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
import org.junit.Test;

public class PublicKeyRepositoryTest
{

  /**
   * @return the PublicKeyRepository under test.
   */
  protected PublicKeyRepository getPublicKeyRepository() {
    return new SimplePublicKeyRepository();
  }

  @Test
  public void testAddPublicKey() {
    PublicKeyRepository keyRepo = this.getPublicKeyRepository();

    PublicKey key1 = new MockPublicKey("key1");
    PublicKey key2 = new MockPublicKey("key2");
    PublicKey invalidKey = new MockPublicKey("invalidKey");

    keyRepo.addPublicKey("user", key1);
    keyRepo.addPublicKey("user", key2);

    Set<PublicKey> keys = keyRepo.getPublicKeys("user");
    Assert.assertTrue(keys.contains(key1));
    Assert.assertTrue(keys.contains(key2));
    Assert.assertFalse(keys.contains(invalidKey));
    Assert.assertEquals(2, keys.size());
  }

  @Test
  public void testAddPublicKeys() {
    PublicKeyRepository keyRepo = this.getPublicKeyRepository();

    PublicKey key1 = new MockPublicKey("key1");
    PublicKey key2 = new MockPublicKey("key2");
    PublicKey key3 = new MockPublicKey("key3");

    Set<PublicKey> userKeys = new HashSet<PublicKey>();
    userKeys.add(key1);
    userKeys.add(key2);

    keyRepo.addPublicKeys("user", userKeys);
    keyRepo.addPublicKey("user", key3);

    Set<PublicKey> keys = keyRepo.getPublicKeys("user");
    Assert.assertTrue(keys.contains(key1));
    Assert.assertTrue(keys.contains(key2));
    Assert.assertTrue(keys.contains(key3));
    Assert.assertEquals(3, keys.size());
  }

  @Test
  public void testRemovePublicKey() {
    PublicKeyRepository keyRepo = this.getPublicKeyRepository();

    PublicKey key1 = new MockPublicKey("key1");
    PublicKey key2 = new MockPublicKey("key2");
    PublicKey key3 = new MockPublicKey("key3");

    keyRepo.addPublicKey("user", key1);
    keyRepo.addPublicKey("user", key2);
    keyRepo.addPublicKey("user", key3);

    // now remove key2
    keyRepo.removePublicKey("user", key2);

    Set<PublicKey> keys = keyRepo.getPublicKeys("user");
    Assert.assertTrue(keys.contains(key1));
    Assert.assertFalse(keys.contains(key2)); // is not here
    Assert.assertTrue(keys.contains(key3));
    Assert.assertEquals(2, keys.size());
  }

  @Test
  public void testHasAccount() {
    PublicKeyRepository keyRepo = this.getPublicKeyRepository();
    keyRepo.addPublicKey("user1", new MockPublicKey("key1"));
    keyRepo.addPublicKey("user2", new MockPublicKey("key2"));

    Assert.assertTrue(keyRepo.hasAccount("user1"));
    Assert.assertTrue(keyRepo.hasAccount("user2"));
    Assert.assertFalse(keyRepo.hasAccount("user3"));
  }
}
