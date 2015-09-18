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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.apache.shiro.authc.credential.HashingPasswordService;
import org.apache.shiro.authc.credential.PasswordService;
import org.apache.shiro.crypto.hash.DefaultHashService;
import org.apache.shiro.crypto.hash.Hash;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link PasswordService}.
 *
 * A PasswordService that provides a default password policy.
 * 
 * The intent of the password service is to encapsulate all password handling
 * details, such as password comparisons, hashing algorithm, hash iterations, salting policy, etc.
 * 
 * This class is just a wrapper around DefaultPasswordService to apply the default password policy,
 * and provide backward compatibility with legacy SHA1 and MD5 based passwords.
 */
@Named("default")
@Singleton
public class DefaultSecurityPasswordService
    implements HashingPasswordService
{
  private static final String DEFAULT_HASH_ALGORITHM = "SHA-512";

  private static final int DEFAULT_HASH_ITERATIONS = 1024;

  /**
   * Provides the actual implementation of PasswordService.
   * We are just wrapping to apply default policy
   */
  private final DefaultPasswordService passwordService;

  /**
   * Provides password services for legacy passwords (e.g. pre-2.5 SHA-1/MD5-based hashes)
   */
  private final PasswordService legacyPasswordService;

  @Inject
  public DefaultSecurityPasswordService(final @Named("legacy") PasswordService legacyPasswordService) {
    this.passwordService = new DefaultPasswordService();
    this.legacyPasswordService = checkNotNull(legacyPasswordService);

    //Create and set a hash service according to our hashing policies
    DefaultHashService hashService = new DefaultHashService();
    hashService.setHashAlgorithmName(DEFAULT_HASH_ALGORITHM);
    hashService.setHashIterations(DEFAULT_HASH_ITERATIONS);
    hashService.setGeneratePublicSalt(true);
    this.passwordService.setHashService(hashService);
  }

  @Override
  public String encryptPassword(final Object plaintextPassword) {
    return passwordService.encryptPassword(plaintextPassword);
  }

  @Override
  public boolean passwordsMatch(final Object submittedPlaintext, final String encrypted) {
    //When hash is just a string, it could be a legacy password. Check both
    //current and legacy password services

    return passwordService.passwordsMatch(submittedPlaintext, encrypted) ||
        legacyPasswordService.passwordsMatch(submittedPlaintext, encrypted);
  }

  @Override
  public Hash hashPassword(final Object plaintext) {
    return passwordService.hashPassword(plaintext);
  }

  @Override
  public boolean passwordsMatch(final Object plaintext, final Hash savedPasswordHash) {
    return passwordService.passwordsMatch(plaintext, savedPasswordHash);
  }
}
