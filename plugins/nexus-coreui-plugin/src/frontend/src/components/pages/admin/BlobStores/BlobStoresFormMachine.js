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
import {ExtJS, Utils} from '@sonatype/nexus-ui-plugin';
import {map, omit, propOr, replace} from 'ramda';

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
  const result = {};
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

export default Utils.buildFormMachine({
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

          SAVE: [{
            target: 'confirmSave',
            cond: 'isEdit'
          }, {
              target: 'saving',
              cond: 'canSave'
          }],
        }
      },
      confirmSave: {
        invoke: {
          src: 'confirmSave',
          onDone: 'saving',
          onError: 'loaded'
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
      return {
        data: backendData,
        pristineData: backendData,
        types,
        type: types?.find(type => pristineData.type === type.id.toLowerCase()),
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
        const validationErrors = {
          softQuota: {}
        };

        if (Utils.isBlank(type)) {
          validationErrors.type = UIStrings.ERROR.FIELD_REQUIRED;
        }

        if (Utils.isBlank(data.name)) {
          validationErrors.name = UIStrings.ERROR.FIELD_REQUIRED;
        }

        type?.fields?.forEach(field => {
          if (field.required && Utils.isBlank(data[field.id])) {
            validationErrors[field.id] = UIStrings.ERROR.FIELD_REQUIRED;
          }
        });

        if (data.softQuota.enabled) {
          if (Utils.isBlank(data.softQuota.type)) {
            validationErrors.softQuota.type = UIStrings.ERROR.FIELD_REQUIRED;
          }
          if (Utils.isBlank(data.softQuota.limit)) {
            validationErrors.softQuota.limit = UIStrings.ERROR.FIELD_REQUIRED;
          }
          else {
            validationErrors.softQuota.limit = Utils.isInRange({value: data.softQuota.limit, min: 1});
          }
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

    onDeleteError: (_, event) => ExtJS.showErrorMessage(event.data?.message)
  },
  guards: {
    isEdit: ({pristineData, isPristine, validationErrors}) => {
      const isValid = !Utils.isInvalid(validationErrors);
      const isEdit = Utils.notBlank(pristineData.name);
      return isEdit && !isPristine && isValid;
    },
    canDelete: ({blobStoreUsage, repositoryUsage}) => blobStoreUsage === 0 && repositoryUsage === 0
  },
  services: {
    fetchData: ({pristineData}) => {
      if (Utils.notBlank(pristineData.name)) {
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
      const typeId = type.id.toLowerCase();
      if (Utils.notBlank(pristineData.name)) {
        return axios.put(`/service/rest/v1/blobstores/${typeId}/${data.name}`, saveData);
      }
      else {
        return axios.post(`/service/rest/v1/blobstores/${typeId}`, saveData);
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

    delete: ({data}) => Axios.delete(`/service/rest/v1/blobstores/${data.name}`)
  }
});
