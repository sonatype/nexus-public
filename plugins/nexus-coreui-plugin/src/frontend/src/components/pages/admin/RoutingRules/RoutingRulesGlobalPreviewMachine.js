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
import {assign, Machine} from 'xstate';
import Axios from 'axios';

export default Machine(
    {
      id: 'RoutingRulesGlobalPreviewMachine',

      initial: 'idle',

      context: {
        error: null,
        repositories: 'all',
        filter: '',
        path: '',
        preview: [],
        pristinePreview: [],
        selectedRowDetails: null,
        selectedRowError: null
      },

      states: {
        idle: {
          entry: ['filter'],
          on: {
            PREVIEW: {
              target: 'preview'
            },
            SELECT_ROW: {
              target: 'fetchSelectedRowDetails',
              actions: ['setSelectedRule']
            },
            TOGGLE: {
              target: 'idle',
              actions: ['toggle'],
              internal: false
            },
            UPDATE_AND_CLEAR: {
              target: 'idle',
              actions: ['update', 'clearPreview'],
              internal: false
            },
            UPDATE: {
              target: 'idle',
              actions: ['update'],
              internal: false
            }
          }
        },
        preview: {
          invoke: {
            src: 'preview',
            onDone: {
              target: 'idle',
              actions: ['setPreview']
            },
            onError: {
              target: 'idle'
            }
          }
        },
        fetchSelectedRowDetails: {
          id: 'fetchSelectedRowDetails',
          invoke: {
            src: 'fetchSelectedRowDetails',
            onDone: {
              target: 'viewSelectedRow',
              actions: ['setSelectedRowDetails']
            },
            onError: {
              target: 'viewSelectedRow',
              actions: ['setSelectedRowError']
            }
          }
        },
        viewSelectedRow: {
          on: {
            RETRY: {
              target: 'fetchSelectedRowDetails'
            }
          }
        }
      },
      on: {
        CLOSE: {
          target: 'idle'
        }
      }
    },
    {
      actions: {
        clearPreview: assign({
          preview: () => [],
          pristinePreview: () => []
        }),
        filter: assign({
          preview: ({pristinePreview, filter}) => {
            const filterByText = ({repository, type, format, rule}) =>
                repository?.toLowerCase().includes(filter.toLowerCase()) ||
                type?.toLowerCase().includes(filter.toLowerCase()) ||
                format?.toLowerCase().includes(filter.toLowerCase()) ||
                rule?.toLowerCase().includes(filter.toLowerCase());

            return pristinePreview.map((row) => ({
              ...row,
              children: row.children?.filter(filterByText)
            })).filter(filterByText);
          }
        }),
        setPreview: assign({
          preview: (_, event) => event.data.data.children,
          pristinePreview: (_, event) => event.data.data.children
        }),
        setSelectedRule: assign({
          selectedRule: ({preview}, {parent, child}) => {
            const rowDetails = child ? preview[parent].children[child] : preview[parent];
            return rowDetails.rule;
          }
        }),
        setSelectedRowDetails: assign({
          selectedRowDetails: (_, event) => event.data?.data
        }),
        setSelectedRowError: assign({
          selectedRowError: (_, event) => event.data?.data
        }),
        toggle: assign({
          pristinePreview: ({pristinePreview}, {index}) => pristinePreview.map(
              (item, i) => (index === i) ? {...item, expanded: !item.expanded} : item
          )
        }),
        update: assign((context, {name, value}) => {
          let update = {...context};
          update[name] = value;
          return update;
        })
      },
      services: {
        fetchSelectedRowDetails: ({selectedRule}) => Axios.get(
            `/service/rest/internal/ui/routing-rules/${selectedRule}`
        ),
        preview: ({path, repositories}) => Axios.get(
            `/service/rest/internal/ui/routing-rules/preview?path=/${path}&filter=${repositories}`
        )
      }
    }
);
