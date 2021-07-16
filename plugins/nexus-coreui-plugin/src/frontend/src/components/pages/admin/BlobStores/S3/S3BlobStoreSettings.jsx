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
  FieldWrapper,
  FormUtils,
  Textfield,
  Select,
  NxAccordion,
  NxStatefulAccordion,
  NxCheckbox,
  NxTextInput
} from '@sonatype/nexus-ui-plugin';

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
    <FieldWrapper
        labelText={FIELDS.S3BlobStore_Region_FieldLabel}
        descriptionText={FIELDS.S3BlobStore_Region_HelpText}
    >
      <Select {...FormUtils.fieldProps(bucketField('region'), current)}
              onChange={FormUtils.handleUpdate(bucketField('region'), send)}>
        {dropDownValues?.regions?.map(region =>
            <option key={region.id} value={region.id}>{region.name}</option>
        )}
      </Select>
    </FieldWrapper>
    <FieldWrapper
        labelText={FIELDS.S3BlobStore_Bucket_FieldLabel}
        descriptionText={FIELDS.S3BlobStore_Bucket_HelpText}
    >
      <Textfield className="nx-text-input--long"
                 {...FormUtils.fieldProps(bucketField('name'), current)}
                 onChange={FormUtils.handleUpdate(bucketField('name'), send)}/>
    </FieldWrapper>
    <FieldWrapper
        labelText={FIELDS.S3BlobStore_Prefix_FieldLabel}
        descriptionText={FIELDS.S3BlobStore_Prefix_HelpText}
    >
      <Textfield className="nx-text-input--long"
                 {...FormUtils.fieldProps(bucketField('prefix'), current)}
                 onChange={FormUtils.handleUpdate(bucketField('prefix'), send)}/>
    </FieldWrapper>
    <FieldWrapper
        labelText={FIELDS.S3BlobStore_Expiration_FieldLabel}
        descriptionText={FIELDS.S3BlobStore_Expiration_HelpText}
    >
      <Textfield type="number"
                 {...FormUtils.fieldProps(bucketField('expiration'), current)}
                 onChange={FormUtils.handleUpdate(bucketField('expiration'), send)}/>
      <span className='nxrm-s3-blobstore-prefix-text'>{FIELDS.S3BlobStore_Expiration_DaysText}</span>
    </FieldWrapper>

    <NxStatefulAccordion defaultOpen={hasAuthenticationSettings}>
      <NxAccordion.Header>
        <h2 className="nx-accordion__header-title">{FIELDS.S3BlobStore_Authentication_Title}</h2>
      </NxAccordion.Header>
      <FieldWrapper labelText={FIELDS.S3BlobStore_Authentication_AccessKeyId}>
        <Textfield className="nx-text-input--long"
                   {...FormUtils.fieldProps(securityField('accessKeyId'), current)}
                   onChange={FormUtils.handleUpdate(securityField('accessKeyId'), send)}/>
      </FieldWrapper>
      <FieldWrapper labelText={FIELDS.S3BlobStore_Authentication_SecretAccessKey}>
        <Textfield className="nx-text-input--long"
                   type="password"
                   autoComplete="new-password"
                   {...FormUtils.fieldProps(securityField('secretAccessKey'), current)}
                   onChange={FormUtils.handleUpdate(securityField('secretAccessKey'), send)}/>
      </FieldWrapper>
      <FieldWrapper
          labelText={FIELDS.S3BlobStore_Authentication_AssumeRoleArn}
      >
        <Textfield className="nx-text-input--long"
                   {...FormUtils.fieldProps(securityField('role'), current)}
                   onChange={FormUtils.handleUpdate(securityField('role'), send)}/>
      </FieldWrapper>
      <FieldWrapper labelText={FIELDS.S3BlobStore_Authentication_SessionToken}>
        <Textfield className="nx-text-input--long"
                   {...FormUtils.fieldProps(securityField('sessionToken'), current)}
                   onChange={FormUtils.handleUpdate(securityField('sessionToken'), send)}/>
      </FieldWrapper>
    </NxStatefulAccordion>
    <NxStatefulAccordion defaultOpen={hasEncryptionSettings}>
      <NxAccordion.Header>
        <h2 className="nx-accordion__header-title">{FIELDS.S3BlobStore_EncryptionSettings_Title}</h2>
      </NxAccordion.Header>
      <FieldWrapper
          labelText={FIELDS.S3BlobStore_EncryptionSettings_Type_FieldLabel}
          descriptionText={FIELDS.S3BlobStore_EncryptionSettings_Type_HelpText}
      >
        <Select {...FormUtils.fieldProps(encryptionField('encryptionType'), current)}
                onChange={FormUtils.handleUpdate(encryptionField('encryptionType'), send)}>
          <option value={null}></option>
          {dropDownValues?.encryptionTypes?.map(encryptionType =>
              <option key={encryptionType.id} value={encryptionType.id}>{encryptionType.name}</option>
          )}
        </Select>
      </FieldWrapper>
      <FieldWrapper
          labelText={FIELDS.S3BlobStore_EncryptionSettings_KeyID_FieldLabel}
          descriptionText={FIELDS.S3BlobStore_EncryptionSettings_KeyID_HelpText}
      >
        <Textfield className="nx-text-input--long"
                   {...FormUtils.fieldProps(encryptionField('encryptionKey'), current)}
                   onChange={FormUtils.handleUpdate(encryptionField('encryptionKey'), send)}/>
      </FieldWrapper>
    </NxStatefulAccordion>
    <NxStatefulAccordion defaultOpen={hasAdvancedConnectionSettings}>
      <NxAccordion.Header>
        <h2 className="nx-accordion__header-title">{FIELDS.S3BlobStore_AdvancedConnectionSettings_Title}</h2>
      </NxAccordion.Header>
      <FieldWrapper
          labelText={FIELDS.S3BlobStore_AdvancedConnectionSettings_EndPointUrl_FieldLabel}
          descriptionText={FIELDS.S3BlobStore_AdvancedConnectionSettings_EndPointUrl_HelpText}
      >
        <Textfield className="nx-text-input--long"
                   {...FormUtils.fieldProps(advancedField('endpoint'), current)}
                   onChange={FormUtils.handleUpdate(advancedField('endpoint'), send)}/>
      </FieldWrapper>
      <FieldWrapper
        labelText={FIELDS.S3Blobstore_AdvancedConnectionSettings_MaxConnectionPoolSize_FieldLabel}
        descriptionText={FIELDS.S3Blobstore_AdvancedConnectionSettings_MaxConnectionPoolSize_HelpText}
      >
        <NxTextInput {...FormUtils.fieldProps(advancedField('maxConnectionPoolSize'), current)}
          onChange={FormUtils.handleUpdate(advancedField('maxConnectionPoolSize'), send)} />
      </FieldWrapper>
      <FieldWrapper
          labelText={FIELDS.S3BlobStore_AdvancedConnectionSettings_SignatureVersion_FieldLabel}
          descriptionText={FIELDS.S3BlobStore_AdvancedConnectionSettings_SignatureVersion_HelpText}
      >
        <Select {...FormUtils.fieldProps(advancedField('signerType'), current)}
                onChange={FormUtils.handleUpdate(advancedField('signerType'), send)}>
          <option value={null}></option>
          {dropDownValues?.signerTypes?.map(signerType =>
              <option key={signerType.id} value={signerType.id}>{signerType.name}</option>
          )}
        </Select>
      </FieldWrapper>
      <FieldWrapper labelText={FIELDS.S3BlobStore_AdvancedConnectionSettings_PathStyleAccess_FieldLabel}>
        <NxCheckbox {...FormUtils.checkboxProps(advancedField('forcePathStyle'), current)}
                    onChange={FormUtils.handleUpdate(advancedField('forcePathStyle'), send)}>
          {FIELDS.S3BlobStore_AdvancedConnectionSettings_PathStyleAccess_HelpText}
        </NxCheckbox>
      </FieldWrapper>
    </NxStatefulAccordion>
  </div>;
}
