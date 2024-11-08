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
  NxH2,
  NxTile,
  NxButton,
  NxButtonBar,
  NxFieldset,
  NxFormRow,
  NxModal,
  NxTooltip,
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';

import {ExtJS} from '@sonatype/nexus-ui-plugin';
const {
  USERS: {TOKEN: LABELS},
} = UIStrings;

import ConfirmAdminPasswordForm from './ConfirmAdminPasswordForm';

export default function UsersToken({service}) {
  const [current, send] = useActor(service);
  const activeCapabilities =
    ExtJS.state().getValue('capabilityActiveTypes') || [];
  const isCapabilityActive = activeCapabilities.includes('usertoken');
  const confirmingAdminPassword = current.matches(
    'resetToken.confirmAdminPassword'
  );
  const canReset = ExtJS.checkPermission('nexus:usertoken-settings:update');

  const reset = () => {
    if (canReset) {
      send({type: 'RESET_TOKEN'});
    }
  };

  return (
    <>
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>{LABELS.LABEL}</NxH2>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxTile.Content>
        <NxFieldset
          label={
            isCapabilityActive ? LABELS.RESET_USER_TOKEN : LABELS.USER_TOKEN
          }
          sublabel={
            isCapabilityActive
              ? LABELS.ACTIVE_FEATURE
              : LABELS.REQUIRE_ENABLE_FEATURE
          }
          isRequired
        >
          <NxFormRow>
            {isCapabilityActive && (
              <NxTooltip
                title={!canReset && UIStrings.PERMISSION_ERROR}
                placement="bottom"
              >
                <NxButtonBar>
                  <NxButton
                    type="button"
                    variant="tertiary"
                    onClick={reset}
                    className={!canReset ? 'disabled' : ''}
                  >
                    {LABELS.RESET_USER_TOKEN}
                  </NxButton>
                </NxButtonBar>
              </NxTooltip>
            )}
          </NxFormRow>
        </NxFieldset>
      </NxTile.Content>
      {confirmingAdminPassword && (
        <NxModal aria-labelledby="modal-form-header" variant="narrow">
          <ConfirmAdminPasswordForm
            actor={current.children.confirmAdminPasswordMachine}
            title={LABELS.RESET_USER_TOKEN}
            text={LABELS.TEXT}
            confirmLabel={LABELS.AUTHENTICATE}
          />
        </NxModal>
      )}
    </>
  );
}
