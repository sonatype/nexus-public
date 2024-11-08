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

export default createMachine(
    {
      initial: 'retrieve',

      context: {
        data: '',
        error: '',
        mark: '',
        period: 0,
        size: 25
      },

      states: {
        retrieve: {
          invoke: {
            src: 'retrieve',
            onDone: {
              target: 'retrieved',
              actions: ['setData']
            },
            onError: {
              target: 'error',
              actions: ['setError']
            }
          }
        },
        retrieved: {
          on: {
            '': [
              { target: 'manual', cond: 'isManual' },
              { target: 'waiting' }
            ]
          },
        },
        waiting: {
          on: {
            UPDATE_PERIOD: {
              target: 'retrieve',
              actions: ['setPeriod']
            },
            MANUAL_REFRESH: {
              target: 'manual',
              actions: ['setPeriod']
            },
            UPDATE_SIZE: {
              target: 'retrieve',
              actions: ['setSize']
            },
            UPDATE_MARK: {
              target: 'waiting',
              actions: ['setMark']
            },
            INSERT_MARK: {
              target: 'insertingMark'
            }
          },
          after: {
            REFRESH: 'retrieve'
          }
        },
        manual: {
          on: {
            UPDATE_PERIOD: {
              target: 'retrieve',
              actions: ['setPeriod']
            },
            UPDATE_SIZE: {
              target: 'retrieve',
              actions: ['setSize']
            },
            UPDATE_MARK: {
              target: 'manual',
              actions: ['setMark']
            },
            INSERT_MARK: {
              target: 'insertingMark'
            }
          }
        },
        insertingMark: {
          invoke: {
            src: 'insertMark',
            onDone: {
              target: 'retrieve'
            },
            onError: {
              target: 'error',
              actions: 'setError'
            }
          }
        },
        error: {}
      }
    },
    {
      actions: {
        setData: assign({
          data: (_, {data}) => data.data
        }),
        setError: assign({
          error: (_, event) => event.data.message
        }),
        setPeriod: assign({
          period: (_, {period}) => period
        }),
        setSize: assign({
          size: (_, {size}) => size
        }),
        setMark: assign({
          mark: (_, {mark}) => mark
        })
      },
      delays: {
        REFRESH: ({period}, event) => {
          return period * 1000;
        }
      },
      guards: {
        isManual: ({period}) => period == 0
      },
      services: {
        retrieve: ({itemId, size}) => Axios.get(`/service/rest/internal/logging/logs/${itemId}`, {params: {bytesCount: size * -1024}}),
        insertMark: ({mark}) => Axios.post('service/rest/internal/logging/log/mark', mark, {headers: {'Content-Type': 'text/plain'}})
      }
    }
);
