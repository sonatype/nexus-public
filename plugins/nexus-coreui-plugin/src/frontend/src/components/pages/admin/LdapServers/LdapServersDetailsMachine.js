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
import {mergeDeepRight, omit} from 'ramda';

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
  isAnonymousAuth,
  isSimpleAuth,
  validateUrlValues,
  generateUrl,
} from './LdapServersHelper';

const {EXT} = APIConstants;

const initialState = {
  authScheme: '',
  authRealm: '',
  authUsername: '',
  authPassword: '',
  connectionRetryDelay: 300,
  connectionTimeout: 30,
  groupType: null,
  host: '',
  id: '',
  ldapGroupsAsRoles: true,
  maxIncidentsCount: 3,
  name: '',
  port: '',
  protocol: '',
  searchBase: '',
  template: null,
  userBaseDn: '',
  userEmailAddressAttribute: '',
  userIdAttribute: '',
  userLdapFilter: '',
  userObjectClass: '',
  userPasswordAttribute: '',
  userRealNameAttribute: '',
  userSubtree: false,
  useTrustStore: false,
};

export default FormUtils.buildFormMachine({
  id: 'LdapServersDetailsMachine',
  config: (config) =>
    mergeDeepRight(config, {
      states: {
        loaded: {
          initial: 'creatingConnection',
          states: {
            creatingConnection: {
              on: {
                VERIFY_CONNECTION: {
                  target: 'verifyingConnection',
                },
                NEXT: {
                  target: 'creatingUserAndGroup',
                },
                UPDATE_PROTOCOL: {
                  actions: 'updateProtocol',
                },
              },
            },
            verifyingConnection: {
              invoke: {
                src: 'verifyConnection',
                onDone: {
                  target: 'creatingConnection',
                  actions: 'verificationSuccess',
                },
                onError: {
                  target: 'creatingConnection',
                  actions: 'verificationError',
                },
              },
            },
            creatingUserAndGroup: {},
          },
        },
      },
    }),
}).withConfig({
  actions: {
    validate: assign({
      validationErrors: ({data}) => {
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
          authPassword: isAnonymousAuth(data.authScheme)
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

      ExtJS.showSuccessMessage(LABELS.SUCCESS_MESSAGE(url));
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
  },
  services: {
    fetchData: () => {
      return Promise.resolve({data: initialState});
    },
    verifyConnection: async ({data}) => {
      let requestObject = data;

      if (isAnonymousAuth(requestObject.authScheme)) {
        requestObject.authUsername = '';
        requestObject.authPassword = '';
        requestObject.authScheme = 'none';
        requestObject = omit(['authRealm'], requestObject);
      } else if (isSimpleAuth(requestObject.authScheme)) {
        requestObject.authScheme = 'simple';
        requestObject = omit(['authRealm'], requestObject);
      }

      const response = await ExtAPIUtils.extAPIRequest(
        EXT.LDAP.ACTION,
        EXT.LDAP.METHODS.VERIFY_CONNECTION,
        {data: [requestObject]}
      );

      ExtAPIUtils.checkForError(response)

      return response;
    },
  },
});
