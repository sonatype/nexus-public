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
package com.sonatype.nexus.ssl.plugin.internal.ui

import java.security.cert.Certificate

import javax.annotation.Nullable
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import com.sonatype.nexus.ssl.plugin.PemCertificate
import org.sonatype.nexus.ssl.TrustStore
import com.sonatype.nexus.ssl.plugin.internal.CertificateRetriever

import org.sonatype.nexus.extdirect.DirectComponent
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.validation.Validate

import com.codahale.metrics.annotation.ExceptionMetered
import com.codahale.metrics.annotation.Timed
import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import org.hibernate.validator.constraints.NotBlank
import org.hibernate.validator.constraints.NotEmpty

import static com.sonatype.nexus.ssl.plugin.internal.ui.TrustStoreComponent.asCertificateXO
import static org.sonatype.nexus.ssl.CertificateUtil.calculateFingerprint
import static org.sonatype.nexus.ssl.CertificateUtil.decodePEMFormattedCertificate

/**
 * SSL Certificate {@link DirectComponent}.
 *
 * @since 3.0
 */
@Named
@Singleton
@DirectAction(action = 'ssl_Certificate')
class CertificateComponent
extends DirectComponentSupport
{
  @Inject
  TrustStore trustStore

  @Inject
  CertificateRetriever certificateRetriever

  /**
   * Retrieves certificate given a host/port.
   * @param host to get certificate from
   * @param port
   * @param protocolHint
   * @return certificate
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @Validate
  CertificateXO retrieveFromHost(final @NotEmpty String host,
                                 final @Nullable Integer port,
                                 final @Nullable String protocolHint)
  {
    int actualPort = port ?: 443
    Certificate[] chain
    try {
      chain = retrieveCertificates(host, actualPort, protocolHint)
    }
    catch (Exception e) {
      String errorMessage = e.message
      if (e instanceof UnknownHostException) {
        errorMessage = "Unknown host '${host}'"
      }
      throw new IOException(errorMessage)
    }
    if (!chain || chain.length == 0) {
      throw new IOException("Could not retrieve an SSL certificate from '${host}:${actualPort}'")
    }
    return asCertificateXO(chain[0], isInTrustStore(chain[0]))
  }

  /**
   * Retrieves certificate given a certificate pem.
   * @param pem certificate in PEM format
   * @return certificate
   */
  @DirectMethod
  @Timed
  @ExceptionMetered
  @Validate
  CertificateXO details(final @NotBlank @PemCertificate String pem) {
    Certificate certificate = decodePEMFormattedCertificate(pem)
    return asCertificateXO(certificate, isInTrustStore(certificate))
  }

  Certificate[] retrieveCertificates(final String host,
                                     final int port,
                                     final String protocolHint)
  {
    if (!protocolHint) {
      try {
        return certificateRetriever.retrieveCertificates(host, port)
      }
      catch (Exception e) {
        log.debug('Cannot connect directly to {}:{}. Will retry using https protocol.', host, port, e)
        return certificateRetriever.retrieveCertificatesFromHttpsServer(host, port)
      }
    }
    else if ('https'.equalsIgnoreCase(protocolHint)) {
      return certificateRetriever.retrieveCertificatesFromHttpsServer(host, port)
    }
    return certificateRetriever.retrieveCertificates(host, port)
  }

  boolean isInTrustStore(final Certificate certificate) {
    try {
      return trustStore.getTrustedCertificate(calculateFingerprint(certificate)) != null
    }
    catch (Exception ignore) {
      return false
    }
  }
}
