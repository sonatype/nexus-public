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
import {createMachine, assign} from 'xstate';
import axios from 'axios';

export const FILTER_BY_REPOSITORY_NAME = 'FILTER_BY_REPOSITORY_NAME';
export const FILTER_BY_IP_ADDRESS = 'FILTER_BY_IP_ADDRESS';
export const FILTER_BY_USERNAME = 'FILTER_BY_USERNAME';

export default createMachine(
    {
      id: 'InsightFrontendMachine',
      initial: 'loading',
      context: {
        pristineData: {
          downloadsByRepositoryName: [],
          downloadsByIpAddress: [],
          downloadsByUsername: [],
          downloadsByDay: [],
          unaffectedDownloadsByDay: [],
          totalDownloads: 0
        },
        data: {
          downloadsByRepositoryName: [],
          downloadsByIpAddress: [],
          downloadsByUsername: [],
          downloadsByDay: [],
          unaffectedDownloadsByDay: [],
          totalDownloads: 0
        },
        filters: {
          filterByRepositoryNameValue: '',
          filterByIpAddressValue: '',
          filterByUsernameValue: '',
        }
      },
      states: {
        loading: {
          invoke: {
            src: 'fetchData',
            onDone: {
              target: 'viewing',
              actions: ['setData']
            },
          }
        },
        viewing: {
          on: {
            FILTER_TABLE_BY: {
              // target: 'viewing',
              actions: ['doFilterTable']
            }
          }
        }
      },
    },
    {
      actions: {
        setData: assign((context, event) => {
          return {
            ...context,
            pristineData: {
              ...event.data
            },
            data: {
              ...event.data
            }
          }
        }),
        doFilterTable: assign((context, event) => {
          const {
            filterType,
            filterValue
          } = event.value;

          if (filterType === FILTER_BY_REPOSITORY_NAME) {
            const result = context.pristineData.downloadsByRepositoryName
                .filter(it => it.identifier.includes(filterValue));

            return {
              ...context,
              data: {
                ...context.data,
                downloadsByRepositoryName: result
              },
              filters: {
                ...context.filters,
                filterByRepositoryNameValue: filterValue
              }
            }
          }
          if (filterType === FILTER_BY_IP_ADDRESS) {
            const result = context.pristineData.downloadsByIpAddress
                .filter(it => it.identifier.includes(filterValue));

            return {
              ...context,
              data: {
                ...context.data,
                downloadsByIpAddress: result
              },
              filters: {
                ...context.filters,
                filterByIpAddressValue: filterValue
              }
            }
          }
          if (filterType === FILTER_BY_USERNAME) {
            const result = context.pristineData.downloadsByUsername
                .filter(it => it.identifier.includes(filterValue));

            return {
              ...context,
              data: {
                ...context.data,
                downloadsByUsername: result
              },
              filters: {
                ...context.filters,
                filterByUsernameValue: filterValue
              }
            }
          }

          return context;
        })
      },
      services: {
        fetchData: async () => {
          const downloadsByRepositoryName = await axios.get('/service/rest/v1/vulnerability/count-by-repository-name');
          const downloadsByUsername = await axios.get('/service/rest/v1/vulnerability/count-by-username');
          const downloadsByIp = await axios.get('/service/rest/v1/vulnerability/count-by-ip');
          const downloadsByDay = await axios.get('/service/rest/v1/vulnerability/count-by-day');
          const unaffectedDownloadsByDay = await axios.get('/service/rest/v1/vulnerability/count-unaffected-by-day');
          const totalDownloads = await axios.get('/service/rest/v1/vulnerability/count-total');

          return {
            downloadsByRepositoryName: downloadsByRepositoryName?.data || [],
            downloadsByIpAddress: downloadsByIp?.data || [],
            downloadsByUsername: downloadsByUsername?.data || [],
            downloadsByDay: downloadsByDay?.data || [],
            unaffectedDownloadsByDay: unaffectedDownloadsByDay?.data || [],
            totalDownloads: totalDownloads?.data || 0
          };
        }
      }
    }
);
