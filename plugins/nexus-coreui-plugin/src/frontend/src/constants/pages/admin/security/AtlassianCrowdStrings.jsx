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
  CROWD_SETTINGS: {
    MENU: {
      text: 'Atlassian Crowd',
      description: 'Manage Atlassian Crowd configuration'
    },
    FIELDS: {
      enabledLabel: 'Enable Crowd',
      enabledDescription: 'Enable Crowd Capability',
      realmActiveLabel: 'Enable Crowd Realm for authentication',
      realmActiveDescription: {
        __html: 'To control ordering, go to <a href="#admin/security/realms" target="_blank">Realms</a> page.'
      },
      urlLabel: 'Crowd server URL',
      urlDescription: 'For example: http://localhost:8095/crowd',
      urlValidationError: 'URL is not valid',
      useTrustStoreLabel: 'Truststore',
      useTrustStoreDescription: {
        __html: 'Use certificates stored in the NXRM truststore to connect to external systems <br/>' +
            '<a href="#admin/security/sslcertificates" target="_blank">Configure the NXRM truststore</a>'
      },
      useTrustStoreText: 'Use the NXRM truststore',
      applicationNameLabel: 'Crowd application name',
      applicationPasswordLabel: 'Crowd application password',
      timeoutLabel: 'Connection timeout',
      timeoutDescription: 'Seconds to wait for activity before stopping and retrying the connection. Leave blank to use the globally defined HTTP timeout.',
    },
    MESSAGES: {
      LOAD_ERROR: 'An error occurred while loading Atlassian Crowd settings',
      SAVE_SUCCESS: 'Atlassian Crowd settings updated',
      SAVE_ERROR: 'An error occurred while updating Atlassian Crowd settings',
      VERIFY_CONNECTION_SUCCESS: 'Connection to Crowd server verified',
      VERIFY_CONNECTION_ERROR: 'An error occurred while verifying connection to the Crowd server',
      CLEAR_CACHE_SUCCESS: 'Crowd cache has been cleared',
      CLEAR_CACHE_ERROR: 'An error occurred while clearing Crowd cache'
    },
    BUTTONS: {
      VERIFY_BUTTON_LABEL: 'Verify connection',
      CLEAR_BUTTON_LABEL: 'Clear cache'
    }
  }
};