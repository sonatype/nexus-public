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

import java.security.cert.Certificate;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.common.node.LocalNodeAccess;
import org.sonatype.nexus.ssl.CertificateUtil;
import org.sonatype.nexus.ssl.KeyStoreManager;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default {@link LocalNodeAccess}.
 *
 * @since 3.0
 */
@Named
@Singleton
public class LocalNodeAccessImpl
    extends LifecycleSupport
    implements LocalNodeAccess
{
  private final KeyStoreManager keyStoreManager;

  private Certificate certificate;

  private String id;

  private String fingerprint;

  @Inject
  public LocalNodeAccessImpl(@Named(KeyStoreManagerImpl.NAME) final KeyStoreManager keyStoreManager) {
    this.keyStoreManager = checkNotNull(keyStoreManager);
  }

  @Override
  protected void doStart() throws Exception {
    // Generate identity key-pair if not already created
    if (!keyStoreManager.isKeyPairInitialized()) {
      log.info("Generating certificate");

      // For now give something unique to the cert for additional identification purposes
      UUID cn = UUID.randomUUID();
      keyStoreManager.generateAndStoreKeyPair(
          cn.toString(),
          "Nexus",
          "Sonatype",
          "Silver Spring",
          "MD",
          "US");
    }

    certificate = keyStoreManager.getCertificate();
    log.trace("Certificate:\n{}", certificate);

    id = NodeIdEncoding.nodeIdForCertificate(certificate);
    log.info("ID: {}", id);

    fingerprint = CertificateUtil.calculateFingerprint(certificate);
    log.debug("Fingerprint: {}", fingerprint);
  }

  @Override
  protected void doStop() throws Exception {
    certificate = null;
    id = null;
    fingerprint = null;
  }

  @Override
  public Certificate getCertificate() {
    ensureStarted();
    return certificate;
  }

  @Override
  public String getId() {
    ensureStarted();
    return id;
  }

  @Override
  public String getFingerprint() {
    ensureStarted();
    return fingerprint;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "id='" + id + '\'' +
        '}';
  }
}
