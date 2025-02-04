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

export default {
  SUPPORT_ZIP: {
    MENU: {
      text: 'Support ZIP',
      description: 'Creates a ZIP file containing useful support information about your server'
    },

    DESCRIPTION: '<p>No information will be sent to Sonatype when creating the support ZIP file.</p>' +
        '<p>Support ZIP creation may take a few minutes to complete.</p>',
    CONTENTS: 'Contents',
    OPTIONS: 'Options',

    REPORT_LABEL: 'System information report',
    DUMP_LABEL: 'JVM thread-dump',
    CONFIGURATION_LABEL: 'Configuration files',
    SECURITY_LABEL: 'Security configuration files',
    LOGFILES_LABEL: 'Log files',
    TASKLOGFILES_LABEL: 'Task log files',
    AUDITLOGFILES_LABEL: 'Audit log files',
    METRICS_LABEL: 'System and component metrics',
    JMX_LABEL: 'JMX information',
    ARCHIVED_LOGS_LABEL: 'Include logs from previous days: None, 1, 2, 3',
    REPLICATION_LABEL: 'Replication log files',
    LIMITFILESIZES_LABEL: 'Limit files in the ZIP archive to 30 MB apiece',
    LIMITZIPSIZE_LABEL: 'Limit the ZIP archive to 50 MB',
    CREATED_TITLE: 'Support ZIP Created',
    CREATED_DESCRIPTION: 'Support ZIP has been created.<br/>You can reference this file on the filesystem or download the file from your browser.',
    CREATED_NODEID_LABEL: 'Node:',
    CREATED_NAME_LABEL: 'Name:',
    CREATED_SIZE_LABEL: 'Size:',
    CREATED_PATH_LABEL: 'Path:',
    CREATED_DOWNLOAD_BUTTON: 'Download',
    AUTHENTICATE_TEXT: 'Downloading support ZIP requires validation of your credentials.',

    MENU_HA: {
      text: 'Support ZIP',
      description: 'Manage your support zips'
    },

    AVAILABLE_NODES: 'Available Nodes',
    NODE_IS_ACTIVE: 'Node Active',
    ZIP_UPDATED: 'Zip updated',
    NODE_IS_INACTIVE: 'Node Inactive',
    OFFLINE: 'Offline',
    NODE_UNAVAILABLE_CANNOT_CREATE: 'Node Unavailable - Support zip cannot be created',
    NO_BLOB_STORE_CONFIGURED: 'No blob store configured. A configured blob store is required to generate a support zip.',
    NO_ZIP_CREATED: 'No Zip created',
    DOWNLOAD_ZIP: 'Download Zip Generated',
    CREATING_ZIP: 'Creating Zip...',

    CREATE_SUPPORT_ZIP_MODAL_HEADER: 'Options for Export',

    GENERATE_ALL_ZIP_FILES: 'Generate Zip from all nodes',
    GENERATE_NEW_ZIP_FILE: 'Generate new Zip',
    GENERATE_ERROR: 'New Support zip Generation Failed',
    RETRY: 'Retry',
  }
};
