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
import {assign, createMachine} from 'xstate';
import Axios from 'axios';

export const ASC = 1;
export const DESC = -1;

export default createMachine(
    {
      id: 'ContentSelectorsPreview',

      initial: 'loading',

      context: {
        type: '',
        expression: '',
        allRepositories: [],
        previewResponse: [],
        preview: [],
        previewError: '',
        repositories: '*',
        filterText: '',
        error: ''
      },

      states: {
        loading: {
          invoke: {
            id: 'fetchRepositories',
            src: 'fetchRepositories',
            onDone: {
              target: 'loaded',
              actions: ['setAllRepositories']
            },
            onError: {
              target: 'loadError',
              actions: ['setLoadError', 'logLoadError']
            }
          }
        },
        loaded: {
          entry: ['filterPreview'],
          on: {
            SET_REPOSITORIES: {
              target: 'loaded',
              actions: ['setRepositories']
            },
            PREVIEW: {
              target: 'preview',
              actions: ['clearPreviewError', 'clearFilter']
            },
            FILTER: {
              target: 'loaded',
              actions: ['setFilter']
            }
          }
        },
        preview: {
          invoke: {
            id: 'preview',
            src: 'preview',
            onDone: {
              target: 'loaded',
              actions: ['setPreview']
            },
            onError: {
              target: 'loaded',
              actions: ['setPreviewError']
            }
          }
        },
        loadError: {}
      },
      on: {
        RETRY: {
          target: 'loading'
        }
      }
    },
    {
      actions: {
        setAllRepositories: assign({
          allRepositories: (_, event) => event.data.data
        }),

        setRepositories: assign({
          repositories: (_, {repositories}) => repositories
        }),

        clearFilter: assign({
          filterText: () => ''
        }),

        setFilter: assign({
          filterText: (_, {filter}) => filter
        }),

        filterPreview: assign({
          preview: ({filterText, previewResponse}, _) => previewResponse.filter(name =>
              name.toLowerCase().indexOf(filterText.toLowerCase()) !== -1
          )
        }),

        setLoadError: assign({
          error: (_, event) => event.data.message
        }),

        setPreview: assign({
          previewResponse: (_, event) => event.data.data.results.map(({name}) => name)
        }),

        setPreviewError: assign({
          previewError: (_, event) => event.data.response.data[0].message
        }),

        clearPreviewError: assign({
          previewError: () => ''
        }),

        logLoadError: (_, event) => console.error(event.data.message)
      },
      services: {
        fetchRepositories: () => Axios.get('service/rest/internal/ui/repositories?withAll=true&withFormats=true'),
        preview: ({repositories}, {selectorType, expression}) => Axios.post('service/rest/internal/ui/content-selectors/preview', {
          repository: repositories,
          type: selectorType,
          expression
        })
      }
    }
);
