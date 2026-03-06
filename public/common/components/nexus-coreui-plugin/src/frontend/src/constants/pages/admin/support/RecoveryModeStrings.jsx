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

import {
  NxTextLink
} from '@sonatype/react-shared-components';
import { faClock } from '@fortawesome/free-solid-svg-icons';

export default {
  RECOVERY_MODE: {
    MENU: {
      text: 'Recovery Mode',
      description: <>
          This mode is a system state designed to safely investigate and repair data consistency issues.
          {' '}<NxTextLink external href="https://links.sonatype.com/products/nxrm3/docs/recovery-mode">
                  Learn more
               </NxTextLink>.
        </>,
      icon: faClock
    },
    ACTIONS: {
      enable: 'Enable Recovery Mode',
      disable: 'Disable Recovery Mode',
      save: 'Save'
    },
    MESSAGES: {
      LOAD_ERROR: 'Failed to load recovery mode settings',
      SAVE_ERROR: 'Failed to save recovery mode settings',
      SAVE_SUCCESS: 'Recovery mode settings saved successfully'
    },

    LABELS : {
      DISABLE_BUTTON: 'Disable',
      ENABLE_BUTTON: 'Enable',

      STATE_DISABLED: 'Disabled',
      STATE_ENABLED: 'Enabled',

      HOW_IT_WORKS: 'How it Works',
      HOW_IT_WORKS_DESCRIPTION: 'While recovery mode is enabled, the system blocks the following task types that conflict with data repair checks or execution:',
      STATUS_LABEL: 'Status',

      DATA_REPAIR_TASKS: 'Data Repair Tasks',

      TASKS_RUNNING_WARNING: <>Recovery mode can't be disabled while data repair tasks are running.<br/>Wait for the task to finish or stop it manually.</>,
    },

    TABLE : {
        NAME_LABEL: 'Name',
        STATUS_LABEL: 'Status',
        LAST_RUN_LABEL: 'Last Run',
        LAST_RESULT_LABEL: 'Last Result',
        EMPTY_MESSAGE: 'Recovery tasks have not been created'
      },

    CONFIRMATION_MODAL: {
      TITLE: 'Disable recovery mode with unexecuted plans?',
      MESSAGE: 'One or more data repair plans have not been executed. If Recovery Mode is disabled now, unexecuted plans may become outdated.',
      CONFIRM_BUTTON: 'Disable Mode',
      CANCEL_BUTTON: 'Cancel'
    }
  }
};
