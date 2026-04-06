/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import React from 'react';
import {NxTextLink} from '@sonatype/react-shared-components';
import { faClock } from '@fortawesome/free-solid-svg-icons';

export default {
  TASKS: {
    MENU: {
      text: 'Tasks',
      description: 'Manage scheduled tasks',
      icon: faClock
    },
    LIST: {
      CREATE_BUTTON: 'Create task',
      EMPTY_LIST: 'There are no tasks available',
      COLUMNS: {
        NAME: 'Name',
        TYPE: 'Type',
        STATUS: 'Status',
        SCHEDULE: 'Schedule',
        NEXT_RUN: 'Next run',
        LAST_RUN: 'Last run',
        LAST_RESULT: 'Last result',
      },
      HELP: {
        TITLE: 'What is a scheduled task?',
        TEXT: <>
          The repository manager allows you to schedule the execution of maintenance tasks. The tasks can
          carry out regular maintenance steps that will be applied to all repositories or to specific repositories
          on a configurable schedule or simply perform other system maintenance. See our{' '}
          <NxTextLink external href="http://links.sonatype.com/products/nxrm3/docs/tasks">
            help documentation
          </NxTextLink>
          {' '}for more information.
        </>,
      },
    },
    FORM: {
      SECTIONS: {
        SETTINGS: 'Task Settings',
        SUMMARY: 'Task Summary',
      },
      TYPE: {
        LABEL: 'Type',
      },
      ENABLED: {
        LABEL: 'Task Enabled',
        SUB_LABEL: 'Flag task as active; deselect checkbox to disable this task',
      },
      NAME: {
        LABEL: 'Task Name',
        SUB_LABEL: 'A name for the scheduled task',
      },
      EMAIL: {
        LABEL: 'Notification Email',
        SUB_LABEL: 'The email address where an email will be sent if the condition below is met',
      },
      SEND_NOTIFICATION_ON: {
        LABEL: 'Send Notification On',
        SUB_LABEL: 'Conditions that will trigger the notification email',
        OPTIONS: {
          FAILURE: 'Failure',
          SUCCESS_FAILURE: 'Success or Failure',
        }
      },
    },
    SUMMARY: {
      ID: 'ID',
      NAME: 'Name',
      TYPE: 'Type',
      STATUS: 'Status',
      LAST_RESULT: 'Last Result',
      NEXT_RUN: 'Next Run',
      LAST_RUN: 'Last Run',
      BUTTONS: {
        RUN: 'Run',
        STOP: 'Stop',
      },
    },
    MESSAGES: {
      CONFIRM_DELETE: {
        TITLE: 'Delete Task',
        MESSAGE: (name) => `Are you sure you want to delete the task named "${name}"?`,
        YES: 'Delete',
      },
      CONFIRM_RUN: {
        TITLE: 'Run Task',
        MESSAGE: (name) => `Run "${name}" task?`,
        YES: 'Run',
      },
      CONFIRM_STOP: {
        TITLE: 'Stop Task',
        MESSAGE: (name) => `Stop "${name}" task?`,
        YES: 'Stop',
      },
      DELETE_SUCCESS: (name) => `Task deleted: ${name}`,
      RUN_SUCCESS: (name) => `Task started: ${name}`,
      STOP_SUCCESS: (name) => `Task stopped: ${name}`,
    }
  },
};
