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

import java.util.ConcurrentModificationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.common.Description;
import org.sonatype.nexus.common.app.FeatureFlags;
import org.sonatype.nexus.crypto.secrets.EncryptedSecret;
import org.sonatype.nexus.datastore.api.DataAccessException;
import org.sonatype.nexus.security.NexusSimpleAuthenticationInfo;
import org.sonatype.nexus.security.RealmCaseMapping;
import org.sonatype.nexus.security.authc.NexusAuthenticationException;
import org.sonatype.nexus.security.config.CUser;
import org.sonatype.nexus.security.config.SecurityConfigurationManager;
import org.sonatype.nexus.security.user.UserNotFoundException;

import org.apache.shiro.authc.AccountException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.CredentialsException;
import org.apache.shiro.authc.DisabledAccountException;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.PasswordMatcher;
import org.apache.shiro.authc.credential.PasswordService;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.crypto.internal.EncryptionHelper.KEY_ITERATION_PHC;
import static org.sonatype.nexus.security.internal.DefaultRealmConstants.DEFAULT_REALM_NAME;
import static org.sonatype.nexus.security.internal.DefaultRealmConstants.DESCRIPTION;

/**
 * Default {@link AuthenticatingRealm}.
 *
 * This realm ONLY handles authentication.
 */
@Component
@Qualifier(DEFAULT_REALM_NAME)
@Description(DESCRIPTION)
public class AuthenticatingRealmImpl
    extends AuthenticatingRealm
    implements Realm
{
  private static final Logger logger = LoggerFactory.getLogger(AuthenticatingRealmImpl.class);

  public static final String NAME = DEFAULT_REALM_NAME;

  private final SecurityConfigurationManager configuration;

  private final PasswordService passwordService;

  private final boolean orient;

  private final String nexusPasswordAlgorithm;

  private final Integer configuredIterations;

  @Autowired
  public AuthenticatingRealmImpl(
      final SecurityConfigurationManager configuration,
      final PasswordService passwordService,
      @Value("${nexus.orient.enabled:false}") final boolean orient,
      @Value(FeatureFlags.NEXUS_SECURITY_PASSWORD_ALGORITHM_NAMED_VALUE) final String nexusPasswordAlgorithm,
      @Value(FeatureFlags.NEXUS_SECURITY_PASSWORD_ITERATIONS_NAMED_VALUE) final Integer configuredIterations)
  {
    this.configuration = configuration;
    this.passwordService = passwordService;
    this.nexusPasswordAlgorithm = nexusPasswordAlgorithm;
    this.configuredIterations = configuredIterations;

    PasswordMatcher passwordMatcher = new PasswordMatcher();
    passwordMatcher.setPasswordService(this.passwordService);
    setCredentialsMatcher(passwordMatcher);
    setName(DEFAULT_REALM_NAME);
    setAuthenticationCachingEnabled(true);
    this.orient = orient;
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(final AuthenticationToken token) {
    UsernamePasswordToken upToken = (UsernamePasswordToken) token;

    CUser user;
    try {
      user = configuration.readUser(upToken.getUsername());
    }
    catch (UserNotFoundException e) {
      throw new UnknownAccountException("User '" + upToken.getUsername() + "' cannot be retrieved.", e);
    }
    catch (DataAccessException e) {
      logger.warn("Infrastructure failure when reading user '{}': database unavailable", upToken.getUsername(), e);
      throw e;
    }

    if (user.getPassword() == null) {
      throw new CredentialsException("User '" + upToken.getUsername() + "' has no password, cannot authenticate.");
    }

    if (user.isActive()) {
      // Transparently re-hash the password when:
      // 1. Password algorithm doesn't match the configured algorithm (nexus.security.password.algorithm), OR
      // 2. Password iterations don't match the configured iterations (nexus.security.password.iteration)
      if (!isUsingConfiguredAlgorithmAndIterations(user.getPassword()) && isValidCredentials(upToken, user)) {
        reHashPassword(user, new String(upToken.getPassword()));
      }

      return createAuthenticationInfo(user);
    }
    else if (CUser.STATUS_DISABLED.equals(user.getStatus())) {
      throw new DisabledAccountException("User '" + upToken.getUsername() + "' is disabled.");
    }
    else {
      throw new AccountException(
          "User '" + upToken.getUsername() + "' is in illegal status '" + user.getStatus() + "'.");
    }
  }

  /**
   * Checks if the password hash uses both the configured algorithm and iterations.
   *
   * @param password the password hash to check (in PHC string format or legacy format)
   * @return true if the password uses the configured algorithm AND iterations, false otherwise
   */
  private boolean isUsingConfiguredAlgorithmAndIterations(final String password) {
    if (password == null) {
      return false;
    }

    boolean isUsingConfiguredAlgorithm = password.startsWith("$" + this.nexusPasswordAlgorithm);
    boolean isUsingConfiguredIterations = isUsingConfiguredOrDefaultIterations(password);

    return isUsingConfiguredAlgorithm && isUsingConfiguredIterations;
  }

  /**
   * Checks if the password hash uses the configured iterations, or if no specific iterations are required.
   * Returns true in the following cases:
   * - No specific iterations configured (nexus.security.password.iteration not set)
   * - Password hash contains iterations that match the configured iterations
   * - Password hash is in legacy/unparseable format (backwards compatibility)
   *
   * @param password the password hash to check (in PHC string format or legacy format)
   * @return true if the password uses the configured iterations or no specific iterations are required
   */
  private boolean isUsingConfiguredOrDefaultIterations(final String password) {
    // If no specific iterations configured, accept any iterations (early return for performance)
    if (this.configuredIterations == null) {
      return true;
    }

    try {
      EncryptedSecret encryptedSecret = EncryptedSecret.parse(password);
      String iterationsStr = encryptedSecret.getAttributes().get(KEY_ITERATION_PHC);

      if (iterationsStr == null) {
        return false;
      }

      try {
        int storedIterations = Integer.parseInt(iterationsStr);
        return storedIterations == this.configuredIterations;
      }
      catch (NumberFormatException e) {
        logger.debug("Could not parse iterations from password hash");
        return false;
      }
    }
    catch (Exception e) {
      logger.debug("Could not parse password hash", e);
      // If error parsing, it's an old format - assume it's acceptable for backwards compatibility
      return true;
    }
  }

  /**
   * Re-hash user password, and persist changes.
   *
   * @param user to update
   * @param password clear-text password to hash
   */
  private void reHashPassword(final CUser user, final String password) {
    try {
      String hashedPassword = passwordService.encryptPassword(password);
      boolean updated = false;
      do {
        try {
          updated = updateUserPassword(user, hashedPassword);
        }
        catch (ConcurrentModificationException e) {
          logger.debug("Could not re-hash user '{}' password as user was concurrently being updated. Retrying...",
              user.getId());
        }
      }
      while (!updated);
      user.setPassword(hashedPassword);
    }
    catch (NexusAuthenticationException e) {
      logger.error("Unable to hash password for user {}", user.getId(), e);
      throw e;
    }
    catch (Exception e) {
      logger.error("Unable to update hashed password for user {}", user.getId(), e);
    }
  }

  private boolean updateUserPassword(
      final CUser user,
      final String newPassword) throws ConcurrentModificationException, UserNotFoundException
  {
    CUser toUpdate = configuration.readUser(user.getId());
    toUpdate.setPassword(newPassword);
    configuration.updateUser(toUpdate);
    return true;
  }

  /**
   * Checks to see if the credentials in token match the credentials stored on user
   *
   * @param token the username/password token containing the credentials to verify
   * @param user object containing the stored credentials
   * @return true if credentials match, false otherwise
   */
  private boolean isValidCredentials(final UsernamePasswordToken token, final CUser user) {
    boolean credentialsValid = false;

    AuthenticationInfo info = createAuthenticationInfo(user);
    CredentialsMatcher matcher = getCredentialsMatcher();
    if (matcher != null) {
      if (matcher.doCredentialsMatch(token, info)) {
        credentialsValid = true;
      }
    }

    return credentialsValid;
  }

  private AuthenticationInfo createAuthenticationInfo(final CUser user) {
    return orient
        ? new NexusSimpleAuthenticationInfo(user.getId(), user.getPassword().toCharArray(),
            new RealmCaseMapping(getName(), true))
        : new SimpleAuthenticationInfo(user.getId(), user.getPassword().toCharArray(), getName());
  }

  /**
   * Exposed to support flushing authc cache for a specific user
   */
  protected void clearCache(final String userId) {
    clearCache(new SimplePrincipalCollection(userId, DEFAULT_REALM_NAME));
  }
}
