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

import {FormUtils} from '@sonatype/nexus-ui-plugin';
import UIStrings from '../../../../../constants/UIStrings';
import {
  NxFieldset,
  NxFormGroup,
  NxTextInput,
  NxRadio
} from '@sonatype/react-shared-components';

const GOOGLE = UIStrings.BLOB_STORES.GOOGLE;

export default function GoogleBlobStoreSettings({service}) {
  const [current, send] = useService(service);
  const {bucketConfiguration = {}} = current.context.data;

  function bucketField(field) {
    return `bucketConfiguration.bucket.${field}`;
  }

  function authenticationField(field) {
    return `bucketConfiguration.bucketSecurity.${field}`;
  }

  return <div className="nxrm-google-blobstore">
    <NxFormGroup {...GOOGLE.PROJECT_ID}>
        <NxTextInput
            {...FormUtils.fieldProps(bucketField('projectId'), current)}
            onChange={FormUtils.handleUpdate(bucketField('projectId'), send)}
        />
    </NxFormGroup>
    <NxFormGroup {...GOOGLE.BUCKET} isRequired>
      <NxTextInput
          {...FormUtils.fieldProps(bucketField('name'), current)}
          onChange={FormUtils.handleUpdate(bucketField('name'), send)}/>
    </NxFormGroup>
    <NxFormGroup {...GOOGLE.REGION} isRequired>
      <NxTextInput
          {...FormUtils.fieldProps(bucketField('region'), current)}
          onChange={FormUtils.handleUpdate(bucketField('region'), send)}/>
    </NxFormGroup>
    <NxFieldset label={GOOGLE.AUTHENTICATION.LABEL} sublabel={GOOGLE.AUTHENTICATION.SUBLABEL}>
      <NxRadio
          radioId="applicationDefaultCredentials"
          name="bucketConfiguration.bucketSecurity.authenticationMethod"
          value="applicationDefault"
          isChecked={bucketConfiguration.bucketSecurity?.authenticationMethod === 'applicationDefault'}
          onChange={FormUtils.handleUpdate(authenticationField('authenticationMethod'), send)}>
        {GOOGLE.AUTHENTICATION.APPLICATION_DEFAULT_CREDENTIALS}
      </NxRadio>
      <NxRadio
          radioId="credentialJSONFile"
          name="bucketConfiguration.bucketSecurity.authenticationMethod"
          value="accountKey"
          isChecked={bucketConfiguration.bucketSecurity?.authenticationMethod === 'accountKey'}
          onChange={FormUtils.handleUpdate(authenticationField('authenticationMethod'), send)}>
        {GOOGLE.AUTHENTICATION.CREDENTIAL_JSON_FILE}
      </NxRadio>
    </NxFieldset>
    {bucketConfiguration.bucketSecurity?.authenticationMethod === 'accountKey' && (
        <NxFormGroup {...GOOGLE.AUTHENTICATION.JSON_PATH} isRequired>
          <NxTextInput
              {...FormUtils.fieldProps(authenticationField('accountKey'), current)}
              onChange={FormUtils.handleUpdate(authenticationField('accountKey'), send)}/>
        </NxFormGroup>
    )}
  </div>
}
