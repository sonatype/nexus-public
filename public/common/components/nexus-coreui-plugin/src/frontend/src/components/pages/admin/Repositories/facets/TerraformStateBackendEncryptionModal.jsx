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
import axios from 'axios';

import {
  NxModal,
  NxButton,
  NxFormGroup,
  NxTextInput,
  NxCheckbox,
  NxFieldset,
  NxWarningAlert
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';

const {
  MODAL_TITLE,
  ENCRYPTION_KEY_LABEL,
  ENCRYPTION_KEY_SUBLABEL,
  ENCRYPTION_KEY_CONFIRM_LABEL,
  ENCRYPTION_KEY_CONFIRM_SUBLABEL,
  KEY_MISMATCH_ERROR,
  REENCRYPT_LABEL,
  REENCRYPT_DESCRIPTION,
  EXISTING_KEY_MESSAGE,
  UPDATE_BUTTON,
  CANCEL_BUTTON,
  LOADING_ERROR,
  UPDATE_SUCCESS,
  UPDATE_ERROR
} = UIStrings.REPOSITORIES.EDITOR.TERRAFORM_STATE_BACKEND.ENCRYPTION_MODAL;

export default function TerraformStateBackendEncryptionModal({
  isOpen,
  onClose,
  repositoryName
}) {
  const [newEncryptionKey, setNewEncryptionKey] = useState('');
  const [confirmEncryptionKey, setConfirmEncryptionKey] = useState('');
  const [reencryptAll, setReencryptAll] = useState(false);
  const [keyMismatch, setKeyMismatch] = useState(null);
  const [isUpdating, setIsUpdating] = useState(false);
  const [updateError, setUpdateError] = useState(null);

  useEffect(() => {
    if (newEncryptionKey && confirmEncryptionKey) {
      setKeyMismatch(newEncryptionKey !== confirmEncryptionKey ? KEY_MISMATCH_ERROR : null);
    } else {
      setKeyMismatch(null);
    }
  }, [newEncryptionKey, confirmEncryptionKey]);

  useEffect(() => {
    if (!isOpen) {
      // Reset state when modal closes
      setNewEncryptionKey('');
      setConfirmEncryptionKey('');
      setReencryptAll(false);
      setKeyMismatch(null);
      setUpdateError(null);
      setIsUpdating(false);
    }
  }, [isOpen]);

  const handleUpdate = async () => {
    if (keyMismatch) {
      return;
    }

    if (!newEncryptionKey || !confirmEncryptionKey) {
      setUpdateError(ENCRYPTION_KEY_LABEL + ' is required');
      return;
    }

    setIsUpdating(true);
    setUpdateError(null);

    try {
      // API endpoint to update encryption key
      const response = await axios.post(
        `/service/rest/v1/repositories/${repositoryName}/terraform-backend/encryption`,
        {
          encryptionKey: newEncryptionKey,
          reencryptAll: reencryptAll
        }
      );

      setIsUpdating(false);
      onClose(true);
    } catch (error) {
      setIsUpdating(false);
      setUpdateError(error.response?.data?.message || UPDATE_ERROR);
    }
  };

  const isFormValid = newEncryptionKey &&
    confirmEncryptionKey &&
    !keyMismatch &&
    !isUpdating;

  if (!isOpen) {
    return null;
  }

  return (
    <NxModal
      onCancel={() => onClose(false)}
      aria-label={MODAL_TITLE}
    >
      <NxModal.Header>
        <h2 className="nx-h2">{MODAL_TITLE}</h2>
      </NxModal.Header>

      <NxModal.Body>
        <NxWarningAlert>
          {EXISTING_KEY_MESSAGE}
        </NxWarningAlert>

        <NxFormGroup
          label={ENCRYPTION_KEY_LABEL}
          sublabel={ENCRYPTION_KEY_SUBLABEL}
          isRequired
        >
          <NxTextInput
            type="password"
            value={newEncryptionKey}
            onChange={setNewEncryptionKey}
            autoComplete="new-password"
          />
        </NxFormGroup>

        <NxFormGroup
          label={ENCRYPTION_KEY_CONFIRM_LABEL}
          sublabel={ENCRYPTION_KEY_CONFIRM_SUBLABEL}
          isRequired
        >
          <NxTextInput
            type="password"
            value={confirmEncryptionKey}
            onChange={setConfirmEncryptionKey}
            validatable
            validationErrors={keyMismatch ? [keyMismatch] : null}
            autoComplete="new-password"
          />
        </NxFormGroup>

        <NxFieldset label={REENCRYPT_LABEL}>
          <NxCheckbox
            isChecked={reencryptAll}
            onChange={setReencryptAll}
          >
            {REENCRYPT_DESCRIPTION}
          </NxCheckbox>
        </NxFieldset>

        {updateError && (
          <NxWarningAlert>
            {updateError}
          </NxWarningAlert>
        )}
      </NxModal.Body>

      <NxModal.Footer>
        <NxButton
          variant="tertiary"
          onClick={() => onClose(false)}
          disabled={isUpdating}
        >
          {CANCEL_BUTTON}
        </NxButton>
        <NxButton
          variant="primary"
          onClick={handleUpdate}
          disabled={!isFormValid}
        >
          {UPDATE_BUTTON}
        </NxButton>
      </NxModal.Footer>
    </NxModal>
  );
}
