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
import {FormUtils, ValidationUtils} from '@sonatype/nexus-ui-plugin';
import UIStrings from '../../../../constants/UIStrings';

const IQ_API = 'service/rest/v1/iq';

const PASSWORD_PLACEHOLDER = '#~NXRM~PLACEHOLDER~PASSWORD~#';

export default FormUtils.buildFormMachine({
  id: 'IQServerMachine',
  context: {
    data: {}
  },
  stateAfterSave: 'loading',
  config: (config) => mergeDeepRight(config, {
    states: {
      loaded: {
        initial: 'idle',
        states: {
          idle: {
            on: {
              VERIFY_CONNECTION: {
                target: 'verifyingConnection',
                actions: ['clearVerifyConnectionError']
              },
            }
          },
          verifyingConnection: {
            invoke: {
              src: 'verifyConnection',
              onDone: {
                target: 'success',
                actions: ['setVerifyConnectionSuccessMessage']
              },
              onError: {
                target: 'error',
                actions: ['setVerifyConnectionError']
              }
            }
          },
          success: {
            on: {
              DISMISS: {
                target: 'idle'
              }
            }
          },
          error: {
            on: {
              DISMISS: {
                target: 'idle'
              },
              VERIFY_CONNECTION: {
                target: 'verifyingConnection',
                actions: ['clearVerifyConnectionError']
              }
            }
          }
        },
        on: {
          UPDATE_URL: {
            actions: ['updateUrl', 'validate', 'setDirtyFlag', 'setIsPristine'],
            internal: false
          }
        }
      }
    }
  })
}).withConfig({
  actions: {
    validate: assign({
      validationErrors: ({data, pristineData}) => ({
        url: ValidationUtils.validateIsUrl(data.url),
        authenticationType: ValidationUtils.validateNotBlank(data.authenticationType),
        username: data.authenticationType === 'USER' ? ValidationUtils.validateNotBlank(data.username) : null,
        password: validatePassword(data, pristineData),
        timeoutSeconds:  ValidationUtils.isInRange({
          value: data.timeoutSeconds,
          min: 1,
          max: 3600
        })
      })
    }),
    updateUrl: assign({
      data: ({data}, {data:{url}}) => {
        const useTrustStoreForUrl = data.useTrustStoreForUrl && ValidationUtils.isSecureUrl(url);
        const newData = {
          ...data,
          useTrustStoreForUrl,
          url
        };
        if (isPlaceholder(data.password)) {
          newData.password = '';
        }

        return newData;
      },
      isTouched: ({data, pristineData, isTouched}) => ({
        ...isTouched,
        url: data.url !== pristineData.url,
        password: ValidationUtils.isBlank(data.password) || isPlaceholder(pristineData.password)
      })
    }),
    setVerifyConnectionError: assign({
      verifyConnectionError: (_, event) => {
        return event.data?.response?.data;
      }
    }),
    setVerifyConnectionSuccessMessage: assign({
      verifyConnectionSuccessMessage: (_, event) => {
        return event.data?.data?.reason;
      }
    }),
    clearVerifyConnectionError: assign({
      verifyConnectionError: () => null
    }),
    onLoadedEntry: assign({
      data: ({data}) => {
        const useTrustStoreForUrl = data.useTrustStoreForUrl && ValidationUtils.isSecureUrl(data.url);
        const newData = {
          ...data,
          useTrustStoreForUrl
        };
        return newData;
      }
    }),
  },
  guards: {
    isValidUrl: (context) => ValidationUtils.isSecureUrl(context.data.url)
  },
  services: {
    fetchData: () => Axios.get(IQ_API),
    saveData: ({data}) => Axios.put(IQ_API, data),
    verifyConnection: ({data}) => Axios.post('service/rest/internal/ui/iq/verify-connection', data)
  }
});

const validatePassword = (data, pristineData) => {
  if (data.authenticationType === 'USER') {
    const urlUpdated = ValidationUtils.notBlank(pristineData.url) && (pristineData.url !== data.url);

    if (ValidationUtils.isBlank(data.password) && urlUpdated) {
      return UIStrings.IQ_SERVER.PASSWORD_ERROR;
    }
    else {
      return ValidationUtils.validateNotBlank(data.password);
    }
  }
}

const isPlaceholder = password => password === PASSWORD_PLACEHOLDER;
