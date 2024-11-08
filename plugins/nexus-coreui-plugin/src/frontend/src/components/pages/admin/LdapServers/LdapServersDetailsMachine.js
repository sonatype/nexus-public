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
import {assign, spawn} from 'xstate';
import {mergeDeepRight, omit, isEmpty, isNil} from 'ramda';

import {FormUtils, ExtJS} from '@sonatype/nexus-ui-plugin';

import {TABS_INDEX, URL} from './LdapServersHelper';
import LdapServersUserAndGroupMachine, {
  initialState as initialStateUserAndGroup,
} from './LdapServersUserAndGroupMachine';
import LdapServersConfigurationMachine, {
  initialState as initialStateConfiguration,
} from './LdapServersConfigurationMachine';

import UIStrings from '../../../../constants/UIStrings';

const {
  LDAP_SERVERS: {FORM: LABELS},
} = UIStrings;

export default FormUtils.buildFormMachine({
  id: 'LdapServersDetailsMachine',
  config: (config) =>
    mergeDeepRight(config, {
      context: {
        userAndGroup: null,
        createConnection: null,
        activeTab: TABS_INDEX.CREATE_CONNECTION,
      },
      states: {
        delete: {
          id: 'delete',
        },
        loaded: {
          id: 'loaded',
          initial: 'creatingConnection',
          states: {
            creatingConnection: {
              entry: ['initConnectionActor', 'initUserAndGroupActor'],
              on: {
                NEXT: {
                  target: 'creatingUserAndGroup',
                  actions: 'updateData',
                },
                DELETE: {
                  target: '#delete',
                },
                SAVE_FORM: {
                  actions: [
                    'clearPassword',
                    'updateUserAndGroupActor',
                    'saveForm',
                  ],
                },
                SAVE: {
                  target: '#loaded',
                  actions: 'logSaveSuccess',
                },
              },
            },
            creatingUserAndGroup: {
              entry: ['updateUserAndGroupActor', 'moveToUserAndGroup'],
              on: {
                CREATE_CONNECTION: {
                  target: 'creatingConnection',
                },
                SAVE: {
                  target: 'success',
                },
                VERIFY_CONNECTION: {
                  actions: 'verifyConnection',
                },
                CONNECTION_STATUS: {
                  actions: 'connectionStatus',
                },
              },
            },
            success: {
              type: 'final',
            },
          },
          onDone: 'saving',
        },
      },
    }),
}).withConfig({
  actions: {
    validate: assign({
      validationErrors: () => {},
    }),
    initConnectionActor: assign({
      createConnection: ({
        isEdit,
        itemId,
        createConnection,
        data,
        pristineData,
      }) => {
        const contextData = {
          data,
          pristineData,
          itemId,
          isEdit,
        };

        return (
          createConnection ||
          spawn(
            LdapServersConfigurationMachine.withContext(contextData),
            'createConnection'
          )
        );
      },

      activeTab: TABS_INDEX.CREATE_CONNECTION,
    }),
    initUserAndGroupActor: assign({
      userAndGroup: (context) => {
        const {isEdit} = context;

        const contextData = {
          data: context.data,
          pristineData: context.pristineData,
          templates: [],
          template: '',
          isEdit,
        };

        return (
          context.userAndGroup ||
          spawn(
            LdapServersUserAndGroupMachine.withContext(contextData),
            'userAndGroup'
          )
        );
      },
    }),
    updateUserAndGroupActor: assign((context, event) => {
      const data = event.type === 'SAVE_FORM' ? event.data : context.data;

      context.userAndGroup.send({
        type: 'UPDATE_DATA',
        data,
      });

      return context;
    }),
    clearPassword: assign((context) =>
      context.userAndGroup.send({type: 'CLEAR_PASSWORD'})
    ),
    connectionStatus: assign((context, event) => {
      const isCorrect = event.data?.data?.result.success;
      const errorMessage = isCorrect ? null : event.data?.message;
      const type = isCorrect ? 'CONNECTION_CORRECT' : 'CONNECTION_ERROR';

      context.userAndGroup.send({
        type,
        errorMessage,
      });

      return context;
    }),
    verifyConnection: assign((context, event) =>
      context.createConnection.send({
        type: 'VERIFY_CONNECTION',
        data: event.data,
        isParent: true,
      })
    ),
    moveToUserAndGroup: assign({
      activeTab: TABS_INDEX.USER_AND_GROUP,
    }),
    saveForm: assign((context) =>
      context.userAndGroup.send({type: 'SAVE', isParent: true})
    ),
    logSaveSuccess: assign((context) => {
      const data = context?.context?.data || context.data;
      const isEdit = context?.isEdit;

      if (isEdit) {
        ExtJS.showSuccessMessage(LABELS.UPDATE_SUCCESS_MESSAGE(data.name));
      } else {
        ExtJS.showSuccessMessage(LABELS.SAVE_SUCCESS_MESSAGE(data.name));
      }
      return context;
    }),
    logDeleteSuccess: ({data}) => {
      ExtJS.showSuccessMessage(LABELS.DELETE_SUCCESS_MESSAGE(data.name));
    },
    updateData: assign((ctx, {data = {}, pristineData = {}}) =>
      mergeDeepRight(ctx, {data, pristineData})
    ),
    setData: assign(({isEdit}, event) => {
      const response = event.data?.data || {};

      if (isEdit) {
        let data = response;

        // There is a mismatch between REST api and Ext api
        data.connectionRetryDelay = response.connectionRetryDelaySeconds;
        data.connectionTimeout = response.connectionTimeoutSeconds;
        data = omit(
          ['connectionRetryDelaySeconds', 'connectionTimeoutSeconds'],
          data
        );
        // Since this values is not part of the payload, it's needed to be able to check the pristine state.
        data.authPassword = initialStateConfiguration.authPassword;

        return {
          data,
          pristineData: data,
        };
      }

      return {
        data: response,
        pristineData: response,
      };
    }),
  },
  services: {
    fetchData: async ({itemId}) => {
      if (!isNil(itemId) && !isEmpty(itemId)) {
        return Axios.get(URL.singleLdapServersUrl(itemId));
      }

      return Promise.resolve({
        data: mergeDeepRight(
          initialStateConfiguration,
          initialStateUserAndGroup
        ),
      });
    },
    saveData: () => Promise.resolve(),
    delete: () => Promise.resolve(),
  },
});
