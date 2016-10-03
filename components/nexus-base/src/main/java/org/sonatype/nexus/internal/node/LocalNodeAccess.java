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
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.LifecycleSupport;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.ssl.CertificateUtil;
import org.sonatype.nexus.ssl.KeyStoreManager;

import com.google.common.collect.ImmutableSet;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Local {@link NodeAccess}.
 *
 * @since 3.0
 */
@Named("local")
@Singleton
public class LocalNodeAccess
    extends LifecycleSupport
    implements NodeAccess
{
  private final KeyStoreManager keyStoreManager;

  private Certificate certificate;

  private String fingerprint;

  private String id;

  @Inject
  public LocalNodeAccess(@Named(KeyStoreManagerImpl.NAME) final KeyStoreManager keyStoreManager) {
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

    fingerprint = CertificateUtil.calculateFingerprint(certificate);
    log.debug("Fingerprint: {}", fingerprint);

    id = NodeIdEncoding.nodeIdForCertificate(certificate);
    log.info("ID: {}", id);
  }

  @Override
  protected void doStop() throws Exception {
    certificate = null;
    fingerprint = null;
    id = null;
  }

  @Override
  public Certificate getCertificate() {
    ensureStarted();
    return certificate;
  }

  @Override
  public String getFingerprint() {
    ensureStarted();
    return fingerprint;
  }

  @Override
  public String getId() {
    ensureStarted();
    return id;
  }

  @Override
  public boolean isClustered() {
    return false;
  }

  @Override
  public Set<String> getMemberIds() {
    return ImmutableSet.of(getId());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "id='" + id + '\'' +
        '}';
  }
}
