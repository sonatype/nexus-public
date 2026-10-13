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

import { useMemo, useRef } from 'react';
import { useMachine } from '@xstate/react';
import { createResourceMachine, ResourceMachineOptions, ResourceError } from './createResourceMachine';

// Refs so late fetcher swaps reach the machine (useMachine caches machine
// identity from mount). Exposes structured `error` + string `errorMessage`.

export interface UseResourceMachineResult<T> {
  data: T;
  loading: boolean;
  loaded: boolean;
  error: ResourceError | null;
  errorMessage: string | null;
  retry: () => void;
  refresh: () => void;
}

export function useResourceMachine<T>(
  options: ResourceMachineOptions<T>,
): UseResourceMachineResult<T> {
  const { id, initialData, fetch } = options;

  const fetchRef = useRef(fetch);
  fetchRef.current = fetch;

  const machine = useMemo(
    () =>
      createResourceMachine<T>({
        id,
        initialData,
        fetch: () => fetchRef.current(),
      }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [],
  );

  const [state, send] = useMachine(machine);

  return {
    data: state.context.data,
    loading: state.matches('loading'),
    loaded: state.matches('loaded'),
    error: state.context.error,
    errorMessage: state.context.error?.message ?? null,
    retry: () => send({ type: 'RETRY' }),
    refresh: () => send({ type: 'REFRESH' }),
  };
}
