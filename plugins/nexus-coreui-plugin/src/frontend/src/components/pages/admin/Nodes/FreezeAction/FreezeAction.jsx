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
import {useMachine} from '@xstate/react';
import {NxButton, NxFontAwesomeIcon, NxStatefulSubmitMask} from '@sonatype/react-shared-components';
import {faBinoculars} from '@fortawesome/free-solid-svg-icons';
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import UIStrings from '../../../../../constants/UIStrings';
import FreezeMachine from './FreezeMachine';
import FreezeActionModal from './FreezeActionModal';

const {
  NODES: {READ_ONLY}
} = UIStrings;

export default function FreezeAction() {
  const [state, send, service] = useMachine(FreezeMachine, {
    context: {
      frozen: ExtJS.state().getValue('frozen')
    },
    devTools: true
  });

  const {frozen} = state.context;

  const openModal = () => send({type: 'CONFIRM'});

  const isConfirming = state.matches('confirm');
  const isToggling = state.matches('toggling') || state.matches('releasing');
  const isSuccess = state.matches('success');

  const togglingMessage = frozen ? READ_ONLY.DISABLE.LOADING : READ_ONLY.ENABLE.LOADING;
  const toggleButton = frozen ? READ_ONLY.DISABLE.BUTTON : READ_ONLY.ENABLE.BUTTON;

  return (
    <>
      <NxButton variant="tertiary" onClick={openModal}>
        <NxFontAwesomeIcon icon={faBinoculars} />
        <span>{toggleButton}</span>
      </NxButton>

      {isConfirming && <FreezeActionModal service={service} />}

      {(isToggling || isSuccess) && (
        <NxStatefulSubmitMask success={isSuccess} message={togglingMessage} />
      )}
    </>
  );
}
