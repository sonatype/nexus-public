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
import {NxButton} from '@sonatype/react-shared-components';
import {ExtJS} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';
import SystemNotice from '../SystemNotice';
import { useRouter } from '@uirouter/react';

const {RECOVERY_MODE_ALERT: {LABEL, TEXT, BUTTON}} = UIStrings;
const supportRecoveryIdentifier = 'admin.support.recovery';

export default function RecoveryModeAlert() {
    const user = ExtJS.useUser();
    const isAdmin = user?.administrator;
    const recoveryModeEnabled = ExtJS.useState(() => ExtJS.state().getValue('recovery.mode.enabled'));
    const router = useRouter();

    const onClick = () => router.stateService.go(supportRecoveryIdentifier);

    if (!isAdmin || !recoveryModeEnabled) {
        return null;
    }

    return (
      <SystemNotice noticeLevel="info" title={LABEL} nonDismissable={true}>
        <div className="nxrm-recovery-mode-alert">
          <div>{TEXT}</div>

          <NxButton
              onClick={onClick}
              data-analytics-id="nxrm-recovery-mode-alert-btn"
              variant="primary"
          >
              {BUTTON}
          </NxButton>
        </div>
      </SystemNotice>
    );
}
