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
import {NxModal, NxH2, NxButton, NxButtonBar, NxFooter, NxFieldset, NxCheckbox, useToggle} from '@sonatype/react-shared-components';
import {useMachine} from '@xstate/react';

import UIStrings from '../../constants/UIStrings';
import UpgradeTriggerMachine from './UpgradeTriggerMachine';
import './UpgradeTriggerModal.scss';

const {TRIGGER_MODAL: { TITLE, DESCRIPTION, CHECKBOX_LABEL, CANCEL, CONTINUE}} = UIStrings;

export default function UpgradeTriggerModal({showModal, setShowModal}) {
  const [state, send] = useMachine(UpgradeTriggerMachine, {devTools: true});
  const [isChecked, onCheck] = useToggle(false);

  function saveModal() {
    send('SAVE_MODAL');
    setShowModal(false);
  }

  return <>
    {showModal && 
    <NxModal variant="narrow" className="upgrade-trigger-modal"
        onCancel={() => setShowModal(false)} aria-labelledby="zero-downtime-trigger-upgrade-modal">
      <header className="nx-modal-header">
        <NxH2 id="zero-downtime-trigger-upgrade-modal">{TITLE}</NxH2>
      </header>
      <section className="zdt-trigger-description">
        {DESCRIPTION}
      </section>
      <section className="trigger-upgrade-fieldset">
        <NxFieldset label="Acknowledge">
          <NxCheckbox onChange={onCheck} isChecked={isChecked} className="upgrade-trigger-checkbox">
            {CHECKBOX_LABEL}
          </NxCheckbox>
        </NxFieldset>
      </section>
      <NxFooter>
        <NxButtonBar>
          <NxButton onClick={() => setShowModal(false)}>{CANCEL}</NxButton>
          <NxButton variant="primary" disabled={!isChecked} onClick={() => saveModal()}>{CONTINUE}</NxButton>
        </NxButtonBar>
      </NxFooter>
    </NxModal>
    }
  </>
}
