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
import {useMachine, useActor} from '@xstate/react';

import {FormUtils} from '@sonatype/nexus-ui-plugin';
import {
  NxButton,
  NxErrorAlert,
  NxFieldset,
  NxFormGroup,
  NxLoadingSpinner,
  NxSuccessAlert,
  NxTextInput,
  NxRadio
} from '@sonatype/react-shared-components';

import AzureBlobStoreSettingsMachine from './AzureBlobStoreSettingsMachine';
import UIStrings from '../../../../../constants/UIStrings';

const AZURE = UIStrings.BLOB_STORES.AZURE;

export default function AzureBlobStoreSettings({service}) {
  const [current, send] = useActor(service);
  const {bucketConfiguration = {}} = current.context.data;

  const [azureState, sendAzureEvent] = useMachine(AzureBlobStoreSettingsMachine, {
    context: {
      // blobStoreName will only contain a value when the form is being edited
      blobStoreName: current.context.data.name,
      accountName: bucketConfiguration.accountName,
      containerName: bucketConfiguration.containerName,
      authenticationMethod: bucketConfiguration.authentication?.authenticationMethod,
      accountKey: bucketConfiguration.authentication?.accountKey
    },
    devTools: true
  });

  function testConnection() {
    sendAzureEvent({
      type: 'TEST_CONNECTION'
    });
  }

  function handleAccountNameChange(value) {
    send({
      type: 'UPDATE',
      name: 'bucketConfiguration.accountName',
      value
    });

    sendAzureEvent({
      type: 'UPDATE_ACCOUNT_NAME',
      accountName: value
    });
  }

  function handleContainerNameChange(value) {
    send({
      type: 'UPDATE',
      name: 'bucketConfiguration.containerName',
      value
    });

    sendAzureEvent({
      type: 'UPDATE_CONTAINER_NAME',
      containerName: value
    });
  }

  function handleAccountKeyChange(value) {
    send({
      type: 'UPDATE',
      name: 'bucketConfiguration.authentication.accountKey',
      value
    });

    sendAzureEvent({
      type: 'UPDATE_ACCOUNT_KEY',
      accountKey: value
    });
  }

  function handleAuthenticationMethodChange(value) {
    send({
      type: 'UPDATE',
      name: 'bucketConfiguration.authentication.authenticationMethod',
      value
    });

    sendAzureEvent({
      type: 'UPDATE_AUTH_METHOD',
      authenticationMethod: value
    });
  }

  return (
      <div className="nxrm-azure-blobstore">
        <NxFormGroup {...AZURE.ACCOUNT_NAME} isRequired>
          <NxTextInput
              {...FormUtils.fieldProps(['bucketConfiguration', 'accountName'], current)}
              onChange={handleAccountNameChange}/>
        </NxFormGroup>
        <NxFormGroup {...AZURE.CONTAINER_NAME} isRequired>
          <NxTextInput
              {...FormUtils.fieldProps(['bucketConfiguration', 'containerName'], current)}
              onChange={handleContainerNameChange}/>
        </NxFormGroup>
        <NxFieldset label={AZURE.AUTHENTICATION.label}>
          <NxRadio
              radioId="useEnvironmentVariables"
              name="bucketConfiguration.authentication.authenticationMethod"
              value="ENVIRONMENTVARIABLE"
              isChecked={bucketConfiguration.authentication?.authenticationMethod === 'ENVIRONMENTVARIABLE'}
              onChange={handleAuthenticationMethodChange}>
            {AZURE.AUTHENTICATION.ENVIRONMENTVARIABLE}
          </NxRadio>
          <NxRadio
              radioId="useManagedIdentity"
              name="bucketConfiguration.authentication.authenticationMethod"
              value="MANAGEDIDENTITY"
              isChecked={bucketConfiguration.authentication?.authenticationMethod === 'MANAGEDIDENTITY'}
              onChange={handleAuthenticationMethodChange}>
            {AZURE.AUTHENTICATION.MANAGED}
          </NxRadio>
          <NxRadio
              radioId="useAccountKey"
              name="bucketConfiguration.authentication.authenticationMethod"
              value="ACCOUNTKEY"
              isChecked={bucketConfiguration.authentication?.authenticationMethod === 'ACCOUNTKEY'}
              onChange={handleAuthenticationMethodChange}>
            {AZURE.AUTHENTICATION.ACCOUNT_KEY.label}
          </NxRadio>
        </NxFieldset>

        {bucketConfiguration.authentication?.authenticationMethod === 'ACCOUNTKEY' &&
        <NxFormGroup {...AZURE.AUTHENTICATION.ACCOUNT_KEY} isRequired>
          <NxTextInput type="password"
                       autoComplete="new-password"
                       {...FormUtils.fieldProps(['bucketConfiguration', 'authentication', 'accountKey'], current)}
                       onChange={handleAccountKeyChange}/>
        </NxFormGroup>
        }

        <div className="nx-form-row">
          <NxButton type="button" variant="tertiary" disabled={azureState.matches('testing')} onClick={testConnection}>
            {AZURE.TEST_CONNECTION}
          </NxButton>
        </div>

        <div className="nx-form-row">
          {azureState.matches('editing.error') && <NxErrorAlert>{AZURE.TEST_CONNECTION_ERROR}</NxErrorAlert>}
          {azureState.matches('editing.success') && <NxSuccessAlert>{AZURE.TEST_CONNECTION_SUCCESS}</NxSuccessAlert>}
          {azureState.matches('testing') && <NxLoadingSpinner>{AZURE.TESTING}</NxLoadingSpinner>}
        </div>
      </div>
  );
}
