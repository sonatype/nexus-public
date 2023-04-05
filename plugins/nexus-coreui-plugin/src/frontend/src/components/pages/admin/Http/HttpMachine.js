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
import {
  FormUtils,
  ExtAPIUtils,
  APIConstants,
  ValidationUtils,
} from '@sonatype/nexus-ui-plugin';
import {omit, isNil, whereEq, mergeDeepRight, hasIn} from 'ramda';

const {
  EXT: {
    HTTP: {ACTION, METHODS},
  },
} = APIConstants;

const validateAuthentication = (data, prefix) => {
  let newData = {...data};
  const _ = (name) => `${prefix}${name}`;

  if (!data[_('Enabled')]) {
    newData = omit([_('Host'), _('Port')], newData);
  }

  if (!data[_('AuthEnabled')]) {
    newData = omit(
      [
        _('AuthNtlmDomain'),
        _('AuthNtlmHost'),
        _('AuthPassword'),
        _('AuthUsername'),
      ],
      newData
    );
  }

  if (
    data[_('AuthEnabled')] &&
    ValidationUtils.isBlank(newData[_('AuthPassword')])
  ) {
    newData[_('AuthPassword')] = '';
  }

  return newData;
};

const validatePristine = (data, prefix, name, value) => {
  const _ = (name) => `${prefix}${name}`;
  const omitProxy = name === _('Enabled') && value === false;
  let omitValues = [];

  if (omitProxy) {
    omitValues = [_('Host'), _('Port'), _('AuthEnabled')];
  }

  const omitAuth =
    name === _('AuthEnabled') && data[_('AuthEnabled')] === false;

  if (omitAuth || omitProxy) {
    omitValues = [
      ...omitValues,
      _('AuthNtlmDomain'),
      _('AuthNtlmHost'),
      _('AuthPassword'),
      _('AuthUsername'),
    ];
  }

  return omit(omitValues, data);
};

const validateProxyFields = (data, prefix) => {
  const proxyErrors = {};
  const fieldName = (name) => `${prefix}${name}`;

  if (data[fieldName('Enabled')]) {
    proxyErrors[fieldName('Host')] = 
      ValidationUtils.validateNotBlank(data[fieldName('Host')]) || 
      ValidationUtils.validateHost(data[fieldName('Host')]);

    proxyErrors[fieldName('Port')] =
      ValidationUtils.validateNotBlank(data[fieldName('Port')]) ||
      ValidationUtils.isInRange({
        value: data[fieldName('Port')],
        min: 1,
        max: 65535,
      });

    if (data[fieldName('AuthEnabled')]) {
      proxyErrors[fieldName('AuthUsername')] = ValidationUtils.validateNotBlank(
        data[fieldName('AuthUsername')]
      );
    }
  }
  return proxyErrors;
};

const removeEmptyValues = (obj) => {
  const keys = Object.keys(obj).filter((key) => isNil(obj[key]) && key);

  return omit(keys, obj);
};

const update = assign((_, event) => {
  const data = ExtAPIUtils.extractResult(event.data) || {};
  const result = {
    ...data,
    httpEnabled: data.httpEnabled || false,
    httpsEnabled: data.httpsEnabled || false,
    httpAuthEnabled: data.httpAuthEnabled || false,
    httpsAuthEnabled: data.httpsAuthEnabled || false,
    nonProxyHosts: data.nonProxyHosts || [],
  };

  return {
    data: result,
    pristineData: result,
  };
});

const resetNonProxyHosts = (data) => {
  const {httpEnabled, httpsEnabled, nonProxyHosts} = data;
  return {
    ...data,
    nonProxyHosts: (httpEnabled || httpsEnabled) ? nonProxyHosts : []
  }
} 

const normalizeNumericValues = (data) => ({
  ...data,
  timeout: data.timeout || null,
  retries: data.retries || null 
})

export default FormUtils.buildFormMachine({
  id: 'HttpMachine',
  stateAfterSave: 'loading',
  config: (config) =>
    mergeDeepRight(config, {
      states: {
        loaded: {
          on: {
            SET_NON_PROXY_HOST: {
              target: 'loaded',
              actions: 'setNonProxyHost'
            },

            ADD_NON_PROXY_HOST: {
              target: 'loaded',
              actions: 'addNonProxyHost',
            },

            REMOVE_NON_PROXY_HOST: {
              target: 'loaded',
              actions: 'removeNonProxyHost',
            },

            TOGGLE_AUTHENTICATION: {
              target: 'loaded',
              actions: 'toggleAuthentication',
            },

            TOGGLE_HTTP_PROXY: {
              target: 'loaded',
              actions: 'toggleHttpProxy',
            },

            TOGGLE_HTTPS_PROXY: {
              target: 'loaded',
              actions: 'toggleHttpsProxy',
            },
          },
        },
      },
    }),
}).withConfig({
  actions: {
    validate: assign({
      validationErrors: ({data}) => {
        const proxyErrors = {
          ...validateProxyFields(data, 'http'),
          ...validateProxyFields(data, 'https'),
        };

        return {
          timeout: ValidationUtils.isInRange({
            value: data.timeout,
            min: 1,
            max: 3600,
          }),
          retries: ValidationUtils.isInRange({
            value: data.retries,
            min: 0,
            max: 10,
          }),
          ...proxyErrors,
        };
      },
    }),
    setIsPristine: assign({
      isPristine: ({data, pristineData}, {name, value}) => {
        let currentData = validatePristine(data, 'http', name, value);
        let pristine = validatePristine(pristineData, 'http', name, value);

        currentData = validatePristine(currentData, 'https', name, value);
        pristine = validatePristine(pristine, 'https', name, value);

        return whereEq(pristine)(currentData);
      },
    }),
    setNonProxyHost: assign({
      nonProxyHost: (_, {value}) => value 
    }),
    removeNonProxyHost: assign({
      data: ({data}, {index}) => ({
        ...data,
        nonProxyHosts: data.nonProxyHosts.filter((_, i) => i !== index),
      }),
    }),
    addNonProxyHost: assign(({data, nonProxyHost}) => {
      const currentList = data.nonProxyHosts || [];
      const notValid =
        ValidationUtils.isBlank(nonProxyHost) || 
        ValidationUtils.hasWhiteSpace(nonProxyHost) ||
        currentList.includes(nonProxyHost);

      if (notValid) {
        return;
      }

      return {
        data: {
          ...data,
          nonProxyHosts: currentList.includes(nonProxyHost)
            ? currentList
            : [...currentList, nonProxyHost]
        },
        nonProxyHost: ''
      };
    }),
    toggleAuthentication: assign({
      data: ({data}, {name, value}) => ({
        ...data,
        [name]: isNil(value) ? !data[name] : value,
      }),
      isTouched: ({isTouched, data}, {name}) => {
        const checkHttpAuthentication =
          name === 'httpAuthEnabled' && data[name];
        const checkHttpsAuthentication =
          name === 'httpsAuthEnabled' && data[name];

        return {
          ...isTouched,
          httpAuthUsername: checkHttpAuthentication,
          httpsAuthUsername: checkHttpsAuthentication,
        };
      },
    }),
    toggleHttpProxy: assign({
      data: ({data}) => ({
        ...data,
        httpEnabled: !data.httpEnabled,
        httpAuthEnabled: false,
      }),
    }),
    toggleHttpsProxy: assign({
      data: ({data}) => ({
        ...data,
        httpsEnabled: !data.httpsEnabled,
        httpsAuthEnabled: false,
      }),
    }),
    setData: update,
    onSaveSuccess: update,
    setSaveError: assign({
      saveErrors: (_, event) => {
        const {message} = event.data;
        try {
          const error = JSON.parse(event.data.message);
          if (hasIn('nonProxyHosts', error)) {
            return {nonProxyHost: error.nonProxyHosts};
          }
        } catch (e) {}
        return message;
      },
      saveErrorData: ({data}) => data,
      saveError: (_, event) => {
        const data = event.data?.response?.data;
        return typeof data === 'string' ? data : null;
      },
    }),
  },
  services: {
    fetchData: async () => {
      const response = await ExtAPIUtils.extAPIRequest(ACTION, METHODS.READ);
      return ExtAPIUtils.checkForError(response) || response;
    },
    saveData: async ({data}) => {
      let saveData = validateAuthentication(data, 'http');

      saveData = validateAuthentication(saveData, 'https');
      saveData = resetNonProxyHosts(saveData);
      saveData = removeEmptyValues(saveData);
      saveData = normalizeNumericValues(saveData);

      const response = await ExtAPIUtils.extAPIRequest(ACTION, METHODS.UPDATE, {
        data: [saveData],
      });

      return ExtAPIUtils.checkForError(response) || response;
    },
  },
});
