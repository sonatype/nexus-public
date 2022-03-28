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

import getTemplate from './RepositoryTemplates';

export const repositoryUrl = (format, type) => `/service/rest/v1/repositories/${formatFormat(format)}/${type}`;
export const editRepositoryUrl = (repositoryName) => `/service/rest/internal/ui/repositories/repository/${repositoryName}`;

export default FormUtils.buildFormMachine({
  id: 'RepositoriesFormMachine',
  config: (config) =>
      mergeDeepRight(config, {
        states: {
          loaded: {
            on: {
              RESET_DATA: {
                actions: ['resetData'],
                target: 'loaded'
              },
              SET_DEFAULT_BLOB_STORE: {
                cond: 'hasNoBlobStoreName',
                target: 'loaded',
                actions: ['update'],
                internal: false
              }
            }
          }
        }
      })
}).withConfig({
  actions: {
    validate: assign({
      validationErrors: ({data}) => ({
        name: validateName(data.name),
        format: ValidationUtils.validateNotBlank(data.format),
        type: ValidationUtils.validateNotBlank(data.type),
        storage: {
          blobStoreName: validateBlobSoreName(data)
        },
        group: {
          memberNames: validateMemberNames(data)
        },
        proxy: {
          remoteUrl: validateRemoteUrl(data),
          contentMaxAge: validateTimeToLive(data.proxy?.contentMaxAge),
          metadataMaxAge: validateTimeToLive(data.proxy?.metadataMaxAge)
        },
        negativeCache: {
          timeToLive: validateTimeToLive(data.negativeCache?.timeToLive)
        },
        httpClient: {
          authentication: {
            username: validateHttpAuthCreds(data, 'username'),
            password: validateHttpAuthCreds(data, 'password'),
            ntlmHost: validateHttpAuthNtlm(data, 'ntlmHost'),
            ntlmDomain: validateHttpAuthNtlm(data, 'ntlmDomain')
          },
          connection: {
            retries: validateHttpConnectionRetries(data),
            timeout: validateHttpConnectionTimeout(data)
          }
        }
      })
    }),
    resetData: assign({
      data: (_, event) => {
        const {format, repoType} = event;
        return {
          ...getTemplate(repoType),
          format
        };
      }
    })
  },
  guards: {
    hasNoBlobStoreName: ({data}) => ValidationUtils.isBlank(data.storage?.blobStoreName)
  },
  services: {
    fetchData: async ({pristineData}) => {
      if (isEdit(pristineData)) {
        const response = await Axios.get(editRepositoryUrl(pristineData.name));
        return mergeDeepRight(response, {
          data: {
            routingRule: response.data.routingRuleName
          }
        });

      }
      else {
        return {data: {name: ''}};
      }
    },
    saveData: ({data, pristineData}) => {
      const {format, type} = data;
      const payload = data;
      return isEdit(pristineData)
          ? Axios.put(repositoryUrl(format, type) + '/' + pristineData.name, payload)
          : Axios.post(repositoryUrl(format, type), payload);
    }
  }
});

const isEdit = ({name}) => ValidationUtils.notBlank(name);

const formatFormat = (format) => (format === 'maven2' ? 'maven' : format);

const isProxyType = (type) => type === 'proxy';
const isGroupType = (type) => type === 'group';

const validateName = (value) =>
    ValidationUtils.validateNotBlank(value) ||
    ValidationUtils.validateLength(value) ||
    ValidationUtils.validateName(value);

const validateTimeToLive = (value) =>
    ValidationUtils.isInRange({value, min: -1, max: 1440, allowDecimals: false});

const validateRemoteUrl = (data) =>
    isProxyType(data.type)
        ? ValidationUtils.validateNotBlank(data.proxy.remoteUrl) ||
        ValidationUtils.validateIsUrl(data.proxy.remoteUrl)
        : null;

const validateMemberNames = (data) =>
    isGroupType(data.type) && !data.group.memberNames.length ? UIStrings.ERROR.FIELD_REQUIRED : null;

const validateBlobSoreName = (data) =>
    ValidationUtils.validateNotBlank(data.storage?.blobStoreName);

const validateHttpAuthCreds = (data, attrName) => {
  if (!isProxyType(data.type)) {
    return;
  }
  const type = data.httpClient?.authentication?.type;
  if (type) {
    const attrValue = data.httpClient.authentication[attrName];
    return ValidationUtils.validateNotBlank(attrValue);
  }
};

const validateHttpAuthNtlm = (data, attrName) => {
  if (!isProxyType(data.type)) {
    return;
  }
  const type = data.httpClient?.authentication?.type;
  if (type && type === 'ntlm') {
    const attrValue = data.httpClient.authentication[attrName];
    return ValidationUtils.validateNotBlank(attrValue);
  }
};

const validateHttpConnectionRetries = (data) =>
    isProxyType(data.type) &&
    data.httpClient.connection &&
    ValidationUtils.isInRange({
      value: data.httpClient.connection.retries,
      min: 0,
      max: 10,
      allowDecimals: false
    });

const validateHttpConnectionTimeout = (data) =>
    isProxyType(data.type) &&
    data.httpClient.connection &&
    ValidationUtils.isInRange({
      value: data.httpClient.connection.timeout,
      min: 0,
      max: 3600,
      allowDecimals: false
    });
