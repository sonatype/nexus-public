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
package org.sonatype.nexus.internal.node;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.Time;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.ssl.KeyStoreManagerConfiguration;
import org.sonatype.nexus.ssl.KeyStoreManagerConfigurationSupport;

import com.google.common.annotations.VisibleForTesting;

/**
 * Node {@link KeyStoreManagerConfiguration}.
 *
 * @since 3.0
 */
@Named(KeyStoreManagerImpl.NAME)
@Singleton
public class KeyStoreManagerConfigurationImpl
    extends KeyStoreManagerConfigurationSupport
{
  private static final String CPREFIX = "${node.keyStoreManager";

  /**
   * Private key-store password.
   */
  private static final char[] PKSP = "uuPWrk3UEQRaolpd".toCharArray();

  /**
   * Trusted key-store password.
   */
  private static final char[] TKSP = "1bmcqcHV3sp6fVKD".toCharArray();

  /**
   * Private-key password.
   */
  private static final char[] PKP = "CyQM8zCFeorarTA8".toCharArray();

  @Inject
  public KeyStoreManagerConfigurationImpl(
      final ApplicationDirectories directories,
      final @Named(CPREFIX + ".keyStoreType:-JKS}") String keyStoreType,
      final @Named(CPREFIX + ".keyAlgorithm:-RSA}") String keyAlgorithm,
      final @Named(CPREFIX + ".keyAlgorithmSize:-2048}") int keyAlgorithmSize,
      final @Named(CPREFIX + ".certificateValidity:-36500d}") Time certificateValidity,
      final @Named(CPREFIX + ".signatureAlgorithm:-SHA1WITHRSA}") String signatureAlgorithm,
      final @Named(CPREFIX + ".keyManagerAlgorithm:-DEFAULT}") String keyManagerAlgorithm,
      final @Named(CPREFIX + ".trustManagerAlgorithm:-DEFAULT}") String trustManagerAlgorithm)
  {
    setBaseDir(new File(directories.getWorkDirectory("keystores"), KeyStoreManagerImpl.NAME));

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

  @VisibleForTesting
  public KeyStoreManagerConfigurationImpl(final File baseDir) {
    setBaseDir(baseDir);
    setPrivateKeyStorePassword(PKSP);
    setTrustedKeyStorePassword(TKSP);
    setPrivateKeyPassword(PKP);
  }
}
