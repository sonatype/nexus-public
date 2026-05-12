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

import React, { useState, useEffect } from 'react';
import { setDialogSetter, handleContinue, handleCancel } from '../../../router/unsavedChangesDialog';
import { NxButton, NxModal, NxWarningAlert } from '@sonatype/react-shared-components';

export default function UnsavedChangesModal() {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    setDialogSetter(setVisible);
  }, []);

  if (!visible) return null;

  return (
    <NxModal id="unsaved-modal" variant="narrow" onCancel={handleCancel}>
      <header className="nx-modal-header">
        <h2 className="nx-h2">Unsaved Changes</h2>
      </header>
      <div className="nx-modal-content">
        <NxWarningAlert className="nx-alert--modifier">
          <span>You have unsaved changes. Continuing will discard them.</span>
        </NxWarningAlert>
      </div>
      <footer className="nx-footer">
        <div className="nx-btn-bar">
          <NxButton onClick={handleCancel} id="unsaved-changes-modal-cancel-button">
            Cancel
          </NxButton>
          <NxButton variant="primary" id="unsaved-changes-modal-continue-button" onClick={handleContinue}>
            Continue
          </NxButton>
        </div>
      </footer>
    </NxModal>
  );
}
