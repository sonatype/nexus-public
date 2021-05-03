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
  FieldWrapper,
  Utils,
  Textfield,
  Section,
  Select,
  NxAccordion,
  NxStatefulAccordion,
  NxCheckbox
} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings'

import './S3BlobStoreSettings.scss';

const FIELDS = UIStrings.S3_BLOBSTORE_CONFIGURATION;

export default function S3BlobStoreSettings({current, updateS3Settings, toggleCustomSetting, dropDownValues}) {

  return <Section className="nxrm-s3-blobstore">
    <FieldWrapper
        labelText={FIELDS.S3BlobStore_Region_FieldLabel}
        descriptionText={FIELDS.S3BlobStore_Region_HelpText}
    >
      <Select {...Utils.fieldProps(['s3Settings', 'region'], current)} onChange={updateS3Settings}>
        {dropDownValues?.regions?.map(region =>
            <option key={region.id} value={region.id}>{region.name}</option>
        )}
      </Select>
    </FieldWrapper>
    <FieldWrapper
        labelText={FIELDS.S3BlobStore_Bucket_FieldLabel}
        descriptionText={FIELDS.S3BlobStore_Bucket_HelpText}
    >
      <Textfield {...Utils.fieldProps(['s3Settings', 'bucket'], current)} className="nx-text-input--long" onChange={updateS3Settings}/>
    </FieldWrapper>
    <FieldWrapper
        labelText={FIELDS.S3BlobStore_Prefix_FieldLabel}
        descriptionText={FIELDS.S3BlobStore_Prefix_HelpText}
    >
      <Textfield {...Utils.fieldProps(['s3Settings', 'prefix'], current)} className="nx-text-input--long" onChange={updateS3Settings}/>
    </FieldWrapper>
    <FieldWrapper
        labelText={FIELDS.S3BlobStore_Expiration_FieldLabel}
        descriptionText={FIELDS.S3BlobStore_Expiration_HelpText}
    >
      <Textfield {...Utils.fieldProps(['s3Settings', 'expiration'], current)} type="number" onChange={updateS3Settings}/>
      <span className='nxrm-s3-blobstore-prefix-text'>{FIELDS.S3BlobStore_Expiration_DaysText}</span>
    </FieldWrapper>

    <NxStatefulAccordion defaultOpen={Boolean(current?.context?.data?.s3Settings?.accessKeyId)}>
      <NxAccordion.Header>
        <h2 className="nx-accordion__header-title">{FIELDS.S3BlobStore_Authentication_Title}</h2>
      </NxAccordion.Header>
      <FieldWrapper
          labelText={FIELDS.S3BlobStore_Authentication_AccessKeyId}
      >
        <Textfield {...Utils.fieldProps(['s3Settings', 'accessKeyId'], current)} onChange={updateS3Settings} className="nx-text-input--long"/>
      </FieldWrapper>
      <FieldWrapper labelText={FIELDS.S3BlobStore_Authentication_SecretAccessKey}>
        <Textfield type='password' {...Utils.fieldProps(['s3Settings', 'secretAccessKey'], current)} onChange={updateS3Settings} className="nx-text-input--long"
        />
      </FieldWrapper>
      <FieldWrapper
          labelText={FIELDS.S3BlobStore_Authentication_AssumeRoleArn}
      >
        <Textfield {...Utils.fieldProps(['s3Settings', 'role'], current)} onChange={updateS3Settings} className="nx-text-input--long"/>
      </FieldWrapper>
      <FieldWrapper
          labelText={FIELDS.S3BlobStore_Authentication_SessionToken}
      >
        <Textfield {...Utils.fieldProps(['s3Settings', 'sessionToken'], current)} onChange={updateS3Settings} className="nx-text-input--long"/>
      </FieldWrapper>
    </NxStatefulAccordion>
    <NxStatefulAccordion defaultOpen={Boolean(current?.context?.data?.s3Settings?.encryptionType) || Boolean(current?.context?.data?.s3Settings?.encryptionKey)}>
      <NxAccordion.Header>
        <h2 className="nx-accordion__header-title">{FIELDS.S3BlobStore_EncryptionSettings_Title}</h2>
      </NxAccordion.Header>
      <FieldWrapper
          labelText={FIELDS.S3BlobStore_EncryptionSettings_Type_FieldLabel}
          descriptionText={FIELDS.S3BlobStore_EncryptionSettings_Type_HelpText}
      >
        <Select {...Utils.fieldProps(['s3Settings', 'encryptionType'], current)} onChange={updateS3Settings}>
          {dropDownValues?.encryptionTypes?.map(encryptionType =>
              <option key={encryptionType.id} value={encryptionType.id}>{encryptionType.name}</option>
          )}
        </Select>
      </FieldWrapper>
      <FieldWrapper
          labelText={FIELDS.S3BlobStore_EncryptionSettings_KeyID_FieldLabel}
          descriptionText={FIELDS.S3BlobStore_EncryptionSettings_KeyID_HelpText}
      >
        <Textfield {...Utils.fieldProps(['s3Settings', 'encryptionKey'], current)} onChange={updateS3Settings}
            className="nx-text-input--long"
        />
      </FieldWrapper>
    </NxStatefulAccordion>
    <NxStatefulAccordion defaultOpen={Boolean(current?.context?.data?.s3Settings?.endpoint) || Boolean(current?.context?.data?.s3Settings?.signerType)}>
      <NxAccordion.Header>
        <h2 className="nx-accordion__header-title">{FIELDS.S3BlobStore_AdvancedConnectionSettings_Title}</h2>
      </NxAccordion.Header>
      <FieldWrapper
          labelText={FIELDS.S3BlobStore_AdvancedConnectionSettings_EndPointUrl_FieldLabel}
          descriptionText={FIELDS.S3BlobStore_AdvancedConnectionSettings_EndPointUrl_HelpText}
      >
        <Textfield {...Utils.fieldProps(['s3Settings', 'endpoint'], current)} onChange={updateS3Settings}
            className="nx-text-input--long"
        />
      </FieldWrapper>
      <FieldWrapper
          labelText={FIELDS.S3BlobStore_AdvancedConnectionSettings_SignatureVersion_FieldLabel}
          descriptionText={FIELDS.S3BlobStore_AdvancedConnectionSettings_SignatureVersion_HelpText}
      >
        <Select {...Utils.fieldProps(['s3Settings', 'signerType'], current)} onChange={updateS3Settings}>
          {dropDownValues?.signerTypes?.map(signerType =>
              <option key={signerType.id} value={signerType.id}>{signerType.name}</option>
          )}
        </Select>
      </FieldWrapper>
      <FieldWrapper labelText={FIELDS.S3BlobStore_AdvancedConnectionSettings_PathStyleAccess_FieldLabel}>
        <NxCheckbox {...Utils.checkboxProps(['s3Settings', 'forcePathStyle'], current)} onChange={toggleCustomSetting}>
          {FIELDS.S3BlobStore_AdvancedConnectionSettings_PathStyleAccess_HelpText}
        </NxCheckbox>
      </FieldWrapper>
    </NxStatefulAccordion>
  </Section>;
}
