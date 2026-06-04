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
import {
  NxWarningAlert,
  NxH3,
  NxButtonBar,
} from '@sonatype/react-shared-components';
import { ExtJS } from '../../../../interface/ExtJS';
import { restClient, urlBuilder } from '../../../../interface/api';
import TelemetryStrings from '../../constants/pages/telemetry/TelemetryStrings';
import './TelemetryWarningBanner.scss';

const TELEMETRY = TelemetryStrings.TELEMETRY.WARNING_BANNER;
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
 * TelemetryWarningBanner - Warning banner shown when telemetry submissions
 * have failed beyond the threshold configured in the backend.
 * <p>
 * Uses ExtJS state to determine visibility and threshold. The state is provided by
 * TelemetryHealthStateContributor on the backend.
 * <p>
 * This is a page-level alert (non-dismissible) that only shows for admin users.
 */
export default function TelemetryWarningBanner(): React.ReactElement | null {
  const user = ExtJS.useUser();
  const isAdmin = user?.administrator ?? false;

  const telemetryHealth = ExtJS.useState(() => ExtJS.state()?.getValue?.('nexus.telemetry.health'));

  const [telemetryTaskId, setTelemetryTaskId] = useState<string | null>(null);

  useEffect(() => {
    if (isAdmin && telemetryHealth?.showWarning) {
      fetchTelemetryTasks();
    }
  }, [isAdmin, telemetryHealth?.showWarning]);

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

  if (!isAdmin) return null;

  if (!telemetryHealth?.showWarning) return null;

  const getTaskUrl = () => {
    if (telemetryTaskId) {
      return `${TASKS_PATH}:${encodeURIComponent(telemetryTaskId)}`;
    }
    return TASKS_PATH;
  };

  return (
    <NxWarningAlert className="nxrm-telemetry-warning-banner">
      <NxH3>{TELEMETRY.TITLE}</NxH3>
      <div>
        {TELEMETRY.MESSAGE.replace('{count}', String(telemetryHealth?.failedReportsThreshold ?? 3))}
      </div>
      <div>
        {TELEMETRY.DESCRIPTION}
      </div>
      <NxButtonBar>
        <a className="nx-btn nx-btn--primary" href={getTaskUrl()}>
          {TELEMETRY.VIEW_TASKS}
        </a>
      </NxButtonBar>
    </NxWarningAlert>
  );
}
