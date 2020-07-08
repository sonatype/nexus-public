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
import { UIStrings } from 'nexus-ui-plugin';
import React from 'react';

export default {
  ...UIStrings,

  ANONYMOUS_SETTINGS: {
    MENU: {
      text: 'Anonymous Access',
      description: 'Configure anonymous access to server contents'
    },
    ENABLED_CHECKBOX_LABEL: 'Access:',
    ENABLED_CHECKBOX_DESCRIPTION: 'Allow anonymous users to access the server',
    USERNAME_TEXTFIELD_LABEL: 'Username:',
    REALM_SELECT_LABEL: 'Realm:',
    MESSAGES: {
      LOAD_ERROR: 'An error occurred while loading Anonymous settings, see console for more details',
      SAVE_SUCCESS: 'Anonymous security settings updated',
      SAVE_ERROR: 'An error occurred while updating Anonymous settings, see console for more details'
    }
  },

  LOGGING: {
    MENU: {
      text: 'Logging',
      description: 'Control logging levels'
    },

    CREATE_BUTTON: 'Create Logger',
    RESET_ALL_BUTTON: 'Reset to Default Levels',
    RESET_BUTTON: 'Reset to Default Level',

    FILTER_PLACEHOLDER: 'Filter by Logger Name',

    NAME_LABEL: 'Logger Name',
    LEVEL_LABEL: 'Logger Level',

    MESSAGES: {
      SAVE_ERROR: 'An error occurred while saving the logger',
      RESETTING: 'Resetting to Default Level...',
      RESET_ERROR: 'An error occurred while resetting all loggers, see console for more details'
    },

    CONFIRM_UPDATE: {
      TITLE: 'Confirm Update?',
      MESSAGE: ({name, level}) => `Logger ${name} is already configured. Would you like to update its level to "${level}"?`,
      CONFIRM_BUTTON: 'Update Logger Level'
    },

    CONFIRM_RESET_ALL: {
      TITLE: 'Confirm Reset?',
      MESSAGE: 'Reset all loggers to their default levels?',
      CONFIRM_BUTTON: 'Reset All Loggers'
    },

    CONFIRM_RESET: {
      TITLE: 'Confirm Reset?',
      MESSAGE: 'Reset this logger to its default level? If this is a custom logger it will be removed.',
      CONFIRM_BUTTON: 'Reset Logger'
    }
  },

  LOG_VIEWER: {
    MENU: {
      text: 'Log Viewer',
      description: 'View the current log contents'
    },
    REFRESH: {
      TEXT: 'Refresh Interval',
      MANUAL_ITEM: 'Manual',
      TWENTY_SECONDS_ITEM: 'Every 20 seconds',
      MINUTE_ITEM: 'Every minute',
      TWO_MINUTES_ITEM: 'Every 2 minutes',
      FIVE_MINUTES_ITEM: 'Every 5 minutes'
    },
    SIZE: {
      LAST25KB_ITEM: 'Last 25KB',
      LAST50KB_ITEM: 'Last 50KB',
      LAST100KB_ITEM: 'Last 100KB'
    },
    DOWNLOAD: 'Download',
    INSERT_MARK: 'Insert Mark',
    MARK_PLACEHOLDER: 'Marker to insert into log'
  },

  SYSTEM_INFORMATION: {
    MENU: {
      text: 'System Information'
    },
    ACTIONS: {
      download: 'Download as JSON'
    }
  },

  SUPPORT_REQUEST: {
    MENU: {
      text: 'Support Request',
      description: 'Submit a support request to Sonatype'
    },
    ACTIONS: {
      submitRequest: 'Submit Request'
    },
    DESCRIPTION: `Please include a complete description of your problem and steps to allow us to reproduce the problem (if available).`,
    ATTACH_SUPPORT_ZIP: `Attaching a support ZIP to your request will help our engineers give you a faster response.`
  },

  USER_ACCOUNT: {
    MENU: {
      text: 'Account',
      description: 'Manage your account',
    },
    ACTIONS: {
      changePassword: 'Change password',
      discardChangePassword: 'Discard password change',
    },
    MESSAGES: {
      LOAD_ERROR: 'An error occurred while loading User Account, see console for more details',
      UPDATE_SUCCESS: 'User account settings updated',
      UPDATE_ERROR: 'An error occurred while updating user account settings',
      PASSWORD_NO_MATCH_ERROR: 'Passwords do not match',
      PASSWORD_MUST_DIFFER_ERROR: 'New password must be different',
    },
    ID_FIELD_LABEL: 'Username',
    FIRST_FIELD_LABEL: 'First Name',
    LAST_FIELD_LABEL: 'Last Name',
    EMAIL_FIELD_LABEL: 'Email',
    PASSWORD_CURRENT_FIELD_LABEL: 'Current Password',
    PASSWORD_NEW_FIELD_LABEL: 'New Password',
    PASSWORD_NEW_CONFIRM_FIELD_LABEL: 'New Password (Confirm)',
  },

  NUGET_API_KEY: {
    MENU: {
      text: 'NuGet API Key',
      description: 'Configure credentials for NuGet repositories'
    },
    INSTRUCTIONS: 'A new API Key will be created the first time it is accessed. Resetting your API Key will invalidate the current key.',
    AUTH_INSTRUCTIONS: 'Accessing NuGet API Key requires validation of your credentials.',
    AUTH_ERROR: 'Authentication Failure',
    CLOSE: 'Close',
    ACCESS: {
      BUTTON: 'Access API Key',
      HELP_TEXT: 'Accessing NuGet API Key requires validation of your credentials.',
      ERROR: 'Failed to access API Key'
    },
    RESET: {
      BUTTON: 'Reset API Key',
      HELP_TEXT: 'Resetting NuGet API Key requires validation of your credentials.',
      ERROR: 'Failed to Reset API Key'
    },
    DETAILS: {
      MAIN: 'Your NuGet API Key enables pushing packages using nuget.exe.' ,
      WARNING:'Keep this key secret!' ,
      API_KEY_TEXT: 'Your NuGet API Key is:',
      REGISTER_TEXT: 'You can register this key for a given repository with the following command:',
      REGISTER_COMMAND: 'nuget setapikey {0} -source {1}',
      AUTO_CLOSE: 'This window will automatically close after one minute.'
    }
  },

  METRIC_HEALTH: {
    MENU: {
      text: 'Status',
      description: 'System status checks'
    },
    NAME_HEADER: 'Name',
    MESSAGE_HEADER: 'Message',
    ERROR_HEADER: 'Error',
  },

  SUPPORT_ZIP: {
    MENU: {
      text: 'Support ZIP',
      description: 'Creates a ZIP file containing useful support information about your server'
    },
    DESCRIPTION: '<p>No information will be sent to Sonatype when creating the support ZIP file.</p>' +
        '<p>Support ZIP creation may take a few minutes to complete.</p>',
    REPORT_LABEL: 'System information report',
    DUMP_LABEL: 'JVM thread-dump',
    CONFIGURATION_LABEL: 'Configuration files',
    SECURITY_LABEL: 'Security configuration files',
    LOGFILES_LABEL: 'Log files',
    TASKLOGFILES_LABEL: 'Task log files',
    AUDITLOGFILES_LABEL: 'Audit log files',
    METRICS_LABEL: 'System and component metrics',
    JMX_LABEL: 'JMX information',
    LIMITFILESIZES_LABEL: 'Limit files in the ZIP archive to 30 MB apiece',
    LIMITZIPSIZE_LABEL: 'Limit the ZIP archive to 20 MB',
    CREATED_TITLE: 'Support ZIP Created',
    CREATED_DESCRIPTION: 'Support ZIP has been created.<br/>You can reference this file on the filesystem or download the file from your browser.',
    CREATED_NODEID_LABEL: 'Node:',
    CREATED_NAME_LABEL: 'Name:',
    CREATED_SIZE_LABEL: 'Size:',
    CREATED_PATH_LABEL: 'Path:',
    CREATED_DOWNLOAD_BUTTON: 'Download',
    AUTHENTICATE_TEXT: 'Downloading support ZIP requires validation of your credentials.'
  }
};
