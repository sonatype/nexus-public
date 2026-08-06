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
import {useIsPreviewUI, SystemAlerts, CELimitsAlert} from '@sonatype/nexus-ui-plugin';

import SystemNotices from './SystemNotices';
import './SystemNoticesSwitch.scss';

/**
 * Global system-notice host.
 *   - CELimitsAlert: the fully-migrated Radix CE usage banner, rendered in BOTH
 *     Classic and Preview UI (replaces the legacy RSC CEHardLimitAlert).
 *   - SystemAlerts: Radix host for banners that are Preview-only (recovery mode);
 *     the classic equivalent still lives in SystemNotices.
 *   - SystemNotices: remaining legacy RSC banners (telemetry, upgrade/recovery,
 *     license expiry).
 *
 * All hosts claim `grid-area: notices`; the `.nxrm-notices-stack` wrapper makes
 * them share that single full-width row and stack vertically.
 */
export default function SystemNoticesSwitch() {
  const isPreviewUI = useIsPreviewUI();

  return (
    <div className="nxrm-notices-stack">
      <CELimitsAlert />
      {isPreviewUI && <SystemAlerts />}
      <SystemNotices isPreviewUI={isPreviewUI} />
    </div>
  );
}
