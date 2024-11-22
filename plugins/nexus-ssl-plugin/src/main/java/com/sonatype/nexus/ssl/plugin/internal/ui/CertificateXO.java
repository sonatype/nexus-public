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

import groovy.transform.ToString;

/**
 * Certificate exchange object.
 *
 * @since 3.0
 */
@ToString(includePackage = false, includeNames = true)
public class CertificateXO
{
  private final String id;

  private final String fingerprint;

  private final String pem;

  private String serialNumber;

  private String subjectCommonName;

  private String subjectOrganization;

  private String subjectOrganizationalUnit;

  private String issuerCommonName;

  private String issuerOrganization;

  private String issuerOrganizationalUnit;

  private long issuedOn;

  private long expiresOn;

  private boolean inTrustStore;

  public CertificateXO(
      final String id,
      final String fingerprint,
      final String pem,
      final String serialNumber,
      final String subjectCommonName,
      final String subjectOrganization,
      final String subjectOrganizationalUnit,
      final String issuerCommonName,
      final String issuerOrganization,
      final String issuerOrganizationalUnit,
      final long issuedOn,
      final long expiresOn,
      final boolean inTrustStore)
  {
    this.id = id;
    this.fingerprint = fingerprint;
    this.pem = pem;
    this.serialNumber = serialNumber;
    this.subjectCommonName = subjectCommonName;
    this.subjectOrganization = subjectOrganization;
    this.subjectOrganizationalUnit = subjectOrganizationalUnit;
    this.issuerCommonName = issuerCommonName;
    this.issuerOrganization = issuerOrganization;
    this.issuerOrganizationalUnit = issuerOrganizationalUnit;
    this.issuedOn = issuedOn;
    this.expiresOn = expiresOn;
    this.inTrustStore = inTrustStore;
  }

  public CertificateXO(final String id, final String fingerprint, final String pem) {
    this.id = id;
    this.fingerprint = fingerprint;
    this.pem = pem;
  }
}
