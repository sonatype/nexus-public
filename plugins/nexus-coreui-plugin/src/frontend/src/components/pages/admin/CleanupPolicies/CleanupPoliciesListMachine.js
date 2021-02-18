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
import {Utils} from '@sonatype/nexus-ui-plugin';

export const ASC = 'asc';
export const DESC = 'desc';

export default Utils.buildListMachine({
  id: 'CleanupPoliciesListMachine',
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
          SORT_BY_FORMAT: {
            target: 'loaded',
            actions: ['setSortByFormat']
          },
          SORT_BY_NOTES: {
            target: 'loaded',
            actions: ['setSortByNotes']
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
      setSortByFormat: assign({
        sortField: 'format',
        sortDirection: Utils.nextSortDirection('format')
      }),
      setSortByNotes: assign({
        sortField: 'notes',
        sortDirection: Utils.nextSortDirection('notes')
      }),
      filterData: assign({
        data: ({filter, data, pristineData}, _) => pristineData.filter(
            ({name, format, notes}) => name.toLowerCase().indexOf(filter.toLowerCase()) !== -1 ||
                format.toLowerCase().indexOf(filter.toLowerCase()) !== -1 ||
                notes.toLowerCase().indexOf(filter.toLowerCase()) !== -1)
      })
    },
    services: {
      ...options.services,
      fetchData: () => Axios.get('/service/rest/internal/cleanup-policies'),
    }
  })
});
