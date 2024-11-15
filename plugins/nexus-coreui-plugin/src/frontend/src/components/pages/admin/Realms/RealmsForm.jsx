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
import {
  NxButton,
  NxH2,
  NxStatefulForm,
  NxTooltip,
  NxStatefulTransferList
} from '@sonatype/react-shared-components';
import {FormUtils} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

import RealmsMachine from './RealmsMachine';
import RealmsRemovalModal from './RealmsRemovalModal';

const {REALMS: {CONFIGURATION: LABELS}, SETTINGS} = UIStrings;

export default function RealmsForm() {
  const [state, send] = useMachine(RealmsMachine, {devTools: true});
  const {data, isInvalid, isPristine, validationErrors} = state.context;

  const showLocalRealmRemovalModal = 
    state.matches('showLocalRealmRemovalModal');

  const discard = () => send({type: 'RESET'});
  const checkLocalRealmRemoval = () => send({type: 'CHECK_LOCAL_REALM_REMOVAL'});
  const save = () => send({type: 'SAVE'});
  const closeModal = () => send({type: 'CLOSE'});

  const available =
    data?.available?.map(({ id, name: displayName }) => ({
      id,
      displayName,
    })) || [];

  return (
    <NxStatefulForm
        {...FormUtils.formProps(state, send)}
        validationErrors={validationErrors?.active || FormUtils.saveTooltip({isPristine, isInvalid})}
        additionalFooterBtns={
          <NxTooltip title={FormUtils.discardTooltip({isPristine})}>
            <NxButton
              type="button"
              className={isPristine && 'disabled'}
              onClick={discard}
            >
              {SETTINGS.DISCARD_BUTTON_LABEL}
            </NxButton>
          </NxTooltip>
        }
        onSubmit={checkLocalRealmRemoval}
    >
      <>
        <NxH2>{LABELS.SUB_LABEL}</NxH2>
        <NxStatefulTransferList
            id="realms_select"
            availableItemsLabel={LABELS.AVAILABLE_TITLE}
            selectedItemsLabel={LABELS.SELECTED_TITLE}
            allItems={available}
            selectedItems={data.active || []}
            onChange={FormUtils.handleUpdate('active', send)}
            showMoveAll
            allowReordering
        />
      </>
      {showLocalRealmRemovalModal &&
        <RealmsRemovalModal onClose={closeModal} onConfirm={save} />
      }
    </NxStatefulForm>
  );
}
