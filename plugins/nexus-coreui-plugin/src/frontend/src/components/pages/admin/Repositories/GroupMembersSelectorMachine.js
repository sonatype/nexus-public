/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import Axios from 'axios';
import {assign, Machine} from 'xstate';

export const repositoriesUrl = (format) =>
  '/service/rest/internal/ui/repositories?format=' + format;

export default Machine(
  {
    id: 'GroupMembersSelectorMachine',
    initial: 'idle',
    context: {
      repositories: [],
      error: null
    },
    states: {
      idle: {
        on: {
          LOAD_REPOSITORIES: {
            target: 'loading'
          }
        }
      },
      loading: {
        invoke: {
          src: 'fetchRepositories',
          onDone: [
            {
              target: 'idle',
              actions: 'setRepositories'
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
      setRepositories: assign({
        repositories: (_, event) =>
          event.data?.data?.map((r) => ({id: r.id, displayName: r.name})) || []
      }),
      setError: assign({
        error: (_, event) => event.data.message
      }),
      clearError: assign({
        error: () => null
      })
    },
    services: {
      fetchRepositories: (_, event) => Axios.get(repositoriesUrl(event.format))
    }
  }
);
