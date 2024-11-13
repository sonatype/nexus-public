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
import {assign, spawn} from 'xstate';
import Axios from 'axios';
import {mergeDeepRight} from 'ramda';

import {ExtJS, FormUtils, ValidationUtils, ExtAPIUtils} from '@sonatype/nexus-ui-plugin';

import ExternalRolesComboboxMachine from './ExternalRolesComboboxMachine';

import UIStrings from '../../../../constants/UIStrings';
import {TYPES, EMPTY_DATA, URL} from './RolesHelper';

const {ROLES: {MESSAGES: LABELS}} = UIStrings;
const {rolesUrl, privilegesUrl, sourcesApi, defaultRolesUrl, singleRoleUrl} = URL;

const isEdit = (id) => ValidationUtils.notBlank(id);

const validateId = (id) => {
  if (ValidationUtils.isBlank(id)) {
    return UIStrings.ERROR.FIELD_REQUIRED;
  } else {
    return ValidationUtils.validateLeadingOrTrailingSpace(id)
  }
}

export default FormUtils.buildFormMachine({
  id: 'RolesFormMachine',
  config: (config) =>
      mergeDeepRight(config, {
        context: {
          privilegesListFilter: '',
          rolesListFilter: ''
        },
        states: {
          loaded: {
            on: {
              SET_ROLE_TYPE: {
                target: 'loaded',
                actions: ['clearSaveError', 'resetData', 'setRoleType']
              },
              SET_EXTERNAL_ROLE_TYPE: {
                target: 'loaded',
                actions: [
                    'clearSaveError',
                    'resetData',
                    'setExternalRoleType',
                    'initExternalRolesActor',
                    'sendDataToActor',
                    'sendLdapDataToActor'
                ],
              },
              UPDATE_ROLES: {
                target: 'loaded',
                actions: ['updateRoles']
              },
              UPDATE_PRIVILEGES: {
                target: 'loaded',
                actions: ['updatePrivileges']
              },
              SET_PRIVILEGES_LIST_FILTER: {
                target: 'loaded',
                actions: ['updatePrivilegesListFilter']
              },
              SET_ROLES_LIST_FILTER: {
                target: 'loaded',
                actions: ['updateRolesListFilter']
              }
            }
          },
        }
      })
}).withConfig({
  actions: {
    initExternalRolesActor: assign({
      externalRolesRef: ({externalRolesRef}) =>
          externalRolesRef || spawn(ExternalRolesComboboxMachine, 'externalRolesCombobox'),
    }),
    sendDataToActor: ({externalRoleType, externalRolesRef}) => externalRolesRef.send(
        {type: 'UPDATE_TYPE', externalRoleType}, {to: 'externalRolesCombobox'},
    ),
    sendLdapDataToActor: ({externalRolesRef, ldapQueryCharacterLimit}) => externalRolesRef.send(
      {type: 'UPDATE_LDAP_LIMIT', ldapQueryCharacterLimit}, {to: 'externalRolesCombobox'},
  ),
    validate: assign({
      validationErrors: ({data}) => ({
        id: validateId(data?.id),
        name: ValidationUtils.validateNotBlank(data.name),
      })
    }),
    setData: assign({
      roles: (_, {data: [roles]}) => roles.data,
      privileges: (_, {data: [, privileges]}) => privileges.data,
      sources: (_, {data: [, , sources]}) => sources.data,
      data: (_, {data: [, , , role]}) => role.data,
      pristineData: (_, {data: [, , , role]}) => role.data,
    }),
    setRoleType: assign({
      roleType: (_, {roleType}) => roleType
    }),
    setExternalRoleType: assign({
      roleType: TYPES.EXTERNAL,
      externalRoleType: (_, {externalRoleType}) => externalRoleType,
    }),
    resetData: assign({
      data: () => EMPTY_DATA,
      roleType: '',
      externalRoleType: '',
    }),
    updateRoles: assign({
      data: ({data}, {newRoles}) => ({...data, roles: newRoles}),
    }),
    updatePrivileges: assign({
      data: ({data}, {newPrivileges}) => ({...data, privileges: newPrivileges}),
    }),
    updatePrivilegesListFilter: assign({
      privilegesListFilter: (_, {privilegesListFilter}) => privilegesListFilter
    }),
    updateRolesListFilter: assign({
      rolesListFilter: (_, {rolesListFilter}) => rolesListFilter
    }),
    onDeleteError: (_, event) => ExtJS.showErrorMessage(event.data?.response?.data),
    logDeleteSuccess: ({data}) => ExtJS.showSuccessMessage(LABELS.DELETE_SUCCESS(data.name)),
  },
  services: {
    fetchData: ({pristineData: {id}}) => {
      return Axios.all([
        Axios.get(defaultRolesUrl),
        Axios.get(privilegesUrl),
        isEdit(id)
            ? Promise.resolve({data: []} )
            : ExtAPIUtils.extAPIRequest(sourcesApi.action, sourcesApi.method).then(v => v.data.result),
        isEdit(id)
            ? Axios.get(singleRoleUrl(id))
            : Promise.resolve({data: EMPTY_DATA}),
      ]);
    },
    saveData: ({data, pristineData: {id}}) => {
      if (isEdit(id)) {
        return Axios.put(singleRoleUrl(data.id), data);
      } else {
        return Axios.post(rolesUrl, data);
      }
    },
    confirmDelete: ({data}) => ExtJS.requestConfirmation({
      title: LABELS.CONFIRM_DELETE.TITLE,
      message: LABELS.CONFIRM_DELETE.MESSAGE(data.name),
      yesButtonText: LABELS.CONFIRM_DELETE.YES,
      noButtonText: LABELS.CONFIRM_DELETE.NO
    }),
    delete: ({data}) => Axios.delete(singleRoleUrl(data.id)),
  }
});
