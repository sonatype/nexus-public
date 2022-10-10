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
import {assign} from 'xstate';

import {APIConstants, FormUtils} from "@sonatype/nexus-ui-plugin";

export default FormUtils.buildFormMachine({
  id: 'NodeCardMachine',
  initial: 'clearHistory',

  config: (config) => ({
    ...config,

    context: {
      ...config.context,
      node: null
    },

    states: {
      ...config.states,

      clearHistory: {
        invoke: {
          src: 'clearHistory',
          onDone: 'loading',
          onError: 'failure'
        }
      },

      loaded: {
        ...config.states.loaded,
        after: {
          TIMEOUT: 'loading'
        }
      },

      failure: {
        after: {
          INTERVAL: 'loading'
        }
      }
    }
  })
}).withConfig({
  actions: {
    setData: assign({
      node: (context, event) => event?.data?.data || context.node
    })
  },

  services: {
    clearHistory: (context) => Axios.delete(APIConstants.REST.INTERNAL.CLEAR_SUPPORT_ZIP_HISTORY + context.node.nodeId),
    fetchData: (context) => Axios.get(APIConstants.REST.INTERNAL.GET_ZIP_STATUS + context.node.nodeId)
  },

  delays: {
    INTERVAL: 2000, TIMEOUT: 2000
  }
});
