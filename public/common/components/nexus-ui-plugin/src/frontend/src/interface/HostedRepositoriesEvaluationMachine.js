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
import {FormUtils, APIConstants} from '@sonatype/nexus-ui-plugin';
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
      repositories: [],
      totalCount: 0,
      totalPages: 1,
      formats: [],
      pristineData: {
        selectedRepositories: []
      },
      filter: '',
      formatFilter: 'all',
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
              target: 'loaded',
              actions: ['setRepositoryData']
            },
            onError: {
              target: 'loaded',
              actions: ['setLoadError']
            }
          },
          {
            src: 'fetchFormats',
            onDone: {
              actions: ['setFormatsData']
            }
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
          }
        }
      }
    }
  })
}).withConfig({
  actions: {
    setRepositoryData: assign((context, event) => {
      // CLM-38692: Dashboard API response format
      // { items: [{repositoryName, format, size, numberOfComponents}], pagination: {totalItems, totalPages} }
      const responseData = event.data?.data;

      // Map dashboard API fields to component field names
      const items = (responseData?.items || []).map(item => ({
        id: item.repositoryId,
        name: item.repositoryName,
        format: item.format,
        size: item.size,
        artifactCount: item.numberOfComponents
      }));

      return {
        repositories: items,
        totalCount: responseData?.pagination?.totalItems || 0,
        totalPages: responseData?.pagination?.totalPages || 1,
        data: {
          selectedRepositories: context.data?.selectedRepositories || []
        },
        pristineData: {
          selectedRepositories: context.pristineData?.selectedRepositories || []
        }
      };
    }),

    setFormatsData: assign((context, event) => {
      const formats = event.data?.data || [];
      return {
        formats: ['all', ...formats]
      };
    }),

    validate: assign({
      validationErrors: () => ({})
    })
  },

  services: {
    fetchData: ({filter, formatFilter, offsetPage, sortField, sortDirection}) => {
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
      return Axios.get(PUBLIC.REPOSITORY_DASHBOARD, {params});
    },

    fetchFormats: () => {
      const PUBLIC = getPublicAPI();
      return Axios.get(PUBLIC.REPOSITORY_DASHBOARD_FORMATS);
    },

    saveData: async ({data}) => {
      const PUBLIC = getPublicAPI();
      const settings = data.settings || {};
      const policyStage = (settings.policyEvaluationStage || '').toUpperCase().replace(/-/g, '_');
      const settingsPayload = {
        activityTimeFrame: settings.activityTimeFrame,
        artifactLatestVersions: settings.artifactLatestVersions,
        policyEvaluationStage: policyStage,
        autoEnrollNewRepos: settings.applyToNewRepos || false
      };
      await Axios.put(PUBLIC.EVALUATION_SETTINGS, settingsPayload);
      return Axios.post(PUBLIC.HOSTED_REPO_EVALUATION_MONITOR, {
        repositoryIds: data.selectedRepositories
      });
    }
  }
});
