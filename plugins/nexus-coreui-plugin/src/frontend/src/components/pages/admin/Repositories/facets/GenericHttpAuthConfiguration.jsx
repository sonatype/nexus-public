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

import {FormUtils} from '@sonatype/nexus-ui-plugin';

import {NxFormGroup, NxFormSelect, NxTextInput} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';

const {EDITOR} = UIStrings.REPOSITORIES;

export default function GenericHttpAuthConfiguration({parentMachine}) {
  const [currentParent, sendParent] = parentMachine;

  const {httpClient} = currentParent.context.data;

  const type = httpClient.authentication?.type;

  const updateType = (authType) => {
    if (authType === '') {
      sendParent({
        type: 'UPDATE',
        name: 'httpClient.authentication',
        value: null
      });
    } else {
      sendParent({
        type: 'UPDATE',
        name: 'httpClient.authentication.type',
        value: authType
      });
    }
  };

  return (
    <>
      <h2 className="nx-h2">{EDITOR.HTTP_AUTH_CAPTION}</h2>
      <NxFormGroup label={EDITOR.AUTH_TYPE_LABEL} className="nxrm-form-group-http-auth-type">
        <NxFormSelect
          {...FormUtils.selectProps('httpClient.authentication.type', currentParent)}
          onChange={updateType}
        >
          <option value="">{EDITOR.NONE_OPTION}</option>
          <option value="username">{EDITOR.USERNAME_OPTION}</option>
          <option value="ntlm">{EDITOR.NTLM_OPTION}</option>
        </NxFormSelect>
      </NxFormGroup>

      {type && type !== '' && (
        <div className="nx-form-row">
          <NxFormGroup
            label={EDITOR.USERNAME_LABEL}
            isRequired
            className="nxrm-form-group-http-username"
          >
            <NxTextInput
              {...FormUtils.fieldProps('httpClient.authentication.username', currentParent)}
              onChange={FormUtils.handleUpdate('httpClient.authentication.username', sendParent)}
            />
          </NxFormGroup>

          <NxFormGroup
            label={EDITOR.PASSWORD_LABEL}
            isRequired
            className="nxrm-form-group-http-password"
          >
            <NxTextInput
              {...FormUtils.fieldProps('httpClient.authentication.password', currentParent)}
              onChange={FormUtils.handleUpdate('httpClient.authentication.password', sendParent)}
              type="password"
            />
          </NxFormGroup>
        </div>
      )}

      {type === 'ntlm' && (
        <div className="nx-form-row">
          <NxFormGroup
            label={EDITOR.NTLM_HOST_LABEL}
            isRequired
            className="nxrm-form-group-ntlm-host"
          >
            <NxTextInput
              {...FormUtils.fieldProps('httpClient.authentication.ntlmHost', currentParent)}
              onChange={FormUtils.handleUpdate('httpClient.authentication.ntlmHost', sendParent)}
            />
          </NxFormGroup>

          <NxFormGroup
            label={EDITOR.NTLM_DOMAIN_LABEL}
            isRequired
            className="nxrm-form-group-ntlm-domain"
          >
            <NxTextInput
              {...FormUtils.fieldProps('httpClient.authentication.ntlmDomain', currentParent)}
              onChange={FormUtils.handleUpdate('httpClient.authentication.ntlmDomain', sendParent)}
            />
          </NxFormGroup>
        </div>
      )}
    </>
  );
}
