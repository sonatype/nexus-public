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
  IQ_SERVER: {
    MENU: {
      text: 'IQ Server',
      description: 'Manage Sonatype Repository Firewall and Sonatype Lifecycle Configuration'
    },

    OPEN_DASHBOARD: 'Open IQ Server Dashboard',

    ENABLED: {
      label: 'Enable IQ Server',
      sublabel: 'Enable the use of IQ Server'
    },

    IQ_SERVER_URL: {
      label: 'IQ Server URL',
      sublabel: 'This is the address of your IQ server'
    },

    TRUST_STORE: {
      label: 'Use the Nexus Repository Truststore',
      sublabel: 'Use certificate connected to the Nexus Repository Truststore'
    },

    CERTIFICATE: 'View Certificate',

    AUTHENTICATION_TYPE: {
      label: 'Authentication Method',
      USER: 'User Authentication',
      PKI: 'PKI Authentication'
    },

    USERNAME: {
      label: 'Username',
      sublabel: 'User with Access to the IQ Server'
    },

    PASSWORD: {
      label: 'Password',
      sublabel: 'Credentials for the IQ Server User'
    },

    CONNECTION_TIMEOUT: {
      label: 'Connection Timeout',
      sublabel: <>
        Seconds to wait for activity before stopping and retrying the Connection.
        <br/>
        Leave blank to use the globally defined HTTP timeout.
      </>,
    },
    CONNECTION_TIMEOUT_DEFAULT_VALUE_LABEL: 'Globally Defined',
    PROPERTIES: {
      label: 'Properties',
      sublabel: 'Additional properties to configure for IQ Server'
    },
    SHOW_LINK: {
      label: 'Show IQ Server Link',
      sublabel: 'Show IQ Server link in the Browse menu when the server is enabled'
    },

    VERIFY_CONNECTION_BUTTON_LABEL: 'Verify Connection',
    VERIFY_CONNECTION_SUCCESSFUL: (msg) => `Connection Successful! Applications: ${msg}`,
    VERIFY_CONNECTION_ERROR: (msg) => `Connection Failed: ${msg}`,
    FORM_NOTES: 'can evaluate application and organizing policies',
    HELP_TEXT: 'To enable this feature configure the IQ Server URL, username and password',
    PASSWORD_ERROR: 'Reenter your password to validate and save your changes.'
  }
};