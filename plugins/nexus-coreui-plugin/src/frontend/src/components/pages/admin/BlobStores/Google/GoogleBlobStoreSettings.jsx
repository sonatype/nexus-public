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
import React, { useState, useEffect } from 'react';
import {useActor} from '@xstate/react';
import { FormUtils } from '@sonatype/nexus-ui-plugin';
import UIStrings from '../../../../../constants/UIStrings';
import {
  NxFieldset,
  NxFileUpload,
  NxFormGroup,
  NxRadio,
  NxTextInput,
  NxCheckbox,
  NxP
} from '@sonatype/react-shared-components';

const GOOGLE = UIStrings.BLOB_STORES.GOOGLE;

export default function GoogleBlobStoreSettings({ service }) {
  const [current, send] = useActor(service);
  const { bucketConfiguration = {} } = current.context.data;
  const { pristineData } = current.context;

  function bucketField(field) {
    return `bucketConfiguration.bucket.${field}`;
  }

  function authenticationField(field) {
    return `bucketConfiguration.bucketSecurity.${field}`;
  }

  function encryptionField(field) {
    return `bucketConfiguration.encryption.${field}`;
  }

  const setFiles = (file) => {
    send({ type: 'SET_FILES', file });
  };

  const isEdit = Boolean(pristineData.name);

  const [isChecked, setIsChecked] = useState(bucketConfiguration.encryption?.encryptionType === 'kmsManagedEncryption');

  useEffect(() => {
    setIsChecked(bucketConfiguration.encryption?.encryptionType === 'kmsManagedEncryption');
  }, [bucketConfiguration.encryption?.encryptionType]);

  const handleCheckboxChange = (isChecked) => {
    setIsChecked(isChecked);
    send({
      type: 'UPDATE',
      name: encryptionField('encryptionType'),
      value: isChecked ? 'kmsManagedEncryption' : 'default'
    });
  };

  return (
      <div className="nxrm-google-blobstore">
        <NxFormGroup {...GOOGLE.REGION}>
          <NxP className="nx-sub-label" aria-label={GOOGLE.REGION.label}>
            The region is automatically set based on where Nexus Repository is running in GCP. Ensure the bucket is in the same region.
          </NxP>
        </NxFormGroup>
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
        <NxFormGroup {...GOOGLE.PREFIX}>
          <NxTextInput
              {...FormUtils.fieldProps(bucketField('prefix'), current)}
                 onChange={FormUtils.handleUpdate(bucketField('prefix'), send)}/>
        </NxFormGroup>
        <NxFieldset label={GOOGLE.AUTHENTICATION.LABEL}>
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
            <NxFormGroup {...GOOGLE.AUTHENTICATION.JSON_PATH} isRequired={!isEdit}>
              <NxFileUpload
                  onChange={setFiles}
                  isRequired={!isEdit}
                  aria-label="gcp credential json file upload"
                  {...FormUtils.fileUploadProps(authenticationField('file'), current)}
              />
            </NxFormGroup>
        )}
        <NxFieldset label={GOOGLE.ENCRYPTION.LABEL}>
          <p>Data is encrypted by default. Enable KMS to use a custom encryption key.<br />Note: Encryption settings cannot be changed once the bucket is created</p>
          <NxCheckbox
              checkboxId="encryptionKey"
              name="bucketConfiguration.encryption.encryptionType"
              isChecked={isChecked}
              isDisabled={bucketConfiguration.encryption?.encryptionType === 'default'}
              onChange={handleCheckboxChange}
          >
            {GOOGLE.ENCRYPTION.KMS_MANAGED}
          </NxCheckbox>
        </NxFieldset>
        {isChecked && (
            <NxFormGroup {...GOOGLE.ENCRYPTION.KEY_NAME} isRequired={!isEdit}>
              <NxTextInput
                  {...FormUtils.fieldProps(encryptionField('encryptionKey'), current)}
                  onChange={FormUtils.handleUpdate(encryptionField('encryptionKey'), send)}
                  placeholder="e.g. projects/PROJECT_ID/locations/LOCATION/keyRings/KEY_RING_NAME/cryptoKeys/KEY_NAME"
              />
            </NxFormGroup>
        )}
      </div>
  );
}
