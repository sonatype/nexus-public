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
import { useCurrentStateAndParams, useRouter } from '@uirouter/react';
import { ExtJS } from '../../../../interface/ExtJS';

import { SystemAlert } from './SystemAlert';

/** UI-Router state name for the Preview UI Recovery Mode page. */
const RECOVERY_MODE_STATE = 'preview.admin.support.recoverymode';

const LABELS = {
  TITLE: 'Recovery Mode Enabled',
  MESSAGE:
    'While Recovery Mode is on, data repair conflicting tasks are blocked to protect data consistency.',
  VIEW_DETAILS: 'View Details',
};

/**
 * RecoveryModeAlert - Self-gating system banner for the Preview UI.
 *
 * Renders only for administrators while recovery mode is active. State comes
 * from the `recovery.mode.enabled` app-state key (contributed by the backend),
 * read reactively so the banner appears/disappears as the mode toggles.
 *
 * Designed to be composed inside the SystemAlerts host alongside other
 * self-gating alerts, mirroring the classic SystemNotices pattern.
 */
export function RecoveryModeAlert(): React.ReactElement | null {
  const user = ExtJS.useUser ? ExtJS.useUser() : undefined;
  const isAdmin = !!user?.administrator;

  const recoveryModeEnabled = ExtJS.useState?.(() =>
    ExtJS.state()?.getValue?.('recovery.mode.enabled')
  );

  const router = useRouter();

  // Use UI-Router's active state (not raw hash matching) to know if we're on
  // the Recovery Mode page; robust to query params and route renames.
  const { state } = useCurrentStateAndParams();
  const isOnRecoveryModePage = state?.name === RECOVERY_MODE_STATE;

  if (!isAdmin || !recoveryModeEnabled) {
    return null;
  }

  return (
    <SystemAlert
      tier="info"
      title={LABELS.TITLE}
      message={LABELS.MESSAGE}
      // Hide the "View Details" CTA when already on the Recovery Mode page.
      action={
        isOnRecoveryModePage
          ? undefined
          : {
              label: LABELS.VIEW_DETAILS,
              onClick: () => router.stateService.go(RECOVERY_MODE_STATE),
              analyticsId: 'nxrm-recovery-mode-alert-view-details',
            }
      }
      dismissable={false}
    />
  );
}

export default RecoveryModeAlert;
