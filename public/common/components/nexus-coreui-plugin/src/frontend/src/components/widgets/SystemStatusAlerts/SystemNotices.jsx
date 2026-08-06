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
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import {
  NxSystemNotice,
} from '@sonatype/react-shared-components';
import TelemetryWarningBanner from '../../shared/telemetry/TelemetryWarningBanner';

import './SystemNotices.scss';
import UpgradeAlert from './UpgradeAlert/UpgradeAlert';
import LicenseExpiryAlert from './LicenseExpiryAlert/LicenseExpiryAlert';
import RecoveryModeAlert from './RecoveryModeAlert/RecoveryModeAlert';

export default function SystemNotices ({isPreviewUI = false} = {}) {
  const recoveryModeEnabled = ExtJS.useState(() => ExtJS.state().getValue('recovery.mode.enabled'));

  // In Preview UI the recovery banner is rendered separately by the Radix
  // SystemAlerts host, so skip the classic RecoveryModeAlert here to avoid a
  // duplicate. All other banners render as before.
  const showClassicRecoveryAlert = recoveryModeEnabled && !isPreviewUI;

  return <NxSystemNotice.Container className="nxrm-system-notices">
    <TelemetryWarningBanner />

    {showClassicRecoveryAlert ? <RecoveryModeAlert /> : <UpgradeAlert />}

    <LicenseExpiryAlert />
  </NxSystemNotice.Container>;
}
