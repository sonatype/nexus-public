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
import {useService} from '@xstate/react';

import {
  FormUtils,
  ValidationUtils,
  UseNexusTruststore
} from '@sonatype/nexus-ui-plugin';

import {
  NxButton,
  NxFormGroup,
  NxFormSelect,
  NxH3,
  NxLoadError,
  NxP,
  NxSuccessAlert,
  NxTextInput,
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';

import './ReplicationForm.scss';

const FORM = UIStrings.REPLICATION.FORM;

export default function ReplicationTargetFields({service}) {
  const [state, send] = useService(service);
  const {
    connectionStatus,
    connectionMessage,
    data,
    destinationRepositories,
    testStatus,
    testMessage
  } = state.context;

  const canTest = ValidationUtils.notBlank(data.destinationInstanceUrl) &&
      ValidationUtils.notBlank(data.destinationInstanceUsername) &&
      ValidationUtils.notBlank(data.destinationInstancePassword);

  function testRepositoryConnection() {
    send('TEST_REPOSITORY_CONNECTION');
  }

  function closeSuccessNotification() {
    send('CLOSE_SUCCESS_NOTIFICATION');
  }

  return (
      <>
        <div className="nxrm-replication-form--target-information">
          <NxFormGroup label={FORM.INSTANCE_URL_LABEL} sublabel={FORM.INSTANCE_URL_DESCRIPTION} isRequired>
            <NxTextInput
                {...FormUtils.fieldProps('destinationInstanceUrl', state)}
                onChange={FormUtils.handleUpdate('destinationInstanceUrl', send, 'UPDATE_TEST_CONNECTION')}/>
          </NxFormGroup>

          <NxH3>{FORM.USER_AUTH_LABEL}</NxH3>
          <NxP dangerouslySetInnerHTML={FORM.USER_AUTH_TEXT}/>

          <NxFormGroup label={FORM.USER_LABEL} isRequired>
            <NxTextInput
                {...FormUtils.fieldProps('destinationInstanceUsername', state)}
                onChange={FormUtils.handleUpdate('destinationInstanceUsername', send, 'UPDATE_TEST_CONNECTION')}/>
          </NxFormGroup>

          <NxFormGroup label={FORM.PASSWORD_LABEL} isRequired>
            <NxTextInput
                type="password"
                autoComplete="new-password"
                {...FormUtils.fieldProps('destinationInstancePassword', state)}
                onChange={FormUtils.handleUpdate('destinationInstancePassword', send, 'UPDATE_TEST_CONNECTION')}/>
          </NxFormGroup>

          <UseNexusTruststore
            remoteUrl={data.destinationInstanceUrl}
            {...FormUtils.checkboxProps('useTrustStore', state)}
            onChange={FormUtils.handleUpdate('useTrustStore', send)}
          />

          <div className="nx-form-row">
            <div className="nx-btn-bar">
              <NxButton type="button" disabled={!canTest} variant="tertiary"
                        onClick={testRepositoryConnection}>
                {FORM.TEST_BUTTON}
              </NxButton>
            </div>
          </div>

          {testStatus === 200 &&
              <NxSuccessAlert className="testStatus" onClose={closeSuccessNotification}>
                {FORM.TEST_STATUS_MESSAGE[200]}
              </NxSuccessAlert>
          }
          {testStatus >= 400 && FORM.TEST_STATUS_MESSAGE[testStatus] &&
              <NxLoadError titleMessage={FORM.TEST_STATUS_MESSAGE[testStatus].titleMessage}
                           error={FORM.TEST_STATUS_MESSAGE[testStatus].error(testMessage)}
                           className="testStatus"
                           retryHandler={testRepositoryConnection}/>
          }
          {testStatus >= 400 && !FORM.TEST_STATUS_MESSAGE[testStatus] &&
              <NxLoadError {...FORM.TEST_STATUS_MESSAGE.UNKNOWN}
                           className="testStatus"
                           retryHandler={testRepositoryConnection}/>
          }

          <NxFormGroup label={FORM.TARGET_REPO_LABEL} isRequired>
            <NxFormSelect {...FormUtils.fieldProps('destinationRepositoryName', state)}
                          className={testStatus !== 200 ? 'disabled' : ''}
                          disabled={testStatus !== 200}
                          onChange={FormUtils.handleUpdate('destinationRepositoryName', send)}
                          validatable>
              <option value=""/>
              {destinationRepositories?.map(({id, name}) =>
                  <option key={'destinationRepositoryName' + id} value={id}>{name}</option>
              )}
            </NxFormSelect>
          </NxFormGroup>

          {connectionStatus >= 400 &&
              <NxLoadError titleMessage={FORM.CONNECTION_STATUS_MESSAGE[connectionStatus].titleMessage}
                           error={FORM.CONNECTION_STATUS_MESSAGE[connectionStatus].error(connectionMessage)}
                           className="connectionStatus"
              />
          }
          {connectionStatus >= 400 && !FORM.CONNECTION_STATUS_MESSAGE[connectionStatus] &&
              <NxLoadError titleMessage={FORM.CONNECTION_STATUS_MESSAGE.UNKNOWN.titleMessage}
                           error={FORM.CONNECTION_STATUS_MESSAGE.UNKNOWN.error(connectionMessage)}
                           className="connectionStatus"
              />
          }
        </div>
      </>
  );
}
