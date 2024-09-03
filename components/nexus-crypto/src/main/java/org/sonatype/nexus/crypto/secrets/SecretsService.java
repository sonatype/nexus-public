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
{
  public static final String SECRETS_MIGRATION_VERSION = "99.9";

  /**
   * Encrypts the token using the current key and stores it in the DB.
   * <p>
   * Callers are responsible for removing the secrets (use {@link SecretsService#remove(Secret)} for this).
   * @implNote if the system is not ready, will use {@link org.sonatype.nexus.crypto.LegacyCipherFactory.PbeCipher} to encrypt values
   */
  Secret encrypt(String purpose, char[] secret, @Nullable String userId) throws CipherException;

  /**
   * Encrypts the token using the current key and stores it in the DB.
   * <p>
   * Callers are responsible for removing the secrets (use {@link SecretsService#remove(Secret)} for this).
   * @implNote if the system is not ready, will use {@link org.sonatype.nexus.crypto.maven.MavenCipher} to encrypt values
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
}
