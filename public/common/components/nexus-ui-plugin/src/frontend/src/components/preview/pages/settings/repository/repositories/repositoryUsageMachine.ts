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

import { createMachine, assign } from 'xstate';

import { restClient, parseApiError } from '../../../../../../interface/api';
import { fetchRepositoryMetricsDetail } from '../../../../shared/hooks/fetchRepositoryMetricsDetail';
import type { RepositoryTypeKind } from './repositoryTypeIcon';

export type RepositoryUsageKind = RepositoryTypeKind;

export interface RepositoryMetrics {
  componentCount: number | undefined;
  assetCount: number | undefined;
  totalSize: number | undefined;
}

export interface GroupMember {
  name: string;
  type: 'hosted' | 'proxy';
}

export interface RepositoryUsageContext {
  repositoryName: string;
  repositoryType: RepositoryUsageKind;
  metrics: RepositoryMetrics | null;
  groupMembers: GroupMember[];
  whereUsed: string[];
  error: string | null;
  membershipError: string | null;
}

export type RepositoryUsageEvent =
  | { type: 'REFRESH' }
  | { type: 'RETRY' };

const REPOSITORIES_REST_URL = '/service/rest/v1/repositories';
const REPOSITORY_DETAIL_URL = (name: string) =>
  `/service/rest/internal/ui/repositories/repository/${encodeURIComponent(name)}`;

interface RepositoryDetail {
  name: string;
  type: string;
  group?: { memberNames?: string[] };
}

async function fetchRepositoryDetail(repositoryName: string): Promise<RepositoryDetail> {
  return restClient.get<RepositoryDetail>(REPOSITORY_DETAIL_URL(repositoryName));
}

async function fetchMetrics(repositoryName: string): Promise<RepositoryMetrics> {
  const detail = await fetchRepositoryMetricsDetail(repositoryName);

  return {
    componentCount: detail?.componentCount,
    assetCount: detail?.assetCount,
    totalSize: detail?.size,
  };
}

async function fetchGroupMembers(repositoryName: string): Promise<GroupMember[]> {
  const repo = await fetchRepositoryDetail(repositoryName);
  const memberNames = repo?.group?.memberNames || [];

  if (memberNames.length === 0) {
    return [];
  }

  // Fetch all repositories to build a name→type lookup
  const allRepos = await restClient.get<Array<{
    name: string;
    type: string;
  }>>(REPOSITORIES_REST_URL);

  if (!Array.isArray(allRepos)) {
    return memberNames.map(name => ({ name, type: 'hosted' as const })); // fallback default
  }

  // Build a Map for O(1) lookup
  const repoTypeMap = new Map<string, string>();
  for (const r of allRepos) {
    repoTypeMap.set(r.name, r.type);
  }

  // Enrich member names with their types
  return memberNames.map(name => {
    const type = repoTypeMap.get(name);
    // Defensive: if type is missing or not hosted/proxy, default to hosted
    if (type !== 'hosted' && type !== 'proxy') {
      console.warn(`repositoryUsageMachine: unexpected member type "${type}" for "${name}", defaulting to hosted`);
    }
    const safeType: 'hosted' | 'proxy' = (type === 'hosted' || type === 'proxy') ? type : 'hosted';
    return { name, type: safeType };
  });
}

// N+1: fetches every group's detail individually to check membership, after listing all
// repos. Acceptable for now — sharing one repository across a large number of groups is
// rare in practice — but a dedicated where-used endpoint would avoid this if it becomes
// a real cost on large instances.
async function fetchWhereUsed(repositoryName: string): Promise<string[]> {
  const allRepos = await restClient.get<Array<{
    name: string;
    type: string;
  }>>(REPOSITORIES_REST_URL);

  if (!Array.isArray(allRepos)) {
    return [];
  }

  const groups = allRepos.filter(r => r.type === 'group');

  const groupDetailResults = await Promise.allSettled(groups.map(g => fetchRepositoryDetail(g.name)));

  return groupDetailResults
    .filter((result): result is PromiseFulfilledResult<RepositoryDetail> => result.status === 'fulfilled')
    .map(result => result.value)
    .filter(detail => detail?.group?.memberNames?.includes(repositoryName) ?? false)
    .map(detail => detail.name);
}

export interface RepositoryUsageMachineOptions {
  repositoryName: string;
  repositoryType: RepositoryUsageKind;
}

interface FetchDataResult {
  metrics: RepositoryMetrics | null;
  metricsError: string | null;
  groupMembers: GroupMember[];
  whereUsed: string[];
  membershipError: string | null;
}

export function createRepositoryUsageMachine(options: RepositoryUsageMachineOptions) {
  const { repositoryName, repositoryType } = options;

  return createMachine<RepositoryUsageContext, RepositoryUsageEvent>(
    {
      id: 'repositoryUsage',
      initial: 'loading',
      context: {
        repositoryName,
        repositoryType,
        metrics: null,
        groupMembers: [],
        whereUsed: [],
        error: null,
        membershipError: null,
      },
      states: {
        loading: {
          invoke: {
            src: 'fetchData',
            onDone: [
              {
                target: 'error',
                cond: 'allFailed',
                actions: 'assignData',
              },
              {
                target: 'loaded',
                actions: 'assignData',
              },
            ],
          },
        },
        loaded: {
          on: {
            REFRESH: 'loading',
          },
        },
        error: {
          on: {
            RETRY: 'loading',
          },
        },
      },
    },
    {
      services: {
        fetchData: async (): Promise<FetchDataResult> => {
          const membershipPromise: Promise<GroupMember[] | string[]> = repositoryType === 'group'
            ? fetchGroupMembers(repositoryName)
            : fetchWhereUsed(repositoryName);

          const [metricsResult, membershipResult] = await Promise.allSettled([
            fetchMetrics(repositoryName),
            membershipPromise,
          ]);

          const metrics = metricsResult.status === 'fulfilled' ? metricsResult.value : null;
          const metricsError = metricsResult.status === 'rejected'
            ? parseApiError(metricsResult.reason).message
            : null;

          const membershipValue = membershipResult.status === 'fulfilled' ? membershipResult.value : [];
          const membershipError = membershipResult.status === 'rejected'
            ? parseApiError(membershipResult.reason).message
            : null;

          return {
            metrics,
            metricsError,
            groupMembers: repositoryType === 'group' ? (membershipValue as GroupMember[]) : [],
            whereUsed: repositoryType === 'group' ? [] : (membershipValue as string[]),
            membershipError,
          };
        },
      },
      guards: {
        allFailed: (_context, event) => {
          const data = event.data as FetchDataResult;
          return data.metricsError !== null && data.membershipError !== null;
        },
      },
      actions: {
        assignData: assign({
          metrics: (_context, event) => (event.data as FetchDataResult).metrics,
          groupMembers: (_context, event) => (event.data as FetchDataResult).groupMembers,
          whereUsed: (_context, event) => (event.data as FetchDataResult).whereUsed,
          error: (_context, event) => (event.data as FetchDataResult).metricsError,
          membershipError: (_context, event) => (event.data as FetchDataResult).membershipError,
        }),
      },
    }
  );
}
