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
package org.sonatype.nexus.self.hosted.node;

import org.sonatype.nexus.common.time.Time;
import org.sonatype.nexus.common.app.FeatureFlags;
import org.sonatype.nexus.ssl.KeyStoreManagerConfiguration;
import org.sonatype.nexus.ssl.KeyStoreManagerConfigurationSupport;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Node {@link KeyStoreManagerConfiguration}.
 *
 * @since 3.0
 */
@Component
@Qualifier(NodeKeyStoreManagerImpl.NAME)
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

  @Autowired
  public KeyStoreManagerConfigurationImpl(
      @Value(CPREFIX + ".keyStoreType:JKS}") final String keyStoreType,
      @Value(CPREFIX + ".keyAlgorithm:RSA}") final String keyAlgorithm,
      @Value(CPREFIX + ".keyAlgorithmSize:2048}") final int keyAlgorithmSize,
      @Value(CPREFIX + ".certificateValidity:36500d}") final Time certificateValidity,
      @Value(CPREFIX + ".signatureAlgorithm:SHA1WITHRSA}") final String signatureAlgorithm,
      @Value(CPREFIX + ".keyManagerAlgorithm:DEFAULT}") final String keyManagerAlgorithm,
      @Value(CPREFIX + ".trustManagerAlgorithm:DEFAULT}") final String trustManagerAlgorithm,
      @Value(FeatureFlags.NEXUS_SECURITY_FIPS_ENABLED_NAMED_VALUE) final boolean nexusSecurityFipsEnabled)
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
    setFipsEnabled(nexusSecurityFipsEnabled);
  }

  @VisibleForTesting
  public KeyStoreManagerConfigurationImpl() {
    setPrivateKeyStorePassword(PKSP);
    setTrustedKeyStorePassword(TKSP);
    setPrivateKeyPassword(PKP);
  }
}
