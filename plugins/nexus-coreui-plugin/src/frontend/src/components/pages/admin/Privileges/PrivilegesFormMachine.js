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
import Axios from 'axios';
import {mergeDeepRight} from 'ramda';

import {
  ExtJS,
  FormUtils,
  ValidationUtils,
  ExtAPIUtils,
  APIConstants,
  FormFieldsFactory,
} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';
import {EMPTY_DATA, URL, modifyFormFields, convertActionsToArray, convertActionsToString} from './PrivilegesHelper';

const {EXT: {PRIVILEGE: {ACTION, METHODS: {READ_TYPES}}, }} = APIConstants;
const {PRIVILEGES: {MESSAGES: LABELS}} = UIStrings;

const {singlePrivilegeUrl, updatePrivilegeUrl, createPrivilegeUrl} = URL;

const isEdit = (name) => ValidationUtils.notBlank(name);

function validateName(name) {
  if (ValidationUtils.isBlank(name)) {
    return UIStrings.ERROR.FIELD_REQUIRED;
  }
  else if (!ValidationUtils.isName(name)) {
    return UIStrings.ERROR.INVALID_NAME_CHARS;
  }
  return null;
}

export default FormUtils.buildFormMachine({
  id: 'PrivilegesFormMachine',
  config: (config) =>
      mergeDeepRight(config, {
        states: {
          loaded: {
            on: {
              SET_TYPE: {
                target: 'loaded',
                actions: ['clearSaveError', 'resetData']
              },
            },
          },
        },
      })
}).withConfig({
  actions: {
    validate: assign({
      validationErrors: ({data, types}) => ({
        name: ValidationUtils.validateNotBlank(data.name) || ValidationUtils.validateName(data.name),
        type: ValidationUtils.validateNotBlank(data.type),
        ...FormFieldsFactory.getValidations(data, types),
      })
    }),
    setData: assign((_, {data: [types, privilege]}) => {
      const data = convertActionsToString(privilege.data);
      return {
        types: modifyFormFields(types.data),
        data,
        pristineData: data,
      };
    }),
    resetData: assign({
      data: ({types}, {privilegeType}) => ({
        ...EMPTY_DATA,
        ...FormFieldsFactory.defaultValues(privilegeType, types),
        type: privilegeType,
      }),
    }),
    onDeleteError: (_, event) => ExtJS.showErrorMessage(event.data?.response?.data),
    logDeleteSuccess: ({data}) => ExtJS.showSuccessMessage(LABELS.DELETE_SUCCESS(data.name)),
  },
  services: {
    fetchData: ({pristineData: {name}}) => {
      return Axios.all([
        ExtAPIUtils.extAPIRequest(ACTION, READ_TYPES).then(v => v.data.result),
        isEdit(name)
            ? Axios.get(singlePrivilegeUrl(name))
            : Promise.resolve({data: EMPTY_DATA}),
      ]);
    },
    saveData: ({data, pristineData: {name}}) => {
      const requestData = convertActionsToArray(data);
      if (isEdit(name)) {
        return Axios.put(updatePrivilegeUrl(data.type, name), requestData);
      } else {
        return Axios.post(createPrivilegeUrl(data.type), requestData);
      }
    },
    confirmDelete: ({data}) => ExtJS.requestConfirmation({
      title: LABELS.CONFIRM_DELETE.TITLE,
      message: LABELS.CONFIRM_DELETE.MESSAGE(data.name),
      yesButtonText: LABELS.CONFIRM_DELETE.YES,
      noButtonText: LABELS.CONFIRM_DELETE.NO
    }),
    delete: ({data}) => Axios.delete(singlePrivilegeUrl(data.name)),
  }
});
