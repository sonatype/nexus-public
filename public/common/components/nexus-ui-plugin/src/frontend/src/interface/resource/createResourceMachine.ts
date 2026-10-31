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
import type { FieldError } from '../api/types';

// Generic fetch-on-mount resource machine: loading -> loaded | errored, with
// RETRY / REFRESH re-entering loading.

// Structured error mirrors ThrownApiError so consumers can branch on status.
export interface ResourceError {
  status: number;
  message: string;
  code?: string;
  fieldErrors?: FieldError[];
  requestId?: string;
}

export const DEFAULT_RESOURCE_ERROR_MESSAGE = 'Failed to load data';

export interface ResourceContext<T> {
  data: T;
  error: ResourceError | null;
}

export type ResourceEvent = { type: 'RETRY' } | { type: 'REFRESH' };

export interface ResourceMachineOptions<T> {
  id: string;
  initialData: T;
  fetch: () => Promise<T>;
}

// Extract ResourceError from any thrown value; keeps ThrownApiError fields intact.
export function toResourceError(err: unknown): ResourceError {
  if (err && typeof err === 'object') {
    const asAny = err as Record<string, unknown>;
    const message = typeof asAny.message === 'string' ? asAny.message : DEFAULT_RESOURCE_ERROR_MESSAGE;
    const status = typeof asAny.status === 'number' ? asAny.status : 0;
    const resourceError: ResourceError = { status, message };
    if (typeof asAny.code === 'string') resourceError.code = asAny.code;
    if (Array.isArray(asAny.fieldErrors)) resourceError.fieldErrors = asAny.fieldErrors as FieldError[];
    if (typeof asAny.requestId === 'string') resourceError.requestId = asAny.requestId;
    return resourceError;
  }
  if (typeof err === 'string' && err.length > 0) {
    return { status: 0, message: err };
  }
  return { status: 0, message: DEFAULT_RESOURCE_ERROR_MESSAGE };
}

export function createResourceMachine<T>(options: ResourceMachineOptions<T>) {
  const { id, initialData, fetch } = options;

  return createMachine<ResourceContext<T>, ResourceEvent>(
    {
      id,
      initial: 'loading',
      context: {
        data: initialData,
        error: null,
      },
      states: {
        loading: {
          invoke: {
            src: 'fetch',
            onDone: {
              target: 'loaded',
              actions: 'assignData',
            },
            onError: {
              target: 'errored',
              actions: 'assignError',
            },
          },
        },
        loaded: {
          on: {
            REFRESH: 'loading',
          },
        },
        errored: {
          on: {
            RETRY: 'loading',
            REFRESH: 'loading',
          },
        },
      },
    },
    {
      services: {
        fetch: () => fetch(),
      },
      actions: {
        assignData: assign<ResourceContext<T>, ResourceEvent>({
          data: (_context, event) => (event as { data: T }).data,
          error: (_context) => null,
        }),
        assignError: assign<ResourceContext<T>, ResourceEvent>({
          error: (_context, event) => toResourceError((event as { data?: unknown }).data),
        }),
      },
    },
  );
}
