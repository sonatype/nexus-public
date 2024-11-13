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

import UIStrings from '../../../../constants/UIStrings';

const HOSTED_REPOSITORIES = 'service/rest/internal/ui/replication/repositories';
const REPLICATION_URL = (name) => `/service/rest/beta/replication/connection/${name}`;
const TEST_URL = 'service/rest/internal/ui/replication/test-connection/';

const transformDataFromResponse = (_, event) => {
  const data = event.data[1].data || {};
  if (data.contentRegexes?.length > 0) {
    data.replicatedContent = 'regex';
    data.contentRegex = data.contentRegexes;
  }
  else {
    data.replicatedContent = 'all';
  }
  return data;
};

const transformDataForSave = (data) => ({
  ...data,
  contentRegexes: data.replicatedContent === 'regex' ? [data.contentRegex] : []
});

export default FormUtils.buildFormMachine({
  id: 'ReplicationFormMachine',
  config: (config) => ({
    ...config,
    context: {
      ...config.context,
      sourceRepositories: [],
      destinationRepositories: []
    },
    states: {
      ...config.states,
      loaded: {
        ...config.states.loaded,
        on: {
          ...config.states.loaded.on,
          UPDATE_TEST_CONNECTION: {
            target: 'loaded',
            actions: ['clearDestinationRepositoryName', 'clearTestStatus', 'update'],
            internal: false
          },
          TEST_REPOSITORY_CONNECTION: {
            target: 'testingConnection'
          },
          CLOSE_SUCCESS_NOTIFICATION: {
            actions: ['clearTestStatus']
          },
        }
      },
      testingConnection: {
        invoke: {
          src: 'testRepositoryConnection',
          onDone: {
            target: 'loaded',
            actions: ['setDestinationRepositories', 'setTestStatusSuccess']
          },
          onError: {
            target: 'loaded',
            actions: ['setTestStatus']
          }
        }
      },
    }
  })
}).withConfig({
  actions: {
    clearDestinationRepositoryName: assign({
      destinationRepositories: () => [],
      data: ({data}) => ({
        ...data,
        destinationRepositoryName: ''
      })
    }),

    setData: assign({
      sourceRepositories: (_, event) => event.data[0].data,
      data: transformDataFromResponse,
      pristineData: transformDataFromResponse,
      destinationRepositories: (_, event) => {
        const data = event.data[1].data;
        if (data.destinationRepositoryName) {
          return [{id: data.destinationRepositoryName, name: data.destinationRepositoryName}];
        }
        return [];
      },
      connectionStatus: () => null,
      connectionMessage: () => null
    }),

    setSaveError: assign({
      saveErrorData: ({data}) => data,
      saveError: (_, event) => {
        const data = event.data?.response?.data;
        return data instanceof String ? data : null;
      },
      saveErrors: (_, event) => {
        const data = event.data?.response?.data;
        if (data instanceof Array) {
          let saveErrors = {};
          data.forEach(({id, message}) => {
            id = id.replace(FormUtils.FIELD_ID, '');
            id = id.replace(FormUtils.PARAMETER_ID, '');
            id = id.replace(FormUtils.HELPER_BEAN, '');
            saveErrors[id] = message;
          });
          return saveErrors;
        }
      },
      connectionStatus: (_, event) => {
        const data = event.data?.response?.data;
        if (data instanceof Array) {
          for (const {id, message} of data) {
            if (id === 'connectionStatus') {
              return parseInt(message);
            }
          }
        }
      },
      connectionMessage: (_, event) => {
        const data = event.data?.response?.data;
        if (data instanceof Array) {
          for (const {id, message} of data) {
            if (id === 'connectionMessage') {
              return message;
            }
          }
        }
      }
    }),

    clearSaveError: assign({
      saveErrorData: () => ({}),
      saveError: () => undefined,
      saveErrors: () => ({}),
      connectionStatus: () => null
    }),

    setDestinationRepositories: assign({
      destinationRepositories: (_, event) => event.data.data
    }),

    clearTestStatus: assign({
      testStatus: () => null
    }),

    setTestStatusSuccess: assign({
      testStatus: () => 200,
      connectionStatus: () => null
    }),

    setTestStatus: assign({
      testStatus: (_, {data}) => data.response.status,
      testMessage: (_, event) => event.data?.response?.data,
      connectionStatus: () => null
    }),

    validate: assign({
      validationErrors: ({data}) => ({
        name: ValidationUtils.validateNotBlank(data.name) ||
            ValidationUtils.validateLength(data.name, 200) ||
            ValidationUtils.validateName(data.name),
        sourceRepositoryName: ValidationUtils.validateNotBlank(data.sourceRepositoryName),
        replicatedContent: ValidationUtils.validateNotBlank(data.replicatedContent),
        contentRegex: data.replicatedContent === 'regex' && ValidationUtils.validateNotBlank(data.contentRegex),
        includeExistingContent: data.includeExistingContent,
        destinationInstanceUrl: ValidationUtils.validateIsUrl(data.destinationInstanceUrl),
        destinationInstanceUsername: ValidationUtils.validateNotBlank(data.destinationInstanceUsername),
        destinationInstancePassword: ValidationUtils.validateNotBlank(data.destinationInstancePassword),
        destinationRepositoryName: ValidationUtils.validateNotBlank(data.destinationRepositoryName)
      })
    })
  },

  services: {
    fetchData: ({pristineData}) => {
      if (ValidationUtils.notBlank(pristineData.name)) {
        return axios.all([
          axios.get(HOSTED_REPOSITORIES),
          axios.get(REPLICATION_URL(pristineData.name))
        ]);
      }
      else {
        return axios.all([
          axios.get(HOSTED_REPOSITORIES),
          Promise.resolve({
            data: {
              id: '',
              name: '',
              sourceRepositoryName: '',
              includeExistingContent: false,
              destinationInstanceUrl: '',
              destinationInstanceUsername: '',
              destinationInstancePassword: '',
              destinationRepositoryName: '',
              contentRegexes: null
            }
          })
        ]);
      }
    },

    testRepositoryConnection: ({pristineData, data, sourceRepositories}) => {
      let {destinationInstanceUrl, destinationInstanceUsername, destinationInstancePassword, useTrustStore} = data;
      let selectedRepository = sourceRepositories.filter((repository) => repository.name === data.sourceRepositoryName)[0];
      let repositoryFormat = selectedRepository === undefined ? '' : selectedRepository.format;

      return axios.post(TEST_URL, {
        name: pristineData.name,
        destinationInstanceUrl,
        destinationInstanceUsername,
        destinationInstancePassword,
        useTrustStore: ValidationUtils.isSecureUrl(destinationInstanceUrl) ? useTrustStore : false,
        repositoryFormat
      });
    },

    saveData: ({data, pristineData}) => {
      const replicationUrl = REPLICATION_URL(pristineData.name);

      if (ValidationUtils.notBlank(pristineData.name)) {
        return axios.put(replicationUrl, transformDataForSave(data));
      }
      else {
        return axios.post(replicationUrl, transformDataForSave(data));
      }
    },

    confirmDelete: ({data}) => ExtJS.requestConfirmation({
      title: UIStrings.REPLICATION.MESSAGES.CONFIRM_DELETE.TITLE,
      message: UIStrings.REPLICATION.MESSAGES.CONFIRM_DELETE.MESSAGE(data.name),
      yesButtonText: UIStrings.REPLICATION.MESSAGES.CONFIRM_DELETE.YES,
      noButtonText: UIStrings.REPLICATION.MESSAGES.CONFIRM_DELETE.NO
    }),

    delete: ({pristineData}) => {
      return axios.delete(REPLICATION_URL(pristineData.name));
    }
  },
});
