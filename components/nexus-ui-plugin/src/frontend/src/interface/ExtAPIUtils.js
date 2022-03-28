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
import {mergeDeepRight} from 'ramda';
import UIStrings from "../constants/UIStrings";

/**
 * @param url URL string for the request
 * @param action the ExtJS action (example: coreui_Bundle, coreui_AnonymousSettings etc.)
 * @param method the method for the request (example: read, write etc.)
 */
export const extAPIRequest = (action, method, data = null) => Axios.post('/service/extdirect',
    {
      "action": action,
      "method": method,
      "data": data,
      "type": "rpc",
      "tid": 8
    }
);

export default class ExtAPIUtils {
  static request = extAPIRequest;

  static useExtMachine(action, method, options = {}) {
    const machine = createMachine({
      id: `ExtMachine(${action}, ${method})`,

      initial: options.initial || 'loaded',

      states: {
        loading: {
          invoke: {
            src: 'fetch',
            onDone: {
              target: 'loaded',
              actions: ['setData']
            },
            onError: {
              target: 'error',
              actions: ['setError']
            }
          }
        },
        loaded: {},
        error: {}
      },

      on: {
        LOAD: {
          target: 'loading'
        }
      }
    }, {
      actions: {
        setData: assign({
          data: (_, event) => ExtAPIUtils.extractResult(event)
        }),

        setError: assign({
          error: (_, event) => event?.message || UIStrings.ERROR.UNKNOWN
        })
      },
      services: {
        fetch: async (_, {data = null}) => extAPIRequest(action, method, data)
      }
    });

    return useMachine(machine, mergeDeepRight({devTools: true}, options));
  }

  static extractResult(event, defaultOption = []) {
    return event?.data?.data?.result?.data || defaultOption;
  }
}
