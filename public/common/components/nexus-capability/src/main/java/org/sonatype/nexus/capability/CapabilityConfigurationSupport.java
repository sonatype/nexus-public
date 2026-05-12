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
package org.sonatype.nexus.capability;

import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.crypto.secrets.SecretsStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support for {@link Capability} configuration implementations.
 *
 * @since 2.7
 */
public abstract class CapabilityConfigurationSupport
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  protected boolean isEmpty(final String value) {
    return Strings2.isEmpty(value);
  }

  /**
   * Re-throws {@link URISyntaxException} as runtime exception.
   */
  protected URI parseUri(final String value) {
    try {
      return new URI(value);
    }
    catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * If given value is null or empty, returns default.
   *
   * @see #parseUri(String)
   */
  protected URI parseUri(final String value, @Nullable final URI defaultValue) {
    if (isEmpty(value)) {
      return defaultValue;
    }
    return parseUri(value);
  }

  protected Boolean parseBoolean(@Nullable final String value, @Nullable final Boolean defaultValue) {
    if (!isEmpty(value)) {
      return Boolean.parseBoolean(value);
    }
    return defaultValue;
  }

  /**
   * Parses the given string value as an integer or returns the specified default value if the string is {@code null}
   * or empty.
   *
   * @since 3.1
   */
  protected Integer parseInteger(@Nullable final String value, @Nullable final Integer defaultValue) {
    if (isEmpty(value)) {
      return defaultValue;
    }
    return Integer.parseInt(value);
  }

  /**
   * Decrypts a secret ID on-demand using the provided SecretsStore and SecretsService.
   * <p>
   * This method handles the common pattern for capability password/secret fields:
   * <ul>
   * <li>Checks for null/empty secret IDs</li>
   * <li>Handles missing secrets service/store (e.g., during validation)</li>
   * <li>Strips underscore prefix from secret IDs (secrets service returns IDs with underscore prefix)</li>
   * <li>Parses the secret ID as an integer and retrieves the encrypted secret</li>
   * <li>Decrypts the secret and returns the plaintext value</li>
   * <li>Handles invalid secret ID formats for backwards compatibility</li>
   * </ul>
   *
   * @param secretId the secret ID (may be prefixed with underscore, e.g., "_123")
   * @param secretsStore the secrets store to retrieve the encrypted secret from
   * @param secretsService the secrets service to decrypt the secret
   * @return the decrypted secret value, or null if the secret is not found or empty
   * @since 3.87
   */
  @Nullable
  protected String decryptSecret(
      @Nullable final String secretId,
      @Nullable final SecretsStore secretsStore,
      @Nullable final SecretsService secretsService)
  {
    // Return null if secret ID is null or empty
    if (secretId == null || secretId.isEmpty()) {
      return secretId;
    }

    // If SecretsService not available (e.g., in tests or validation), return the value as-is
    if (secretsService == null) {
      log.debug("SecretsService not available, returning secret ID as-is");
      return secretId;
    }

    // Decrypt using secret ID - secretsService.from() expects the secret ID string (e.g., "_1")
    try {
      char[] decryptedChars = secretsService.from(secretId).decrypt();
      return String.valueOf(decryptedChars);
    }
    catch (NumberFormatException e) {
      // If not a valid secret ID, return the value as-is (for backwards compatibility)
      log.debug("Secret ID is not a valid integer format, returning as-is: {}", secretId);
      return secretId;
    }
    catch (Exception e) {
      log.error("Error decrypting secret ID: {}", secretId, e);
      return null;
    }
  }
}
