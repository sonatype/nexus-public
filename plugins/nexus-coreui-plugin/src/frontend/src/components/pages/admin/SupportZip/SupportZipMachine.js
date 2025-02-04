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
import Axios from "axios";

export default Machine(
    {
      initial: 'selectContents',

      context: {
        params: {
          systemInformation: true,
          threadDump: true,
          configuration: true,
          security: true,
          log: true,
          taskLog: true,
          auditLog: true,
          metrics: true,
          jmx: true,
          replication: true,
          archivedLog: 0,
          limitFileSizes: true,
          limitZipSize: true
        },
        response: {}
      },

      states: {
        selectContents: {
          on: {
            UPDATE: {
              target: 'selectContents',
              actions: ['setParams']
            },
            CREATE_SUPPORT_ZIPS: {
              target: 'creatingSupportZips'
            },
            CREATE_HA_SUPPORT_ZIPS: {
              target: 'creatingHaSupportZips'
            }
          }
        },
        creatingSupportZips: {
          invoke: {
            src: 'create',
            onDone: {
              target: 'supportZipsCreated',
              actions: ['setResponse']
            },
            onError: {
              target: 'error',
              actions: ['setCreateError']
            }
          }
        },
        supportZipsCreated: {},
        creatingHaSupportZips: {
          invoke: {
            src: 'createHaZips',
            onDone: {
              target: 'haSupportZipsCreated',
              actions: ['setResponse']
            },
            onError: {
              target: 'error',
              actions: ['setCreateError']
            }
          }
        },
        haSupportZipsCreated: {},
        error: {
          on: {
            RETRY: {
              target: 'creatingSupportZips'
            },
            RETRY_HA: {
              target: 'creatingHaSupportZips'
            }
          }
        }
      },
    },
    {
      actions: {
        setParams: assign({
          params: (_, {params}) => params
        }),
        setCreateError: assign({
          createError: (_, event) => event.data.message
        }),
        setResponse: assign({
          response: (_, event) => event.data.data,
        })
      },
      services: {
        create: ({params}) => Axios.post('service/rest/v1/support/supportzippath', params),
        createHaZips: ({params}) => Axios.post('service/rest/v1/nodes/supportzips', params)
      }
    }
);
