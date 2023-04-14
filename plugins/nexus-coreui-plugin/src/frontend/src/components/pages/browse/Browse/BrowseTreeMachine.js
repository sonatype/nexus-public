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
import {assign, createMachine, send, spawn} from 'xstate';

import {APIConstants, ExtAPIUtils} from '@sonatype/nexus-ui-plugin';
import UIStrings from '../../../../constants/UIStrings';

const {EXT: {BROWSE: {ACTION, METHODS: {READ}}}} = APIConstants;

const BrowseTreeMachine = createMachine(
  {
    id: 'TreeNode-Root',

    context: {
      node: {
        id: '/',
        leaf: false
      },
      children: null,
      // Set open to true in useMachine to force an initial load
      // Leaf nodes should not be allowed to "expand"
      open: true
    },

    initial: 'chooseInitialState',

    states: {
      chooseInitialState: {
        always: [
          {
            cond: 'shouldOpenAndLoad',
            target: 'loading'
          },
          {
            target: 'idle'
          }
        ]
      },

      idle: {
        on: {
          TOGGLE: [
            {
              cond: 'shouldOpenAndLoadOnToggle',
              target: 'loading',
              actions: ['setOpen']
            },
            {
              cond: 'shouldOpenSingleChild',
              actions: ['toggle', 'setChildOpen']
            },
            {
              actions: ['toggle']
            }
          ]
        }
      },

      loading: {
        entry: ['clearError'],

        invoke: {
          src: 'fetchChildren',
          onDone: {
            target: 'idle',
            actions: ['setChildren']
          },
          onError: {
            target: 'loadError',
            actions: ['setError']
          }
        }
      },

      loadError: {
        on: {
          RETRY: {
            target: 'loading'
          }
        }
      }
    }
  },
  {
    actions: {
      setChildren: assign({
        children: ({initialOpenPath, repositoryName}, event) => {
          return event.data.map((child) => {
            let context = {
              node: child,
              initialOpenPath,
              repositoryName,
              open: (event.data.length === 1 && !child.leaf) || initialOpenPath.indexOf(child.id) === 0
            };
            return spawn(BrowseTreeMachine.withContext(context), `TreeNode-${child.id}`);
          });
        },

        // Clear the bookmark after the load completes
        initialOpenPath: () => ''
      }),

      toggle: assign({
        open: ({open}, {shouldOpen}) => shouldOpen ?? !open
      }),

      setOpen: assign({
        open: () => true
      }),

      setChildOpen: send({
        type: 'TOGGLE',
        shouldOpen: true
      }, { to: context => context.children[0].id}),

      setError: assign({
        loadError: (_, event) => event?.data?.message || UIStrings.ERROR.UNKNOWN
      }),

      clearError: assign({
        loadError: () => null
      })
    },

    guards: {
      shouldOpenAndLoad: ({children, open}) => !children && open,
      shouldOpenAndLoadOnToggle: ({children, open}) => !children && !open,
      shouldOpenSingleChild: ({children}) => children.length === 1
    },

    services: {
      fetchChildren: async ({node, repositoryName}) => {
        const response = await ExtAPIUtils.extAPIRequest(ACTION, READ, {data: [{repositoryName, node: node.id}]});
        return ExtAPIUtils.checkForErrorAndExtract(response);
      }
    }
  }
);

export default BrowseTreeMachine;
