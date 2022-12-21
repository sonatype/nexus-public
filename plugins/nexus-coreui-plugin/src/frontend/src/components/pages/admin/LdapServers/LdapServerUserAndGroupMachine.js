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
import {mergeDeepRight, omit, dissoc} from 'ramda';

import {isDynamicGroup, isStaticGroup} from './LdapServersHelper';

const {
  EXT: {
    LDAP: {ACTION, METHODS},
  },
  REST: {
    PUBLIC: {LDAP_SERVERS},
  },
} = APIConstants;

const initialState = {
  userBaseDn: '',
  userSubtree: false,
  ldapGroupsAsRoles: true,
  userObjectClass: '',
  userLdapFilter: '',
  userIdAttribute: '',
  userRealNameAttribute: '',
  userEmailAddressAttribute: '',
  userPasswordAttribute: '',
  groupType: 'dynamic',
  userMemberOfAttribute: '',
  groupBaseDn: '',
  groupSubtree: false,
  groupObjectClass: '',
  groupIdAttribute: '',
  groupMemberAttribute: '',
  groupMemberFormat: '',
};

export default FormUtils.buildFormMachine({
  id: 'LdapServersUserAndGroupMachine',
  config: (config) =>
    mergeDeepRight(config, {
      context: {
        templates: [],
        template: '',
        data: initialState,
        pristineData: initialState,
      },
      states: {
        loading: {
          on: {
            CONNECTION_READY: {
              actions: 'saveConnectionData',
            },
          },
        },
        loaded: {
          on: {
            UPDATE_TEMPLATE: {
              target: 'loaded',
              actions: 'updateTemplate',
            },
          },
        },
      },
    }),
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
    setData: assign((ctx, event) => {
      const templates = ExtAPIUtils.extractResult(event.data) || {};

      return {
        ...ctx,
        templates,
      };
    }),
    updateTemplate: assign((ctx, {value}) => {
      const template = ctx.templates.find(
        (template) => template.name === value
      );

      // Omits the template name to no override the current name key.
      const data = dissoc('name', template);

      return mergeDeepRight(ctx, {template: value, data});
    }),
    saveConnectionData: assign((ctx, {data}) => mergeDeepRight(ctx, {data})),
    onSaveSuccess: sendParent('SAVE'),
    logSaveSuccess: () => ({}),
    logSaveError: (_, {data}) => {
      ExtJS.showErrorMessage(data.response.data);
    },
  },
  services: {
    fetchData: () => ExtAPIUtils.extAPIRequest(ACTION, METHODS.READ_TEMPLATES),
    saveData: async ({data}) => {
      let request = data;

      // There is a mismatch between REST api and Ext api
      request = omit(['connectionRetryDelay', 'connectionTimeout'], request);
      request.connectionRetryDelaySeconds = data.connectionRetryDelay;
      request.connectionTimeoutSeconds = data.connectionTimeout;

      if (!data.ldapGroupsAsRoles || isDynamicGroup(data.groupType)) {
        request.groupBaseDn = '';
        request.groupSubtree = false;
        request.groupObjectClass = '';
        request.groupIdAttribute = '';
        request.groupMemberAttribute = '';
        request.groupMemberFormat = '';
      }

      if (!data.ldapGroupsAsRoles || isStaticGroup(data.groupType)) {
        request.userMemberOfAttribute = '';
      }

      return Axios.post(LDAP_SERVERS, request);
    },
  },
});
