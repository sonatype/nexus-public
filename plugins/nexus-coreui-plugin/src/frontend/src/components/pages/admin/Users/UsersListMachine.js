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
import {mergeDeepRight, indexBy, prop} from 'ramda';

import {ListMachineUtils, ExtAPIUtils, APIConstants} from '@sonatype/nexus-ui-plugin';

const {EXT: {USER: {ACTION, METHODS: {READ, READ_SOURCES}}, MIDDLE_PAGE_SIZE}} = APIConstants;

export default ListMachineUtils.buildListMachine({
  id: 'UsersListMachine',
  initial: 'loadingSources',
  sortableFields: ['userId', 'realm', 'firstName', 'lastName', 'email', 'status'],
  sortField: 'userId',
  apiFiltering: true,
  config: (config) =>
      mergeDeepRight(config, {
        context: {
          sourceFilter: 'default',
        },
        states: {
          loadingSources: {
            invoke: {
              id: 'fetchSources',
              src: 'fetchSources',
              onDone: {
                target: 'loading',
                actions: ['setSources']
              },
              onError: {
                target: 'error',
                actions: ['setError']
              }
            }
          },
          loaded: {
            on: {
              FILTER_BY_SOURCE: {
                target: 'loaded',
                actions: ['setSourceFilter', 'debounceApiFilter', 'apiFilter'],
              },
            },
          },
        },
      })
}).withConfig({
  actions: {
    setData: assign({
      data: (_, event) => event.data?.data?.result?.data,
      pristineData: (_, event) => event.data?.data?.result?.data,
    }),
    setSources: assign({
      sources: (_, event) => indexBy(prop('id'), event.data?.data?.result?.data || []),
    }),
    setSourceFilter: assign({
      sourceFilter: (_, {filter}) => filter
    }),
  },
  services: {
    fetchSources: () => ExtAPIUtils.extAPIRequest(ACTION, READ_SOURCES),
    fetchData: ({filter, sourceFilter}) => {
      return ExtAPIUtils.extAPIRequest(ACTION, READ, {
        limit: MIDDLE_PAGE_SIZE,
        filter: [
          {property: 'userId', value: filter},
          {property: 'source', value: sourceFilter},
        ],
      });
    },
  }
});
