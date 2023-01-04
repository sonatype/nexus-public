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

import {
  NxAccordion,
  NxCheckbox,
  NxFieldset,
  NxFormGroup,
  NxFormSelect,
  NxStatefulAccordion,
  NxTextInput
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings'

import './S3BlobStoreSettings.scss';

const FIELDS = UIStrings.S3_BLOBSTORE_CONFIGURATION;

export default function S3BlobStoreSettings({service}) {
  const [current, send] = useService(service);
  const {bucketConfiguration} = current.context.data;
  const {dropDownValues} = current.context.type;
  const hasAuthenticationSettings = Boolean(bucketConfiguration.bucketSecurity?.accessKeyId);
  const hasEncryptionSettings =
      Boolean(bucketConfiguration.encryption?.encryptionType) ||
      Boolean(bucketConfiguration.encryption?.encryptionKey);
  const hasAdvancedConnectionSettings =
      Boolean(bucketConfiguration.advancedBucketConnection?.endpoint) ||
      Boolean(bucketConfiguration.advancedBucketConnection?.signerType) ||
      bucketConfiguration.advancedBucketConnection?.forcePathStyle;

  function bucketField(field) {
    return `bucketConfiguration.bucket.${field}`;
  }

  function securityField(field) {
    return `bucketConfiguration.bucketSecurity.${field}`;
  }

  function encryptionField(field) {
    return `bucketConfiguration.encryption.${field}`;
  }

  function advancedField(field) {
    return `bucketConfiguration.advancedBucketConnection.${field}`;
  }

  return <div className="nxrm-s3-blobstore">
    <NxFormGroup
        label={FIELDS.S3BlobStore_Region_FieldLabel}
        sublabel={FIELDS.S3BlobStore_Region_HelpText}
    >
      <NxFormSelect {...FormUtils.fieldProps(bucketField('region'), current)}
              onChange={FormUtils.handleUpdate(bucketField('region'), send)}>
        {dropDownValues?.regions?.map(region =>
            <option key={region.id} value={region.id}>{region.name}</option>
        )}
      </NxFormSelect>
    </NxFormGroup>
    <NxFormGroup
        label={FIELDS.S3BlobStore_Bucket_FieldLabel}
        sublabel={FIELDS.S3BlobStore_Bucket_HelpText}
        isRequired
    >
      <NxTextInput className="nx-text-input--long"
                 {...FormUtils.fieldProps(bucketField('name'), current)}
                 onChange={FormUtils.handleUpdate(bucketField('name'), send)}/>
    </NxFormGroup>
    <NxFormGroup
        label={FIELDS.S3BlobStore_Prefix_FieldLabel}
        sublabel={FIELDS.S3BlobStore_Prefix_HelpText}
    >
      <NxTextInput className="nx-text-input--long"
                 {...FormUtils.fieldProps(bucketField('prefix'), current)}
                 onChange={FormUtils.handleUpdate(bucketField('prefix'), send)}/>
    </NxFormGroup>
    <div className="nx-form-group">
      <label className="nx-label" htmlFor="bucketConfiguration.bucket.expiration">
        <span className="nx-label__text">{FIELDS.S3BlobStore_Expiration_FieldLabel}</span>
      </label>
      <div className="nx-sub-label">{FIELDS.S3BlobStore_Expiration_HelpText}</div>
      <NxTextInput
                 {...FormUtils.fieldProps(bucketField('expiration'), current)}
                 onChange={FormUtils.handleUpdate(bucketField('expiration'), send)}/>
      <span className='nxrm-s3-blobstore-prefix-text'>{FIELDS.S3BlobStore_Expiration_DaysText}</span>
    </div>

    <NxStatefulAccordion defaultOpen={hasAuthenticationSettings}>
      <NxAccordion.Header>
        <h2 className="nx-accordion__header-title">{FIELDS.S3BlobStore_Authentication_Title}</h2>
      </NxAccordion.Header>
      <NxFormGroup label={FIELDS.S3BlobStore_Authentication_AccessKeyId} isRequired>
        <NxTextInput className="nx-text-input--long"
                   {...FormUtils.fieldProps(securityField('accessKeyId'), current)}
                   onChange={FormUtils.handleUpdate(securityField('accessKeyId'), send)}/>
      </NxFormGroup>
      <NxFormGroup label={FIELDS.S3BlobStore_Authentication_SecretAccessKey} isRequired>
        <NxTextInput className="nx-text-input--long"
                   type="password"
                   autoComplete="new-password"
                   {...FormUtils.fieldProps(securityField('secretAccessKey'), current)}
                   onChange={FormUtils.handleUpdate(securityField('secretAccessKey'), send)}/>
      </NxFormGroup>
      <NxFormGroup
          label={FIELDS.S3BlobStore_Authentication_AssumeRoleArn}
      >
        <NxTextInput className="nx-text-input--long"
                   {...FormUtils.fieldProps(securityField('role'), current)}
                   onChange={FormUtils.handleUpdate(securityField('role'), send)}/>
      </NxFormGroup>
      <NxFormGroup label={FIELDS.S3BlobStore_Authentication_SessionToken}>
        <NxTextInput className="nx-text-input--long"
                   {...FormUtils.fieldProps(securityField('sessionToken'), current)}
                   onChange={FormUtils.handleUpdate(securityField('sessionToken'), send)}/>
      </NxFormGroup>
    </NxStatefulAccordion>
    <NxStatefulAccordion defaultOpen={hasEncryptionSettings}>
      <NxAccordion.Header>
        <h2 className="nx-accordion__header-title">{FIELDS.S3BlobStore_EncryptionSettings_Title}</h2>
      </NxAccordion.Header>
      <NxFormGroup
          label={FIELDS.S3BlobStore_EncryptionSettings_Type_FieldLabel}
          sublabel={FIELDS.S3BlobStore_EncryptionSettings_Type_HelpText}
      >
        <NxFormSelect {...FormUtils.fieldProps(encryptionField('encryptionType'), current)}
                onChange={FormUtils.handleUpdate(encryptionField('encryptionType'), send)}>
          <option value={null}></option>
          {dropDownValues?.encryptionTypes?.map(encryptionType =>
              <option key={encryptionType.id} value={encryptionType.id}>{encryptionType.name}</option>
          )}
        </NxFormSelect>
      </NxFormGroup>
      <NxFormGroup
          label={FIELDS.S3BlobStore_EncryptionSettings_KeyID_FieldLabel}
          sublabel={FIELDS.S3BlobStore_EncryptionSettings_KeyID_HelpText}
      >
        <NxTextInput className="nx-text-input--long"
                   {...FormUtils.fieldProps(encryptionField('encryptionKey'), current)}
                   onChange={FormUtils.handleUpdate(encryptionField('encryptionKey'), send)}/>
      </NxFormGroup>
    </NxStatefulAccordion>
    <NxStatefulAccordion defaultOpen={hasAdvancedConnectionSettings}>
      <NxAccordion.Header>
        <h2 className="nx-accordion__header-title">{FIELDS.S3BlobStore_AdvancedConnectionSettings_Title}</h2>
      </NxAccordion.Header>
      <NxFormGroup
          label={FIELDS.S3BlobStore_AdvancedConnectionSettings_EndPointUrl_FieldLabel}
          sublabel={FIELDS.S3BlobStore_AdvancedConnectionSettings_EndPointUrl_HelpText}
      >
        <NxTextInput className="nx-text-input--long"
                   {...FormUtils.fieldProps(advancedField('endpoint'), current)}
                   onChange={FormUtils.handleUpdate(advancedField('endpoint'), send)}/>
      </NxFormGroup>
      <NxFormGroup
        label={FIELDS.S3Blobstore_AdvancedConnectionSettings_MaxConnectionPoolSize_FieldLabel}
        sublabel={FIELDS.S3Blobstore_AdvancedConnectionSettings_MaxConnectionPoolSize_HelpText}
      >
        <NxTextInput {...FormUtils.fieldProps(advancedField('maxConnectionPoolSize'), current)}
          onChange={FormUtils.handleUpdate(advancedField('maxConnectionPoolSize'), send)} />
      </NxFormGroup>
      <NxFormGroup
          label={FIELDS.S3BlobStore_AdvancedConnectionSettings_SignatureVersion_FieldLabel}
          sublabel={FIELDS.S3BlobStore_AdvancedConnectionSettings_SignatureVersion_HelpText}
      >
        <NxFormSelect {...FormUtils.fieldProps(advancedField('signerType'), current)}
                onChange={FormUtils.handleUpdate(advancedField('signerType'), send)}>
          <option value={null}></option>
          {dropDownValues?.signerTypes?.map(signerType =>
              <option key={signerType.id} value={signerType.id}>{signerType.name}</option>
          )}
        </NxFormSelect>
      </NxFormGroup>
      <NxFieldset label={FIELDS.S3BlobStore_AdvancedConnectionSettings_PathStyleAccess_FieldLabel}>
        <NxCheckbox {...FormUtils.checkboxProps(advancedField('forcePathStyle'), current)}
                    onChange={FormUtils.handleUpdate(advancedField('forcePathStyle'), send)}>
          {FIELDS.S3BlobStore_AdvancedConnectionSettings_PathStyleAccess_HelpText}
        </NxCheckbox>
      </NxFieldset>
    </NxStatefulAccordion>
  </div>;
}
