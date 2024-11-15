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
  NxFooter,
  NxButtonBar,
  NxButton,
  NxH2,
  NxTooltip,
} from '@sonatype/react-shared-components';
import {
  ContentBody,
  Page,
  PageHeader,
  PageTitle,
} from '@sonatype/nexus-ui-plugin';

import {faIdCardAlt} from '@fortawesome/free-solid-svg-icons';

import UIStrings from '../../../../constants/UIStrings';

import SslCertificatesDetails from './SslCertificatesDetails';
import Machine from './SslCertificatesDetailsFormMachine';

import {canDeleteCertificate} from './SslCertificatesHelper';

const {
  SSL_CERTIFICATES: {FORM: LABELS},
} = UIStrings;

export default function SslCertificatesDetailsForm({itemId, onDone}) {
  const canDelete = canDeleteCertificate();

  const [state, send] = useMachine(Machine, {
    context: {
      pristineData: {
        id: decodeURIComponent(itemId),
      },
    },
    actions: {
      onSaveSuccess: onDone,
      onDeleteSuccess: onDone,
    },
    devTools: true,
  });

  const {data = {}, loadError} = state.context;
  const isLoading = state.matches('loading');

  const retry = () => send({type: 'RETRY'});

  const confirmDelete = () => {
    if (canDelete) {
      send({type: 'CONFIRM_DELETE'});
    }
  };

  return (
    <Page className="nxrm-ssl-certificate">
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
            <NxLoadWrapper
              loading={isLoading}
              error={loadError}
              retryHandler={retry}
            >
              <NxH2>{LABELS.SECTIONS.CERTIFICATE}</NxH2>
              <SslCertificatesDetails data={data} />
              <NxFooter>
                <NxButtonBar>
                  <NxButton type="button" onClick={onDone}>
                    {UIStrings.SETTINGS.CANCEL_BUTTON_LABEL}
                  </NxButton>
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
                </NxButtonBar>
              </NxFooter>
            </NxLoadWrapper>
          </NxTile.Content>
        </NxTile>
      </ContentBody>
    </Page>
  );
}
