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
import { Box, Flex, Text, Badge } from '@radix-ui/themes';
import { Clock, CheckCircle, XCircle, History } from 'lucide-react';

import { TaskHistoryProps, formatDate } from './types';

import './TaskHistory.scss';

/**
 * TaskHistory - Displays task execution history and last run information
 * 
 * Note: The current API only provides last run information.
 * A full execution history would require a dedicated API endpoint.
 */
export function TaskHistory({ task }: TaskHistoryProps) {
  const hasLastRun = !!task.lastRun;
  const isSuccess = task.lastRunResult?.toLowerCase().startsWith('ok');
  const isFailed = task.lastRunResult?.toLowerCase().includes('error') || 
                   task.lastRunResult?.toLowerCase().includes('fail');
  const isCanceled = task.lastRunResult?.toLowerCase().includes('cancel');

  const getResultIcon = () => {
    if (isSuccess) return <CheckCircle size={16} className="task-history__icon task-history__icon--success" />;
    if (isFailed) return <XCircle size={16} className="task-history__icon task-history__icon--error" />;
    if (isCanceled) return <XCircle size={16} className="task-history__icon task-history__icon--canceled" />;
    return <Clock size={16} className="task-history__icon" />;
  };

  const getResultColor = (): 'green' | 'red' | 'yellow' | 'gray' => {
    if (isSuccess) return 'green';
    if (isFailed) return 'red';
    if (isCanceled) return 'yellow';
    return 'gray';
  };

  return (
    <Box className="task-history">
      <Flex align="center" gap="2" className="task-history__header">
        <History size={18} />
        <Text size="3" weight="medium">Execution History</Text>
      </Flex>

      <Box className="task-history__content">
        {hasLastRun ? (
          <Box className="task-history__run" data-testid="task-history-last-run">
            <Text size="3" weight="medium" mb="3">Last Execution</Text>
            <Flex direction="column" gap="3">
              <Flex justify="between" align="start" gap="4">
                <Box>
                  <Text size="1" color="gray">Started</Text>
                  <Text size="2">{formatDate(task.lastRun)}</Text>
                </Box>
                <Box>
                  <Text size="1" color="gray">Result</Text>
                  <Flex align="center" gap="2">
                    {getResultIcon()}
                    <Badge color={getResultColor()} size="1">
                      {task.lastRunResult || 'Unknown'}
                    </Badge>
                  </Flex>
                </Box>
              </Flex>
              {task.statusDescription && (
                <Box>
                  <Text size="1" color="gray">Details</Text>
                  <Text size="2">{task.statusDescription}</Text>
                </Box>
              )}
            </Flex>
          </Box>
        ) : (
          <Box className="task-history__empty" data-testid="task-history-empty">
            <Flex direction="column" align="center" gap="2" py="4">
              <Clock size={24} className="task-history__icon" />
              <Text size="2" weight="medium">No execution history</Text>
              <Text size="1" color="gray">This task has not been executed yet. Run it manually or wait for the next scheduled run.</Text>
            </Flex>
          </Box>
        )}

        {/* Next run information */}
        {task.nextRun && (
          <Box className="task-history__next">
            <Text size="2" weight="medium" className="task-history__label">Next Scheduled Run</Text>
            <Flex align="center" gap="2">
              <Clock size={14} className="task-history__icon task-history__icon--next" />
              <Text size="2" className="task-history__value">
                {formatDate(task.nextRun)}
              </Text>
            </Flex>
          </Box>
        )}

        {/* Schedule info */}
        <Box className="task-history__schedule">
          <Text size="2" weight="medium" className="task-history__label">Schedule</Text>
          <Text size="2" className="task-history__value task-history__value--schedule">
            {task.schedule === 'manual' ? 'Manual (run on demand)' :
             task.schedule === 'once' ? 'Once' :
             task.schedule === 'hourly' ? 'Hourly' :
             task.schedule === 'daily' ? 'Daily' :
             task.schedule === 'weekly' ? 'Weekly' :
             task.schedule === 'monthly' ? 'Monthly' :
             task.schedule === 'advanced' ? `Cron: ${task.cronExpression}` :
             task.schedule}
          </Text>
        </Box>
      </Box>
    </Box>
  );
}

export default TaskHistory;


