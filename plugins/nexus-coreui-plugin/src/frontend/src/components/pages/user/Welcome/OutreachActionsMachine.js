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

const SHOW_CONNECT_ACTION_KEY = 'nx-welcome-show-connect-action';
const SHOW_CONNECT_ACTION_DEFAULT = true;

export default Machine({
  id: 'OutreachActionsMachine',
  initial: 'readingShowConnectAction',
  context: {
    showConnectAction: SHOW_CONNECT_ACTION_DEFAULT,
    url: '',
  },

  states: {
    idle: {
      on: {
        REDIRECT: {
          target: 'redirecting',
          actions: ['setUrl'],
        },
        OPEN: {
          target: 'idle',
          actions: ['open'],
        },
        OPEN_CONNECT_MODAL: {
          target: 'showingConnectModal',
        },
      },
    },
    redirecting: {
      type: 'final',
      invoke: {
        src: 'redirect',
      },
    },
    readingShowConnectAction: {
      invoke: {
        src: 'readShowConnectAction',
      },
    },
    savingShowConnectAction: {
      invoke: {
        src: 'saveShowConnectAction',
      },
    },
    showingConnectModal: {
      on: {
        CLOSE_MODAL: {
          target: 'savingShowConnectAction',
        }
      }
    },
  },
  on: {
    SET_DATA: {
      target: 'idle',
      actions: ['setShowConnectAction'],
    }
  },
}, {
  actions: {
    setShowConnectAction: assign({
      showConnectAction: (_, event) => event.showConnectAction,
    }),
    setUrl: assign({
      url: (_, event) => event.url,
    }),
    open: (_, event) => window.open(event.url, '_blank'),
  },
  services: {
    readShowConnectAction: () => callback => {
      const showConnectAction = localStorage.getItem(SHOW_CONNECT_ACTION_KEY);
      callback({
        type: 'SET_DATA',
        showConnectAction: showConnectAction ? JSON.parse(showConnectAction) : SHOW_CONNECT_ACTION_DEFAULT,
      });
    },
    saveShowConnectAction: () => callback => {
      localStorage.setItem(SHOW_CONNECT_ACTION_KEY, JSON.stringify(false));
      callback({ type: 'SET_DATA', showConnectAction: false});
    },
    redirect: ({url}) => {
      window.location.hash = url;
    },
  }
});
