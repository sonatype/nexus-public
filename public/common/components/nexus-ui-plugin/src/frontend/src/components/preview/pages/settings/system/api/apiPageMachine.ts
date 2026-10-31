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

import { assign, createMachine } from 'xstate';

export type SwaggerSpec = Record<string, unknown>;

export interface ApiPageContext {
  swaggerUrl: string;
  swagger: SwaggerSpec | null;
}

export type ApiPageEvent =
  | { type: 'REFRESH' }
  | { type: 'RETRY' }
  | { type: 'SET_SWAGGER_URL'; url: string };

export interface CreateApiPageMachineOptions {
  swaggerUrl: string;
  loadSwagger: (url: string) => Promise<SwaggerSpec>;
}

/**
 * Display-only machine that loads the OpenAPI/Swagger specification for the API page.
 *
 * The page also uses independent hooks for endpoint permissions and (conditionally)
 * role-lens data; those remain stateful hooks — see the design doc's non-goals.
 * Rolling the swagger load into a machine gives explicit `loading | loaded | loadError`
 * states with REFRESH/RETRY events, replacing the ad-hoc useEffect + useState pair.
 */
export function createApiPageMachine(options: CreateApiPageMachineOptions) {
  const { swaggerUrl, loadSwagger } = options;

  return createMachine<ApiPageContext, ApiPageEvent>(
    {
      id: 'api-page',
      predictableActionArguments: true,
      initial: 'loading',
      context: {
        swaggerUrl,
        swagger: null,
      },
      states: {
        loading: {
          entry: 'clearSwagger',
          invoke: {
            src: 'load',
            onDone: {
              target: 'loaded',
              actions: 'setSwagger',
            },
            onError: {
              target: 'loadError',
            },
          },
        },
        loaded: {
          on: {
            REFRESH: 'loading',
            SET_SWAGGER_URL: {
              target: 'loading',
              actions: 'setSwaggerUrl',
            },
          },
        },
        loadError: {
          on: {
            RETRY: 'loading',
            SET_SWAGGER_URL: {
              target: 'loading',
              actions: 'setSwaggerUrl',
            },
          },
        },
      },
    },
    {
      actions: {
        clearSwagger: assign({ swagger: (_ctx) => null }),
        setSwagger: assign({
          swagger: (_, event) => {
            const doneEvent = event as unknown as { data: SwaggerSpec };
            return doneEvent.data;
          },
        }),
        setSwaggerUrl: assign({
          swaggerUrl: (_, event) => {
            const setEvent = event as { type: 'SET_SWAGGER_URL'; url: string };
            return setEvent.url;
          },
        }),
      },
      services: {
        load: async (ctx) => loadSwagger(ctx.swaggerUrl),
      },
    }
  );
}

/**
 * Standard swagger loader. Wraps the native `fetch` call the page previously ran inline
 * so the machine's `load` service is a pure function of its arguments.
 */
export async function defaultLoadSwagger(url: string): Promise<SwaggerSpec> {
  const response = await fetch(url, { credentials: 'same-origin' });
  if (!response.ok) {
    throw new Error(String(response.status));
  }
  const json = await response.json();
  if (!json || typeof json !== 'object') {
    throw new Error('Invalid swagger response');
  }
  return json as SwaggerSpec;
}
