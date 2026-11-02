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
import {assign, createMachine} from 'xstate';
import {useMachine} from '@xstate/react';

/**
 * @deprecated Classic UI only; unreachable from the Preview UI. Left on XState v4 semantics
 * pending Classic UI deletion (NEXUS-55299) ahead of the XState v5 upgrade (NEXUS-51654).
 */
export default function useSimpleMachine({
  id,
  url,
  initial = 'loading'
}) {
  const machine = _buildSimpleMachine(id, url, initial);

  const [current, send] = useMachine(machine, {devTools: true});

  const load = (eventPayload = {}) => send({type: 'LOAD_DATA', ...eventPayload});

  const retry = () => send({type: 'RETRY'});

  const isLoading = current.matches('loading');

  return {current, send, load, retry, isLoading};
}

/**
 * @private
 * Internal machine builder for SimpleMachine - DO NOT USE DIRECTLY.
 * This is exported only for testing purposes.
 * For production code, always use the useSimpleMachine hook.
 *
 * @param {string} id - unique identifier for the machine
 * @param {string|function} url - URL string or function that returns URL
 * @param {string} initial - initial state name
 */
export const _buildSimpleMachine = (id, url, initial) =>
  createMachine(
    {
      id,
      initial,
      context: {
        data: undefined,
        error: undefined
      },
      states: {
        loaded: {
          on: {
            LOAD_DATA: {
              target: 'loading'
            }
          }
        },
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
        }
      }
    },
    {
      actions: {
        setData: assign({
          data: (_, event) => event.data?.data
        }),
        setError: assign({
          error: (_, event) => event.data?.message
        }),
        clearError: assign({
          error: () => null
        })
      },
      services: {
        fetchData: (_, event) => Axios.get(typeof url === 'function' ? url(event) : url)
      }
    }
  );
