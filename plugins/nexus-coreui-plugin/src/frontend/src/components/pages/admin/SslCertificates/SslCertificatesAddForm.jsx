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
  NxH2,
  NxFormGroup,
  NxTextInput,
  NxFieldset,
  NxRadio,
  NxStatefulForm,
} from '@sonatype/react-shared-components';
import {
  ContentBody,
  Page,
  PageHeader,
  PageTitle,
  FormUtils
} from '@sonatype/nexus-ui-plugin';

import {faIdCardAlt} from '@fortawesome/free-solid-svg-icons';

import UIStrings from '../../../../constants/UIStrings';

import SslCertificatesAlreadyExistsModal from './SslCertificatesAlreadyExistsModal';
import SslCertificatesDetails from "./SslCertificatesDetails";

import Machine, {SOURCES} from "./SslCertificatesAddFormMachine";

const {ADD_FORM: {CAPTION, LOAD_BUTTON, PEM, SERVER}, MENU, FORM} = UIStrings.SSL_CERTIFICATES;

export default function SslCertificatesAddForm({onDone}) {
  const [state, send] = useMachine(Machine, {
    actions: {
      onSaveSuccess: onDone,
    },
    devTools: true,
  });

  const {source, data} = state.context;
  const {id, inTrustStore} = data;

  const showAddForm = FormUtils.isInState(state, ['loaded', 'loadingDetails']);
  const showDetails = FormUtils.isInState(state, ['previewDetails', 'saving']);
  const submitMaskState = FormUtils.submitMaskState(state, ['loadingDetails', 'saving']);
  const submitMaskMessage = showAddForm ? UIStrings.LOADING : UIStrings.SAVING;

  const updateSource = (value) => send({type: 'SET_SOURCE', value});
  const onSubmit = () => showAddForm ? send({type: 'LOAD_DETAILS'}) : send({type: 'ADD_CERTIFICATE'});

  return (
    <Page className="nxrm-ssl-certificate">
      <PageHeader>
        <PageTitle icon={faIdCardAlt} {...MENU} />
      </PageHeader>
      <ContentBody className="nxrm-ssl-certificate-add-form">
        <NxTile>
          <NxTile.Content>
            <NxStatefulForm
                {...FormUtils.formProps(state, send)}
                onSubmit={onSubmit}
                submitBtnText={showAddForm ? LOAD_BUTTON : FORM.BUTTONS.ADD}
                onCancel={onDone}
                submitMaskMessage={submitMaskMessage}
                submitMaskState={submitMaskState}
            >
              <NxH2>{CAPTION}</NxH2>
              {showAddForm && (
                <>
                  <NxFieldset label="">
                    <NxRadio
                        name="source"
                        value={SOURCES.REMOTE_HOST}
                        onChange={updateSource}
                        isChecked={source === SOURCES.REMOTE_HOST}
                    >
                      {SERVER.RADIO_DESCRIPTION}
                    </NxRadio>
                  </NxFieldset>
                  <NxFormGroup label={SERVER.LABEL} isRequired={source === SOURCES.REMOTE_HOST}>
                    <NxTextInput
                        {...FormUtils.fieldProps('remoteHostUrl', state)}
                        onChange={FormUtils.handleUpdate('remoteHostUrl', send)}
                        className="nx-text-input--long"
                        disabled={source === SOURCES.PEM}
                    />
                  </NxFormGroup>
                  <NxFieldset label="">
                    <NxRadio
                        name="source"
                        value={SOURCES.PEM}
                        onChange={updateSource}
                        isChecked={source === SOURCES.PEM}
                    >
                      {PEM.RADIO_DESCRIPTION}
                    </NxRadio>
                  </NxFieldset>
                  <NxFormGroup label={PEM.LABEL} isRequired ={source === SOURCES.PEM}>
                    <NxTextInput
                        type="textarea"
                        {...FormUtils.fieldProps('pemContent', state)}
                        onChange={FormUtils.handleUpdate('pemContent', send)}
                        className="nx-text-input--long"
                        placeholder={PEM.PLACEHOLDER}
                        disabled={source === SOURCES.REMOTE_HOST}
                    />
                  </NxFormGroup>
                </>
              )}
              {showDetails && <SslCertificatesDetails data={data} />}
            </NxStatefulForm>

          </NxTile.Content>
        </NxTile>
      </ContentBody>

      {inTrustStore && <SslCertificatesAlreadyExistsModal certificateId={id} cancel={onDone} />}
    </Page>
  );
}
