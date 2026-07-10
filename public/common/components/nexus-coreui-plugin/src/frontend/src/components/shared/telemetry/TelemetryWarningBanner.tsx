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
import React, {useState, useEffect} from 'react';
import { NxTextLink } from '@sonatype/react-shared-components';
import { ExtJS } from '@sonatype/nexus-ui-plugin';
import { restClient, urlBuilder } from '@/utils/api';
import UIStrings from '../../../constants/UIStrings';
import SystemNotice from '../../widgets/SystemStatusAlerts/SystemNotice';

import './TelemetryWarningBanner.scss';

const TASKS_PATH = '#admin/system/tasks';
const TELEMETRY_TASK_TYPE = 'telemetry.upload.retry';

/**
 * REST API Task shape (matches server response)
 */
interface RestTask {
  id: string;
  name: string;
  type: string;
  enabled: boolean;
  currentState: string;
}

interface RestTasksResponse {
  items: RestTask[];
}

/**
 * Telemetry health state from backend.
 * An empty object (all fields undefined) indicates opt-out mode - banner should be hidden.
 */
interface TelemetryHealth {
  showWarning?: boolean;
  failedReportsThreshold?: number;
  readOnly?: boolean;
  introducedWarning?: boolean;
}

/**
 * TelemetryWarningBanner - Warning banner shown when telemetry submissions
 * have failed beyond the threshold, or read-only banner when in read-only mode.
 * <p>
 * Uses ExtJS state to determine visibility. The state is provided by
 * TelemetryHealthStateContributor on the backend.
 * <p>
 * Mode mutual exclusivity (enforced by backend):
 * - Mandatory mode (telemetryMandatoryEnabled=true): Sets readOnly and showWarning
 * - Warning-only mode (telemetryMandatoryEnabled=false): Sets introducedWarning only
 * - Opt-out mode: Returns empty object (banner hidden)
 * <p>
 * Shows multiple banner types based on state:
 * - Read-only mode banner (highest priority): when readOnly is true
 * - Mandatory warning banner: when showWarning is true (mandatory mode, not read-only)
 * - introducedWarning banner: when introducedWarning is true (warning-only mode)
 * <p>
 */
export default function TelemetryWarningBanner(): React.ReactElement | null {
  const user = ExtJS.useUser();
  const isAdmin = user?.administrator ?? false;

  const telemetryHealth = ExtJS.useState(
    () => ExtJS.state()?.getValue?.('nexus.telemetry.health')
  ) as TelemetryHealth | undefined;

  const [telemetryTaskId, setTelemetryTaskId] = useState<string | null>(null);

  const isReadOnly = telemetryHealth?.readOnly ?? false;
  const showWarning = telemetryHealth?.showWarning ?? false;
  const introducedWarning = telemetryHealth?.introducedWarning ?? false;

  useEffect(() => {
    if (isAdmin && (isReadOnly || showWarning || introducedWarning)) {
      const fetchTelemetryTasks = async () => {
        try {
          const url = urlBuilder.tasks.list();
          const response = await restClient.get<RestTasksResponse>(url);
          const telemetryTasks = response.items.filter(
            (task) => task.type === TELEMETRY_TASK_TYPE
          );
          if (telemetryTasks.length === 1) {
            setTelemetryTaskId(telemetryTasks[0].id);
          } else {
            setTelemetryTaskId(null);
          }
        } catch (error) {
          setTelemetryTaskId(null);
        }
      };
      fetchTelemetryTasks();
    }
  }, [isAdmin, isReadOnly, showWarning, introducedWarning]);

  if (!isAdmin) return null;

  if (!isReadOnly && !showWarning && !introducedWarning) return null;

  const getTaskUrl = () => {
    if (telemetryTaskId) {
      return `${TASKS_PATH}:${encodeURIComponent(telemetryTaskId)}`;
    }
    return TASKS_PATH;
  };

  // Determine which content to show (read-only takes priority)
  const content = isReadOnly
    ? UIStrings.TELEMETRY.READONLY_BANNER
    : introducedWarning
      ? UIStrings.TELEMETRY.INTRODUCED_WARNING_BANNER
      : UIStrings.TELEMETRY.WARNING_BANNER;

  const title = content.TITLE;
  const message = content.MESSAGE.replace('{count}', String(telemetryHealth?.failedReportsThreshold ?? 3));
  const description = content.DESCRIPTION;
  const viewTasksLabel = content.VIEW_TASKS;

  const noticeLevel = isReadOnly ? 'error' : 'warning';

  return (
    <SystemNotice
      title={title}
      noticeLevel={noticeLevel}
      additionalAlertClassNames="nxrm-telemetry-warning-banner"
      nonDismissable
    >
      {message} {description}{' '}
      <NxTextLink
        href={getTaskUrl()}
        className="nxrm-telemetry-view-tasks-link"
      >
        {viewTasksLabel}
      </NxTextLink>
    </SystemNotice>
  );
}
