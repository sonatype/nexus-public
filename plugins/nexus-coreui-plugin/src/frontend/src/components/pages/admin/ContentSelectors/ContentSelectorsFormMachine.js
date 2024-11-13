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

import {ExtJS, FormUtils, ValidationUtils} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

const baseUrl = 'service/rest/v1/security/content-selectors';
const url = (name) => `${baseUrl}/${name}`;

function isEdit({name}) {
  return ValidationUtils.notBlank(name);
}

export default FormUtils.buildFormMachine({
  id: 'ContentSelectorsFormMachine',
}).withConfig({
  actions: {
    validate: assign({
      validationErrors: ({data}) => ({
        name: ValidationUtils.validateNameField(data.name),
        expression: ValidationUtils.isBlank(data.expression) ? UIStrings.ERROR.FIELD_REQUIRED : null
      })
    }),

    onDeleteError: ({data}) => ExtJS.showErrorMessage(UIStrings.CONTENT_SELECTORS.MESSAGES.DELETE_ERROR(data.name)),

    setSaveError: assign({
      saveErrorData: ({data}) => data,
      saveErrors: ({data}, event) => {
        const error = event.data?.response?.data;

        if (typeof error === 'string') {
          if (error.includes('ORecordDuplicatedException')) {
            return {
              name: UIStrings.CONTENT_SELECTORS.MESSAGES.DUPLICATE_ERROR(data.name)
            };
          }
          else if (error.includes('OTooBigIndexKeyException')) {
            return {
              name: UIStrings.CONTENT_SELECTORS.MESSAGES.NAME_TOO_LONG
            }
          }
        }

        if (error instanceof Array) {
          let errors = {};
          error.forEach(e => {
            if (e.id.includes('PARAMETER ')) {
              errors[e.id.replace(/PARAMETER /, '')] = e.message;
            }
            else if (e.id === 'HelperBean.expression') {
              errors.expression = e.message;
            }
            else if (e.id === 'name') {
              errors.name = e.message;
            }
            else {
              console.error('Unhandled backend error', e);
            }
          });
          return errors;
        }

        return {};
      }
    })
  },
  guards: {
    isEdit: ({pristineData}) => isEdit(pristineData),
    canDelete: () => true
  },
  services: {
    fetchData: ({pristineData}) => {
      if (isEdit(pristineData)) {
        return Axios.get(url(pristineData.name));
      }
      else { // New
        return Promise.resolve({
          data: {
            name: '',
            type: 'CSEL',
            description: '',
            expression: ''
          }
        });
      }
    },
    saveData: ({data, pristineData}) => {
      if (isEdit(pristineData)) {
        return Axios.put(url(data.name), {
          description: data.description,
          expression: data.expression
        });
      }
      else { // New
        return Axios.post(baseUrl, {
          name: data.name,
          description: data.description,
          expression: data.expression
        });
      }
    },

    confirmDelete: ({data}) => ExtJS.requestConfirmation({
      title: UIStrings.CONTENT_SELECTORS.MESSAGES.CONFIRM_DELETE.TITLE,
      message: UIStrings.CONTENT_SELECTORS.MESSAGES.CONFIRM_DELETE.MESSAGE(data.name),
      yesButtonText: UIStrings.CONTENT_SELECTORS.MESSAGES.CONFIRM_DELETE.YES,
      noButtonText: UIStrings.CONTENT_SELECTORS.MESSAGES.CONFIRM_DELETE.NO
    }),

    delete: ({data}) => Axios.delete(url(data.name))
  }
});
