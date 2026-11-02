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

import { assign, send } from 'xstate';
import { mergeDeepRight } from 'ramda';
import { ListMachineUtils, ExtAPIUtils, APIConstants } from '@sonatype/nexus-ui-plugin';

const {
  EXT: {
    PRIVILEGE: {
      ACTION,
      METHODS: { READ },
    },
    SMALL_PAGE_SIZE,
  },
} = APIConstants;

/**
 * @deprecated Classic UI only; unreachable from the Preview UI. Left on XState v4 semantics
 * pending Classic UI deletion (NEXUS-55299) ahead of the XState v5 upgrade (NEXUS-51654).
 */
export default ListMachineUtils.buildListMachine({
  id: 'PrivilegesListMachine',
  sortableFields: ['name', 'description', 'type', 'permission'],
  apiSorting: true,
  apiFiltering: true,
  config: config =>
    mergeDeepRight(config, {
      context: {
        currentPage: 0,
        pageSize: SMALL_PAGE_SIZE,
        totalCount: 0,
      },
      states: {
        loading: {
          on: {
            FILTER: {
              target: 'loading',
              actions: ['setFilter', 'resetPage', 'cancelApiFilter', 'debounceApiFilter'],
            },
            CHANGE_PAGE: {
              target: 'loading',
              actions: ['setCurrentPage'],
            },
          },
        },
        loaded: {
          on: {
            CHANGE_PAGE: {
              target: 'loading',
              actions: ['setCurrentPage'],
            },
            FILTER: {
              target: 'loaded',
              actions: ['setFilter', 'resetPage', 'debounceApiFilter'],
            },
            API_FILTER: {
              target: 'loading',
            },
            CANCEL_API_FILTER: {
              target: 'loaded',
            },
          },
        },
      },
    }),
}).withConfig({
  actions: {
    setCurrentPage: assign({
      currentPage: (_, { page }) => page,
    }),
    setFilter: assign({
      filter: (_, { filter }) => filter,
    }),
    resetPage: assign({
      currentPage: 0,
    }),
    cancelApiFilter: send({ type: 'CANCEL_API_FILTER' }, { id: 'debounced-filter' }),
    debounceApiFilter: send({ type: 'API_FILTER' }, { delay: 400, id: 'debounced-filter' }),
    setData: assign({
      data: (_, event) => event.data?.data || [],
      totalCount: (_, event) => event.data?.total || 0,
    }),
  },
  services: {
    fetchData: ({ sortField, sortDirection, filter, currentPage, pageSize }) => {
      const params = {
        limit: pageSize,
        start: currentPage * pageSize,
        sortField,
        sortDirection,
        filterValue: filter,
      };
      return ExtAPIUtils.extAPIRequest(ACTION, READ.NAME, params).then(v => {
        return v.data.result;
      });
    },
  },
});
