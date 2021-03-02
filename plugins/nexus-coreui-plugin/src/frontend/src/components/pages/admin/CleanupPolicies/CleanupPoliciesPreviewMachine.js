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
import {assign, actions, Machine, send} from 'xstate';
import Axios from 'axios';
import {Utils} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

export const ASC = 1;
export const DESC = -1;

export default Machine({
  id: 'CleanupPoliciesPreviewMachine',
  type: 'parallel',
  context: {
    data: [],
    filter: '',
    formError: null,
    previewError: null,
    repositories: [],
    sortField: 'name',
    sortDirection: Utils.ASC
  },
  states: {
    form: {
      initial: 'loading',
      states: {
        loading: {
          invoke: {
            id: 'fetchRepositories',
            src: 'fetchRepositories',
            onDone: {
              target: 'loaded',
              actions: ['setRepositories']
            },
            onError: {
              target: 'error',
              actions: ['setFormError']
            }
          }
        },
        loaded: {
          entry: ['sortData'],
          on: {
            SET_REPOSITORY: {
              actions: ['setRepository']
            },
            FILTER: {
              actions: [
                'setFilter',
                'debouncePreview',
                'sendPreview'
              ]
            },
            SORT_BY_NAME: {
              target: 'loaded',
              actions: ['setSortByName']
            },
            SORT_BY_GROUP: {
              target: 'loaded',
              actions: ['setSortByGroup']
            },
            SORT_BY_VERSION: {
              target: 'loaded',
              actions: ['setSortByVersion']
            }
          }
        },
        error: {
          on: {
            RETRY_FORM: {
              target: 'loading',
              actions: ['clearFormError']
            }
          }
        }
      }
    },
    preview: {
      initial: 'loaded',
      states: {
        loaded: {},
        loading: {
          invoke: {
            id: 'fetchData',
            src: 'fetchData',
            onDone: {
              target: 'loaded',
              actions: ['setData']
            },
            onError: {
              target: 'error',
              actions: ['setPreviewError']
            }
          }
        },
        error: {
          on: {
            RETRY_PREVIEW: {
              target: 'loading',
              actions: ['clearPreviewError']
            }
          }
        }
      },
      on: {
        PREVIEW: {
          target: '.loading',
          cond: 'hasRepository',
          actions: ['setPolicyData']
        }
      }
    }
  }
}, {
  actions: {
    clearFormError: assign({
      formError: () => null
    }),
    clearPreviewError: assign({
      previewError: () => null
    }),
    debouncePreview: actions.cancel('cleanup-preview'),
    sendPreview: send('PREVIEW', {
      id: 'cleanup-preview',
      delay: 500
    }),
    setData: assign({
      data: (_, event) => event.data.data.results,
      pristineData: (_, event) => event.data.data.results
    }),
    setFilter: assign({
      filter: (_, {filter}) => filter,
      policyData: (_, {policyData}) => policyData
    }),
    setFormError: assign({
      formError: (_, event) => event.data.message
    }),
    setPolicyData: assign({
        policyData: (context, event) => event.policyData || context.policyData
    }),
    setPreviewError: assign({
      previewError: (_, event) => {
        if (event.data.response.data.includes('Invalid query')) {
          return UIStrings.CLEANUP_POLICIES.PREVIEW.INVALID_QUERY;
        }
        return event.data.message;
      }
    }),
    setRepositories: assign({
      repositories: (_, event) => event.data.data
    }),
    setRepository: assign({
      repository: (_, {repository}) => repository
    }),
    setSortByName: assign({
      sortField: 'name',
      sortDirection: Utils.nextSortDirection('name')
    }),
    setSortByGroup: assign({
      sortField: 'group',
      sortDirection: Utils.nextSortDirection('group')
    }),
    setSortByVersion: assign({
      sortField: 'version',
      sortDirection: Utils.nextSortDirection('version')
    }),
    sortData: assign({
      data: Utils.sortDataByFieldAndDirection
    })
  },
  guards: {
    hasRepository: ({repository}) => Utils.notBlank(repository)
  },
  services: {
    fetchRepositories: () => Axios.get('/service/rest/internal/ui/repositories'),
    fetchData: ({
                  filter,
                  policyData,
                  repository
                }) => {
      if (!policyData) {
        console.log('no policy data', policyData);
      }
      return Axios.post(
          '/service/rest/internal/cleanup-policies/preview/components', {
            criteriaLastBlobUpdated: policyData.criteriaLastBlobUpdated,
            criteriaLastDownloaded: policyData.criteriaLastDownloaded,
            criteriaReleaseType: policyData.criteriaReleaseType,
            criteriaAssetRegex: policyData.criteriaAssetRegex,
            filter: filter,
            repository: repository
          })
    }
  }
});
