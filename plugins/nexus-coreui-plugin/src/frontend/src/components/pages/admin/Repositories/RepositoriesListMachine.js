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
import {ListMachineUtils} from '@sonatype/nexus-ui-plugin';
import {ExtAPIUtils, APIConstants} from '@sonatype/nexus-ui-plugin';
import {mergeDeepRight} from 'ramda';

const {EXT, REST: {INTERNAL}} = APIConstants;

export default ListMachineUtils.buildListMachine({
  id: 'RepositoriesListMachine',
  sortableFields: ['name', 'type', 'format', 'status'],

  config: (config) =>
    mergeDeepRight(config, {
      states: {
        loading: {
          states: {
            fetch: {
              invoke: {
                onDone: {
                  target: '#RepositoriesListMachine.readingHealthCheck'
                }
              }
            }
          }
        },
        loaded: {
          on: {
            ENABLE_HELTH_CHECK_SINGLE_REPO: {
              target: 'enablingHealthCheckSingleRepo'
            },
            ENABLE_HELTH_CHECK_ALL_REPOS: {
              target: 'enablingHealthCheckAllRepos'
            },
            READ_HELTH_CHECK: {
              target: 'readingHealthCheck'
            }
          }
        },
        enablingHealthCheckSingleRepo: {
          invoke: {
            src: 'enableHealthCheckSingleRepo',
            onDone: {
              target: 'readingHealthCheck',
              actions: 'clearEnablingHealthCheckRepoName'
            },
            onError: {
              target: 'loaded',
              actions: ['setEnableHealthCheckError']
            }
          },
          entry: ['setEnablingHealthCheckRepoName', 'clearEnableHealthCheckError']
        },
        enablingHealthCheckAllRepos: {
          invoke: {
            src: 'enableHealthCheckAllRepos',
            onDone: {
              target: 'readingHealthCheck'
            },
            onError: {
              target: 'loaded',
              actions: ['setEnableHealthCheckError']
            }
          },
          entry: ['clearEnablingHealthCheckRepoName', 'clearEnableHealthCheckError']
        },
        readingHealthCheck: {
          invoke: {
            src: 'readHealthCheck',
            onDone: {
              target: 'loaded',
              actions: 'setHealthCheck'
            },
            onError: {
              target: 'loaded',
              actions: ['setReadHealthCheckError']
            }
          },
          entry: ['clearReadHealthCheckError']
        }
      }
    })
}).withConfig({
  actions: {
    setData: assign((_, event) => {
      const data = event.data?.data;
      return {
        data: data,
        pristineData: data
      };
    }),

    filterData: assign({
      data: ({filter, pristineData}, _) =>
        pristineData.filter(({name}) => name.toLowerCase().indexOf(filter.toLowerCase()) !== -1)
    }),

    setHealthCheck: assign({
      data: ({data}, event) => {
        data.forEach((repo) => {
          const health = event.data.find((it) => it.repositoryName === repo.name);
          if (health) {
            repo.health = health;
          }
        });
        return data;
      }
    }),
    setEnableHealthCheckError: assign({
      enableHealthCheckError: (_, event) => event.data?.message
    }),
    clearEnableHealthCheckError: assign({
      enableHealthCheckError: () => null
    }),
    setReadHealthCheckError: assign({
      readHealthCheckError: (_, event) => event.data?.message
    }),
    clearReadHealthCheckError: assign({
      readHealthCheckError: () => null
    }),
    setEnablingHealthCheckRepoName: assign({
      enablingHealthCheckRepoName: (_, event) => event.repoName
    }),
    clearEnablingHealthCheckRepoName: assign({
      enablingHealthCheckRepoName: () => null
    })
  },
  services: {
    fetchData: () => Axios.get(INTERNAL.REPOSITORIES_DETAILS),

    enableHealthCheckSingleRepo: async (_, event) => {
      const response = await ExtAPIUtils.extAPIRequest(
        EXT.HEALTH_CHECK.ACTION,
        EXT.HEALTH_CHECK.METHODS.UPDATE,
        [true, event.repoName, true]
      );
      ExtAPIUtils.checkForError(response);
      return ExtAPIUtils.extractResult(response);
    },

    enableHealthCheckAllRepos: async () => {
      const response = await ExtAPIUtils.extAPIRequest(
        EXT.HEALTH_CHECK.ACTION,
        EXT.HEALTH_CHECK.METHODS.ENABLE_ALL,
        [true]
      );
      ExtAPIUtils.checkForError(response);
      return ExtAPIUtils.extractResult(response);
    },

    readHealthCheck: async () => {
      const response = await ExtAPIUtils.extAPIRequest(
        EXT.HEALTH_CHECK.ACTION,
        EXT.HEALTH_CHECK.METHODS.READ
      );
      ExtAPIUtils.checkForError(response);
      return ExtAPIUtils.extractResult(response);
    }
  }
});
