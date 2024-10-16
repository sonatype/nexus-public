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
import {assign, Machine} from 'xstate';

import {URL} from './RolesHelper';

const {getLdapRolesUrl} = URL;

const EMPTY_CONTEXT = {
  query: '',
  data: [],
  error: null,
};

export default Machine(
    {
      id: 'ExternalRolesCombobox',
      initial: 'loaded',
      context: EMPTY_CONTEXT,
      states: {
        loaded: {},
        loading: {
          invoke: {
            src: 'fetchData',
            onDone: [
              {
                target: 'loaded',
                actions: 'setData'
              }
            ],
            onError: {
              target: 'error',
              actions: ['setError']
            }
          }
        },
        error: {
          on: {
            RETRY: {
              target: 'loading',
              actions: ['clearError']
            }
          }
        },
        debouncing: {
          after: {
            500: {
              target: 'loading',
              cond: 'meetsCharacterLimit',
            }
          }
        }
      },
      on: {
        SET_QUERY: [
          {
            target: 'debouncing',
            cond: 'doesNotMeetCharacterLimit' && 'isLdap',
            actions: ['resetData', 'setQuery'],
          },
          {
            target: 'debouncing',
            cond: 'meetsCharacterLimit' && 'isLdap',
            actions: ['setQuery'],
          },
          {
            target: 'loaded',
            cond: 'isNotLdap',
            actions: ['setQuery'],
          }
        ],
        UPDATE_TYPE: {
          target: 'loaded',
          actions: ['updateType'],
        },
        UPDATE_LDAP_LIMIT: {
          target: 'loaded',
          actions: ['updateLdapCharacterLimit'],
        },
      }
    },
    {
      actions: {
        setData: assign({
          data: (_, event) => {
            return event.data?.data?.map(it => ({id: it.id, displayName: it.name})) || [];
          }
        }),
        updateType: assign({
          externalRoleType: (_, {externalRoleType}) => externalRoleType,
          ...EMPTY_CONTEXT,
        }),
        updateLdapCharacterLimit: assign({
          ldapQueryCharacterLimit: (_, {ldapQueryCharacterLimit}) => ldapQueryCharacterLimit,
        }),
        resetData: assign({
          data: [],
        }),
        setQuery: assign({
          query: (_, {query}) => query,
        }),
        setError: assign({
          error: (_, event) => event.data?.message,
        }),
        clearError: assign({
          error: () => null,
        })
      },
      guards: {
        doesNotMeetCharacterLimit: ({ _, query, ldapQueryCharacterLimit}) => {
          return query.length < ldapQueryCharacterLimit;
        },
        meetsCharacterLimit: ({ _, query, ldapQueryCharacterLimit}) => {
          return query.length >= ldapQueryCharacterLimit;
        },
        isLdap: ({ _, externalRoleType}) => {
          return externalRoleType.toLowerCase() === 'ldap';
        },
        isNotLdap: ({ _, externalRoleType}) => {
          return externalRoleType.toLowerCase() !== 'ldap';
        },
      },
      services: {
        fetchData: ({externalRoleType, query}) => {
          if (externalRoleType.toLowerCase() === 'ldap') {
            return Axios.get(getLdapRolesUrl(query, externalRoleType));
          }
        },
      }
    }
);
