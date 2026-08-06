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

import { useEffect, useMemo } from 'react';
import { useMachine } from '@xstate/react';
import {
  createApiPageMachine,
  defaultLoadSwagger,
  type SwaggerSpec,
} from './apiPageMachine';

export interface UseApiPageOptions {
  swaggerUrl: string;
}

export interface UseApiPageReturn {
  swagger: SwaggerSpec | null;
  isLoading: boolean;
  hasError: boolean;
  refresh: () => void;
  retry: () => void;
}

/**
 * Hook wrapping the API-page state machine. Loads the swagger spec via the machine's
 * loading/loaded/loadError states and exposes flags plus REFRESH/RETRY handlers so the
 * page component stays presentation-only.
 */
export function useApiPage(options: UseApiPageOptions): UseApiPageReturn {
  const { swaggerUrl } = options;

  const machine = useMemo(
    () =>
      createApiPageMachine({
        swaggerUrl,
        loadSwagger: defaultLoadSwagger,
      }),
    // eslint-disable-next-line react-hooks/exhaustive-deps -- machine is created once with the initial swaggerUrl; URL changes are handled via the SET_SWAGGER_URL event in the effect below
    []
  );

  const [state, send] = useMachine(machine);

  useEffect(() => {
    send({ type: 'SET_SWAGGER_URL', url: swaggerUrl });
  }, [swaggerUrl, send]);

  return {
    swagger: state.context.swagger,
    isLoading: state.matches('loading'),
    hasError: state.matches('loadError'),
    refresh: () => send({ type: 'REFRESH' }),
    retry: () => send({ type: 'RETRY' }),
  };
}
