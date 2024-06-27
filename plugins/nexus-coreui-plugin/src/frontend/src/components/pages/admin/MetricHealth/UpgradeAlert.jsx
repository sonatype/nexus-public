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
import React, {useState} from 'react';

import {NxButton, NxButtonBar, NxInfoAlert, useToggle, NxWarningAlert} from '@sonatype/react-shared-components';
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import {UpgradeAlertFunctions} from '../../../UpgradeAlert/UpgradeAlertHelper';
import UIStrings from '../../../../constants/UIStrings';
import UpgradeTriggerModal from '../../../UpgradeAlert/UpgradeTriggerModal';
const {UPGRADE_ALERT: {PENDING, WARN}} = UIStrings;

export default function UpgradeAlert() {

  const featureEnabled = ExtJS.useState(UpgradeAlertFunctions.featureEnabled);
  const hasUser = ExtJS.useState(UpgradeAlertFunctions.hasUser);
  const state = ExtJS.useState(UpgradeAlertFunctions.currentState);
  const hasPermission = ExtJS.usePermission(UpgradeAlertFunctions.checkPermissions);
  const [isOpen, dismiss] = useToggle(true);
  const [showModal, setShowModal] = useState(false);

  return (
  <>
    {state === 'needsUpgrade' && featureEnabled && hasUser && hasPermission &&
      <NxInfoAlert>
        <strong>{PENDING.STATUS_LABEL}&ensp;</strong>
        {PENDING.TEXT}
        <NxButtonBar>
          <NxButton variant="primary" onClick={() => setShowModal(true)}>
            {PENDING.FINALIZE_BUTTON}
          </NxButton>
        </NxButtonBar>
      </NxInfoAlert>
    }
    {state === 'needsUpgrade' && featureEnabled && hasUser && hasPermission &&
      <UpgradeTriggerModal showModal={showModal} setShowModal={setShowModal}/>
    }

    {state === 'versionMismatch' && featureEnabled && hasUser && hasPermission && isOpen &&
      <NxWarningAlert onClose={dismiss}>
        <strong>{WARN.LABEL}&ensp;</strong>
        {WARN.TEXT}
      </NxWarningAlert>
    }
    {state === 'nexusNeedsUpgrade1Year' && featureEnabled && hasUser && hasPermission &&
      <NxInfoAlert>
        <strong>{PENDING.OLDER_LABEL}&ensp;</strong>
        {PENDING.YEAR_OLD_TEXT}
      </NxInfoAlert>
     }
     {state === 'nexusNeeds18MonthsUpgrade' && featureEnabled && hasUser && hasPermission &&
      <NxInfoAlert>
        <strong>{PENDING.OLDER_LABEL}&ensp;</strong>
        {PENDING.TEXT_18_MONTHS}
      </NxInfoAlert>
     }
  </>
  )
}
