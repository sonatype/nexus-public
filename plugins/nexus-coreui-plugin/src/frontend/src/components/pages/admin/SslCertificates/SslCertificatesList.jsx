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
  ContentBody,
  HelpTile,
  ListMachineUtils,
  Page,
  PageHeader,
  PageTitle,
  PageActions,
  Section,
  SectionToolbar
} from '@sonatype/nexus-ui-plugin';
import {
  NxButton,
  NxFilterInput,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  NxTooltip
} from '@sonatype/react-shared-components';

import {faIdCardAlt} from '@fortawesome/free-solid-svg-icons';

import Machine from './SslCertificatesListMachine';
import UIStrings from '../../../../constants/UIStrings';
import {canCreateCertificate} from './SslCertificatesHelper';

const {
  LIST: {COLUMNS},
  LIST: LABELS
} = UIStrings.SSL_CERTIFICATES;

export default function SslCertificatesList({onCreate, onEdit}) {
  const [current, send] = useMachine(Machine, {devTools: true});
  const isLoading = current.matches('loading');
  const {data, error, filter: filterText} = current.context;

  const subjectCommonNameSortDir = ListMachineUtils.getSortDirection(
    'subjectCommonName',
    current.context
  );
  const subjectOrganizationSortDir = ListMachineUtils.getSortDirection(
    'subjectOrganization',
    current.context
  );
  const issuerOrganizationSortDir = ListMachineUtils.getSortDirection(
    'issuerOrganization',
    current.context
  );
  const fingerprintSortDir = ListMachineUtils.getSortDirection('fingerprint', current.context);

  const sortBySubjectCommonName = () => send({type: 'SORT_BY_SUBJECT_COMMON_NAME'});
  const sortBySubjectOrganization = () => send({type: 'SORT_BY_SUBJECT_ORGANIZATION'});
  const sortByIssuerOrganization = () => send({type: 'SORT_BY_ISSUER_ORGANIZATION'});
  const sortByFingerprint = () => send({type: 'SORT_BY_FINGERPRINT'});

  const filter = (value) => send({type: 'FILTER', filter: value});

  const canCreate = canCreateCertificate();

  return (
    <Page className="nxrm-ssl-certificates">
      <PageHeader>
        <PageTitle
          icon={faIdCardAlt}
          text={UIStrings.SSL_CERTIFICATES.MENU.text}
          description={UIStrings.SSL_CERTIFICATES.MENU.description}
        />
        <PageActions>
          <NxTooltip title={!canCreate && UIStrings.PERMISSION_ERROR} placement="bottom">
            <NxButton
              type="button"
              variant="primary"
              className={!canCreate && 'disabled'}
              onClick={() => canCreate && onCreate()}
            >
              {LABELS.CREATE_BUTTON}
            </NxButton>
          </NxTooltip>
        </PageActions>
      </PageHeader>
      <ContentBody className="nxrm-ssl-certificates-list">
        <Section>
          <SectionToolbar>
            <div className="nxrm-spacer" />
            <NxFilterInput
              id="filter"
              onChange={filter}
              value={filterText}
              placeholder={UIStrings.FILTER}
            />
          </SectionToolbar>
          <NxTable>
            <NxTableHead>
              <NxTableRow>
                <NxTableCell
                  onClick={sortBySubjectCommonName}
                  isSortable
                  sortDir={subjectCommonNameSortDir}
                >
                  {COLUMNS.NAME}
                </NxTableCell>
                <NxTableCell
                  onClick={sortBySubjectOrganization}
                  isSortable
                  sortDir={subjectOrganizationSortDir}
                >
                  {COLUMNS.ISSUED_TO}
                </NxTableCell>
                <NxTableCell
                  onClick={sortByIssuerOrganization}
                  isSortable
                  sortDir={issuerOrganizationSortDir}
                >
                  {COLUMNS.ISSUED_BY}
                </NxTableCell>
                <NxTableCell onClick={sortByFingerprint} isSortable sortDir={fingerprintSortDir}>
                  {COLUMNS.FINGERPRINT}
                </NxTableCell>
                <NxTableCell chevron />
              </NxTableRow>
            </NxTableHead>
            <NxTableBody isLoading={isLoading} error={error} emptyMessage={LABELS.EMPTY_LIST}>
              {data.map(
                ({id, subjectCommonName, subjectOrganization, issuerOrganization, fingerprint}) => (
                  <NxTableRow key={id} onClick={() => onEdit(encodeURIComponent(id))} isClickable>
                    <NxTableCell>{subjectCommonName}</NxTableCell>
                    <NxTableCell>{subjectOrganization}</NxTableCell>
                    <NxTableCell>{issuerOrganization}</NxTableCell>
                    <NxTableCell>{fingerprint}</NxTableCell>
                    <NxTableCell chevron />
                  </NxTableRow>
                )
              )}
            </NxTableBody>
          </NxTable>
        </Section>
        <HelpTile header={LABELS.HELP.TITLE} body={LABELS.HELP.TEXT} />
      </ContentBody>
    </Page>
  );
}
