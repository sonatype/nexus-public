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
package org.sonatype.nexus.internal.ssl;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditorSupport;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.ssl.CertificateCreatedEvent;
import org.sonatype.nexus.ssl.CertificateDeletedEvent;
import org.sonatype.nexus.ssl.CertificateEvent;

import com.google.common.base.Throwables;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

/**
 * SSL certificate auditor.
 *
 * @since 3.1
 */
@Named
@Singleton
public class SslCertificateAuditor
    extends AuditorSupport
    implements EventAware
{
  public static final String DOMAIN = "security.sslcertificate";

  public SslCertificateAuditor() {
    registerType(CertificateCreatedEvent.class, CREATED_TYPE);
    registerType(CertificateDeletedEvent.class, DELETED_TYPE);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final CertificateEvent event) {
    if (isRecording()) {
      Certificate certificate = event.getCertificate();

      AuditData data = new AuditData();
      data.setDomain(DOMAIN);
      data.setType(type(event.getClass()));

      Map<String, Object> attributes = data.getAttributes();
      attributes.put("alias", event.getAlias());
      attributes.put("type", certificate.getType());

      if (certificate instanceof X509Certificate) {
        X509Certificate x509 = (X509Certificate) certificate;
        Map<String, String> rdns = parseLdapName(x509.getSubjectX500Principal().getName());
        data.setContext(rdns.get("CN"));
        attributes.putAll(rdns);
      }
      else {
        data.setContext(event.getAlias());
      }

      record(data);
    }
  }

  private static Map<String, String> parseLdapName(final String dn) {
    try {
      Map<String, String> result = new HashMap<>();
      LdapName ldapName = new LdapName(dn);
      for (Rdn rdn : ldapName.getRdns()) {
        result.put(rdn.getType(), rdn.getValue().toString());
      }
      return result;
    }
    catch (Exception e) {
      Throwables.throwIfUnchecked(e);
      throw new RuntimeException(e);
    }
  }
}
