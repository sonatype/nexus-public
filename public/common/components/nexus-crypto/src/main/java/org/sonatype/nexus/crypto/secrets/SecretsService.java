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
package org.sonatype.nexus.crypto.secrets;

import javax.annotation.Nullable;

import org.sonatype.nexus.crypto.internal.error.CipherException;

/**
 * Service responsible for storing secrets (e.g. passwords) with reversible encryption.
 */
public interface SecretsService
    extends SecretsFactory
{
  String SECRETS_MIGRATION_VERSION = "2.2";

  /**
   * Encrypts the token using the current key and stores it in the DB.
   * <p>
   * Callers are responsible for removing the secrets (use {@link SecretsService#remove(Secret)} for this).
   * 
   * @implNote if the system is not ready, will use {@link org.sonatype.nexus.crypto.LegacyCipherFactory.PbeCipher} to
   *           encrypt values
   */
  Secret encrypt(String purpose, char[] secret, @Nullable String userId) throws CipherException;

  /**
   * Encrypts the token using the current key and stores it in the DB.
   * <p>
   * Callers are responsible for removing the secrets (use {@link SecretsService#remove(Secret)} for this).
   * 
   * @implNote if the system is not ready, will use {@link org.sonatype.nexus.crypto.maven.MavenCipher} to encrypt
   *           values
   */
  Secret encryptMaven(String purpose, char[] secret, @Nullable String userid) throws CipherException;

  /**
   * Removes a previously stored secret, if a legacy secret is sent does nothing.
   *
   * @param secret the secret to be removed
   */
  void remove(Secret secret);

  /**
   * Changes the current encryption key to the specified key ID
   */
  void reEncrypt(SecretData secretData, String keyId) throws CipherException;

  /**
   * Checks if there are any secrets that have not been re-encrypted with the default key.
   */
  boolean isReEncryptRequired();

  /**
   * Exports the encrypted secret value for the given secret ID token.
   * This is used during configuration export to get the encrypted value that can be imported into another system.
   *
   * @param secretId the secret ID token (e.g., "_123" or legacy encrypted string)
   * @return the encrypted secret string, or null if the secret ID is null or the secret cannot be found
   */
  String exportEncrypted(String secretId);

  /**
   * Imports an encrypted secret value into the secrets store.
   * This is used during configuration import to create a new secret entry with an existing encrypted value.
   *
   * @param purpose the purpose of the secret (e.g., "email", "ldap")
   * @param encryptedValue the encrypted secret string (PHC format or legacy encrypted string)
   * @param userId the user performing the import, may be null
   * @return a Secret instance with the new ID
   * @throws CipherException if the import fails
   */
  Secret importEncrypted(String purpose, String encryptedValue, String userId) throws CipherException;

  /**
   * Imports an encrypted secret value into the secrets store, using an optional migration cipher password
   * for decryption.
   * This is used during configuration import to create a new secret entry with an existing encrypted value
   * that was encrypted with a different key.
   *
   * @param purpose the purpose of the secret (e.g., "email", "ldap")
   * @param encryptedValue the encrypted secret string (PHC format or legacy encrypted string)
   * @param userId the user performing the import, may be null
   * @param decryptionCipherPassword optional password to use for decrypting the imported secret, may be null
   * @return a Secret instance with the new ID
   * @throws CipherException if the import fails
   */
  Secret importEncrypted(
      String purpose,
      String encryptedValue,
      String userId,
      @Nullable String decryptionCipherPassword) throws CipherException;
}
