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
package org.sonatype.nexus.ssl;

import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.naming.InvalidNameException;

/**
 * @since 3.19
 */
public class ApiCertificate
{
  private long expiresOn;

  private String fingerprint;

  private String id;

  private long issuedOn;

  private String issuerCommonName;

  private String issuerOrganization;

  private String issuerOrganizationalUnit;

  private String pem;

  private String serialNumber;

  private String subjectCommonName;

  private String subjectOrganization;

  private String subjectOrganizationalUnit;

  public long getExpiresOn() {
    return expiresOn;
  }

  public String getFingerprint() {
    return fingerprint;
  }

  public String getId() {
    return id;
  }

  public long getIssuedOn() {
    return issuedOn;
  }

  public String getIssuerCommonName() {
    return issuerCommonName;
  }

  public String getIssuerOrganization() {
    return issuerOrganization;
  }

  public String getIssuerOrganizationalUnit() {
    return issuerOrganizationalUnit;
  }

  public String getPem() {
    return pem;
  }

  public String getSerialNumber() {
    return serialNumber;
  }

  public String getSubjectCommonName() {
    return subjectCommonName;
  }

  public String getSubjectOrganization() {
    return subjectOrganization;
  }

  public String getSubjectOrganizationalUnit() {
    return subjectOrganizationalUnit;
  }

  public void setExpiresOn(final long expiresOn) {
    this.expiresOn = expiresOn;
  }

  public void setFingerprint(final String fingerprint) {
    this.fingerprint = fingerprint;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public void setIssuedOn(final long issuedOn) {
    this.issuedOn = issuedOn;
  }

  public void setIssuerCommonName(final String issuerCommonName) {
    this.issuerCommonName = issuerCommonName;
  }

  public void setIssuerOrganization(final String issuerOrganization) {
    this.issuerOrganization = issuerOrganization;
  }

  public void setIssuerOrganizationalUnit(final String issuerOrganizationalUnit) {
    this.issuerOrganizationalUnit = issuerOrganizationalUnit;
  }

  public void setPem(final String pem) {
    this.pem = pem;
  }

  public void setSerialNumber(final String serialNumber) {
    this.serialNumber = serialNumber;
  }

  public void setSubjectCommonName(final String subjectCommonName) {
    this.subjectCommonName = subjectCommonName;
  }

  public void setSubjectOrganization(final String subjectOrganization) {
    this.subjectOrganization = subjectOrganization;
  }

  public void setSubjectOrganizationalUnit(final String subjectOrganizationalUnit) {
    this.subjectOrganizationalUnit = subjectOrganizationalUnit;
  }

  public static ApiCertificate convert( // NOSONAR
      final Certificate cert) throws InvalidNameException, CertificateEncodingException, IOException
  {
    ApiCertificate apiCertificate = new ApiCertificate();

    String fingerprint = CertificateUtil.calculateFingerprint(cert);

    if (cert instanceof X509Certificate) {
      X509Certificate x509Certificate = (X509Certificate) cert;

      Map<String, String> subjectRdns = CertificateUtil.getSubjectRdns(x509Certificate);
      Map<String, String> issuerRdns = CertificateUtil.getIssuerRdns(x509Certificate);

      apiCertificate.setId(fingerprint);
      apiCertificate.setPem(CertificateUtil.serializeCertificateInPEM(cert));
      apiCertificate.setFingerprint(fingerprint);
      apiCertificate.setSerialNumber(String.valueOf(x509Certificate.getSerialNumber()));
      apiCertificate.setSubjectCommonName(subjectRdns.get("CN"));
      apiCertificate.setSubjectOrganization(subjectRdns.get("O"));
      apiCertificate.setSubjectOrganizationalUnit(subjectRdns.get("OU"));
      apiCertificate.setIssuerCommonName(issuerRdns.get("CN"));
      apiCertificate.setIssuerOrganization(issuerRdns.get("O"));
      apiCertificate.setIssuerOrganizationalUnit(issuerRdns.get("OU"));
      apiCertificate.setIssuedOn(x509Certificate.getNotBefore().getTime());
      apiCertificate.setExpiresOn(x509Certificate.getNotAfter().getTime());
      return apiCertificate;
    }
    else {
      apiCertificate.setId(fingerprint);
      apiCertificate.setPem(cert.toString());
      apiCertificate.setFingerprint(fingerprint);
    }
    return apiCertificate;
  }
}
