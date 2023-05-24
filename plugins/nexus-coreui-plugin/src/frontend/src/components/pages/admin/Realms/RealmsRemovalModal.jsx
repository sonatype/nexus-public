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
import React, {useEffect, useRef, useState} from 'react';

import {
  NxModal,
  NxButton,
  NxH2,
  NxFooter,
  NxButtonBar,
  NxWarningAlert,
  NxFormGroup,
  NxTextInput,
  nxTextInputStateHelpers,
  hasValidationErrors,
  NxForm
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';

const {initialState, userInput} = nxTextInputStateHelpers;

const {
  REALMS: {
    LOCAL_REALM_REMOVAL_MODAL: {
      HEADER,
      WARNING,
      CONFIRM_BUTTON,
      ACKNOWLEDGEMENT: {
        STRING,
        VALIDATION_ERROR,
        LABEL,
        SUBLABEL,
        PLACEHOLDER
      }
    }
  },
  SETTINGS: {CANCEL_BUTTON_LABEL}
} = UIStrings;

export default function RealmsRemovalModal({onClose, onConfirm}) {
  const [state, setState] = useState(initialState(''));

  const ref = useRef();
  useEffect(() => {
    const input = ref.current.children[0].children[0];
    input.focus();
  }, []);

  const validator = (val) => (val === STRING ? null : VALIDATION_ERROR);

  const handleChange = (val) => setState(userInput(validator, val));

  const isSaveAllowed = !state.isPristine && !hasValidationErrors(state.validationErrors);

  const handleEnter = (e) => isSaveAllowed && e.key === 'Enter' && onConfirm();

  return (
    <NxModal
      className="nxrm-local-realm-removal-modal"
      onCancel={onClose}
      aria-labelledby="modal-header"
      variant="narrow"
    >
      <NxModal.Header>
        <NxH2 id="modal-header">{HEADER}</NxH2>
      </NxModal.Header>
      <NxModal.Content>
        <NxWarningAlert>{WARNING}</NxWarningAlert>
        <NxForm.RequiredFieldNotice />
        <NxFormGroup label={LABEL} sublabel={SUBLABEL} isRequired={true}>
          <NxTextInput
            {...state}
            onChange={handleChange}
            onKeyDown={handleEnter}
            validatable={true}
            placeholder={PLACEHOLDER}
            ref={ref}
          />
        </NxFormGroup>
      </NxModal.Content>
      <NxFooter>
        <NxButtonBar>
          <NxButton onClick={onClose}>{CANCEL_BUTTON_LABEL}</NxButton>
          <NxButton onClick={onConfirm} variant="primary" disabled={!isSaveAllowed}>
            {CONFIRM_BUTTON}
          </NxButton>
        </NxButtonBar>
      </NxFooter>
    </NxModal>
  );
}
