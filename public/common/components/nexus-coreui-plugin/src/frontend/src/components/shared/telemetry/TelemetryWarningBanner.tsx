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
import {Link} from '@radix-ui/themes';
import { ExternalLink } from 'lucide-react';
import { ExtJS, SystemAlert } from '@sonatype/nexus-ui-plugin';
import { restClient, urlBuilder } from '@/utils/api';
import UIStrings from '../../../constants/UIStrings';

const TASKS_PATH = '#admin/system/tasks';
const TELEMETRY_TASK_TYPE = 'telemetry.upload.retry';

/**
 * Renders text with markdown-style bold (**text**) as React elements.
 */
function renderBoldText(text: string): React.ReactNode {
  const parts = text.split(/\*\*(.*?)\*\*/g);
  return parts.map((part, index) =>
    index % 2 === 1 ? <strong key={index}>{part}</strong> : part
  );
}

interface HelperLink {
  TEXT: string;
  HREF: string;
}

/**
 * Replaces all occurrences of the helper link text within the message with a
 * clickable external Link. Falls back to plain (bold-rendered) text when no
 * helper link is configured or the link text is not present in the message.
 * Note: split() is intentionally used to replace ALL occurrences, not just the first.
 */
function messageWithLink(messageText: string, helperLink: HelperLink | undefined): React.ReactNode {
  if (!helperLink) {
    return renderBoldText(messageText);
  }

  const linkText = helperLink.TEXT;
  if (!messageText.includes(linkText)) {
    return renderBoldText(messageText);
  }

  const parts = messageText.split(linkText);
  return parts.reduce((acc: React.ReactNode[], part, index) => {
    if (index > 0) {
      acc.push(
        <Link
          key={`link-${index}`}
          href={helperLink.HREF}
          target="_blank"
          rel="noopener noreferrer"
          data-analytics-id="nxrm-telemetry-message-link"
        >
          {linkText}
          <ExternalLink size={12} aria-hidden="true" />
        </Link>
      );
    }
    if (part) {
      acc.push(<React.Fragment key={`text-${index}`}>{renderBoldText(part)}</React.Fragment>);
    }
    return acc;
  }, []);
}

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
 * Telemetry health state from backend. Empty object indicates opt-out mode (banner hidden).
 */
interface TelemetryHealth {
  showWarning?: boolean;
  failedReportDays?: number;
  remainingGracePeriodDays?: number;
  readOnly?: boolean;
  introducedWarning?: boolean;
}

/**
 * Displays warning banner when telemetry uploads fail persistently.
 *
 * Three mutually exclusive modes (determined by backend):
 * - Read-only mode: System is locked due to prolonged failures (readOnly=true) - red/error
 * - Mandatory warning: Grace period active, failures above threshold (showWarning=true) - yellow/warning
 * - Early warning: Failures detected but grace period not active (introducedWarning=true) - yellow/warning
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
  const messageText = content.MESSAGE
    .replace('{failedReportDays}', String(telemetryHealth?.failedReportDays ?? 0))
    .replace('{remainingGracePeriodDays}', String(telemetryHealth?.remainingGracePeriodDays ?? 0));
  const generalRecommendation = content.GENERAL_RECOMMENDATION;
  const retryRecommendation = content.RETRY_RECOMMENDATION;
  const retryLink = content.RETRY_LINK;
  const helperLink = content.HELPER_LINK;

  // Red for read-only mode, yellow/warning for both warning modes
  const tier = isReadOnly ? 'error' : 'warning';

  const message = (
    <>
      {messageWithLink(messageText, helperLink)}{' '}
      {generalRecommendation}{' '}
      {retryRecommendation}{' '}
      <Link
        data-analytics-id="nxrm-telemetry-view-tasks-link"
        href={getTaskUrl()}
      >
        {retryLink}
      </Link>
    </>
  );

  return (
    <SystemAlert
      tier={tier}
      title={title}
      message={message}
      dismissable={!isReadOnly}
    />
  );
}
