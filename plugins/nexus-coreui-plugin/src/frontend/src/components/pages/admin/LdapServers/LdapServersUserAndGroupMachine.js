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
import {assign, sendParent} from 'xstate';
import Axios from 'axios';
import {
  FormUtils,
  ExtAPIUtils,
  APIConstants,
  ValidationUtils,
  ExtJS,
} from '@sonatype/nexus-ui-plugin';
import {
  mergeDeepRight,
  omit,
  dissoc,
  lensProp,
  set,
  toUpper,
  modify,
} from 'ramda';

import {
  isDynamicGroup,
  isStaticGroup,
  isAnonymousAuth,
  URL,
} from './LdapServersHelper';

import UIStrings from '../../../../constants/UIStrings';

const {
  LDAP_SERVERS: {FORM: LABELS},
} = UIStrings;

const {singleLdapServersUrl, ldapServersUrl} = URL;

const {
  EXT: {
    LDAP: {ACTION, METHODS},
  },
} = APIConstants;

export const initialState = {
  userBaseDn: '',
  userSubtree: false,
  ldapGroupsAsRoles: true,
  userObjectClass: '',
  userLdapFilter: '',
  userIdAttribute: '',
  userRealNameAttribute: '',
  userEmailAddressAttribute: '',
  userPasswordAttribute: '',
  groupType: LABELS.GROUP_TYPE.OPTIONS.dynamic.id,
  userMemberOfAttribute: null,
  groupBaseDn: null,
  groupSubtree: false,
  groupObjectClass: null,
  groupIdAttribute: null,
  groupMemberAttribute: null,
  groupMemberFormat: null,
};

export default FormUtils.buildFormMachine({
  id: 'LdapServersUserAndGroupMachine',
  config: (config) =>
    mergeDeepRight(config, {
      states: {
        saving: {
          id: 'saving',
        },
        loaded: {
          id: 'loaded',
          on: {
            SAVE: [
              {
                target: 'askingPassword.verifyingConnection',
                cond: 'isEditAndAnonymous',
              },
              {
                target: 'askingPassword',
                cond: 'isEdit',
              },
              {
                target: 'saving',
                cond: 'canSave',
              },
            ],
            UPDATE_TEMPLATE: {
              target: 'loaded',
              actions: 'updateTemplate',
            },
            UPDATE_DATA: {
              target: 'loaded',
              actions: 'updateData',
            },
            CLEAR_PASSWORD: {
              actions: ['clearPassword', 'setIsPristine'],
            },
            VERIFY_LOGIN: [
              {
                target: 'askingPassword.verifyingLogin',
                cond: 'shouldAskPassword'
              },
              {
                target: 'showingVerifyLoginModal'
              }
            ],
            VERIFY_USER_MAPPING: [
              {
                target: 'askingPassword.verifyingUserMapping',
                cond: 'shouldAskPassword'
              },
              {
                target: 'showingVerifyUserMappingModal'
              }
            ]
          }
        },
        askingPassword: {
          initial: 'idle',
          on: {
            SET_PASSWORD: {
              target: '.verifyingConnection',
              actions: ['update', 'validate', 'setDirtyFlag', 'setIsPristine'],
            },
          },
          states: {
            idle: {},
            verifyingConnection: {
              entry: 'verifyConnection',
              on: {
                CONNECTION_CORRECT: {
                  target: '#saving',
                },
                CONNECTION_ERROR: {
                  target: '#loaded',
                  actions: 'showPasswordError',
                },
              },
            },
            verifyingLogin: {
              on: {
                SET_PASSWORD: {
                  target: '#showingVerifyLoginModal',
                  actions: ['update', 'validate', 'setDirtyFlag', 'setIsPristine']
                }
              }
            },
            verifyingUserMapping: {
              on: {
                SET_PASSWORD: {
                  target: '#showingVerifyUserMappingModal',
                  actions: ['update', 'validate', 'setDirtyFlag', 'setIsPristine']
                }
              }
            }
          }
        },
        showingVerifyLoginModal: {
          id: 'showingVerifyLoginModal'
        },
        showingVerifyUserMappingModal: {
          id: 'showingVerifyUserMappingModal'
        }
      },
      on: {
        CANCEL: {
          target: 'loaded'
        }
      }
    })
}).withConfig({
  actions: {
    validate: assign({
      validationErrors: ({data}) => {
        let dynamicGroupsValidation = {};
        let staticGroupsValidation = {};

        if (data.ldapGroupsAsRoles && isDynamicGroup(data.groupType)) {
          dynamicGroupsValidation = {
            userMemberOfAttribute: ValidationUtils.validateNotBlank(
              data.userMemberOfAttribute
            ),
          };
        }

        if (data.ldapGroupsAsRoles && isStaticGroup(data.groupType)) {
          staticGroupsValidation = {
            groupObjectClass: ValidationUtils.validateNotBlank(
              data.groupObjectClass
            ),
            groupIdAttribute: ValidationUtils.validateNotBlank(
              data.groupIdAttribute
            ),
            groupMemberAttribute: ValidationUtils.validateNotBlank(
              data.groupMemberAttribute
            ),
            groupMemberFormat: ValidationUtils.validateNotBlank(
              data.groupMemberFormat
            ),
          };
        }

        return {
          userObjectClass: ValidationUtils.validateNotBlank(
            data.userObjectClass
          ),
          userIdAttribute: ValidationUtils.validateNotBlank(
            data.userIdAttribute
          ),
          userRealNameAttribute: ValidationUtils.validateNotBlank(
            data.userRealNameAttribute
          ),
          userEmailAddressAttribute: ValidationUtils.validateNotBlank(
            data.userEmailAddressAttribute
          ),
          ...dynamicGroupsValidation,
          ...staticGroupsValidation,
        };
      },
    }),
    clearPassword: assign({
      pristineData: ({pristineData}) =>
        set(lensProp('authPassword'), '', pristineData),
    }),
    showPasswordError: assign({
      saveError: (_, event) => event.errorMessage,
      saveErrors: (_, event) => ({
        authPassword: event.errorMessage,
      }),
      saveErrorData: ({data}) => data,
    }),
    verifyConnection: sendParent(({data}) => ({
      type: 'VERIFY_CONNECTION',
      data,
    })),
    setData: assign((ctx, event) => {
      const templates = ExtAPIUtils.extractResult(event.data) || ctx.templates;

      return mergeDeepRight(ctx, {templates});
    }),
    updateData: assign({
      data: (ctx, {data}) => mergeDeepRight(ctx.data, data),
    }),
    updateTemplate: assign((ctx, {value}) => {
      const template = ctx.templates.find(
        (template) => template.name === value
      );

      // Omits the template name to no override the current name key.
      let data = dissoc('name', template);

      data = modify('groupType', toUpper, data);

      return mergeDeepRight(ctx, {template: value, data});
    }),
    onSaveSuccess: sendParent('SAVE'),
    logSaveSuccess: assign((context) => context),
    logSaveError: (_, {data}) => {
      ExtJS.showErrorMessage(data.response.data);
    },
  },
  guards: {
    isEdit: (context, event) => !event.isParent && context.isEdit,
    isEditAndAnonymous: (context) =>
      context.isEdit && isAnonymousAuth(context.data?.authScheme),
    shouldAskPassword: (context, event) =>
      !event.isParent && context.isEdit && !context.data.authPassword
  
  },
  services: {
    fetchData: async () =>
      await ExtAPIUtils.extAPIRequest(ACTION, METHODS.READ_TEMPLATES),
    saveData: async ({data, isEdit}) => {
      let request = data;

      // There is a mismatch between REST api and Ext api
      request = omit(['connectionRetryDelay', 'connectionTimeout'], request);
      request.connectionRetryDelaySeconds = data.connectionRetryDelay;
      request.connectionTimeoutSeconds = data.connectionTimeout;

      if (!data.ldapGroupsAsRoles || isDynamicGroup(data.groupType)) {
        request.groupBaseDn = initialState.groupBaseDn;
        request.groupSubtree = initialState.groupSubtree;
        request.groupObjectClass = initialState.groupObjectClass;
        request.groupIdAttribute = initialState.groupIdAttribute;
        request.groupMemberAttribute = initialState.groupMemberAttribute;
        request.groupMemberFormat = initialState.groupMemberFormat;
      }

      if (!data.ldapGroupsAsRoles || isStaticGroup(data.groupType)) {
        request.userMemberOfAttribute = initialState.userMemberOfAttribute;
      }

      if (isEdit) {
        return Axios.put(singleLdapServersUrl(request.name), request);
      }

      return Axios.post(ldapServersUrl, request);
    },
  },
});
