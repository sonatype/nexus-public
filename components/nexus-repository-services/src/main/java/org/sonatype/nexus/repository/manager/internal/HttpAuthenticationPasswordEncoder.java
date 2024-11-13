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
package org.sonatype.nexus.repository.manager.internal;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.httpclient.config.AuthenticationConfiguration;
import org.sonatype.nexus.security.UserIdHelper;

import org.apache.commons.lang3.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Optional.ofNullable;

/**
 * Used for management of Repository secrets
 *
 * @see org.sonatype.nexus.repository.manager.RepositoryManager implementation
 */
@Named
@Singleton
public class HttpAuthenticationPasswordEncoder
    extends ComponentSupport
{
  private static final String PASSWORD = "password";

  private final SecretsService secretsService;

  @Inject
  public HttpAuthenticationPasswordEncoder(final SecretsService secretsService) {
    this.secretsService = checkNotNull(secretsService);
  }

  /**
   * Encode password if present in the provided attributes
   */
  public void encodeHttpAuthPassword(final Map<String, Map<String, Object>> attributes) {
    getAuthentication(attributes).ifPresent(authentication -> {
      authentication.computeIfPresent(PASSWORD, (key, value) -> encrypt((String) value));
    });
  }

  /**
   * Encode password if present in the provided {@code newAttributes} and it is different from the value in
   * {@code attributesToPrune}
   */
  public void encodeHttpAuthPassword(
      final Map<String, Map<String, Object>> attributesToPrune,
      final Map<String, Map<String, Object>> newAttributes)
  {
    getAuthentication(newAttributes).ifPresent(newAuth -> {
      encodeAuthSecret(PASSWORD, newAuth, attributesToPrune);
    });
  }

  /**
   * Remove secret if password is not in {@code persistedAttributes} or if present
   * then remove secret only if the value in {@code attributesToPrune} and
   * {@code persistedAttributes} are different
   */
  public void removeSecret(
      final Map<String, Map<String, Object>> attributesToPrune,
      final Map<String, Map<String, Object>> persistedAttributes)
  {
    ofNullable(findAuthKey(attributesToPrune))
        .ifPresent(authKey -> removeAuthSecret(attributesToPrune, persistedAttributes, authKey));
  }

  /**
   * Remove secret
   */
  public void removeSecret(final Map<String, Map<String, Object>> attributes) {
    final String authSecretKey = findAuthKey(attributes);
    ofNullable(authSecretKey).ifPresent(authKey -> removeSecret(attributes, authKey));
  }

  private void removeSecret(final Map<String, Map<String, Object>> attributes, final String authSecretKey) {
    try {
      getAuthentication(attributes).ifPresent(authentication -> {
        final String authSecret = (String) authentication.get(authSecretKey);
        log.debug("Removing secret.");
        if (Objects.nonNull(authSecret)) {
          secretsService.remove(secretsService.from(authSecret));
        }
      });
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  private String findAuthKey(final Map<String, Map<String, Object>> attributesToPrune) {
    final Optional<Map<String, Object>> authentication = getAuthentication(attributesToPrune);
    return authentication
        .map(auth -> auth.get(PASSWORD))
        .map(value -> PASSWORD)
        .orElse(null);
  }

  private void removeAuthSecret(
      final Map<String, Map<String, Object>> attributesToPrune,
      final Map<String, Map<String, Object>> persistedAttributes,
      final String authSecretKey)
  {
    final Optional<Map<String, Object>> newAuthentication = getAuthentication(persistedAttributes);
    if (!newAuthentication.map(auth -> auth.get(authSecretKey)).isPresent()) {
      log.debug("No new secret. Removing old secret.");
      removeSecret(attributesToPrune, authSecretKey);
      return;
    }

    newAuthentication.ifPresent(newAuth ->
        getAuthentication(attributesToPrune).ifPresent(oldAuth -> {
          final String newPassword = (String) newAuth.get(authSecretKey);
          final String oldPassword = (String) oldAuth.get(authSecretKey);
          if (!StringUtils.equals(oldPassword, newPassword)) {
            log.debug("Secret changed. Removing old secret.");
            removeSecret(attributesToPrune, authSecretKey);
          }
        }));
  }

  private void encodeAuthSecret(
      final String authSecretKey,
      final Map<String, Object> authenticationDetails,
      final Map<String, Map<String, Object>> attributesToPrune)
  {
    final String newAuthSecret = (String) authenticationDetails.get(authSecretKey);
    final String oldAuthSecret =
        (String) getAuthentication(attributesToPrune).map(oldAuth -> oldAuth.get(authSecretKey)).orElse(null);
    if (StringUtils.isNotBlank(newAuthSecret) && !StringUtils.equals(oldAuthSecret, newAuthSecret)) {
      log.debug("Secret changed. Creating new secret");
      authenticationDetails.computeIfPresent(authSecretKey, (key, value) -> encrypt((String) value));
    }
  }

  private String encrypt(final String value) {
    return secretsService.encryptMaven(AuthenticationConfiguration.AUTHENTICATION_CONFIGURATION,
        value.toCharArray(),
        UserIdHelper.get()).getId();
  }

  private static Optional<Map<String, Object>> getAuthentication(final Map<String, Map<String, Object>> attributes) {
    return ofNullable(attributes)
        .map(attr -> attr.get("httpclient"))
        .map(httpclient -> httpclient.get("authentication"))
        .map(Map.class::cast)
        .map(map -> ((Map<String, Object>) map));
  }
}
