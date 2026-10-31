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
import { Box } from '@radix-ui/themes';

import { useIsPreviewUI } from '../Navigation';
import { RecoveryModeAlert } from './RecoveryModeAlert';

import './SystemAlerts.scss';

export interface SystemAlertsProps {
  /** Additional CSS class. */
  className?: string;
}

/**
 * SystemAlerts - Application-scoped banner host for the Preview UI.
 *
 * Renders only in Preview UI (gated by useIsPreviewUI). Mounted once per app
 * shell (coreui / cloudui) above the global header.
 *
 * Composes self-gating alert children (each decides its own visibility),
 * mirroring the classic SystemNotices pattern. Add new system banners here.
 */
export function SystemAlerts({ className = '' }: SystemAlertsProps): React.ReactElement | null {
  const isPreview = useIsPreviewUI();

  if (!isPreview) {
    return null;
  }

  return (
    <Box className={`nxrm-system-alerts ${className}`.trim()} data-testid="nxrm-system-alerts">
      <RecoveryModeAlert />
    </Box>
  );
}

export default SystemAlerts;
