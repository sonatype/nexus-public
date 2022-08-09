/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import {indexBy, prop} from 'ramda';

export const SSL_CERTIFICATES = [{
  expiresOn: 1682126242000,
  fingerprint: 'AF:F8:E4:61:E4:B3:4D:D2:26:9E:C0:F1:F7:0E:1B:14:D7:A8:D1:BA',
  id: 'AF:F8:E4:61:E4:B3:4D:D2:26:9E:C0:F1:F7:0E:1B:14:D7:A8:D1:BA',
  issuedOn: 1651022242000,
  issuerCommonName: 'Microsoft Azure TLS Issuing CA 05',
  issuerOrganization: 'Microsoft Corporation',
  issuerOrganizationalUnit: null,
  pem: '-----BEGIN CERTIFICATE-----\nMIIIVTCCBj2gAwIBAgITMwA5aK1RzdXyPE86j',
  serialNumber: '1137357540359614375032878159276968911912396973',
  subjectCommonName: '*.nuget.org',
  subjectOrganization: 'Microsoft Corporation',
  subjectOrganizationalUnit: null,
}, {
  expiresOn: 1687046399000,
  fingerprint: 'F6:DB:65:A4:0D:38:75:86:90:96:29:5F:36:EA:44:7F:3D:98:4B:3A',
  id: 'F6:DB:65:A4:0D:38:75:86:90:96:29:5F:36:EA:44:7F:3D:98:4B:3A',
  issuedOn: 1652918400000,
  issuerCommonName: 'Amazon',
  issuerOrganization: 'Amazon',
  issuerOrganizationalUnit: 'Server CA 1B',
  pem: '-----BEGIN CERTIFICATE-----\nMIIF4TCCBMmgAwIBAgIQAllC7',
  serialNumber: '3121928060269049011789892858942813771',
  subjectCommonName: 'test.iq.sonatype.dev',
  subjectOrganization: null,
  subjectOrganizationalUnit: null,
}, {
  expiresOn: 1689259508000,
  fingerprint: '73:A4:83:C2:E4:22:30:55:EC:16:91:2D:0E:39:90:83:24:BA:A9:6C',
  id: '73:A4:83:C2:E4:22:30:55:EC:16:91:2D:0E:39:90:83:24:BA:A9:6C',
  issuedOn: 1654958709000,
  issuerCommonName: 'GlobalSign Atlas R3 DV TLS CA 2022 Q2',
  issuerOrganization: 'GlobalSign nv-sa',
  issuerOrganizationalUnit: null,
  pem: '-----BEGIN CERTIFICATE-----\nMIIGXzCCBUegAwIBAgIQAWT5fvBMN1ZW3hl6',
  serialNumber: '1853518058746726741248501373032988485',
  subjectCommonName: 'repo1.maven.org',
  subjectOrganization: null,
  subjectOrganizationalUnit: null,
}];

export const SSL_CERTIFICATES_MAP = indexBy(prop('id'), SSL_CERTIFICATES);

export const XSS_STRING = 'XSS!<img src="/static/rapture/resources/icons/x16/user.png" onload="alert(0)">';
