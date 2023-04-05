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
import Axios from 'axios';
import {assign, sendParent} from 'xstate';
import {isNil, mergeDeepRight, omit} from 'ramda';

import {
  FormUtils,
  APIConstants,
  ValidationUtils,
  ExtAPIUtils,
  ExtJS,
} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

const {
  LDAP_SERVERS: {FORM: LABELS},
} = UIStrings;

import {
  URL,
  isAnonymousAuth,
  isSimpleAuth,
  validateUrlValues,
  generateUrl,
} from './LdapServersHelper';

const {singleLdapServersUrl} = URL;

const {EXT} = APIConstants;

export const initialState = {
  authScheme: '',
  authRealm: null,
  authUsername: '',
  authPassword: '',
  connectionRetryDelay: 300,
  connectionTimeout: 30,
  host: '',
  id: '',
  maxIncidentsCount: 3,
  name: '',
  port: '',
  protocol: '',
  searchBase: '',
  order: null,
  useTrustStore: false,
};

export default FormUtils.buildFormMachine({
  id: 'LdapServersConfigurationMachine',
  initial: 'loaded',
  config: (config) =>
    mergeDeepRight(config, {
      context: {
        isEdit: false,
      },
      states: {
        loaded: {
          id: 'loaded',
          on: {
            READY: {
              actions: 'saveConnectionData',
            },
            VERIFY_CONNECTION: [
              {
                target: 'confirmingConnection',
                actions: 'updateData',
                cond: 'isParent',
              },
              {
                target: 'askingPassword',
                cond: 'isEdit',
              },
              {
                target: 'verifyingConnection',
              },
            ],
            UPDATE_PROTOCOL: {
              actions: 'updateProtocol',
            },
            NEXT: {
              actions: 'nextForm',
            },
            DELETE_CONNECTION: {
              target: 'confirmingDelete',
            },
            CHANGE_PASSWORD: {
              target: 'changingPassword',
            },
          },
        },
        verifyingConnection: {
          invoke: {
            src: 'verifyConnection',
            onDone: {
              target: 'loaded',
              actions: 'verificationSuccess',
            },
            onError: {
              target: 'loaded',
              actions: 'verificationError',
            },
          },
        },
        changingPassword: {
          initial: 'idle',
          on: {
            CANCEL: {
              target: 'loaded',
            },
            DONE: {
              target: '.verifyingConnection',
              actions: ['update', 'validate', 'setDirtyFlag', 'setIsPristine'],
            },
          },
          states: {
            idle: {},
            verifyingConnection: {
              invoke: {
                src: 'verifyConnection',
                onDone: {
                  target: '#loaded',
                  actions: 'saveForm',
                },
                onError: {
                  target: '#loaded',
                  actions: 'verificationError',
                },
              },
            },
          },
        },
        askingPassword: {
          on: {
            CANCEL: {
              target: 'loaded',
            },
            DONE: {
              target: 'verifyingConnection',
              actions: 'update',
            },
          },
        },
        confirmingConnection: {
          invoke: {
            src: 'verifyConnection',
            onDone: {
              target: 'loaded',
              actions: 'connectionStatus',
            },
            onError: {
              target: 'loaded',
              actions: 'connectionStatus',
            },
          },
        },
        confirmingDelete: {
          on: {
            CANCEL: {
              target: 'loaded',
            },
            ACCEPT: {
              target: 'delete',
              actions: 'sendDelete',
            },
          },
        },
      },
    }),
}).withConfig({
  actions: {
    validate: assign({
      validationErrors: ({data, isEdit}) => {
        return {
          name: ValidationUtils.validateNotBlank(data.name),
          protocol: ValidationUtils.validateNotBlank(data.protocol),
          host:
            ValidationUtils.validateNotBlank(data.host) ||
            ValidationUtils.validateHost(data.host),
          port:
            ValidationUtils.validateNotBlank(data.port) ||
            ValidationUtils.isInRange({
              value: data.port,
              min: 1,
              max: 65535,
              allowDecimals: false,
            }),
          searchBase: ValidationUtils.validateNotBlank(data.searchBase),
          authUsername: isAnonymousAuth(data.authScheme)
            ? null
            : ValidationUtils.validateNotBlank(data.authUsername),
          authPassword:
            isAnonymousAuth(data.authScheme) || isEdit
              ? null
              : ValidationUtils.validateNotBlank(data.authPassword),
          connectionTimeout: ValidationUtils.validateNotBlank(
            data.connectionTimeout
          ),
          connectionRetryDelay: ValidationUtils.validateNotBlank(
            data.connectionRetryDelay
          ),
          maxIncidentsCount: ValidationUtils.validateNotBlank(
            data.maxIncidentsCount
          ),
        };
      },
    }),
    verificationSuccess: ({data: {protocol, host, port}}) => {
      const url = generateUrl(protocol, host, port);

      ExtJS.showSuccessMessage(LABELS.VERIFY_SUCCESS_MESSAGE(url));
    },
    verificationError: (_, {data}) => ExtJS.showErrorMessage(data),
    updateProtocol: assign({
      data: ({data}, {value}) => {
        const useTrustStore = validateUrlValues(
          data.protocol,
          data.host,
          data.port
        );

        return {
          ...data,
          useTrustStore,
          protocol: value,
        };
      },
    }),
    nextForm: sendParent(({data, pristineData}) => ({
      type: 'NEXT',
      data,
      pristineData,
    })),
    saveForm: sendParent(({data, pristineData}) => ({
      type: 'SAVE_FORM',
      data,
      pristineData,
    })),
    logSaveSuccess: ({data}) => {
      ExtJS.showSuccessMessage(LABELS.SAVE_SUCCESS_MESSAGE(data.name));
    },
    saveConnectionData: assign((ctx, {data}) => {
      return mergeDeepRight(ctx, {data, pristineData: data});
    }),
    updateData: assign({
      data: (ctx, {data}) => mergeDeepRight(ctx.data, data),
    }),
    connectionStatus: sendParent((_, event) => ({
      type: 'CONNECTION_STATUS',
      data: event.data,
    })),
    sendDelete: sendParent('DELETE'),
  },
  guards: {
    isEdit: (context) =>
      context.isEdit && !isAnonymousAuth(context.data.authScheme),
    isParent: (_, event) => {
      return !isNil(event?.isParent) && event.isParent;
    },
  },
  services: {
    verifyConnection: async ({data}) => {
      let requestObject = data;

      if (isAnonymousAuth(requestObject.authScheme)) {
        requestObject.authUsername = initialState.authUsername;
        requestObject.authPassword = initialState.authPassword;
        requestObject = omit(['authRealm'], requestObject);
      } else if (isSimpleAuth(requestObject.authScheme)) {
        requestObject = omit(['authRealm'], requestObject);
      }

      // Ext API requires protocol to be lower case and Rest API returns the protocol in upper case.
      requestObject.protocol = requestObject.protocol.toLowerCase();

      const response = await ExtAPIUtils.extAPIRequest(
        EXT.LDAP.ACTION,
        EXT.LDAP.METHODS.VERIFY_CONNECTION,
        {data: [requestObject]}
      );

      ExtAPIUtils.checkForError(response);

      return response;
    },
    saveData: () => Promise.resolve(),
    delete: ({data: {name}}) => Axios.delete(singleLdapServersUrl(name)),
  },
});
