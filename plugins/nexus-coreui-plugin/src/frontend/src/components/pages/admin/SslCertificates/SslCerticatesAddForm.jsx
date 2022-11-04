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
  NxForm,
  NxH2,
  NxFormGroup,
  NxTextInput,
  NxFieldset,
  NxRadio
} from '@sonatype/react-shared-components';
import {ContentBody, Page, PageHeader, PageTitle, FormUtils} from '@sonatype/nexus-ui-plugin';
import {faIdCardAlt} from '@fortawesome/free-solid-svg-icons';
import UIStrings from '../../../../constants/UIStrings';
import {SOURCES} from './SslCertificatesFormMachine';

const {CAPTION, LOAD_BUTTON, PEM, SERVER} = UIStrings.SSL_CERTIFICATES.ADD_FORM;

export default function SslCerticatesAddForm({onDone, machine}) {
  const [state, send] = machine;

  const {isPristine, isInvalid, source} = state.context;

  const updateSource = (value) => send({type: 'SET_SOURCE', value});

  const load = () => send('LOAD_NEW');

  return (
    <Page className="nxrm-ssl-certificates">
      <PageHeader>
        <PageTitle
          icon={faIdCardAlt}
          text={UIStrings.SSL_CERTIFICATES.MENU.text}
          description={UIStrings.SSL_CERTIFICATES.MENU.description}
        />
      </PageHeader>
      <ContentBody className="nxrm-ssl-certificates-add-form">
        <NxTile>
          <NxTile.Content>
            <NxForm
              onSubmit={load}
              submitBtnText={LOAD_BUTTON}
              onCancel={onDone}
              validationErrors={FormUtils.saveTooltip({isPristine, isInvalid})}
            >
              <NxH2>{CAPTION}</NxH2>

              <NxFieldset label="" isRequired>
                <NxRadio
                  name="source"
                  value={SOURCES.REMOTE_HOST}
                  onChange={updateSource}
                  isChecked={source === SOURCES.REMOTE_HOST}
                >
                  {SERVER.RADIO_DESCRIPTION}
                </NxRadio>
              </NxFieldset>

              <NxFormGroup label={SERVER.LABEL} isRequired>
                <NxTextInput
                  {...FormUtils.fieldProps('remoteHostUrl', state)}
                  onChange={FormUtils.handleUpdate('remoteHostUrl', send)}
                  className="nx-text-input--long"
                  disabled={source === SOURCES.PEM}
                />
              </NxFormGroup>

              <NxFieldset label="" isRequired>
                <NxRadio
                  name="source"
                  value={SOURCES.PEM}
                  onChange={updateSource}
                  isChecked={source === SOURCES.PEM}
                >
                  {PEM.RADIO_DESCRIPTION}
                </NxRadio>
              </NxFieldset>

              <NxFormGroup label={PEM.LABEL} isRequired>
                <NxTextInput
                  type="textarea"
                  {...FormUtils.fieldProps('pemContent', state)}
                  onChange={FormUtils.handleUpdate('pemContent', send)}
                  className="nx-text-input--long"
                  placeholder={PEM.PLACEHOLDER}
                  disabled={source === SOURCES.REMOTE_HOST}
                />
              </NxFormGroup>
            </NxForm>
          </NxTile.Content>
        </NxTile>
      </ContentBody>
    </Page>
  );
}
