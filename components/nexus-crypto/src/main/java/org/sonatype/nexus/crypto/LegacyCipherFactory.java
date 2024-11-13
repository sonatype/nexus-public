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
package org.sonatype.nexus.crypto;

import org.sonatype.nexus.crypto.internal.error.CipherException;

/**
 * Factory for {@link PbeCipher}s, for password based encryption (PBE).
 *
 * To be used on smaller payloads like user passwords or smaller messages, due to use of byte arrays for payload.
 *
 * @since 3.0
 */
public interface LegacyCipherFactory
{
  interface PbeCipher
  {
    byte[] encrypt(final byte[] bytes);

    byte[] decrypt(final byte[] bytes);
  }

  /**
   * Creates a {@link PbeCipher} with given parameters. None of the parameters may be {@code null}.
   */
  PbeCipher create(String password, String salt, String iv) throws CipherException;
}
