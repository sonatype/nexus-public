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
package org.sonatype.nexus.security.internal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.apache.shiro.authc.credential.PasswordService;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Legacy {@link PasswordService}.
 *
 * PasswordService for verifying legacy passwords (SHA-1 and MD5).
 *
 * Shiro 2.x removed the iteration and salt-generation setters from DefaultHashService and now always
 * generates a public salt, so the original unsalted, single-iteration legacy hashes can no longer be
 * reproduced through DefaultPasswordService. The hashes are computed directly via {@link SimpleHash}
 * instead, which preserves byte-for-byte the previous behaviour (unsalted, single iteration, hex
 * encoded) so existing SHA-1/MD5 password hashes continue to verify.
 */
@Component
@Qualifier("legacy")
public class LegacyNexusPasswordService
    implements PasswordService
{
  private static final int LEGACY_HASH_ITERATIONS = 1;

  @Override
  public String encryptPassword(final Object plaintextPassword) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean passwordsMatch(final Object submittedPlaintext, final String encrypted) {
    // Legacy passwords can be hashed with sha-1 or md5, check both
    return hexHashMatches("SHA-1", submittedPlaintext, encrypted) ||
        hexHashMatches("MD5", submittedPlaintext, encrypted);
  }

  /**
   * Recomputes the unsalted, single-iteration hex hash of the submitted plaintext for the given
   * algorithm and compares it against the stored value in constant time.
   */
  private boolean hexHashMatches(final String algorithm, final Object submittedPlaintext, final String encrypted) {
    if (encrypted == null || submittedPlaintext == null) {
      return false;
    }
    // Use the no-salt constructor: Shiro 2.x SimpleHash rejects a null salt, and this overload
    // hashes with an empty salt (equivalent to the original unsalted legacy behaviour).
    String computed = new SimpleHash(algorithm, submittedPlaintext, LEGACY_HASH_ITERATIONS).toHex();
    return MessageDigest.isEqual(
        encrypted.getBytes(StandardCharsets.UTF_8), computed.getBytes(StandardCharsets.UTF_8));
  }
}
