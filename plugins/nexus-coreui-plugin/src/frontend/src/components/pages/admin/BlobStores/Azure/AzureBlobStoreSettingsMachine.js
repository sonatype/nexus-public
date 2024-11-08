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

import axios from 'axios';
import {assign, createMachine} from 'xstate';

const TEST_CONNECTION_URL = 'service/rest/internal/ui/azureblobstore/test-connection';

export default createMachine({
  id: 'AzureBlobStoreSettingsMachine',
  initial: 'editing',
  context: {
    accountName: '',
    containerName: '',
    authenticationMethod: undefined,
    accountKey: ''
  },
  states: {
    editing: {
      initial: 'untested',
      states: {
        untested: {},
        error: {},
        success: {}
      },
      on: {
        TEST_CONNECTION: 'testing'
      }
    },

    testing: {
      invoke: {
        src: 'testConnection',
        onDone: 'editing.success',
        onError: 'editing.error'
      }
    }
  },

  on: {
    UPDATE_ACCOUNT_NAME: {
      target: 'editing',
      actions: ['updateAccountName']
    },
    UPDATE_CONTAINER_NAME: {
      target: 'editing',
      actions: ['updateContainerName']
    },
    UPDATE_AUTH_METHOD: {
      target: 'editing',
      actions: ['updateAuthenticationMethod']
    },
    UPDATE_ACCOUNT_KEY: {
      target: 'editing',
      actions: ['updateAccountKey']
    }
  }
}, {
  actions: {
    updateAccountName: assign({
      accountName: (_, {accountName}) => accountName
    }),
    updateContainerName: assign({
      containerName: (_, {containerName}) => containerName
    }),
    updateAuthenticationMethod: assign({
      authenticationMethod: (_, {authenticationMethod}) => authenticationMethod
    }),
    updateAccountKey: assign({
      accountKey: (_, {accountKey}) => accountKey
    })
  },
  services: {
    testConnection: (context) => {
      if (context.blobStoreName) {
        return axios.post(TEST_CONNECTION_URL + "/" + context.blobStoreName, context)
      } else {
        return axios.post(TEST_CONNECTION_URL, context)
      }
    }
  }
});
