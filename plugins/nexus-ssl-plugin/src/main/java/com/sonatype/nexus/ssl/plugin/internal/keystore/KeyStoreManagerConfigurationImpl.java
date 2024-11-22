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
package com.sonatype.nexus.ssl.plugin.internal.keystore;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.Time;
import org.sonatype.nexus.ssl.KeyStoreManagerConfigurationSupport;

/**
 * SSL plugin specific key-store manager configuration.
 *
 * @since ssl 1.0
 */
@Named(KeyStoreManagerImpl.NAME)
@Singleton
public class KeyStoreManagerConfigurationImpl
    extends KeyStoreManagerConfigurationSupport
{
  private static final String CPREFIX = "${ssl.keyStoreManager";

  // Using terse names for password constants for a little more security by obscurity

  private static final char[] PKSP /* PRIVATE_KEY_STORE_PASSWORD */ = "QePgCbrDbQiNdT6X".toCharArray();
  // unobfuscate(new long[]{0xC83937B59BD4E1B3L, 0xE67FF6D75DDC56E6L, 0xAD210BF881D932F2L}).toCharArray(); /* =>
  // "QePgCbrDbQiNdT6X" */

  private static final char[] TKSP /* TRUSTED_KEY_STORE_PASSWORD */ = "xfWHLzWxDF14OUW6".toCharArray();
  // unobfuscate(new long[]{0xC23B2AC13724F4EAL, 0xF9510B04CA4340EEL, 0x98D2647891CB6F66L}).toCharArray(); /* =>
  // "xfWHLzWxDF14OUW6" */

  private static final char[] PKP /* PRIVATE_KEY_PASSWORD */ = "Xw5JCuS5aDZ14oZG".toCharArray();
  // unobfuscate(new long[]{0xD39F439CFB22319FL, 0xE48AAB6E5D073A6BL, 0xA14EE96195DA105AL}).toCharArray(); /* =>
  // "Xw5JCuS5aDZ14oZG" */

  @Inject
  public KeyStoreManagerConfigurationImpl(
      @Named(CPREFIX + ".keyStoreType:-JKS}") final String keyStoreType,
      @Named(CPREFIX + ".keyAlgorithm:-RSA}") final String keyAlgorithm,
      @Named(CPREFIX + ".keyAlgorithmSize:-2048}") final int keyAlgorithmSize,
      @Named(CPREFIX + ".certificateValidity:-36500d}") final Time certificateValidity,
      @Named(CPREFIX + ".signatureAlgorithm:-SHA1WITHRSA}") final String signatureAlgorithm,
      @Named(CPREFIX + ".keyManagerAlgorithm:-DEFAULT}") final String keyManagerAlgorithm,
      @Named(CPREFIX + ".trustManagerAlgorithm:-DEFAULT}") final String trustManagerAlgorithm)
  {
    setPrivateKeyStorePassword(PKSP);
    setTrustedKeyStorePassword(TKSP);
    setPrivateKeyPassword(PKP);
    setKeyStoreType(keyStoreType);
    setKeyAlgorithm(keyAlgorithm);
    setKeyAlgorithmSize(keyAlgorithmSize);
    setCertificateValidity(certificateValidity);
    setSignatureAlgorithm(signatureAlgorithm);
    setKeyManagerAlgorithm(keyManagerAlgorithm);
    setTrustManagerAlgorithm(trustManagerAlgorithm);
  }
}
