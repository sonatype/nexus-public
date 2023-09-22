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
import {mergeDeepRight} from 'ramda';
import {APIConstants} from '@sonatype/nexus-ui-plugin';

export default Machine(
  {
    id: 'NodeCardMachine',
    initial: 'loading',
    context: {
      data: null,
      pristineData: null,
    },
    states: {
      loading: {
        invoke: {
          src: 'fetchData',
          onDone: {
            target: 'loaded',
            actions: 'setData',
          },
        },
      },
      loaded: {
        on: {
          UPDATE_STATUS: {
            target: 'loading',
            actions: 'updateStatus',
          },
        },
        after: {
          TIMEOUT: {
            target: 'loading',
            cond: 'canCheckStatus',
          },
        },
      },
    },
  },
  {
    actions: {
      setData: assign({
        data: (context, event) => event?.data?.data || context?.data,
      }),
      updateStatus: assign({
        data: (context, {status}) => mergeDeepRight(context.data, {status}),
      }),
    },

    guards: {
      canCheckStatus: ({data}) => data.status === 'CREATING',
    },

    services: {
      fetchData: (context) =>
        Axios.get(
          APIConstants.REST.INTERNAL.GET_ZIP_STATUS + context.data?.nodeId
        ),
    },

    delays: {
      TIMEOUT: 2000,
    },
  }
);
