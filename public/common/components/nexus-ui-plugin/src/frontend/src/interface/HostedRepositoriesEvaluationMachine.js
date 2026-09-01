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
import FormUtils from './FormUtils';
import APIConstants from '../constants/APIConstants';
import {mergeDeepRight} from 'ramda';
import Axios from 'axios';

const getPublicAPI = () => APIConstants?.REST?.PUBLIC || {};

export default FormUtils.buildFormMachine({
  id: 'HostedRepositoriesEvaluationMachine',
  initial: 'loading',
  config: (config) => mergeDeepRight(config, {
    context: {
      data: {
        selectedRepositories: []
      },
      repositories: undefined,
      totalCount: 0,
      totalPages: 1,
      formats: [],
      numberOfMonitoredRepositories: 0,
      hasSelections: false,
      globalConfigAvailable: false,
      existingSettings: undefined,
      pristineData: {
        selectedRepositories: []
      },
      filter: '',
      formatFilter: 'all',
      monitoringFilter: 'all',
      offsetPage: 0,
      sortField: 'name',
      sortDirection: 'asc'
    },
    states: {
      loading: {
        invoke: [
          {
            src: 'fetchData',
            onDone: {
              actions: ['setRepositoryData']
            },
            onError: {
              actions: ['setLoadError']
            }
          },
          {
            src: 'fetchFormats',
            onDone: {
              actions: ['setFormatsData']
            },
            onError: {
              actions: ['setDefaultFormats']
            }
          },
          {
            src: 'fetchSettings',
            onDone: {
              actions: ['setExistingSettings']
            },
            onError: {
              actions: [assign({
                existingSettings: null,
                hasSelections: false
              })]
            }
          }
        ],
        always: [
          {
            target: 'loaded',
            cond: (context) => context.repositories !== undefined && context.formats.length > 0 && context.existingSettings !== undefined
          },
          {
            target: 'loaded',
            cond: (context) => context.loadError !== null
          }
        ]
      },
      loaded: {
        initial: 'idle',
        states: {
          idle: {},
          refetching: {
            invoke: {
              src: 'fetchData',
              onDone: {
                target: 'idle',
                actions: ['setRepositoryData']
              },
              onError: {
                target: 'idle',
                actions: ['setLoadError']
              }
            }
          }
        },
        on: {
          FILTER: {
            target: '.refetching',
            actions: [assign({
              filter: (_, event) => event.filter,
              offsetPage: () => 0
            })]
          },
          FILTER_FORMAT: {
            target: '.refetching',
            actions: [assign({
              formatFilter: (_, event) => event.formatFilter,
              offsetPage: () => 0
            })]
          },
          FILTER_MONITORING: {
            target: '.refetching',
            actions: [assign({
              monitoringFilter: (_, event) => event.monitoringFilter,
              offsetPage: () => 0
            })]
          },
          CHANGE_PAGE: {
            target: '.refetching',
            actions: [assign({
              offsetPage: (_, event) => event.offsetPage
            })]
          },
          SORT: {
            target: '.refetching',
            actions: [assign({
              sortField: (_, event) => event.sortField,
              sortDirection: (_, event) => event.sortDirection,
              offsetPage: () => 0
            })]
          },
          PATCH: {
            target: 'patching',
            cond: 'canSave'
          },
          PATCH_SETTINGS: {
            target: 'patchingSettings',
            cond: 'canSave'
          },
          PATCH_REPOSITORIES: {
            target: 'patchingRepositories',
            cond: 'canSave'
          }
        }
      },
      patching: {
        entry: ['clearSaveError'],
        invoke: {
          src: 'patchData',
          onDone: {
            target: 'loaded',
            actions: [
              'clearDirtyFlag',
              'clearSaveError',
              'setSavedData'
            ]
          },
          onError: {
            target: 'loaded',
            actions: ['setSaveError', 'logSaveError']
          }
        }
      },
      patchingSettings: {
        entry: ['clearSaveError'],
        invoke: {
          src: 'patchSettings',
          onDone: {
            target: 'loaded',
            actions: [
              'clearDirtyFlag',
              'clearSaveError',
              'setSavedData'
            ]
          },
          onError: {
            target: 'loaded',
            actions: ['setSaveError', 'logSaveError']
          }
        }
      },
      patchingRepositories: {
        entry: ['clearSaveError'],
        invoke: {
          src: 'patchRepositories',
          onDone: {
            target: 'loaded',
            actions: [
              'clearDirtyFlag',
              'clearSaveError',
              'setSavedData'
            ]
          },
          onError: {
            target: 'loaded',
            actions: ['setSaveError', 'logSaveError']
          }
        }
      }
    }
  })
}).withConfig({
  actions: {
    setRepositoryData: assign((context, event) => {
      const responseData = event.data?.data;

      const items = (responseData?.items || []).map(item => ({
        id: item.repositoryId,
        name: item.repositoryName,
        format: item.format,
        size: item.size,
        artifactCount: item.numberOfComponents,
        isSelected: item.isSelected || false,
        hasCustomConfig: item.hasCustomConfig || false
      }));

      return {
        repositories: items,
        totalCount: responseData?.pagination?.totalItems || 0,
        totalPages: responseData?.pagination?.totalPages || 1,
        numberOfMonitoredRepositories: responseData?.numberOfMonitoredRepositories || 0,
        hasSelections: responseData?.hasSelections || false,
        globalConfigAvailable: responseData?.globalConfigAvailable || false,
        data: {
          selectedRepositories: context.data?.selectedRepositories || []
        },
        pristineData: {
          selectedRepositories: context.pristineData?.selectedRepositories || []
        }
      };
    }),

    setFormatsData: assign((context, event) => {
      const formats = event.data?.data || event.data || [];
      return {
        formats: ['all', ...formats]
      };
    }),

    setDefaultFormats: assign({
      formats: ['all']
    }),

    setExistingSettings: assign((context, event) => {
      return {
        existingSettings: event.data?.data || event.data || null
      };
    }),
    
    // Override of FormUtils.setLoadError to surface the backend message body (CLM-42478).
    setLoadError: assign({
      loadError: (_, event) => {
        const message = event.data?.response?.data?.message || event.data?.message;
        return message ? `${message}` : null;
      }
    }),

    validate: assign({
      validationErrors: () => ({})
    })
  },

  services: {
    fetchData: ({filter, formatFilter, monitoringFilter, offsetPage, sortField, sortDirection}) => {
      // TODO: CLM-38711 - Backend API needs to include: isSelected, hasCustomConfig, monitoredCount, hasSelections
      const PUBLIC = getPublicAPI();
      const params = {
        page: (offsetPage || 0) + 1,
        pageSize: 50,
        sortBy: sortField || 'name',
        sortOrder: sortDirection || 'asc'
      };
      if (filter) {
        params.search = filter;
      }
      if (formatFilter && formatFilter !== 'all') {
        params.format = formatFilter;
      }
      if (monitoringFilter && monitoringFilter !== 'all') {
        params.monitoring = monitoringFilter;
      }

      return Axios.get(PUBLIC.REPOSITORY_DASHBOARD, {params});
    },

    fetchFormats: () => {
      const PUBLIC = getPublicAPI();
      return Axios.get(PUBLIC.REPOSITORY_DASHBOARD_FORMATS);
    },

    fetchSettings: () => {
      const PUBLIC = getPublicAPI();
      return Axios.get(PUBLIC.EVALUATION_SETTINGS);
    },

    saveData: async ({data}) => {
      // TODO: CLM-38817 - Replace with actual API calls once backend is ready
      const PUBLIC = getPublicAPI();
      const settings = data.settings || {};
      const policyStage = (settings.policyEvaluationStage || '').toUpperCase().replace(/-/g, '_');
      const atomicPayload = {
        activityTimeFrame: parseInt(settings.activityTimeFrame, 10) || 1,
        artifactLatestVersions: parseInt(settings.artifactLatestVersions, 10) || 1,
        versionDepth: parseInt(settings.versionDepth, 10) || 0,
        policyEvaluationStage: policyStage,
        autoEnrollNewRepos: settings.applyToNewRepos || false,
        repositoryIds: data.selectedRepositories || []
      };
      const response = await Axios.put(PUBLIC.EVALUATION_SETTINGS_WITH_REPOS, atomicPayload);

      // Backend returns HTTP 200 with {success: false, message: string, errorCode: number} for validation errors
      // instead of proper HTTP 4xx/5xx status codes. We construct a synthetic Axios-like error to maintain
      // consistency with how the UI handles errors elsewhere. This allows the error modal to extract the message
      // via error?.response?.data?.message pattern.
      // TODO: Consider updating backend to return proper HTTP error status codes for consistency.
      if (response.data && !response.data.success) {
        const error = new Error(response.data.message || 'Failed to save settings');
        error.response = {
          data: {
            message: response.data.message,
            id: '*'
          },
          status: response.data.errorCode || 500
        };
        throw error;
      }

      return response;
    },

    patchData: async ({data}) => {
      const PUBLIC = getPublicAPI();
      const settings = data.settings || {};
      const repositoriesToAdd = data.repositoriesToAdd || [];
      const repositoriesToRemove = data.repositoriesToRemove || [];

      const patchPayload = {};

      if (settings.activityTimeFrame !== null && settings.activityTimeFrame !== undefined) {
        patchPayload.activityTimeFrame = settings.activityTimeFrame;
      }
      if (settings.artifactLatestVersions !== null && settings.artifactLatestVersions !== undefined) {
        patchPayload.artifactLatestVersions = settings.artifactLatestVersions;
      }
      if (settings.policyEvaluationStage !== null && settings.policyEvaluationStage !== undefined && settings.policyEvaluationStage !== '') {
        const policyStage = settings.policyEvaluationStage.toUpperCase().replace(/-/g, '_');
        patchPayload.policyEvaluationStage = policyStage;
      }
      if (settings.applyToNewRepos !== undefined) {
        patchPayload.autoEnrollNewRepos = settings.applyToNewRepos;
      }

      if (repositoriesToAdd.length > 0) {
        patchPayload.addRepositoryIds = repositoriesToAdd;
      }
      if (repositoriesToRemove.length > 0) {
        patchPayload.removeRepositoryIds = repositoriesToRemove;
      }

      if (Object.keys(patchPayload).length === 0) {
        throw new Error('At least one field must be provided for PATCH request');
      }

      const response = await Axios.patch(PUBLIC.EVALUATION_SETTINGS_WITH_REPOS, patchPayload);

      if (response.data && !response.data.success) {
        const error = new Error(response.data.message || 'Failed to update settings');
        error.response = {
          data: {
            message: response.data.message,
            id: '*'
          },
          status: response.data.errorCode || 500
        };
        throw error;
      }

      return response;
    },

    patchSettings: async ({data}) => {
      const PUBLIC = getPublicAPI();
      const settings = data.settings || {};
      const patchPayload = {};
      if (settings.activityTimeFrame != null && settings.activityTimeFrame !== '') {
        patchPayload.activityTimeFrame = parseInt(settings.activityTimeFrame, 10);
      }
      if (settings.artifactLatestVersions != null && settings.artifactLatestVersions !== '') {
        patchPayload.artifactLatestVersions = parseInt(settings.artifactLatestVersions, 10);
      }
      if (settings.versionDepth != null && settings.versionDepth !== '') {
        patchPayload.versionDepth = parseInt(settings.versionDepth, 10);
      }
      if (settings.policyEvaluationStage) {
        patchPayload.policyEvaluationStage = settings.policyEvaluationStage.toUpperCase().replace(/-/g, '_');
      }
      if (settings.applyToNewRepos !== undefined) {
        patchPayload.autoEnrollNewRepos = settings.applyToNewRepos;
      }
      if (Object.keys(patchPayload).length === 0) {
        throw new Error('No settings fields provided');
      }
      return await Axios.patch(PUBLIC.EVALUATION_SETTINGS, patchPayload);
    },

    patchRepositories: async ({data}) => {
      const PUBLIC = getPublicAPI();
      const repositoriesToAdd = data.repositoriesToAdd || [];
      const repositoriesToRemove = data.repositoriesToRemove || [];
      const patchPayload = {};
      if (repositoriesToAdd.length > 0) {
        patchPayload.addRepositoryIds = repositoriesToAdd;
      }
      if (repositoriesToRemove.length > 0) {
        patchPayload.removeRepositoryIds = repositoriesToRemove;
      }
      if (Object.keys(patchPayload).length === 0) {
        throw new Error('No repository changes provided');
      }
      return await Axios.patch(PUBLIC.EVALUATION_REPOSITORIES, patchPayload);
    }
  }
});
