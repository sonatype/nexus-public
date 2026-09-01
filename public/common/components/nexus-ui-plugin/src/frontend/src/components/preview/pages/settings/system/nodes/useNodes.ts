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
import { createNodesMachine } from './nodesMachine';
import { NodeInfo } from './types';

export interface UseNodesReturn {
  nodes: NodeInfo[];
  loading: boolean;
  error: string | null;
  refresh: () => void;
  retry: () => void;
}

/**
 * Integration hook for the Nodes list. Wraps the nodesMachine and exposes a
 * simple UI-facing contract; no XState types leak to the component.
 */
export function useNodes(): UseNodesReturn {
  const machine = useMemo(() => createNodesMachine(), []);
  const [state, send] = useMachine(machine);

  const refresh = useCallback(() => send({ type: 'REFRESH' }), [send]);
  const retry = useCallback(() => send({ type: 'RETRY' }), [send]);

  return {
    nodes: state.context.nodes,
    loading: state.matches('loading'),
    error: state.context.loadError,
    refresh,
    retry,
  };
}
