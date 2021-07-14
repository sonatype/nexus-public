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
import {assign} from 'xstate';
import {ExtJS, FormUtils, ValidationUtils} from '@sonatype/nexus-ui-plugin';
import {map, omit, propOr, replace, lensPath, set} from 'ramda';

import UIStrings from '../../../../constants/UIStrings';
import Axios from "axios";

/*
 * Used to extract token strings "${name}" => "name"
 */
const tokenMatcher = /\${(.*?)}/g;
function stripTokenCharacters(match) {
  return match.replace('${', '').replace('}', '');
}

function deriveDynamicFieldData(type) {
  // Check for blobstore specific initializer
  const init = window.BlobStoreTypes[type.id]?.Actions?.init || (() => ({}));
  const result = init();

  type?.fields?.forEach(field => {
    if (field.initialValue) {
      result[field.id] = field.initialValue;
    }
    else if (field.type === 'string') {
      result[field.id] = '';
    }
  });

  return result;
}

export default FormUtils.buildFormMachine({
  id: 'BlobStoresFormMachine',

  config: (config) => ({
    ...config,

    context: {
      ...config.context,
      type: '',
      types: []
    },

    states: {
      ...config.states,

      loaded: {
        ...config.states.loaded,
        on: {
          ...config.states.loaded.on,

          SET_TYPE: {
            target: 'loaded',
            actions: ['setType'],
            internal: false
          },

          UPDATE_SOFT_QUOTA: {
            target: 'loaded',
            actions: ['toggleSoftQuota'],
            internal: false
          },

          UPDATE_CUSTOM_SETTINGS: {
            target: 'loaded',
            actions: ['updateCustomData'],
            internal: false
          },

          SAVE: [{
            target: 'confirmSave',
            cond: 'isEdit'
          }, {
              target: 'saving',
              cond: 'canSave'
          }],

          PROMOTE_TO_GROUP: {
            target: 'confirmPromote'
          }
        }
      },
      confirmSave: {
        invoke: {
          src: 'confirmSave',
          onDone: 'saving',
          onError: 'loaded'
        }
      },
      confirmPromote: {
        invoke: {
          src: 'confirmPromote',
          onDone: 'promoteToGroup',
          onError: 'loaded'
        }
      },
      promoteToGroup: {
        invoke: {
          src: 'promoteToGroup',
          onDone: {
            target: 'loaded',
            actions: ['onSaveSuccess']
          },
          onError: {
            target: 'loaded',
            actions: ['onPromoteError']
          }
        }
      }
    }
  })
}).withConfig({
  actions: {
    setData: assign(({pristineData}, event) => {
      const types = event.data[0]?.data;
      const quotaTypes = event.data[1]?.data;
      const {softQuota, ...otherData} = event.data[2]?.data;

      const backendData = {
        ...otherData,
        name: pristineData?.name,
        softQuota: {
          enabled: softQuota?.type || softQuota?.limit,
          type: propOr('', 'type', softQuota),
          limit: propOr('', 'limit', softQuota)
        }
      };

      const type = types?.find(type => pristineData.type === type.id.toLowerCase());
      type?.fields?.forEach(field => {
        if (field.type === 'itemselect') {
          field.attributes.options = field.attributes.options.filter(name => name !== pristineData.name);
        }
      });

      return {
        data: backendData,
        pristineData: backendData,
        types,
        type: type,
        quotaTypes,
        repositoryUsage: event.data[3]?.data?.repositoryUsage || 0,
        blobStoreUsage: event.data[3]?.data?.blobStoreUsage || 0
      };
    }),

    setType: assign({
      type: ({types}, {value}) => types.find(type => type.id.toLowerCase() === value.toLowerCase()),
      data: ({data, types}, {value}) => {
        const type = types.find(type => type.id === value);
        return {
          name: '',
          ...deriveDynamicFieldData(type),
          softQuota: {
            enabled: false,
            type: '',
            limit: ''
          }
        };
      },

      pristineData: ({data, types}, {value}) => {
        const type = types.find(type => type.id === value);
        return {
          name: '',
          ...deriveDynamicFieldData(type),
          softQuota: {
            enabled: false,
            type: '',
            limit: ''
          }
        };
      }
    }),

    toggleSoftQuota: assign({
      data: ({data}, {name, value}) => {
        const softQuota = data.softQuota || {};
        return {
          ...data,
          softQuota: {
            limit: '',
            type: '',
            ...softQuota,
            [name]: value
          }
        }
      },
      isTouched: ({isTouched}, {name, value}) => {
        if (name === 'enabled' && value === false) {
          return {
            ...isTouched,
            softQuota: {
              enabled: true
            }
          };
        }
        return {
          ...isTouched,
          softQuota: {
            ...(isTouched.softQuota || {}),
            [name]: true
          }
        };
      }
    }),

    validate: assign({
      validationErrors: ({data, type}) => {
        const Actions = type && window.BlobStoreTypes[type.id]?.Actions;
        const validationErrors = {
          ...(Actions?.validation(data) || {}),
          type: ValidationUtils.validateNotBlank(type),
          name: ValidationUtils.validateNotBlank(data.name)
        };

        type?.fields?.forEach(field => {
          if (field.required) {
            validationErrors[field.id] = ValidationUtils.validateNotBlank(data[field.id]);
          }
        });

        if (data.softQuota.enabled) {
          validationErrors.softQuota = {
            type: ValidationUtils.validateNotBlank(data.softQuota.type),
            limit: ValidationUtils.isInRange({
              value: data.softQuota.limit,
              min: 1,
              allowDecimals: false
            }) || ValidationUtils.validateNotBlank(data.softQuota.limit)
          };
        }

        return validationErrors;
      }
    }),

    onLoadedEntry: assign({
      data: ({data, pristineData, isTouched, type}) => {
        const newData = {...data};
        type?.fields?.filter(field => {
          return field.attributes?.tokenReplacement !== null &&
              !isTouched[field.id] &&
              pristineData[field.id] === '';
        })?.forEach(field => {
          const tokenReplacement = match => data[stripTokenCharacters(match)];
          newData[field.id] = replace(tokenMatcher, tokenReplacement, field.attributes.tokenReplacement);
        });
        return newData;
      }
    }),

    onDeleteError: (_, event) => ExtJS.showErrorMessage(event.data?.message),

    onPromoteError: (_, event) => ExtJS.showErrorMessage(event.data?.response?.data),

    logSaveError: (_, event) => {
      let saveError = event.data?.response?.data ? event.data.response.data : UIStrings.ERROR.SAVE_ERROR;
      ExtJS.showErrorMessage(saveError);
      console.log(`Save Error: ${saveError}`);
    }
  },
  guards: {
    isEdit: ({pristineData, isPristine, validationErrors}) => {
      const isValid = !ValidationUtils.isInvalid(validationErrors);
      const isEdit = ValidationUtils.notBlank(pristineData.name);
      return isEdit && !isPristine && isValid;
    },
    canDelete: ({blobStoreUsage, repositoryUsage}) => blobStoreUsage === 0 && repositoryUsage === 0
  },
  services: {
    fetchData: ({pristineData}) => {
      if (ValidationUtils.notBlank(pristineData.name)) {
        return axios.all([
          axios.get('/service/rest/internal/ui/blobstores/types'),
          axios.get('/service/rest/internal/ui/blobstores/quotaTypes'),
          axios.get(`/service/rest/v1/blobstores/${pristineData.type}/${pristineData.name}`),
          axios.get(`/service/rest/internal/ui/blobstores/usage/${pristineData.name}`)
        ]);
      }
      else {
        return axios.all([
          axios.get('/service/rest/internal/ui/blobstores/types'),
          axios.get('/service/rest/internal/ui/blobstores/quotaTypes'),
          Promise.resolve({data: {}}),
          Promise.resolve({data: 0})
        ]);
      }
    },

    saveData: ({data, pristineData, type}) => {
      let saveData = data.softQuota.enabled ? data : omit(['softQuota'], data);
      saveData = map((value) => typeof value === 'string' ? value.trim() : value, saveData);
      if (ValidationUtils.notBlank(pristineData.name)) {
        return axios.put(`/service/rest/v1/blobstores/${type.id}/${data.name}`, saveData);
      }
      else {
        return axios.post(`/service/rest/v1/blobstores/${type.id}`, saveData);
      }
    },

    confirmSave: () => ExtJS.requestConfirmation({
      title: UIStrings.BLOB_STORES.MESSAGES.CONFIRM_SAVE.TITLE,
      yesButtonText: UIStrings.BLOB_STORES.MESSAGES.CONFIRM_SAVE.YES,
      noButtonText: UIStrings.BLOB_STORES.MESSAGES.CONFIRM_SAVE.NO,
      message: UIStrings.BLOB_STORES.MESSAGES.CONFIRM_SAVE.MESSAGE
    }),

    confirmDelete: ({data}) => ExtJS.requestConfirmation({
      title: UIStrings.BLOB_STORES.MESSAGES.CONFIRM_DELETE.TITLE,
      yesButtonText: UIStrings.BLOB_STORES.MESSAGES.CONFIRM_DELETE.YES,
      noButtonText: UIStrings.BLOB_STORES.MESSAGES.CONFIRM_DELETE.NO,
      message: data.name
    }),

    confirmPromote: () => ExtJS.requestConfirmation({
      title: UIStrings.BLOB_STORES.MESSAGES.CONFIRM_PROMOTE.TITLE,
      yesButtonText: UIStrings.BLOB_STORES.MESSAGES.CONFIRM_PROMOTE.YES,
      noButtonText: UIStrings.BLOB_STORES.MESSAGES.CONFIRM_PROMOTE.NO,
      message: UIStrings.BLOB_STORES.MESSAGES.CONFIRM_PROMOTE.MESSAGE
    }),

    delete: ({data}) => Axios.delete(`/service/rest/v1/blobstores/${data.name}`),

    promoteToGroup: ({data}) => Axios.post(`/service/rest/v1/blobstores/group/promote/${data.name}`)
  }
});
