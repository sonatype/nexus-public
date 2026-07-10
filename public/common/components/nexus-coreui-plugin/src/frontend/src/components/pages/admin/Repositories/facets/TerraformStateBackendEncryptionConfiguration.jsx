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
import React, {useState, useEffect} from 'react';

import {FormUtils} from '@sonatype/nexus-ui-plugin';

import {
  NxFormGroup,
  NxTextInput,
  NxStatefulInfoAlert
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';

const {EDITOR} = UIStrings.REPOSITORIES;
const {
  CAPTION,
  ENCRYPTION_KEY,
  ENCRYPTION_KEY_CONFIRM,
  LOCK_TIMEOUT_MINUTES,
  MAX_STATE_SIZE_MB,
  ENCRYPTION_REQUIRED_INFO
} = UIStrings.REPOSITORIES.EDITOR.TERRAFORM_STATE_BACKEND.ENCRYPTION;

/**
 * Terraform State Backend encryption configuration.
 *
 * Encryption is MANDATORY for all Terraform State Backend repositories.
 * State files contain sensitive infrastructure data and must be encrypted at rest.
 */
export default function TerraformStateBackendEncryptionConfiguration({parentMachine}) {
  const [parentState, sendParent] = parentMachine;
  const [encryptionKeyConfirm, setEncryptionKeyConfirm] = useState('');
  const [keyMismatch, setKeyMismatch] = useState(null);
  const [lockTimeoutError, setLockTimeoutError] = useState(null);
  const [maxStateSizeError, setMaxStateSizeError] = useState(null);

  // Note: encryption.enabled is set to true in defaultValues, so no useEffect is needed

  useEffect(() => {
    // Validate that encryption keys match
    const encryptionKey = parentState.context.data.terraformStateBackend?.encryption?.encryptionKey || '';
    if (encryptionKey && encryptionKeyConfirm) {
      setKeyMismatch(encryptionKey !== encryptionKeyConfirm ? ENCRYPTION_KEY_CONFIRM.MISMATCH_ERROR : null);
    } else {
      setKeyMismatch(null);
    }
  }, [parentState.context.data.terraformStateBackend?.encryption?.encryptionKey, encryptionKeyConfirm]);

  const handleEncryptionKeyChange = (value) => {
    sendParent({type: 'UPDATE', name: 'terraformStateBackend.encryption.encryptionKey', value});
    // Clear confirm key when primary key changes to force re-confirmation
    setEncryptionKeyConfirm('');
  };

  const handleConfirmKeyChange = (value) => {
    setEncryptionKeyConfirm(value);
  };

  const handleLockTimeoutChange = (value) => {
    const numValue = parseInt(value, 10);
    if (value === '' || isNaN(numValue)) {
      setLockTimeoutError('Please enter a valid number');
      return;
    }
    if (numValue < 1 || numValue > 1440) {
      setLockTimeoutError('Value must be between 1 and 1440');
      return;
    }
    setLockTimeoutError(null);
    sendParent({type: 'UPDATE', name: 'terraformStateBackend.lockTimeoutMinutes', value: numValue});
  };

  const handleMaxStateSizeChange = (value) => {
    const numValue = parseInt(value, 10);
    if (value === '' || isNaN(numValue)) {
      setMaxStateSizeError('Please enter a valid number');
      return;
    }
    if (numValue < 1 || numValue > 512) {
      setMaxStateSizeError('Value must be between 1 and 512');
      return;
    }
    setMaxStateSizeError(null);
    sendParent({type: 'UPDATE', name: 'terraformStateBackend.maxStateSizeMB', value: numValue});
  };

  return (
    <>
      <h2 className="nx-h2">{CAPTION}</h2>

      {/* Info alert explaining why encryption is mandatory */}
      <NxStatefulInfoAlert>
        {ENCRYPTION_REQUIRED_INFO}
      </NxStatefulInfoAlert>

      {/* Encryption key fields - always required */}
      <NxFormGroup
        label={ENCRYPTION_KEY.LABEL}
        sublabel={ENCRYPTION_KEY.SUBLABEL}
        isRequired
      >
        <NxTextInput
          type="password"
          {...FormUtils.fieldProps('terraformStateBackend.encryption.encryptionKey', parentState)}
          onChange={handleEncryptionKeyChange}
          autoComplete="new-password"
        />
      </NxFormGroup>

      <NxFormGroup
        label={ENCRYPTION_KEY_CONFIRM.LABEL}
        sublabel={ENCRYPTION_KEY_CONFIRM.SUBLABEL}
        isRequired
      >
        <NxTextInput
          type="password"
          value={encryptionKeyConfirm}
          onChange={handleConfirmKeyChange}
          validatable
          validationErrors={keyMismatch ? [keyMismatch] : null}
          autoComplete="new-password"
        />
      </NxFormGroup>

      <NxFormGroup
        label={LOCK_TIMEOUT_MINUTES.LABEL}
        sublabel={LOCK_TIMEOUT_MINUTES.SUBLABEL}
        isRequired
      >
        <NxTextInput
          {...FormUtils.fieldProps('terraformStateBackend.lockTimeoutMinutes', parentState)}
          onChange={handleLockTimeoutChange}
          type="number"
          validatable
          validationErrors={lockTimeoutError ? [lockTimeoutError] : null}
        />
      </NxFormGroup>

      <NxFormGroup
        label={MAX_STATE_SIZE_MB.LABEL}
        sublabel={MAX_STATE_SIZE_MB.SUBLABEL}
        isRequired
      >
        <NxTextInput
          {...FormUtils.fieldProps('terraformStateBackend.maxStateSizeMB', parentState)}
          onChange={handleMaxStateSizeChange}
          type="number"
          validatable
          validationErrors={maxStateSizeError ? [maxStateSizeError] : null}
        />
      </NxFormGroup>
    </>
  );
}
