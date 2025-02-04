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
import {clone, map, omit, propOr, replace} from 'ramda';

import {ExtJS, FormUtils, UnitUtil, ValidationUtils} from '@sonatype/nexus-ui-plugin';
import UIStrings from '../../../../constants/UIStrings';

import {URLs} from './BlobStoresHelper';

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

export const SPACE_USED_QUOTA_ID = 'spaceUsedQuota';

export const canUseSpaceUsedQuotaOnly = (storeType) =>
  ['azure', 's3', 'google'].includes(storeType.id);

function extractErrorMessage(event) {
  let saveErrors = event.data?.response?.data ? event.data.response.data : UIStrings.ERROR.SAVE_ERROR;
  let errorMessage = '';
  // try to extract error message from errors from response
  if (Array.isArray(saveErrors)) {
    let error = saveErrors[0];
    if (error) {
      errorMessage = error.message;
    }
  } else if (typeof saveErrors === 'object') {
    errorMessage = saveErrors.message;
  } else {
    errorMessage = saveErrors;
  }
  return errorMessage ? errorMessage : UIStrings.ERROR.SAVE_ERROR;
}

const bucketRegionMismatchException = UIStrings.BLOB_STORES.GOOGLE.ERROR.bucketRegionMismatchException;
const bucketEncryptionMismatchException = UIStrings.BLOB_STORES.GOOGLE.ERROR.bucketEncryptionMismatchException;

function transformIfNecessary(context, errorMessage) {
  if (errorMessage && errorMessage.includes(bucketRegionMismatchException)) {
    context.bucketRegionMismatch = true;
    return 'Error cause' + errorMessage.substring(
        errorMessage.indexOf(bucketRegionMismatchException) + bucketRegionMismatchException.length);
  }
  if (errorMessage && errorMessage.includes(bucketEncryptionMismatchException)) {
    context.bucketEncryptionMismatch = true;
    return 'Error cause' + errorMessage.substring(
        errorMessage.indexOf(bucketEncryptionMismatchException) + bucketEncryptionMismatchException.length);
  }
  return errorMessage;
}

function messageToShow(errorMessage) {
  if (errorMessage && errorMessage.includes(bucketRegionMismatchException)) {
    return UIStrings.BLOB_STORES.GOOGLE.ERROR.bucketRegionMismatchMessage;
  }
  if (errorMessage && errorMessage.includes(bucketEncryptionMismatchException)) {
    return UIStrings.BLOB_STORES.GOOGLE.ERROR.bucketEncryptionMismatchMessage;
  }
  return errorMessage;
}

export default FormUtils.buildFormMachine({
  id: 'BlobStoresFormMachine',

  config: (config) => ({
    ...config,

    context: {
      ...config.context,
      type: '',
      types: [],
      bucketRegionMismatch: false,
      bucketEncryptionMismatch: false
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

          MODAL_CONVERT_TO_GROUP_OPEN: {
            target: 'modalConvertToGroup'
          },

          SET_FILES: {
            target: 'loaded',
            actions: ['setFile']
          }
        }
      },
      confirmSave: {
        invoke: {
          src: 'confirmSave',
          onDone: 'saving',
          onError: {
            target: 'loaded',
            actions: ['setSaveError', 'logSaveError']
          }
        }
      },
      modalConvertToGroup: {
        on: {
          MODAL_CONVERT_TO_GROUP_CLOSE: {
            target: 'loaded',
          },
        },
      },
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
          limit: UnitUtil.bytesToMegaBytes(propOr('', 'limit', softQuota))
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
      data: ({data, type}, {name, value}) => {
        const softQuota = data.softQuota || {};
        const newSoftQuota = {
          limit: '',
          type: '',
          ...softQuota,
          [name]: value
        }
        if (canUseSpaceUsedQuotaOnly(type)) {
          newSoftQuota.type = SPACE_USED_QUOTA_ID;
        }
        return {
          ...data,
          softQuota: newSoftQuota
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
      validationErrors: ({pristineData, data, type}) => {
        const isCreate = !ValidationUtils.notBlank(pristineData.name);
        const Actions = type && window.BlobStoreTypes[type.id]?.Actions;
        const validationErrors = {
          ...(Actions?.validation(data, pristineData) || {}),
          type: ValidationUtils.validateNotBlank(type),
          name: isCreate ? ValidationUtils.validateNameField(data.name) : null,
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
          if (field.attributes.tokenReplacement) {
            const tokenReplacement = match => data[stripTokenCharacters(match)];
            newData[field.id] = replace(tokenMatcher, tokenReplacement, field.attributes.tokenReplacement);
          }
        });
        return newData;
      }
    }),

    setFile: assign({
      data: ({data}, {file}) => {
        return {
          ...data,
          bucketConfiguration: {
            ...data.bucketConfiguration,
            bucketSecurity: {
              ...data.bucketConfiguration.bucketSecurity,
              file: file
            }
          }
        };
      },
    }),

    setSaveError: assign({
      saveErrorData: ({data}) => data,
      saveError: (context, event) => {
        let errorMessage = extractErrorMessage(event);

        return errorMessage ? transformIfNecessary(context, errorMessage) : UIStrings.ERROR.SAVE_ERROR;
      }
    }),
    
    onDeleteError: (_, event) => ExtJS.showErrorMessage(event.data?.message),

    logSaveError: (_, event) => {
      let errorMessage = extractErrorMessage(event);

      ExtJS.showErrorMessage(messageToShow(errorMessage));
      console.log(`Save Error: ${errorMessage}`);
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
          axios.get(URLs.blobStoreTypesUrl),
          axios.get(URLs.blobStoreQuotaTypesUrl),
          axios.get(URLs.singleBlobStoreUrl(pristineData.type, pristineData.name)),
          axios.get(URLs.blobStoreUsageUrl(pristineData.name)),
        ]);
      }
      else {
        return axios.all([
          axios.get(URLs.blobStoreTypesUrl),
          axios.get(URLs.blobStoreQuotaTypesUrl),
          Promise.resolve({data: {}}),
          Promise.resolve({data: 0}),
        ]);
      }
    },

    saveData: async ({data, pristineData, type}) => {
      let saveData = data.softQuota.enabled ? clone(data) : omit(['softQuota'], data);
      saveData = map((value) => typeof value === 'string' ? value.trim() : value, saveData);
      if (saveData.softQuota?.limit) {
        saveData.softQuota.limit = UnitUtil.megaBytesToBytes(saveData.softQuota.limit);
      }

      if (type.id == "google") {
        const fileList = data.bucketConfiguration.bucketSecurity?.file;
    
        if (fileList && fileList.length > 0) {
          const file = fileList.item(0);
          try {
            const accountKey = await file.text();
            saveData.bucketConfiguration.bucketSecurity.accountKey = accountKey;
          } catch (error) {
            console.error('Error reading or parsing file:', error);
          }
        } else if (saveData.bucketConfiguration?.bucketSecurity?.accountKey == "present-and-encrypted") {
          let {bucketConfiguration : {bucketSecurity , ...bucket} , ...otherData} = saveData;
          saveData = {bucketConfiguration: {...bucket} , ...otherData};
        }
      }

      if (ValidationUtils.notBlank(pristineData.name)) {
        return axios.put(URLs.singleBlobStoreUrl(type.id, data.name), saveData);
      }
      else {
        return axios.post(URLs.createBlobStoreUrl(type.id), saveData);
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

    delete: ({data}) => axios.delete(URLs.deleteBlobStoreUrl(data.name)),
  }
});
