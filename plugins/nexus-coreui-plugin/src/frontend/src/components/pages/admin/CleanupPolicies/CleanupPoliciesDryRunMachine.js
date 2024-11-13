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

const INITIAL_DATA = {
  loadError: null,
  repositories: [],
  repository: ''
};

const cleanupPoliciesDryRunMachine = createMachine({
  id: 'CleanupPoliciesDryRunMachine',
  initial: 'loaded',

  context: INITIAL_DATA,

  states: {
    loaded: {
      on: {
        SET_REPOSITORY: {
          actions: 'setRepository'
        }
      }
    },
    loading: {
      entry: 'resetData',

      invoke: {
        src: 'fetchRepositories',
        onDone: [{
          target: 'loaded',
          actions: 'setRepositories'
        }],
        onError: {
          target: 'error',
          actions: 'setError'
        }
      }
    },
    error: {
      on: {
        RETRY: {
          target: 'loading',
          actions: 'clearError'
        }
      }
    }
  },

  on: {
    LOAD_REPOSITORIES: {
      target: 'loading',
      cond: 'canLoadRepositories'
    }
  }
}, {
  actions: {
    resetData: assign({
      loadError: INITIAL_DATA.loadError,
      repositories: INITIAL_DATA.repositories,
      repository: INITIAL_DATA.repository
    }),

    setRepositories: assign({
      repositories: (_, event) => event.data?.data
    }),

    setRepository: assign({
      repository: (_, {repository}) => repository
    }),

    setError: assign({
      loadError: (_, event) => event.data.message
    }),

    clearError: assign({
      loadError: () => null
    })
  },
  guards: {
    canLoadRepositories: (_, {format}) => Boolean(format)
  },
  services: {
    fetchRepositories: (_, {format}) => Axios.get('service/rest/internal/ui/repositories', {params: {format}})
  }
});

export default cleanupPoliciesDryRunMachine;
