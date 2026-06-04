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

import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import org.sonatype.nexus.crypto.internal.HashingHandlerFactory;
import org.sonatype.nexus.crypto.internal.error.CipherException;
import org.sonatype.nexus.crypto.HashingHandler;
import org.sonatype.nexus.security.authc.AuthenticationFailureReason;
import org.sonatype.nexus.security.authc.NexusAuthenticationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.apache.shiro.authc.credential.HashingPasswordService;
import org.apache.shiro.authc.credential.PasswordService;
import org.apache.shiro.crypto.hash.DefaultHashService;
import org.apache.shiro.crypto.hash.Hash;
import org.bouncycastle.crypto.fips.FipsUnapprovedOperationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.FeatureFlags.NEXUS_SECURITY_PASSWORD_ALGORITHM_NAMED_VALUE;
import static org.sonatype.nexus.common.app.FeatureFlags.NEXUS_SECURITY_PASSWORD_ITERATIONS_NAMED_VALUE;

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
@Primary
@Component
@Qualifier("default")
public class DefaultSecurityPasswordService
    implements HashingPasswordService
{

  private static final Logger log = LoggerFactory.getLogger(DefaultSecurityPasswordService.class);

  private static final String SHIRO_PASSWORD_ALGORITHM = "shiro1";

  private static final String DEFAULT_HASH_ALGORITHM = "SHA-512";

  private static final int DEFAULT_HASH_ITERATIONS = 1024;

  /**
   * Provides the actual implementation of PasswordService.
   * We are just wrapping to apply default policy
   */
  private final DefaultPasswordService defaultShiroPasswordService;

  /**
   * Provides password services for legacy passwords (e.g. pre-2.5 SHA-1/MD5-based hashes)
   */
  private final PasswordService legacyNexusPasswordService;

  private final String nexusPasswordAlgorithm;

  private final HashingHandlerFactory hashingHandlerFactory;

  private final Integer configuredPasswordIterations;

  @Autowired
  public DefaultSecurityPasswordService(
      @Qualifier("legacy") final PasswordService legacyPasswordService,
      @Value(NEXUS_SECURITY_PASSWORD_ALGORITHM_NAMED_VALUE) final String nexusPasswordAlgorithm,
      @Value(NEXUS_SECURITY_PASSWORD_ITERATIONS_NAMED_VALUE) final Integer configuredPasswordIterations,
      HashingHandlerFactory hashingHandlerFactory)
  {
    this.legacyNexusPasswordService = checkNotNull(legacyPasswordService);

    this.defaultShiroPasswordService = new DefaultPasswordService();
    // Create and set a hash service according to our hashing policies
    DefaultHashService hashService = new DefaultHashService();
    hashService.setHashAlgorithmName(DEFAULT_HASH_ALGORITHM);
    hashService.setHashIterations(DEFAULT_HASH_ITERATIONS);
    hashService.setGeneratePublicSalt(true);
    this.defaultShiroPasswordService.setHashService(hashService);

    this.nexusPasswordAlgorithm = checkNotNull(nexusPasswordAlgorithm);
    this.hashingHandlerFactory = hashingHandlerFactory;
    this.configuredPasswordIterations = configuredPasswordIterations;
  }

  @Override
  public String encryptPassword(final Object plaintextPassword) {
    if (nexusPasswordAlgorithm.equals(SHIRO_PASSWORD_ALGORITHM)) {
      return defaultShiroPasswordService.encryptPassword(plaintextPassword);
    }
    try {
      // Pass configuredPasswordIterations explicitly for user passwords
      HashingHandler hashingHandler =
          hashingHandlerFactory.create(nexusPasswordAlgorithm, null, configuredPasswordIterations);
      return hashingHandler.hash(convertToCharArray(plaintextPassword)).toPhcString();
    }
    catch (CipherException | IllegalArgumentException | NullPointerException e) {
      log.error("Failed to encrypt password due to algorithm issue", e);
      throw new NexusAuthenticationException("Password is not strong enough",
          Set.of(AuthenticationFailureReason.UNKNOWN));
    }
    catch (FipsUnapprovedOperationError e) {
      log.error("Failed to encrypt password", e);
      throw new NexusAuthenticationException("Password is not strong enough",
          Set.of(AuthenticationFailureReason.INCORRECT_CREDENTIALS));
    }
  }

  @Override
  public boolean passwordsMatch(final Object submittedPlaintextPassword, final String storedHash) {
    if (storedHash == null || submittedPlaintextPassword == null) {
      return false;
    }

    Optional<Boolean> validPassword = validatePassword(submittedPlaintextPassword, storedHash);
    if (validPassword.isPresent()) {
      return validPassword.get();
    }

    log.debug("PHC format invalid, falling back to legacy password service");
    // When hash is just a string, it could be a legacy password.
    // Check shiro password service or legacy nexus password service
    return defaultShiroPasswordService.passwordsMatch(submittedPlaintextPassword, storedHash) ||
        legacyNexusPasswordService.passwordsMatch(submittedPlaintextPassword, storedHash);
  }

  @Override
  public Hash hashPassword(final Object plaintext) {
    return defaultShiroPasswordService.hashPassword(plaintext);
  }

  @Override
  public boolean passwordsMatch(final Object plaintext, final Hash savedPasswordHash) {
    return defaultShiroPasswordService.passwordsMatch(plaintext, savedPasswordHash);
  }

  private static char[] convertToCharArray(final Object plaintext) {
    if (plaintext == null) {
      return new char[0]; // Return empty array for null input
    }
    else if (plaintext instanceof char[]) {
      return ((char[]) plaintext).clone();
    }
    else if (plaintext instanceof String) {
      return ((String) plaintext).toCharArray();
    }
    else {
      return plaintext.toString().toCharArray();
    }
  }

  private Optional<Boolean> validatePassword(final Object submittedPlaintextPassword, final String storedHash) {
    char[] submittedPassword = convertToCharArray(submittedPlaintextPassword);
    try {
      HashingHandler hashingHandler = hashingHandlerFactory.create(storedHash);
      return Optional.of(hashingHandler.verify(submittedPassword, storedHash));
    }
    catch (IllegalArgumentException | NullPointerException | InvalidKeySpecException e) {
      return Optional.empty();
    }
    finally {
      // Clear the password array to prevent sensitive data
      Arrays.fill(submittedPassword, '\0');
    }
  }
}
