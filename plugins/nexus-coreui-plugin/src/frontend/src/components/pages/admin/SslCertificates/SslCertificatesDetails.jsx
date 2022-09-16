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
import {useMachine} from '@xstate/react';

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
} from '@sonatype/react-shared-components';
import {
  ContentBody,
  Page,
  PageHeader,
  PageTitle,
  ExtJS,
  DateUtils,
} from '@sonatype/nexus-ui-plugin';

import {faIdCardAlt} from '@fortawesome/free-solid-svg-icons';

import Machine from './SslCertificatesDetailsMachine';
import UIStrings from '../../../../constants/UIStrings';

import './SslCertificates.scss';

const {SSL_CERTIFICATES: {FORM: LABELS}} = UIStrings;

export default function SslCertificatesDetails({itemId, onDone}) {

  const canDelete = ExtJS.checkPermission('nexus:ssl-truststore:delete');

  const [current, send] = useMachine(Machine, {
    context: {
      pristineData: {
        id: decodeURIComponent(itemId),
      }
    },
    actions: {
      onSaveSuccess: onDone,
      onDeleteSuccess: onDone,
    },
    guards: {
      canDelete: () => canDelete,
    },
    devTools: true,
  });

  const {data, loadError} = current.context;

  const issuedDate = DateUtils.timestampToString(data?.issuedOn);
  const expiredDate = DateUtils.timestampToString(data?.expiresOn);
  const isLoading = current.matches('loading');

  const retry = () => send('RETRY');

  const confirmDelete = () => {
    if (canDelete) {
      send('CONFIRM_DELETE');
    }
  };

  return <Page className="nxrm-ssl-certificate">
    <PageHeader>
      <PageTitle
          icon={faIdCardAlt}
          text={LABELS.DETAILS_TITLE(data.subjectCommonName || '')}
          description={LABELS.DETAILS_DESCRIPTION}
      />
    </PageHeader>
    <ContentBody className="nxrm-ssl-certificate-form">
      <NxTile>
        <NxTile.Content>
          <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={retry}>
            <NxTile.Content>
              <NxGrid.Row>
                <NxGrid.Column className="nx-grid-col--33">
                  <NxH2>{LABELS.SECTIONS.SUBJECT}</NxH2>
                  <NxReadOnly>
                    <NxReadOnly.Label>{LABELS.COMMON_NAME.LABEL}</NxReadOnly.Label>
                    <NxReadOnly.Data>{data.subjectCommonName}</NxReadOnly.Data>
                    <NxReadOnly.Label>{LABELS.ORGANIZATION.LABEL}</NxReadOnly.Label>
                    <NxReadOnly.Data>{data.subjectOrganization}</NxReadOnly.Data>
                    <NxReadOnly.Label>{LABELS.UNIT.LABEL}</NxReadOnly.Label>
                    <NxReadOnly.Data>{data.subjectOrganizationalUnit}</NxReadOnly.Data>
                  </NxReadOnly>
                </NxGrid.Column>
                <NxGrid.Column className="nx-grid-col--33">
                  <NxH2>{LABELS.SECTIONS.ISSUER}</NxH2>
                  <NxReadOnly>
                    <NxReadOnly.Label>{LABELS.COMMON_NAME.LABEL}</NxReadOnly.Label>
                    <NxReadOnly.Data>{data.issuerCommonName}</NxReadOnly.Data>
                    <NxReadOnly.Label>{LABELS.ORGANIZATION.LABEL}</NxReadOnly.Label>
                    <NxReadOnly.Data>{data.issuerOrganization}</NxReadOnly.Data>
                    <NxReadOnly.Label>{LABELS.UNIT.LABEL}</NxReadOnly.Label>
                    <NxReadOnly.Data>{data.issuerOrganizationalUnit}</NxReadOnly.Data>
                  </NxReadOnly>
                </NxGrid.Column>
                <NxGrid.Column className="nx-grid-col--33">
                  <NxH2>{LABELS.SECTIONS.CERTIFICATE}</NxH2>
                  <NxReadOnly>
                    <NxReadOnly.Label>{LABELS.ISSUED_ON.LABEL}</NxReadOnly.Label>
                    <NxReadOnly.Data className="ellipsis-text">
                      <NxTooltip title={issuedDate}>
                        <div className="ellipsis-text">{issuedDate}</div>
                      </NxTooltip>
                    </NxReadOnly.Data>
                    <NxReadOnly.Label>{LABELS.VALID_UNTIL.LABEL}</NxReadOnly.Label>
                    <NxReadOnly.Data>
                      <NxTooltip title={expiredDate}>
                        <div className="ellipsis-text">{expiredDate}</div>
                      </NxTooltip>
                    </NxReadOnly.Data>
                    <NxReadOnly.Label>{LABELS.FINGERPRINT.LABEL}</NxReadOnly.Label>
                    <NxReadOnly.Data>
                      <NxTooltip title={data.fingerprint}>
                        <div className="ellipsis-text">{data.fingerprint}</div>
                      </NxTooltip>
                    </NxReadOnly.Data>
                  </NxReadOnly>
                </NxGrid.Column>
              </NxGrid.Row>
            </NxTile.Content>
            <NxFooter>
              <NxWarningAlert>{LABELS.WARNING}</NxWarningAlert>
              <NxButtonBar>
                <NxButton
                    type="button"
                    onClick={onDone}
                >
                  {UIStrings.SETTINGS.CANCEL_BUTTON_LABEL}
                </NxButton>
                <NxTooltip title={!canDelete && UIStrings.PERMISSION_ERROR}>
                  <NxButton
                      type="button"
                      variant="primary"
                      onClick={confirmDelete}
                      className={!canDelete && 'disabled'}
                  >
                    {LABELS.BUTTONS.DELETE}
                  </NxButton>
                </NxTooltip>
              </NxButtonBar>
            </NxFooter>
          </NxLoadWrapper>
        </NxTile.Content>
      </NxTile>
    </ContentBody>
  </Page>;
}
