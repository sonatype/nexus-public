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

/**
 * Interface for encrypting and decrypting data.
 * @param <T> - data to be encrypted
 * @param <U> - encrypted data
 * @param <V> - encoded data
 *
 *  Also provide flow for encrypting data -> encoding data -> decoding data -> decrypting data.
 */
public interface EncryptDecrypt<T, U, V>
{
  default V encryptAndEncode(T toEncodeAdnEncrypt) {
    return encode(encrypt(toEncodeAdnEncrypt));
  }

  default T decodeAndDecrypt(V toDecodeAndDecrypt) {
    return decrypt(decode(toDecodeAndDecrypt));
  }

  V encode(U toEncode);

  U decode(V toDecode);

  U encrypt(T toEncrypt);

  T decrypt(U toDecrypt);

}
