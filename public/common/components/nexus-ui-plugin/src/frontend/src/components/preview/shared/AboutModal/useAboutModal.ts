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
import { createAboutLegalMachine } from './aboutLegalMachine';

/*
 * ViewModel (thin projection) over aboutLegalMachine. Owns no state of its own;
 * projects the machine state into a view-facing status flag plus the loaded
 * legal HTML, and exposes a single semantic command (fetchLegal) that the View
 * fires when the modal opens.
 */

export type AboutLegalStatus = 'idle' | 'loading' | 'loaded' | 'error';

export interface UseAboutModalResult {
  status: AboutLegalStatus;
  legalHtml: string;
  fetchLegal: () => void;
}

export function useAboutModal(): UseAboutModalResult {
  const machine = useMemo(() => createAboutLegalMachine(), []);
  const [state, send] = useMachine(machine);

  const fetchLegal = useCallback(() => send({ type: 'FETCH' }), [send]);

  const status = state.matches('loading')
    ? 'loading'
    : state.matches('loaded')
      ? 'loaded'
      : state.matches('error')
        ? 'error'
        : 'idle';

  return {
    status,
    legalHtml: state.context.html,
    fetchLegal,
  };
}
