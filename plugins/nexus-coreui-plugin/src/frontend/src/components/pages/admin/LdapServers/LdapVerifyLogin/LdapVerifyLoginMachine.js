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
import {assign} from 'xstate';
import {mergeDeepRight} from 'ramda';

import {APIConstants, FormUtils, ValidationUtils, ExtAPIUtils} from '@sonatype/nexus-ui-plugin';

const {
  ACTION,
  METHODS: {VERIFY_LOGIN}
} = APIConstants.EXT.LDAP;

const INITIAL_DATA = {username: '', password: ''};

export default FormUtils.buildFormMachine({
  id: 'LdapVerifyLoginMachine',
  initial: 'loaded',
  config: (config) =>
    mergeDeepRight(config, {
      context: {
        data: INITIAL_DATA,
        pristineData: INITIAL_DATA
      },
      states: {
        loaded: {
          on: {
            VERIFY: {
              target: 'verifying'
            }
          }
        },
        verifying: {
          entry: 'clearResult',
          invoke: {
            src: 'verifyLogin',
            onDone: {
              target: 'loaded',
              actions: 'setSuccess'
            },
            onError: {
              target: 'loaded',
              actions: 'setError'
            }
          }
        }
      }
    })
}).withConfig({
  actions: {
    validate: assign({
      validationErrors: ({data}) => ({
        username: ValidationUtils.validateNotBlank(data.username),
        password: ValidationUtils.validateNotBlank(data.password)
      })
    }),
    setSuccess: assign({
      success: true
    }),
    setError: assign({
      error: (_, evt) => evt.data.message
    }),
    clearResult: assign({
      success: false,
      error: null
    })
  },
  services: {
    verifyLogin: async (context) => {
      const {
        ldapConfig,
        data: {username, password}
      } = context;
      const config = {
        ...ldapConfig,
        protocol: ldapConfig.protocol.toLowerCase()
      };
      const response = await ExtAPIUtils.extAPIRequest(ACTION, VERIFY_LOGIN, {
        data: [config, btoa(username), btoa(password)]
      });
      return ExtAPIUtils.checkForError(response) || response;
    }
  }
});
