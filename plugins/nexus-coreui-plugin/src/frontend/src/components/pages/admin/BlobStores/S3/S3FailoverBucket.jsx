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
import {
  NxButton,
  NxDivider,
  NxFontAwesomeIcon,
  NxFormGroup,
  NxFormSelect,
  NxInfoAlert,
  NxReadOnly,
  NxTextInput
} from '@sonatype/react-shared-components';
import React from 'react';
import {faCheckCircle, faTrash} from '@fortawesome/free-solid-svg-icons';
import UIStrings from '../../../../../constants/UIStrings';

const FIELDS = UIStrings.S3_BLOBSTORE_CONFIGURATION;

export default function S3FailoverBucket({
                                           index,
                                           activeRegion,
                                           mainRegionProps,
                                           regionProps,
                                           onRegionChange,
                                           dropDownValues,
                                           selectedRegions,
                                           bucketNameProps,
                                           onBucketNameChange,
                                           onDelete,
                                         }) {
  const showRegionFailoverActiveMessage = activeRegion === regionProps.value;
  const isInvalidRegion = Boolean(regionProps.validationErrors)
  const notPristine = Boolean(!regionProps.isPristine) || Boolean(!mainRegionProps.isPristine)
  const showInvalidRegion = isInvalidRegion && notPristine;
  regionProps = {...regionProps, isPristine: !notPristine}

  return <div id={`replication-bucket-${index}`}>
    <NxFormGroup
        label={FIELDS.S3BlobStore_ReplicationBucketsSettings_Region_FieldLabel}
        sublabel={FIELDS.S3BlobStore_ReplicationBucketsSettings_Region_HelpText}
        className={showRegionFailoverActiveMessage ? "nxrm-s3-region-select-message" : ""}
        isRequired>
      <NxFormSelect {...regionProps}
                    onChange={onRegionChange}>
        <option value={null}></option>
        {dropDownValues?.regions?.filter(region => region.id !== 'DEFAULT' &&
            (!selectedRegions.includes(region.id) || region.id === regionProps.value))
            .map(region => <option key={region.id} value={region.id}>{region.name}</option>)}
      </NxFormSelect>
    </NxFormGroup>
    {showRegionFailoverActiveMessage &&
        <NxReadOnly id="active-failover-region"
                    className={`nxrm-active-failover-region-message ${showInvalidRegion ? "invalid" : ""}`}>
          <NxFontAwesomeIcon icon={faCheckCircle}/>
          <NxReadOnly.Data>{FIELDS.S3BlobStore_Region_FailoverRegionActiveText}</NxReadOnly.Data>
        </NxReadOnly>
    }
    <NxFormGroup
        label={FIELDS.S3BlobStore_ReplicationBucketsSettings_BucketName_FieldLabel}
        sublabel={FIELDS.S3BlobStore_ReplicationBucketsSettings_BucketName_HelpText}
        isRequired>
      <NxTextInput className="nx-text-input--long"
                   {...bucketNameProps}
                   onChange={onBucketNameChange}/>
    </NxFormGroup>
    <NxInfoAlert><span>{FIELDS.S3BlobStore_ReplicationBucketsSettings_ConfigureBucketReplicationMessage}</span>
    </NxInfoAlert>
    <div id="delete-failover-bucket">
      <NxButton
          type="button"
          variant="tertiary"
          className="nxrm-replication-bucket-button"
          onClick={() => onDelete(index)}>
        <NxFontAwesomeIcon icon={faTrash}/>
        <span>{FIELDS.S3BlobStore_ReplicationBucketsSettings_DeleteFailoverBucket}</span>
      </NxButton>
    </div>
    <NxDivider/>
  </div>;
}
