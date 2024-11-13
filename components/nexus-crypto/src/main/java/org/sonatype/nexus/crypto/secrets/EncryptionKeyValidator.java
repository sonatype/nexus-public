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

import java.util.Optional;

/**
 * Validator of encryption keys, this is useful to check if a key is accessible before using it or get the
 * active key id
 */
public interface EncryptionKeyValidator
{
  /**
   * Checks the provided key is accessible
   *
   * @param keyId the key to check
   * @return {@code true} if the key is accessible, {@code false} otherwise
   */
  boolean isValidKey(String keyId);

  /**
   * Gets the active key id , if present
   *
   * @return an {@link Optional} with the key id String if found
   */
  Optional<String> getActiveKeyId();
}
