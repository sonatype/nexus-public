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
import {
  NxButton,
  NxButtonBar,
  NxErrorAlert,
  NxFooter,
  NxFormGroup,
  NxFormSelect,
  NxH2,
  NxModal,
  NxSubmitMask,
  NxTextInput,
  NxWarningAlert,
} from '@sonatype/react-shared-components';
import UIStrings from '../../../../constants/UIStrings';
import {EXPIRATION_OPTIONS} from './ServiceAccountTokensHelper';

const LABELS = UIStrings.SERVICE_ACCOUNT_TOKENS.CREATE_MODAL;

const NAME_PATTERN = /^[a-zA-Z0-9_-]+$/;

function validateName(name, existingNames) {
  const trimmed = name.trim();
  if (name.length > 0 && !NAME_PATTERN.test(trimmed)) return LABELS.NAME_INVALID_CHARS_ERROR;
  if (trimmed.length > 0 && existingNames.includes(trimmed)) {
    return LABELS.NAME_DUPLICATE_ERROR(trimmed);
  }
  return null;
}

export default function ServiceAccountTokensCreateModal({onClose, onCreate, roles, rolesError = null, existingNames = [], isCreating = false}) {
  const [name, setName] = useState('');
  const [roleId, setRoleId] = useState('');
  const [expirationDays, setExpirationDays] = useState(String(EXPIRATION_OPTIONS[0].value));
  const [description, setDescription] = useState('');

  const parsedExpiration = parseInt(expirationDays, 10);
  const nameError = validateName(name, existingNames);
  const isValid = name.trim().length > 0 && roleId.length > 0 && !nameError && !rolesError;
  const isNeverExpires = parsedExpiration === -1;

  useEffect(() => {
    if (isNeverExpires) {
      const alertEl = document.getElementById('nxrm-sa-never-expires-warning');
      if (alertEl) {
        alertEl.scrollIntoView({behavior: 'smooth', block: 'nearest'});
        // Move focus so screen readers announce the warning. tabIndex=-1 keeps
        // it out of the tab order while allowing programmatic focus.
        alertEl.focus({preventScroll: true});
      }
    }
  }, [isNeverExpires]);

  function handleSubmit() {
    if (!isValid || isCreating) return;
    const payload = {
      name: name.trim(),
      roleId,
      description: description.trim(),
    };
    if (!isNeverExpires) {
      payload.expirationDays = parsedExpiration;
    }
    onCreate(payload);
  }

  const submitDisabled = !isValid || isCreating;

  return (
    <NxModal onCancel={isCreating ? undefined : onClose} aria-labelledby="create-sa-token-modal">
      <NxModal.Header>
        <NxH2 id="create-sa-token-modal">{LABELS.TITLE}</NxH2>
      </NxModal.Header>
      <NxModal.Content>
        <NxFormGroup label={LABELS.NAME_LABEL} sublabel={LABELS.NAME_DESCRIPTION} isRequired>
          <NxTextInput
            value={name}
            onChange={(v) => setName(v.slice(0, 128))}
            validatable={true}
            isPristine={name.length === 0}
            validationErrors={nameError ? [nameError] : null}
            inputAttributes={{maxLength: 128}}
          />
        </NxFormGroup>
        <NxFormGroup label={LABELS.ROLE_LABEL} sublabel={LABELS.ROLE_DESCRIPTION} isRequired>
          <NxFormSelect value={roleId} onChange={setRoleId} disabled={!!rolesError}>
            <option value="">{LABELS.ROLE_PLACEHOLDER}</option>
            {roles.map((role) => (
              <option key={role.id} value={role.id}>{role.name}</option>
            ))}
          </NxFormSelect>
        </NxFormGroup>
        {rolesError && (
          <NxErrorAlert>{rolesError}</NxErrorAlert>
        )}
        <NxFormGroup label={LABELS.EXPIRATION_LABEL} sublabel={LABELS.EXPIRATION_DESCRIPTION} isRequired>
          <NxFormSelect value={expirationDays} onChange={setExpirationDays}>
            {EXPIRATION_OPTIONS.map((opt) => (
              <option key={opt.value} value={String(opt.value)}>{opt.label}</option>
            ))}
          </NxFormSelect>
        </NxFormGroup>
        <div className="nxrm-sa-description-wrapper">
          <NxFormGroup label={LABELS.DESCRIPTION_LABEL} sublabel={LABELS.DESCRIPTION_DESCRIPTION}>
            <NxTextInput
              value={description}
              onChange={(v) => setDescription(v.slice(0, 256))}
              type="textarea"
              inputAttributes={{maxLength: 256}}
            />
          </NxFormGroup>
          <span className="nxrm-sa-char-counter">{description.length} / 256</span>
        </div>
        {isNeverExpires && (
          <NxWarningAlert
            id="nxrm-sa-never-expires-warning"
            tabIndex={-1}
            className="nxrm-sa-sr-focus-target"
          >
            {LABELS.NEVER_EXPIRES_WARNING}
          </NxWarningAlert>
        )}
      </NxModal.Content>
      <NxFooter>
        <NxButtonBar>
          <NxButton type="button" onClick={onClose} disabled={isCreating}>
            {LABELS.CANCEL_BUTTON}
          </NxButton>
          <NxButton
            type="button"
            variant="primary"
            disabled={submitDisabled}
            onClick={handleSubmit}
          >
            {LABELS.CREATE_BUTTON}
          </NxButton>
        </NxButtonBar>
      </NxFooter>
      {isCreating && <NxSubmitMask message={LABELS.CREATING_MASK} />}
    </NxModal>
  );
}
