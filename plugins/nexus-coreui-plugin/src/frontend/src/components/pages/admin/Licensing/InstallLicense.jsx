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
  FormUtils,
  ExtJS,
  Permissions,
} from '@sonatype/nexus-ui-plugin';
import {
  NxTile,
  NxH2,
  NxP,
  NxFormGroup,
  NxInfoAlert,
  NxFileUpload,
  NxErrorAlert,
  NxLoadWrapper,
  NxFooter,
  NxButtonBar,
  NxButton,
} from '@sonatype/react-shared-components';

import LicenseAgreementModal from './LicenseAgreementModal';
import UIStrings from '../../../../constants/UIStrings';

const {LICENSING: LABELS, SETTINGS} = UIStrings;

export default function InstallLicense({service}) {
  const [state, send] = useActor(service);
  const canEdit = ExtJS.checkPermission(Permissions.LICENSING.CREATE);

  const {
    data: {files},
    saveError,
    isPristine,
    validationErrors,
  } = state.context;

  const isValid = !FormUtils.isInvalid(validationErrors);
  const isSaving = state.matches('saving');
  const isShowAgreementModal = state.matches('agreement');

  const showAgreementModal = () => send({type: 'SHOW_AGREEMENT_MODAL'});
  const reset = () => send({type: 'RESET'});
  const onAccept = () => send({type: 'ACCEPT'});
  const onDecline = () => send({type: 'DECLINE'});

  const setFiles = (fileList) => {
    if (canEdit) {
      send({type: 'SET_FILES', data: {files: fileList}});
    }
  }

  return <>
    <NxTile>
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>{LABELS.SECTIONS.INSTALL}</NxH2>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxTile.Content>
        {!canEdit && <NxInfoAlert>{SETTINGS.READ_ONLY.WARNING}</NxInfoAlert>}
        {canEdit &&
            <NxLoadWrapper loading={isSaving} retryHandler={()=>{}}>
              <NxP>{LABELS.INSTALL.DESCRIPTION}</NxP>
              <NxFormGroup
                  label={LABELS.INSTALL.LABEL}
                  isRequired
              >
                <NxFileUpload
                    files={files}
                    onChange={setFiles}
                    disabled={!canEdit}
                    isPristine={isPristine}
                    isRequired
                />
              </NxFormGroup>
              <NxFooter>
                {saveError &&
                    <NxErrorAlert onClose={reset}>
                      {LABELS.INSTALL.MESSAGES.ERROR(saveError)}
                    </NxErrorAlert>
                }
                <NxButtonBar>
                  <NxButton
                      variant="primary"
                      onClick={showAgreementModal}
                      disabled={isSaving || !isValid || saveError}
                  >
                    {LABELS.INSTALL.BUTTONS.UPLOAD}
                  </NxButton>
                </NxButtonBar>
              </NxFooter>
            </NxLoadWrapper>
        }
      </NxTile.Content>
    </NxTile>
    {isShowAgreementModal &&
        <LicenseAgreementModal
            onAccept={onAccept}
            onDecline={onDecline}
        />
    }
  </>;
}
