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
package org.sonatype.nexus.crypto.secrets.internal;

import java.util.Optional;

import org.sonatype.nexus.crypto.secrets.internal.EncryptionKeyList.SecretEncryptionKey;

/**
 * Simple interface representing a source of secret keys to be used later on secrets encryption
 */
public interface EncryptionKeySource
{
  /**
   *  Retrieves the currently configured key to use for encrypting secrets
   *
   * @return an {@link Optional<SecretEncryptionKey>} filled if there is a key being used at the moment
   */
  Optional<SecretEncryptionKey> getActiveKey();

  /**
   * Retrieve an encryption key by its identifier
   *
   * @param keyId the id of the secret key to be retrieved
   * @return an {@link Optional} with a {@link SecretEncryptionKey} if the secret key was found
   */
  Optional<SecretEncryptionKey> getKey(String keyId);

  /**
   * Sets the active key to be used for encrypting secrets
   *
   * @param keyId the id of the secret to be used
   */
  void setActiveKey(String keyId);

}
