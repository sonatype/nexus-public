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

import React, {useCallback} from 'react';
import {IconButton, Box} from '@radix-ui/themes';
import {Bell} from 'lucide-react';
import {useRouter} from '@uirouter/react';

import {ExtJS} from '../../../../interface/ExtJS';
import {useIsVisible} from '../../../../interface/NavigationUtils';
import {useIsPreviewUI} from '../Navigation';
import {useUnreadStatusFailure} from '../hooks';

/**
 * Global-header bell that routes to the appropriate system-status page for the
 * user's current UI (Classic vs. Preview) and shows a red "unread" dot when
 * health checks are failing.
 *
 * `SystemStatusRadix` (coreui) and `SystemStatus` (cloud) are thin wrappers
 * around this component — they differ only in the root class name and the
 * analytics attribute the cloud build attaches to its icon button.
 */
export default function SystemStatusBell({className, dataAnalyticsId}) {
  const isPreviewUI = useIsPreviewUI();
  const supportStatusStateIdentifier = isPreviewUI
    ? 'preview.admin.support.metrichealth'
    : 'admin.support.status';
  const healthChecksFailed = ExtJS.useState(() =>
    ExtJS.state().getValue('health_checks_failed', false),
  );
  const {showDot, markAcknowledged} = useUnreadStatusFailure(healthChecksFailed);
  const router = useRouter();

  const visibilityRequirements = router.stateRegistry.get(supportStatusStateIdentifier)?.data
    ?.visibilityRequirements;

  const isVisibleValue = useIsVisible(visibilityRequirements);

  const onClick = useCallback(() => {
    markAcknowledged();
    router.stateService.go(supportStatusStateIdentifier);
  }, [markAcknowledged, router.stateService, supportStatusStateIdentifier]);

  if (!visibilityRequirements || !isVisibleValue) {
    return null;
  }

  return (
    <Box className={className}>
      <IconButton
        variant="ghost"
        onClick={onClick}
        title="System Status"
        aria-label={showDot ? 'System status -- unhealthy' : 'System Status'}
        {...(dataAnalyticsId ? {'data-analytics-id': dataAnalyticsId} : {})}
      >
        <Bell size={18} />
      </IconButton>
      {showDot && <Box className={`${className}__badge`} aria-hidden="true" />}
    </Box>
  );
}
