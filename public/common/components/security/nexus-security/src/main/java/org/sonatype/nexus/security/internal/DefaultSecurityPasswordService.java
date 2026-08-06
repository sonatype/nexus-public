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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.sonatype.nexus.crypto.HashingHandler;
import org.sonatype.nexus.crypto.internal.HashingHandlerFactory;
import org.sonatype.nexus.crypto.internal.error.CipherException;
import org.sonatype.nexus.security.authc.AuthenticationFailureReason;
import org.sonatype.nexus.security.authc.NexusAuthenticationException;

import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.apache.shiro.authc.credential.HashingPasswordService;
import org.apache.shiro.authc.credential.PasswordService;
import org.apache.shiro.crypto.hash.DefaultHashService;
import org.apache.shiro.crypto.hash.Hash;
import org.apache.shiro.crypto.hash.SimpleHashProvider;
import org.apache.shiro.crypto.hash.format.Shiro1CryptFormat;
import org.bouncycastle.crypto.fips.FipsUnapprovedOperationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.FeatureFlags.NEXUS_SECURITY_PASSWORD_ALGORITHM_NAMED_VALUE;
import static org.sonatype.nexus.common.app.FeatureFlags.NEXUS_SECURITY_PASSWORD_CACHE_ENABLED_NAMED_VALUE;
import static org.sonatype.nexus.common.app.FeatureFlags.NEXUS_SECURITY_PASSWORD_CACHE_EXPIRE_SECONDS_NAMED_VALUE;
import static org.sonatype.nexus.common.app.FeatureFlags.NEXUS_SECURITY_PASSWORD_CACHE_SIZE_NAMED_VALUE;
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

  // Deliberately kept at the historical Shiro 1.x value. This is below modern OWASP guidance, but the
  // iteration count is baked into every stored $shiro1$ hash; changing it would invalidate all existing
  // passwords. It is retained for backward compatibility, not as a recommended strength setting.
  private static final int DEFAULT_HASH_ITERATIONS = 1024;

  private static final String CREDENTIAL_KEY_MAC_ALGORITHM = "HmacSHA256";

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

  /**
   * Short-lived cache of successfully verified credentials (see {@code nexus.security.password.cache.*}). {@code null}
   * when disabled. Only positive results are stored, keyed by {@link #credentialCacheKey}.
   */
  private final Cache<String, Boolean> verifiedCredentialsCache;

  /**
   * Per-thread HMAC used to derive {@link #verifiedCredentialsCache} keys. {@code null} when the cache is disabled.
   * One {@link Mac} is intentionally retained per request thread for reuse (never {@code remove()}d); the footprint is
   * bounded by the server request thread pool.
   */
  private final ThreadLocal<Mac> credentialKeyMac;

  @Autowired
  public DefaultSecurityPasswordService(
      @Qualifier("legacy") final PasswordService legacyPasswordService,
      @Value(NEXUS_SECURITY_PASSWORD_ALGORITHM_NAMED_VALUE) final String nexusPasswordAlgorithm,
      @Value(NEXUS_SECURITY_PASSWORD_ITERATIONS_NAMED_VALUE) final Integer configuredPasswordIterations,
      final HashingHandlerFactory hashingHandlerFactory,
      @Value(NEXUS_SECURITY_PASSWORD_CACHE_ENABLED_NAMED_VALUE) final boolean verifiedCredentialsCacheEnabled,
      @Value(NEXUS_SECURITY_PASSWORD_CACHE_SIZE_NAMED_VALUE) final long verifiedCredentialsCacheSize,
      @Value(NEXUS_SECURITY_PASSWORD_CACHE_EXPIRE_SECONDS_NAMED_VALUE) final long verifiedCredentialsCacheExpireSeconds)
  {
    this(legacyPasswordService, nexusPasswordAlgorithm, configuredPasswordIterations, hashingHandlerFactory,
        verifiedCredentialsCacheEnabled, verifiedCredentialsCacheSize, verifiedCredentialsCacheExpireSeconds,
        Ticker.systemTicker());
  }

  /**
   * Package-private constructor that lets tests drive {@link #verifiedCredentialsCache} expiry through an injectable
   * {@link Ticker}. Production wiring uses {@link Ticker#systemTicker()} via the {@link Autowired} constructor above.
   */
  DefaultSecurityPasswordService(
      final PasswordService legacyPasswordService,
      final String nexusPasswordAlgorithm,
      final Integer configuredPasswordIterations,
      final HashingHandlerFactory hashingHandlerFactory,
      final boolean verifiedCredentialsCacheEnabled,
      final long verifiedCredentialsCacheSize,
      final long verifiedCredentialsCacheExpireSeconds,
      final Ticker cacheTicker)
  {
    this.legacyNexusPasswordService = checkNotNull(legacyPasswordService);

    this.defaultShiroPasswordService = new DefaultPasswordService();
    // Create and set a hash service according to our hashing policies.
    // Shiro 2.x removed the setHashAlgorithmName/setHashIterations/setGeneratePublicSalt setters
    // from DefaultHashService: the algorithm is now the "default algorithm name" and the iteration
    // count is passed via the parameters map (public-salt generation is automatic when no salt is
    // supplied, which matches the previous generatePublicSalt(true) behaviour).
    DefaultHashService hashService = new CachingHashService();
    hashService.setDefaultAlgorithmName(DEFAULT_HASH_ALGORITHM);
    hashService.setParameters(Map.of(SimpleHashProvider.Parameters.PARAMETER_ITERATIONS, DEFAULT_HASH_ITERATIONS));
    this.defaultShiroPasswordService.setHashService(hashService);
    // Shiro 2.x defaults to the shiro2 ($shiro2$) crypt format; pin to shiro1 so we keep producing
    // and reconstituting the legacy $shiro1$ hashes already stored for existing users.
    this.defaultShiroPasswordService.setHashFormat(new Shiro1CryptFormat());

    this.nexusPasswordAlgorithm = checkNotNull(nexusPasswordAlgorithm);
    this.hashingHandlerFactory = hashingHandlerFactory;
    this.configuredPasswordIterations = configuredPasswordIterations;

    if (verifiedCredentialsCacheEnabled && verifiedCredentialsCacheSize > 0
        && verifiedCredentialsCacheExpireSeconds > 0) {
      this.verifiedCredentialsCache = CacheBuilder.newBuilder()
          .maximumSize(verifiedCredentialsCacheSize)
          .expireAfterWrite(verifiedCredentialsCacheExpireSeconds, TimeUnit.SECONDS)
          .ticker(cacheTicker)
          .build();
      // Random per-process HMAC key so in-memory cache keys cannot be correlated back to a password.
      byte[] keyMaterial = new byte[32];
      new SecureRandom().nextBytes(keyMaterial);
      SecretKeySpec macKey = new SecretKeySpec(keyMaterial, CREDENTIAL_KEY_MAC_ALGORITHM);
      Arrays.fill(keyMaterial, (byte) 0);
      this.credentialKeyMac = ThreadLocal.withInitial(() -> newCredentialKeyMac(macKey));
      log.debug("Verified-credentials cache enabled (size={}, expireSeconds={})",
          verifiedCredentialsCacheSize, verifiedCredentialsCacheExpireSeconds);
    }
    else {
      this.verifiedCredentialsCache = null;
      this.credentialKeyMac = null;
    }
  }

  private static Mac newCredentialKeyMac(final SecretKeySpec macKey) {
    try {
      Mac mac = Mac.getInstance(CREDENTIAL_KEY_MAC_ALGORITHM);
      mac.init(macKey);
      return mac;
    }
    catch (GeneralSecurityException e) {
      throw new IllegalStateException(
          "Unable to initialise " + CREDENTIAL_KEY_MAC_ALGORITHM + " for the verified-credentials cache", e);
    }
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

    String cacheKey = verifiedCredentialsCache != null
        ? credentialCacheKey(submittedPlaintextPassword, storedHash)
        : null;
    if (cacheKey != null && verifiedCredentialsCache.getIfPresent(cacheKey) != null) {
      return true;
    }

    boolean matched = verifyPassword(submittedPlaintextPassword, storedHash);

    // Cache only successful verifications: failed attempts must keep paying the full hashing cost so the
    // deliberately-slow KDF continues to throttle brute-force guessing, and a wrong-password flood cannot evict
    // legitimate entries.
    if (matched && cacheKey != null) {
      verifiedCredentialsCache.put(cacheKey, Boolean.TRUE);
    }
    return matched;
  }

  private boolean verifyPassword(final Object submittedPlaintextPassword, final String storedHash) {
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

  /**
   * Derives the in-memory cache key for a (stored hash, submitted password) pair using HMAC-SHA256 keyed with a
   * per-process random secret. The keyed hash is collision-resistant, so distinct credentials never share a key, and
   * the random key stops the in-memory keys being correlated back to a password. The stored hash is mixed in so that
   * changing a password (which changes its stored hash) produces a different key, transparently invalidating any
   * previously cached success without an explicit eviction.
   */
  private String credentialCacheKey(final Object submittedPlaintextPassword, final String storedHash) {
    char[] password = convertToCharArray(submittedPlaintextPassword);
    byte[] passwordBytes = toUtf8Bytes(password);
    try {
      Mac mac = credentialKeyMac.get();
      mac.reset();
      mac.update(storedHash.getBytes(StandardCharsets.UTF_8));
      mac.update((byte) 0); // domain separator between stored hash and password
      mac.update(passwordBytes);
      return Base64.getEncoder().encodeToString(mac.doFinal());
    }
    catch (RuntimeException e) {
      // Degrade gracefully: if the MAC is unavailable, skip the cache for this request rather than failing auth.
      // verifyPassword still runs the (correct, just slower) normal verification path.
      log.debug("Unable to derive verified-credentials cache key; skipping cache for this request", e);
      return null;
    }
    finally {
      Arrays.fill(password, '\0');
      Arrays.fill(passwordBytes, (byte) 0);
    }
  }

  private static byte[] toUtf8Bytes(final char[] chars) {
    ByteBuffer encoded = StandardCharsets.UTF_8.encode(CharBuffer.wrap(chars));
    byte[] bytes = new byte[encoded.remaining()];
    encoded.get(bytes);
    if (encoded.hasArray()) {
      Arrays.fill(encoded.array(), (byte) 0);
    }
    return bytes;
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
