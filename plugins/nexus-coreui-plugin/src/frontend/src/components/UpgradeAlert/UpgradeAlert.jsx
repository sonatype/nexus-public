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
import PropTypes from "prop-types";
import axios from 'axios';

import {NxButton, NxButtonBar, NxErrorAlert, NxInfoAlert, NxSuccessAlert, NxLoadingSpinner} from '@sonatype/react-shared-components';
import {ExtJS} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../constants/UIStrings';
import {UpgradeAlertFunctions} from './UpgradeAlertHelper';

import './UpgradeAlert.scss';
import UpgradeTriggerModal from './UpgradeTriggerModal';

const {UPGRADE_ALERT: { PENDING, PROGRESS, ERROR, COMPLETE}} = UIStrings;

export default function UpgradeAlert({onClose}) {
  const hasUser = ExtJS.useState(UpgradeAlertFunctions.hasUser);
  const state = ExtJS.useState(UpgradeAlertFunctions.currentState);
  const message = ExtJS.useState(UpgradeAlertFunctions.message);
  const hasPermission = ExtJS.usePermission(UpgradeAlertFunctions.checkPermissions);
  const [showModal, setShowModal] = useState(false);

  function dismissAlert() {
    onClose();
    axios.delete('/service/rest/clustered/upgrade-database-schema')
  }

  return <>
    {state === 'needsUpgrade' && hasUser && hasPermission &&
      <NxInfoAlert className="nx-upgrade-alert">
        <NxButtonBar className="upgrade-alert-btn-bar">
          <div className="alert-text">
            <div><strong>{PENDING.LABEL}&ensp;</strong></div>
            <div>{PENDING.TEXT}</div>
          </div>
          <NxButton variant="primary" onClick={() => setShowModal(true)}>
            {PENDING.FINALIZE_BUTTON}
          </NxButton>
        </NxButtonBar>
      </NxInfoAlert>
    }
    {state === 'needsUpgrade' && hasUser && hasPermission &&
      <UpgradeTriggerModal showModal={showModal} setShowModal={setShowModal}/>
    }

    {state === 'versionMismatch' && hasUser && hasPermission &&
      <NxErrorAlert className="nx-upgrade-alert">
        <strong>{ERROR.LABEL}&ensp;(1) </strong>
        {ERROR.TEXT_MISMATCH} {ERROR.CONTACT_SUPPORT}
      </NxErrorAlert>
    }
    {state === 'nexusUpgradeInProgress' && hasUser && hasPermission &&
      <NxInfoAlert className="nx-upgrade-alert upgrade-in-progress-alert">
        <NxLoadingSpinner><strong>{PROGRESS.LABEL}</strong></NxLoadingSpinner>
      </NxInfoAlert>
    }
    {state === 'nexusUpgradeError' && hasUser && hasPermission &&
      <NxErrorAlert className="nx-upgrade-alert">
        <strong>{ERROR.LABEL}&ensp;[{message}]</strong> {ERROR.TEXT} {ERROR.CONTACT_SUPPORT}
      </NxErrorAlert>
    }
    {state === 'nexusUpgradeComplete' && hasUser && hasPermission &&
      <NxSuccessAlert className="nx-upgrade-alert">
        <NxButtonBar className="upgrade-alert-btn-bar">
          <div className="alert-text">
            <div><strong>{COMPLETE.LABEL}</strong>&ensp;</div>
            <div>{COMPLETE.TEXT}</div>
          </div>
          <NxButton variant="secondary" onClick={dismissAlert}>
            {COMPLETE.DISMISS}
          </NxButton>
        </NxButtonBar>
      </NxSuccessAlert>
    }
  </>
}

UpgradeAlert.propTypes = {
  onClose: PropTypes.func
}
