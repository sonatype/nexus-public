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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A {@code PublicKeyRepository} which stores its accounts in memory.
 *
 * @author hugo@josefson.org
 */
public class SimplePublicKeyRepository
    implements PublicKeyRepository
{

  /**
   * principal-to-publickeys. note that you must use {@link #accountsLock} when touching this.
   */
  protected final Map<Object, Set<PublicKey>> accounts = new HashMap<Object, Set<PublicKey>>();

  /**
   * lock for {@link #accounts}
   */
  protected final ReentrantReadWriteLock accountsLock = new ReentrantReadWriteLock();

  /**
   * Adds one publicKey with which a specific principal will be allowed to authenticate.
   *
   * @param principal the account's principal
   * @param publicKey the publicKey this principal will be allowed to authenticate with
   * @see #addPublicKeys(Object, java.util.Set)
   */
  public void addPublicKey(Object principal, PublicKey publicKey) {
    final HashSet<PublicKey> publicKeys = new HashSet<PublicKey>(1);
    publicKeys.add(publicKey);
    addPublicKeys(principal, publicKeys);
  }

  /**
   * Adds a set of publicKeys with which a specific principal will be allowed to authenticate.
   *
   * @param principal  the account's principal
   * @param publicKeys the publicKeys this principal is allowed to authenticate with
   */
  public void addPublicKeys(Object principal, Set<PublicKey> publicKeys) {
    accountsLock.writeLock().lock();
    try {
      if (hasAccount(principal)) {
        accounts.get(principal).addAll(publicKeys);
      }
      else {
        accounts.put(principal, new HashSet<PublicKey>(publicKeys));
      }
    }
    finally {
      accountsLock.writeLock().unlock();
    }
  }

  /**
   * Removes a {@code PublicKey} from the specified account.
   *
   * @param principal which account to remove the publicKey from
   * @param publicKey the ssh public key
   */
  public void removePublicKey(Object principal, PublicKey publicKey) {
    accountsLock.writeLock().lock(); // start with a write lock, because we cannot upgrade the lock (only
    // down-grade)
    try {
      if (hasAccount(principal)) {
        accounts.get(principal).remove(publicKey);
      }
      else {
        // good already
      }
    }
    finally {
      accountsLock.writeLock().unlock();
    }
  }

  public Set<PublicKey> getPublicKeys(Object principal) {
    accountsLock.readLock().lock();
    try {
      final Set<PublicKey> publicKeys = accounts.get(principal);
      if (publicKeys != null) {
        return new HashSet<PublicKey>(publicKeys);
      }
      else {
        return Collections.emptySet();
      }
    }
    finally {
      accountsLock.readLock().unlock();
    }
  }

  public boolean hasAccount(Object principal) {
    accountsLock.readLock().lock();
    try {
      return accounts.containsKey(principal);
    }
    finally {
      accountsLock.readLock().unlock();
    }
  }

}
