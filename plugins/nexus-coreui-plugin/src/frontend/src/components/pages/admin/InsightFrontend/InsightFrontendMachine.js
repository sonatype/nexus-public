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
import axios from 'axios';

export default createMachine(
    {
      id: 'InsightFrontendMachine',
      initial: 'loading',
      context: {
        downloadsByRepositoryName: [],
        downloadsByIpAddress: [],
        downloadsByUsername: [],
        downloadsByDay: [],
        downloadsByDayNonVulnerable: [],
        totalDownloads: 0
      },
      states: {
        loading: {
          invoke: {
            src: 'fetchData',
            onDone: {
              target: 'viewing',
              actions: ['setData']
            },
          }
        },
        viewing: {}
      },
    },
    {
      actions: {
        setData: assign((_, event) => event.data)
      },
      services: {
        fetchData: async () => {
          const downloadsByRepositoryName = await axios.get('service/rest/v1/vulnerability/count-by-repository-name');
          const downloadsByUsername = await axios.get('service/rest/v1/vulnerability/count-by-username');
          const downloadsByIp = await axios.get('service/rest/v1/vulnerability/count-by-ip');
          const downloadsByDay = await axios.get('service/rest/v1/vulnerability/count-by-day');
          const downloadsByDayNonVulnerable = await axios.get('service/rest/v1/vulnerability/count-by-day-non-vulnerable');
          const totalDownloads = await axios.get('service/rest/v1/vulnerability/count-total');

          return {
            downloadsByRepositoryName: downloadsByRepositoryName?.data || [],
            downloadsByIpAddress: downloadsByIp?.data || [],
            downloadsByUsername: downloadsByUsername?.data || [],
            downloadsByDay: downloadsByDay?.data || [],
            downloadsByDayNonVulnerable: downloadsByDayNonVulnerable?.data || [],
            totalDownloads: totalDownloads?.data || 0
          };
        }
      }
    }
);
