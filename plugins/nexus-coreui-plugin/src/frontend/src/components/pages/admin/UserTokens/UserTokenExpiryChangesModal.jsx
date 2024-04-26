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

import {NxButton, NxButtonBar, NxFooter, NxH2, NxModal, NxWarningAlert} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';

const {USER_TOKEN_CONFIGURATION: {
  USER_TOKEN_EXPIRY_CONFIRMATION:{
    CAPTION,
    WARNING,
    CONFIRM_BUTTON,
    CANCEL_BUTTON
  }}} = UIStrings;

export default function UserTokenExpiryChangesModal({onClose, onConfirm, expirationEnabled}) {

  return (
    <NxModal aria-label="user-token-expiry-confirmation" variant="narrow">
      <NxModal.Header>
        <NxH2>{CAPTION}</NxH2>
      </NxModal.Header>
      <NxModal.Content>
        <NxWarningAlert role="alert">{WARNING(expirationEnabled)}</NxWarningAlert>
      </NxModal.Content>
      <NxFooter>
        <NxButtonBar>
          <NxButton onClick={onClose}>{CANCEL_BUTTON}</NxButton>
          <NxButton onClick={onConfirm} variant="primary">{CONFIRM_BUTTON}</NxButton>
        </NxButtonBar>
      </NxFooter>
    </NxModal>
  )
}
