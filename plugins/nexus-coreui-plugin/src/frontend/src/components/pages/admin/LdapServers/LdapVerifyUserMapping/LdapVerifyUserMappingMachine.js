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
import {APIConstants, ExtAPIUtils} from '@sonatype/nexus-ui-plugin';

const {
  ACTION,
  METHODS: {VERIFY_USER_MAPPING}
} = APIConstants.EXT.LDAP;

export default createMachine(
  {
    id: 'LdapVerifyUserMappingMachine',
    initial: 'loading',
    states: {
      loaded: {
        on: {
          SHOW_ITEM: {
            target: 'showingItem',
            actions: 'setItem'
          },
          RETRY: {
            target: 'loading'
          }
        }
      },
      loading: {
        id: 'loading',
        entry: 'clearError',
        invoke: {
          src: 'fetchData',
          onDone: {
            target: 'loaded',
            actions: 'setData'
          },
          onError: {
            target: 'loaded',
            actions: 'setError'
          }
        }
      },
      showingItem: {
        on: {
          SHOW_LIST: {
            target: 'loaded'
          }
        }
      }
    },
    on: {
      CANCEL: {
        actions: 'onCancel'
      }
    }
  },
  {
    actions: {
      setData: assign({
        data: (_, evt) => evt.data
      }),
      setItem: assign({
        item: (ctx, evt) => ctx.data[evt.itemIndex]
      }),
      setError: assign({
        error: (_, evt) => evt.data.message
      }),
      clearError: assign({
        error: () => null
      })
    },
    services: {
      fetchData: async (context) => {
        const {
          ldapConfig,
          ldapConfig: {protocol, groupType}
        } = context;
        const config = {
          ...ldapConfig,
          protocol: protocol.toLowerCase(),
          groupType: groupType.toLowerCase()
        };
        const response = await ExtAPIUtils.extAPIRequest(ACTION, VERIFY_USER_MAPPING, {
          data: [config]
        });
        return ExtAPIUtils.checkForError(response) || ExtAPIUtils.extractResult(response, []);
      }
    }
  }
);
