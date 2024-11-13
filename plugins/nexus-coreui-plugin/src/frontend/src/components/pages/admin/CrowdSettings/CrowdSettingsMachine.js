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
import {assign} from 'xstate';

import {ExtJS, FormUtils, ValidationUtils} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

const {ERROR, CROWD_SETTINGS} = UIStrings;

export default FormUtils.buildFormMachine({
  id: 'CrowdSettingsForm',
  stateAfterSave: 'loading',
  config: (config) => ({
    ...config,
    states: {
      ...config.states,
      loaded: {
        ...config.states.loaded,
        on: {
          ...config.states.loaded.on,
          'VERIFY-CONNECTION': 'verifyingConnection',
          'CLEAR-CACHE': 'clearingCache'
        }
      },
      verifyingConnection: {
        invoke: {
          id: 'verifyConnection',
          src: 'verifyConnection',
          onDone: {
            target: 'loaded',
            actions: ['logConnectionSuccess']
          },
          onError: {
            target: 'loaded',
            actions: ['logConnectionError']
          }
        }
      },
      clearingCache: {
        invoke: {
          id: 'clearCache',
          src: 'clearCache',
          onDone: {
            target: 'loading',
            actions: ['logClearCacheSuccess']
          },
          onError: {
            target: 'loaded',
            actions: ['logClearCacheError']
          }
        }
      }
    }
  })
}).withConfig({
  actions: {
    validate: assign({
      validationErrors: ({data}) => ({
        url: ValidationUtils.notUri(data.url) ? CROWD_SETTINGS.FIELDS.urlValidationError : null,
        applicationName: ValidationUtils.isBlank(data.applicationName) ? ERROR.FIELD_REQUIRED : null,
        applicationPassword: ValidationUtils.isBlank(data.applicationPassword) ? ERROR.FIELD_REQUIRED : null,
        timeout: ValidationUtils.isInRange({value: data.timeout, min: 1, max: 3600})
      })
    }),

    logLoadError: ({error}) => {
      if (error) {
        console.error(error);
      }
      ExtJS.showErrorMessage(UIStrings.CROWD_SETTINGS.MESSAGES.LOAD_ERROR);
    },
    logSaveSuccess: () => {
      ExtJS.showSuccessMessage(UIStrings.CROWD_SETTINGS.MESSAGES.SAVE_SUCCESS);
    },
    logSaveError: ({error}) => {
      if (error) {
        console.error(error);
      }
      ExtJS.showErrorMessage(UIStrings.CROWD_SETTINGS.MESSAGES.SAVE_ERROR);
    },
    logConnectionError: (_, event) => {
      ExtJS.showErrorMessage(`${CROWD_SETTINGS.MESSAGES.VERIFY_CONNECTION_ERROR} ${event.data?.response?.data}`);
    },
    logConnectionSuccess: () => {
      ExtJS.showSuccessMessage(CROWD_SETTINGS.MESSAGES.VERIFY_CONNECTION_SUCCESS);
    },
    logClearCacheError: (_, event) => {
      ExtJS.showErrorMessage(`${CROWD_SETTINGS.MESSAGES.CLEAR_CACHE_ERROR} ${event.data?.response?.data}`);
    },
    logClearCacheSuccess: () => {
      ExtJS.showSuccessMessage(CROWD_SETTINGS.MESSAGES.CLEAR_CACHE_SUCCESS);
    }
  },

  guards: {
    canSave: ({isPristine, validationErrors}) => {
      const isValid = !FormUtils.isInvalid(validationErrors);
      return !isPristine && isValid;
    }
  },

  services: {
    fetchData: () => {
      return Axios.get('service/rest/v1/security/atlassian-crowd');
    },
    saveData: ({data}) => {
      return Axios.put('service/rest/v1/security/atlassian-crowd', {
        ...data
      });
    },
    verifyConnection: ({data}) => {
      return Axios.post('service/rest/v1/security/atlassian-crowd/verify-connection', {
        ...data
      });
    },
    clearCache: () => {
      return Axios.post('service/rest/v1/security/atlassian-crowd/clear-cache');
    }
  }
});

