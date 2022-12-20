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
  NxTile,
  NxLoadWrapper,
  NxReadOnly,
  NxFooter,
  NxButtonBar,
  NxButton,
  NxH2,
  NxWarningAlert,
  NxGrid,
  NxTooltip,
  NxDivider,
} from '@sonatype/react-shared-components';
import {
  ContentBody,
  Page,
  PageHeader,
  PageTitle,
  DateUtils,
  ReadOnlyField,
} from '@sonatype/nexus-ui-plugin';

import {faIdCardAlt} from '@fortawesome/free-solid-svg-icons';
import {canDeleteCertificate} from './SslCertificatesHelper';
import UIStrings from '../../../../constants/UIStrings';
import './SslCertificates.scss';

const {
  SSL_CERTIFICATES: {FORM: LABELS},
} = UIStrings;

export default function SslCertificatesDetails({machine, onDone}) {
  const [state, send] = machine;
  const {data, loadError, shouldLoadNew} = state.context;

  const canDelete = canDeleteCertificate();

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
    inTrustStore,
  } = data?.certificate || {};

  const issuedDate = issuedOn ? DateUtils.timestampToString(issuedOn) : '';
  const expiredDate = expiresOn ? DateUtils.timestampToString(expiresOn) : '';

  const isLoading = state.matches('loading');

  const save = () => send('SAVE');
  const retry = () => send('RETRY');

  const confirmDelete = () => {
    if (canDelete) {
      send('CONFIRM_DELETE');
    }
  };

  return (
    <Page className="nxrm-ssl-certificate">
      <PageHeader>
        <PageTitle
          icon={faIdCardAlt}
          text={LABELS.DETAILS_TITLE(subjectCommonName || '')}
          description={LABELS.DETAILS_DESCRIPTION}
        />
      </PageHeader>
      <ContentBody className="nxrm-ssl-certificate-form">
        <NxTile>
          <NxTile.Content>
            <NxLoadWrapper
              loading={isLoading}
              error={loadError}
              retryHandler={retry}
            >
              <NxH2>{LABELS.SECTIONS.CERTIFICATE}</NxH2>
              <NxReadOnly>
                <NxReadOnly.Label>{LABELS.FINGERPRINT.LABEL}</NxReadOnly.Label>
                <NxReadOnly.Data>
                  <NxTooltip title={fingerprint}>
                    <div className="ellipsis-text">{fingerprint}</div>
                  </NxTooltip>
                </NxReadOnly.Data>
                <NxReadOnly.Label>{LABELS.VALID_UNTIL.LABEL}</NxReadOnly.Label>
                <NxReadOnly.Data>
                  <NxTooltip title={expiredDate}>
                    <div className="ellipsis-text">{expiredDate}</div>
                  </NxTooltip>
                </NxReadOnly.Data>
                <NxReadOnly.Label>{LABELS.ISSUED_ON.LABEL}</NxReadOnly.Label>
                <NxReadOnly.Data className="ellipsis-text">
                  <NxTooltip title={issuedDate}>
                    <div className="ellipsis-text">{issuedDate}</div>
                  </NxTooltip>
                </NxReadOnly.Data>
              </NxReadOnly>
              <NxDivider />
              <NxGrid.Row>
                <NxGrid.Column>
                  <NxH2>{LABELS.SECTIONS.SUBJECT}</NxH2>
                  <NxReadOnly>
                    <ReadOnlyField
                      label={LABELS.COMMON_NAME.LABEL}
                      value={subjectCommonName}
                    />
                    <ReadOnlyField
                      label={LABELS.ORGANIZATION.LABEL}
                      value={subjectOrganization}
                    />
                    <ReadOnlyField
                      label={LABELS.UNIT.LABEL}
                      value={subjectOrganizationalUnit}
                    />
                  </NxReadOnly>
                </NxGrid.Column>
                <NxGrid.Column>
                  <NxH2>{LABELS.SECTIONS.ISSUER}</NxH2>
                  <NxReadOnly>
                    <ReadOnlyField
                      label={LABELS.COMMON_NAME.LABEL}
                      value={issuerCommonName}
                    />
                    <ReadOnlyField
                      label={LABELS.ORGANIZATION.LABEL}
                      value={issuerOrganization}
                    />
                    <ReadOnlyField
                      label={LABELS.UNIT.LABEL}
                      value={issuerOrganizationalUnit}
                    />
                  </NxReadOnly>
                </NxGrid.Column>
              </NxGrid.Row>
              <NxFooter>
                <NxWarningAlert>{LABELS.WARNING}</NxWarningAlert>
                <NxButtonBar>
                  <NxButton type="button" onClick={onDone}>
                    {UIStrings.SETTINGS.CANCEL_BUTTON_LABEL}
                  </NxButton>
                  {shouldLoadNew && !inTrustStore ? (
                    <NxButton type="button" variant="primary" onClick={save}>
                      {LABELS.BUTTONS.ADD}
                    </NxButton>
                  ) : (
                    <NxTooltip title={!canDelete && UIStrings.PERMISSION_ERROR}>
                      <NxButton
                        type="button"
                        variant="primary"
                        onClick={confirmDelete}
                        className={!canDelete ? 'disabled' : ''}
                      >
                        {LABELS.BUTTONS.DELETE}
                      </NxButton>
                    </NxTooltip>
                  )}
                </NxButtonBar>
              </NxFooter>
            </NxLoadWrapper>
          </NxTile.Content>
        </NxTile>
      </ContentBody>
    </Page>
  );
}
