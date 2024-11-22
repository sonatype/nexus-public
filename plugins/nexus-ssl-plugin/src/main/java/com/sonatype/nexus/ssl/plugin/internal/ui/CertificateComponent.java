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
package com.sonatype.nexus.ssl.plugin.internal.ui;

import java.io.IOException;
import java.net.UnknownHostException;
import java.security.cert.Certificate;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

import com.sonatype.nexus.ssl.plugin.validator.HostnameOrIpAddress;
import com.sonatype.nexus.ssl.plugin.validator.PemCertificate;

import org.sonatype.nexus.extdirect.DirectComponent;
import org.sonatype.nexus.extdirect.DirectComponentSupport;
import org.sonatype.nexus.ssl.CertificateRetriever;
import org.sonatype.nexus.ssl.TrustStore;
import org.sonatype.nexus.validation.Validate;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.softwarementors.extjs.djn.config.annotations.DirectAction;
import com.softwarementors.extjs.djn.config.annotations.DirectMethod;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sonatype.nexus.ssl.plugin.internal.ui.TrustStoreComponent.asCertificateXO;
import static org.sonatype.nexus.ssl.CertificateUtil.calculateFingerprint;
import static org.sonatype.nexus.ssl.CertificateUtil.decodePEMFormattedCertificate;

/**
 * SSL Certificate {@link DirectComponent}.
 */
@Named
@Singleton
@DirectAction(action = "ssl_Certificate")
public class CertificateComponent
    extends DirectComponentSupport
{
  private final TrustStore trustStore;

  private final CertificateRetriever certificateRetriever;

  @Inject
  public CertificateComponent(final TrustStore trustStore, final CertificateRetriever certificateRetriever) {
    this.trustStore = checkNotNull(trustStore);
    this.certificateRetriever = checkNotNull(certificateRetriever);
  }

  /**
   * Retrieves certificate given a host/port.
   *
   * @param host to get certificate from
   * @param port the port
   * @param protocolHint the protocol hint
   * @return certificate
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @Validate
  @RequiresAuthentication
  @RequiresPermissions("nexus:ssl-truststore:read")
  public CertificateXO retrieveFromHost(
      final @HostnameOrIpAddress @NotEmpty String host,
      final @Nullable Integer port,
      final @Nullable String protocolHint) throws Exception
  {
    Certificate[] chain;
    try {
      chain = certificateRetriever.retrieveCertificates(host, port, protocolHint);
    }
    catch (Exception e) {
      String errorMessage = e.getMessage();
      if (e instanceof UnknownHostException) {
        errorMessage = "Unknown host " + host;
      }
      throw new IOException(errorMessage);
    }
    if (chain == null || chain.length == 0) {
      int actualPort = port == null ? 443 : port;
      throw new IOException(String.format("Could not retrieve an SSL certificate from '%s:%s'", host, actualPort));
    }
    return asCertificateXO(chain[0], isInTrustStore(chain[0]));
  }

  /**
   * Retrieves certificate given a certificate pem.
   *
   * @param pem certificate in PEM format
   * @return certificate
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @Validate
  @RequiresAuthentication
  @RequiresPermissions("nexus:ssl-truststore:read")
  public CertificateXO details(final @NotBlank @PemCertificate String pem) throws Exception {
    Certificate certificate = decodePEMFormattedCertificate(pem);
    return asCertificateXO(certificate, isInTrustStore(certificate));
  }

  boolean isInTrustStore(final Certificate certificate) {
    try {
      return trustStore.getTrustedCertificate(calculateFingerprint(certificate)) != null;
    }
    catch (Exception ignore) {
      return false;
    }
  }
}
