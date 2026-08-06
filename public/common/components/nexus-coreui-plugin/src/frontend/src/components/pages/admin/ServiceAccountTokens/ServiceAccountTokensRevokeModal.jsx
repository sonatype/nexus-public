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
import React, {useEffect, useState} from 'react';
import {
  NxButton,
  NxButtonBar,
  NxFooter,
  NxFormGroup,
  NxH2,
  NxModal,
  NxSubmitMask,
  NxTextInput,
  NxWarningAlert,
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';

const LABELS = UIStrings.SERVICE_ACCOUNT_TOKENS.REVOKE_MODAL;

export default function ServiceAccountTokensRevokeModal({tokenName, onConfirm, onClose, isRevoking = false}) {
  const [confirmation, setConfirmation] = useState('');
  const isPristine = confirmation.length === 0;
  const isMatch = confirmation === tokenName;
  const validationErrors = !isPristine && !isMatch ? LABELS.VALIDATION_ERROR : null;
  const submitDisabled = !isMatch || isRevoking;

  useEffect(() => {
    // Move focus to the warning when the modal opens so screen readers announce it.
    const alertEl = document.getElementById('nxrm-sa-revoke-warning');
    if (alertEl) {
      alertEl.focus({preventScroll: true});
    }
  }, []);

  function handleSubmit() {
    if (submitDisabled) return;
    onConfirm();
  }

  return (
    <NxModal
      onCancel={isRevoking ? undefined : onClose}
      variant="narrow"
      aria-labelledby="revoke-sa-token-modal"
    >
      <NxModal.Header>
        <NxH2 id="revoke-sa-token-modal">{LABELS.TITLE}</NxH2>
      </NxModal.Header>
      <NxModal.Content>
        <NxWarningAlert
          id="nxrm-sa-revoke-warning"
          tabIndex={-1}
          className="nxrm-sa-sr-focus-target"
        >
          {LABELS.WARNING(tokenName)}
        </NxWarningAlert>
        <NxFormGroup label={LABELS.LABEL} sublabel={LABELS.SUBLABEL(tokenName)} isRequired>
          <NxTextInput
            id="revokeConfirmationString"
            name="revokeConfirmationString"
            value={confirmation}
            onChange={setConfirmation}
            validatable
            isPristine={isPristine}
            validationErrors={validationErrors}
          />
        </NxFormGroup>
      </NxModal.Content>
      <NxFooter>
        <NxButtonBar>
          <NxButton type="button" onClick={onClose} disabled={isRevoking}>
            {LABELS.CANCEL_BUTTON}
          </NxButton>
          <NxButton
            type="button"
            variant="error"
            disabled={submitDisabled}
            onClick={handleSubmit}
          >
            {LABELS.REVOKE_BUTTON}
          </NxButton>
        </NxButtonBar>
      </NxFooter>
      {isRevoking && <NxSubmitMask message={LABELS.REVOKING_MASK} />}
    </NxModal>
  );
}
