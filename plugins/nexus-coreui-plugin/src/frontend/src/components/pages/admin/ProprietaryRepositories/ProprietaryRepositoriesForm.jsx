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

import {FormUtils} from '@sonatype/nexus-ui-plugin';
import {
  NxButton,
  NxForm,
  NxTooltip,
  NxStatefulTransferList,
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';

import ProprietaryRepositoriesMachine from './ProprietaryRepositoriesMachine';

const {PROPRIETARY_REPOSITORIES: { CONFIGURATION: LABELS, HELP_TEXT }, SETTINGS} = UIStrings;

export default function ProprietaryRepositories() {
  const [current, send] = useMachine(ProprietaryRepositoriesMachine, {devTools: true});
  const {data: {enabledRepositories}, possibleRepos, isPristine, loadError, saveError, validationErrors} = current.context;
  const isLoading = current.matches('loading') || current.matches('loadingPossibleRepos');
  const isSaving = current.matches('saving')
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

  const repositories = possibleRepos?.map((it) => ({id: it.id, displayName: it.name})) || [];

  return <NxForm
      loading={isLoading}
      loadError={loadError}
      doLoad={retry}
      onSubmit={save}
      submitError={saveError}
      submitMaskState={isSaving ? false : null}
      submitBtnText={SETTINGS.SAVE_BUTTON_LABEL}
      validationErrors={FormUtils.saveTooltip({isPristine, isInvalid})}
      additionalFooterBtns={<>
        <NxTooltip title={FormUtils.discardTooltip({isPristine})}>
          <NxButton type="button" className={isPristine && 'disabled'} onClick={discard}>
            {SETTINGS.DISCARD_BUTTON_LABEL}
          </NxButton>
        </NxTooltip>
      </>}>
    {() => <>
      <p>{HELP_TEXT}</p>
      <NxStatefulTransferList
          id="proprietary_repositories_select"
          availableItemsLabel={LABELS.AVAILABLE_TITLE}
          selectedItemsLabel={LABELS.SELECTED_TITLE}
          allItems={repositories}
          selectedItems={enabledRepositories}
          onChange={FormUtils.handleUpdate('enabledRepositories', send)}
          showMoveAll
      />
    </>}
  </NxForm>;
}
