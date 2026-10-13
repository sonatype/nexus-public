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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.sonatype.nexus.supportzip.ExportSecurityData;
import org.sonatype.nexus.supportzip.ImportData;
import org.sonatype.nexus.supportzip.datastore.JsonExporter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;

/**
 * Export/Import {@link TrustedSSLCertificate} data to/from JSON file for support zip.
 */
@Component
@Qualifier("trustedSslCertificateExport")
public class TrustedSSLCertificateExport
    extends JsonExporter
    implements ExportSecurityData, ImportData
{
  private final TrustedSSLCertificateStore trustedSSLCertificateStore;

  @Autowired
  public TrustedSSLCertificateExport(final TrustedSSLCertificateStore trustedSSLCertificateStore) {
    this.trustedSSLCertificateStore = requireNonNull(trustedSSLCertificateStore);
  }

  @Override
  public void export(final File file) throws IOException {
    log.debug("Export TrustedSSLCertificate to {}", file);
    List<TrustedSSLCertificate> certificateList = trustedSSLCertificateStore.findAll();
    exportToJson(certificateList, file);
  }

  @Override
  public void restore(final File file) throws IOException {
    log.debug("Restoring TrustedSSLCertificate from {}", file);
    List<TrustedSSLCertificateData> certificateList = importFromJson(file, TrustedSSLCertificateData.class);
    for (TrustedSSLCertificateData certificate : certificateList) {
      trustedSSLCertificateStore.save(certificate.getAlias(), certificate.getPem());
    }
  }
}
