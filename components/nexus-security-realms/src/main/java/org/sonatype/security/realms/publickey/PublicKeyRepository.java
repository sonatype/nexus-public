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
import java.util.Set;

/**
 * Repository for obtaining each user account's {@link java.security.PublicKey}s. An implementation of this interface
 * is
 * required by the {@link com.sonatype.sshjgit.core.shiro.publickey.PublicKeyAuthenticatingRealm}.
 *
 * @author hugo@josefson.org
 */
public interface PublicKeyRepository
{

  /**
   * Add a public key to a principal.
   */
  public void addPublicKey(Object principal, PublicKey publicKey);

  /**
   * Add a Set of public keys to a principal.
   */
  public void addPublicKeys(Object principal, Set<PublicKey> publicKeys);

  /**
   * Remove a public key from a principal.
   */
  public void removePublicKey(Object principal, PublicKey publicKey);

  /**
   * Retrieves an account's {@link java.security.PublicKey}s.
   *
   * @param principal the principal to look up.
   * @return a set of keys with which the account is allowed to authenticate. never {@code null}.
   */
  Set<PublicKey> getPublicKeys(Object principal);

  /**
   * Checks to see if this repository has an account with the supplied principal.
   *
   * @param principal the principal to look for.
   * @return {@code true} is the account is known, {@code false} otherwise.
   */
  boolean hasAccount(Object principal);

}
