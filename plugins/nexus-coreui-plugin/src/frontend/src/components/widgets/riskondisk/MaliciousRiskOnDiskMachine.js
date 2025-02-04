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
import {assign, createMachine} from "xstate";

export default createMachine({
  id: 'MaliciousRiskOnDiskMachine',
  initial: 'chooseInitialState',

  context: {},

  states: {
    chooseInitialState: {
      always: [
        {
          cond: 'shouldOpenAndLoad',
          target: 'loaded'
        },
        {
          target: 'close'
        }
      ]
    },
    loaded: {},
    close: {},
    loadError: {
      on: {
        'RETRY': {
          target: 'loaded',
          actions: 'clearError'
        }
      }
    }
  },
  on: {
    DISMISS: {
      target: 'close',
      actions: 'setCookie'
    }
  },
},
{
  actions: {
    clearError: assign({
      loadError: () => null
    }),

    setCookie: () => document.cookie = 'MALWARE_BANNER=close; path=/'
  },
  guards: {
    shouldOpenAndLoad: () => document.cookie.match(/MALWARE_BANNER=([^;]*)/)?.[1] !== 'close' ||
        window.location.hash.includes('#browse/malwarerisk')
  },
  services: {
    fetchData: () => Axios.get(MALICIOUS_RISK_ON_DISK)
  }
});
