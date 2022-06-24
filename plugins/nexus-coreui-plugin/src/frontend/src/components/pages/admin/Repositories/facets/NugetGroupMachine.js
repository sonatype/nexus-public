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
import {APIConstants} from '@sonatype/nexus-ui-plugin';

export const url = `${APIConstants.REST.INTERNAL.REPOSITORIES}?format=nuget`;

export const buildMachine = (name) =>
  createMachine(
    {
      id: 'NugetGroupMachine',
      initial: 'loading',
      states: {
        loaded: {
          on: {
            LOAD_DATA: {
              target: 'loading'
            },
            SET_GROUP_VERSION: {
              target: 'loaded',
              actions: ['setGroupVersion']
            }
          }
        },
        loading: {
          invoke: {
            src: 'fetchData',
            onDone: [
              {
                target: 'loaded',
                actions: 'setData'
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
        setData: assign((_, event) => {
          const repositories = event.data?.data;

          const repositoriesMap = repositories.reduce((acc, repo) => {
            acc[repo.name] = repo;
            return acc;
          }, {});

          repositories.forEach((repo) => {
            if (repo.memberNames) {
              repo.nugetVersion = getVersionFromMembers(repo.memberNames, repositoriesMap);
            }
          });

          const groupVersion = repositoriesMap[name]?.nugetVersion || 'V3';

          return {
            repositories,
            groupVersion
          };
        }),
        setError: assign({
          error: (_, event) => event.data?.message
        }),
        clearError: assign({
          error: () => null
        }),
        setGroupVersion: assign({
          groupVersion: (_, {groupVersion}) => groupVersion
        })
      },
      services: {
        fetchData: () => Axios.get(url)
      }
    }
  );

const getVersionFromMembers = (memberNames, repositoriesMap) => {
  for (let memberName of memberNames) {
    const member = repositoriesMap[memberName];
    if (member.nugetVersion) {
      return member.nugetVersion;
    } else if (member.memberNames) {
      member.nugetVersion = getVersionFromMembers(member.memberNames);
      return member.nugetVersion;
    }
  }
};
