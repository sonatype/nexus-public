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

import { useMemo, useCallback } from 'react';
import { useMachine } from '@xstate/react';

import { createRepositoryUsageMachine } from './repositoryUsageMachine';
import type { RepositoryUsageContext, RepositoryUsageKind } from './repositoryUsageMachine';

export interface UseRepositorySettingsUsageTabReturn {
  metrics: RepositoryUsageContext['metrics'];
  groupMembers: RepositoryUsageContext['groupMembers'];
  whereUsed: RepositoryUsageContext['whereUsed'];

  loading: boolean;
  loaded: boolean;
  error: string | null;
  membershipError: string | null;

  componentCountPending: boolean;
  assetCountPending: boolean;
  totalSizePending: boolean;

  refresh: () => void;
  retry: () => void;
}

export function useRepositorySettingsUsageTab(
  repositoryName: string,
  repositoryType: RepositoryUsageKind,
): UseRepositorySettingsUsageTabReturn {
  const machine = useMemo(
    () => createRepositoryUsageMachine({ repositoryName, repositoryType }),
    [repositoryName, repositoryType]
  );

  const [state, send] = useMachine(machine);

  const { context, matches } = state;

  const loading = matches('loading');
  const loaded = matches('loaded');
  const error = context.error;
  const membershipError = context.membershipError;

  const componentCountPending = context.metrics?.componentCount === undefined
    || context.metrics?.componentCount === null;
  const assetCountPending = context.metrics?.assetCount === undefined
    || context.metrics?.assetCount === null;
  const totalSizePending = context.metrics?.totalSize === undefined
    || context.metrics?.totalSize === null;

  const refresh = useCallback(() => {
    send({ type: 'REFRESH' });
  }, [send]);

  const retry = useCallback(() => {
    send({ type: 'RETRY' });
  }, [send]);

  return {
    metrics: context.metrics,
    groupMembers: context.groupMembers,
    whereUsed: context.whereUsed,

    loading,
    loaded,
    error,
    membershipError,

    componentCountPending,
    assetCountPending,
    totalSizePending,

    refresh,
    retry,
  };
}

export default useRepositorySettingsUsageTab;
