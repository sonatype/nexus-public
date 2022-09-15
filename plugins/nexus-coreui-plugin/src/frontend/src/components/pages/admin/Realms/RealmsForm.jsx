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
  NxForm,
  NxTooltip,
  NxStatefulTransferList,
  NxH2,
} from '@sonatype/react-shared-components';
import {FormUtils} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

import RealmsMachine from './RealmsMachine';

const {
  REALMS: { CONFIGURATION: LABELS, MESSAGES },
  SETTINGS,
} = UIStrings;

export default function RealmsForm() {
  const [current, send] = useMachine(RealmsMachine, {
    devTools: true,
  });
  const { data, validationErrors, isPristine, saveError, loadError } =
    current.context;
  const isLoading = current.matches('loading');
  const isSaving = current.matches('saving');
  const isInvalid = FormUtils.isInvalid(validationErrors);

  function discard() {
    send('RESET');
  }

  function save() {
    send('SAVE');
  }

  function retry() {
    send('RETRY');
  }

  const available =
    data?.available?.map(({ id, name: displayName }) => ({
      id,
      displayName,
    })) || [];

  function errorMessages() {
    if (validationErrors.isActiveListEmpty) {
      return MESSAGES.NO_REALMS_CONFIGURED;
    }
    return FormUtils.saveTooltip({
      isPristine,
      isInvalid,
    });
  }

  return (
    <NxForm
      loading={isLoading}
      loadError={loadError}
      doLoad={retry}
      onSubmit={save}
      submitError={saveError}
      submitMaskState={isSaving ? false : null}
      submitBtnText={SETTINGS.SAVE_BUTTON_LABEL}
      validationErrors={errorMessages()}
      additionalFooterBtns={
        <NxTooltip title={FormUtils.discardTooltip({ isPristine })}>
          <NxButton
            type="button"
            className={isPristine && 'disabled'}
            onClick={discard}
          >
            {SETTINGS.DISCARD_BUTTON_LABEL}
          </NxButton>
        </NxTooltip>
      }
    >
      {() => (
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
      )}
    </NxForm>
  );
}
