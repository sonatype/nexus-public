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
import React from 'react';

import {
  NxReadOnly,
  NxH2,
  NxGrid,
  NxDivider,
  NxWarningAlert,
} from '@sonatype/react-shared-components';
import {
  DateUtils,
  ReadOnlyField,
} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

const {
  SSL_CERTIFICATES: {FORM: LABELS},
} = UIStrings;

export default function SslCertificatesDetails({data}) {
  const {
    issuedOn,
    expiresOn,
    subjectCommonName,
    subjectOrganization,
    subjectOrganizationalUnit,
    issuerCommonName,
    issuerOrganization,
    issuerOrganizationalUnit,
    fingerprint,
  } = data;

  const issuedDate = issuedOn ? DateUtils.timestampToString(issuedOn) : '';
  const expiredDate = expiresOn ? DateUtils.timestampToString(expiresOn) : '';

  return <>
    <NxReadOnly>
      <ReadOnlyField label={LABELS.FINGERPRINT.LABEL} value={fingerprint}/>
      <ReadOnlyField label={LABELS.VALID_UNTIL.LABEL} value={expiredDate}/>
      <ReadOnlyField label={LABELS.ISSUED_ON.LABEL} value={issuedDate}/>
    </NxReadOnly>
    <NxDivider />
    <NxGrid.Row>
      <NxGrid.Column>
        <NxH2>{LABELS.SECTIONS.SUBJECT}</NxH2>
        <NxReadOnly>
          <ReadOnlyField label={LABELS.COMMON_NAME.LABEL} value={subjectCommonName}/>
          <ReadOnlyField label={LABELS.ORGANIZATION.LABEL} value={subjectOrganization}/>
          <ReadOnlyField label={LABELS.UNIT.LABEL} value={subjectOrganizationalUnit}/>
        </NxReadOnly>
      </NxGrid.Column>
      <NxGrid.Column>
        <NxH2>{LABELS.SECTIONS.ISSUER}</NxH2>
        <NxReadOnly>
          <ReadOnlyField label={LABELS.COMMON_NAME.LABEL} value={issuerCommonName}/>
          <ReadOnlyField label={LABELS.ORGANIZATION.LABEL} value={issuerOrganization}/>
          <ReadOnlyField label={LABELS.UNIT.LABEL} value={issuerOrganizationalUnit}/>
        </NxReadOnly>
      </NxGrid.Column>
    </NxGrid.Row>
    <NxWarningAlert className="ssl-certificate-alert">{LABELS.WARNING}</NxWarningAlert>
  </>;
}
