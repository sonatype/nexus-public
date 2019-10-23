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
package org.sonatype.nexus.testsuite.testsupport.fixtures;

import java.security.cert.CertificateException;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Provider;

import org.sonatype.nexus.ssl.CertificateUtil;
import org.sonatype.nexus.ssl.KeystoreException;
import org.sonatype.nexus.ssl.TrustStore;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 3.19
 */
public class TrustStoreRule
    extends ExternalResource
{
  private static final Logger log = LoggerFactory.getLogger(TrustStoreRule.class);

  private final Provider<TrustStore> trustStoreProvider;

  private Set<String> managedAliases = new HashSet<>();

  public TrustStoreRule(final Provider<TrustStore> trustStoreProvider) {
    this.trustStoreProvider = trustStoreProvider;
  }

  @Override
  protected void after() {
    managedAliases.forEach(fingerprint -> {
      try {
        trustStoreProvider.get().removeTrustCertificate(fingerprint);
      }
      catch (Exception e) { // NOSONAR
        log.info("Unable to clean up alias {}", fingerprint, e);
      }
    });
  }

  public void addCertificate(final String pem) {
    try {
      String fingerprint = CertificateUtil.calculateFingerprint(CertificateUtil.decodePEMFormattedCertificate(pem));
      trustStoreProvider.get().importTrustCertificate(pem, fingerprint);
      managedAliases.add(fingerprint);
    }
    catch (CertificateException | KeystoreException e) {
      throw new RuntimeException("Failed to add certificate", e);
    }
  }

  /**
   * Add a certificate alias to automatically cleanup upon test failure
   */
  public void manageAlias(final String fingerprint) {
    managedAliases.add(fingerprint);
  }

  /**
   * Add a certificate alias to automatically cleanup upon test failure
   */
  public void unmanageAlias(final String fingerprint) {
    managedAliases.remove(fingerprint);
  }
}
