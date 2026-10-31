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

import { useCallback, useMemo } from 'react';
import { useMachine } from '@xstate/react';
import { createRoleModalContentMachine } from './roleModalContentMachine';
import { useRolesApi } from './useRolesApi';
import type { Role } from './types';

// ViewModel (thin projection) over roleModalContentMachine. Holds no state of its own;
// interprets the machine's state for the RoleModalContent view. Errors are surfaced by
// state name: `notFound` for a null findRole result (backend 404) and `error` for any
// thrown findRole (network, 5xx, 403, etc.). Only `error` is user-recoverable via retry.

export type RoleModalErrorType = 'notFound' | 'transient';

export interface UseRoleModalContentResult {
  role: Role | null;
  loading: boolean;
  error?: string;
  errorType: RoleModalErrorType | null;
  retry: () => void;
}

export function useRoleModalContent(roleName: string): UseRoleModalContentResult {
  const { findRole } = useRolesApi();

  const machine = useMemo(
    () => createRoleModalContentMachine({ roleName, findRole }),
    [roleName, findRole],
  );

  const [state, send] = useMachine(machine);

  const retry = useCallback(() => {
    send({ type: 'RETRY' });
  }, [send]);

  const loading = state.matches('loading');
  const errorType: RoleModalErrorType | null = state.matches('notFound')
    ? 'notFound'
    : state.matches('error')
      ? 'transient'
      : null;

  return {
    role: state.context.role,
    loading,
    error: state.context.error ?? undefined,
    errorType,
    retry,
  };
}
