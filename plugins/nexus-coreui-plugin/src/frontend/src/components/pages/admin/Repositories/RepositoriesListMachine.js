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
import {Utils} from '@sonatype/nexus-ui-plugin';

export default Utils.buildListMachine({
  id: 'RepositoriesListMachine',
  config: (config) => ({
    ...config,
    states: {
      ...config.states,
      loaded: {
        ...config.states.loaded,
        on: {
          ...config.states.loaded.on,
          SORT_BY_NAME: {
            target: 'loaded',
            actions: ['setSortByName']
          },
          SORT_BY_TYPE: {
            target: 'loaded',
            actions: ['setSortByType']
          },
          SORT_BY_FORMAT: {
            target: 'loaded',
            actions: ['setSortByFormat']
          },
          SORT_BY_STATUS: {
            target: 'loaded',
            actions: ['setSortByStatus']
          }
        }
      }
    }
  }),
  options: (options) => ({
    ...options,
    actions: {
      ...options.actions,
      setSortByName: assign({
        sortField: 'name',
        sortDirection: Utils.nextSortDirection('name')
      }),
      setSortByType: assign({
        sortField: 'type',
        sortDirection: Utils.nextSortDirection('type')
      }),
      setSortByFormat: assign({
        sortField: 'format',
        sortDirection: Utils.nextSortDirection('format')
      }),
      setSortByStatus: assign({
        sortField: 'status',
        sortDirection: Utils.nextSortDirection('status')
      }),
      filterData: assign({
        data: ({filter, data, pristineData}, _) => pristineData.filter(({name}) =>
            name.toLowerCase().indexOf(filter.toLowerCase()) !== -1
        )
      }),
      sortData: assign({
        data: function ({sortField, sortDirection, data}) {
          return (data.slice().sort((a, b) => {
            const dir = sortDirection === Utils.ASC ? 1 : -1;
            if (a[sortField] === b[sortField]) {
              return 0;
            }
            if (typeof(a[sortField]) == 'object') {
              return JSON.stringify(a[sortField]) > JSON.stringify(b[sortField]) ? dir : -dir;
            }
            else {
             return a[sortField] > b[sortField] ? dir : -dir;
            }
          }));
        }
      })
    },
    services: {
      ...options.services,
      fetchData: () => Axios.get('service/rest/internal/ui/repositories/details')
    }
  })
});
