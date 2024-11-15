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
import {useActor} from '@xstate/react';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxModal,
  NxFooter,
  NxButtonBar,
  NxH2
} from '@sonatype/react-shared-components';
import {faQuestionCircle} from '@fortawesome/free-solid-svg-icons';
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import UIStrings from '../../../../../constants/UIStrings';

const {
  NODES: {READ_ONLY},
  SETTINGS: {CANCEL_BUTTON_LABEL}
} = UIStrings;

const getLabels = (frozen, frozenManually) => {
  if (frozenManually) {
    return READ_ONLY.DISABLE;
  } else if (frozen) {
    return READ_ONLY.DISABLE.FORCIBLY;
  } else {
    return READ_ONLY.ENABLE;
  }
};

export default function FreezeActionModal({service}) {
  const [state, send] = useActor(service);

  const {frozen} = state.context;

  const frozenManually = ExtJS.state().getValue('frozenManually');

  const close = () => send({type: 'CANCEL'});
  const EVENT_TYPE = !frozenManually && frozen ? 'FORCE_RELEASE' : 'TOGGLE';
  const action = () => send(EVENT_TYPE);

  const LABELS = getLabels(frozen, frozenManually);

  return (
    <NxModal onCancel={close} aria-labelledby="modal-header-text">
      <NxModal.Header id="modal-header-text">
        <NxH2>
          <NxFontAwesomeIcon icon={faQuestionCircle} />
          <span>{LABELS.TITLE}</span>
        </NxH2>
      </NxModal.Header>
      <NxModal.Content>{LABELS.MESSAGE}</NxModal.Content>
      <NxFooter>
        <NxButtonBar>
          <NxButton onClick={action} variant="primary">{LABELS.BUTTON}</NxButton>
          <NxButton onClick={close}>{CANCEL_BUTTON_LABEL}</NxButton>
        </NxButtonBar>
      </NxFooter>
    </NxModal>
  );
}
