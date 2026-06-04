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

import { useMemo } from 'react';
import { ENDPOINTS, restClient } from '../../../../../../interface/api';
import { useForm, type FormContext } from '../../../../../../interface/form';
import { useToast } from '../../../../shared';
import { createHttpFormMachine, httpConfigToRest } from './httpFormMachine';
import { HttpConfiguration } from './types';

export interface UseHttpFormOptions {
  onCancel?: () => void;
}

const HTTP_SAVE_ERROR_MESSAGES: Record<number, string> = {
  400: 'Invalid HTTP settings. Please check your proxy host and port values.',
  403: 'You do not have permission to modify HTTP settings. Contact your administrator.',
  405: 'Unable to save HTTP settings. The server rejected this request.',
  500: 'Server error while saving HTTP settings. Please try again.',
};

export function humanizeHttpError(err: unknown): string {
  if (err && typeof err === 'object' && 'response' in err) {
    const status = (err as { response?: { status?: number } }).response?.status;
    if (status && HTTP_SAVE_ERROR_MESSAGES[status]) {
      return HTTP_SAVE_ERROR_MESSAGES[status];
    }
  }
  const message = err instanceof Error ? err.message : String(err);
  if (message.includes('status code')) {
    return 'Unable to save HTTP settings. Please try again or contact your administrator.';
  }
  return message;
}

/**
 * Custom hook for managing HTTP settings form state.
 *
 * Uses XState form machine for state management with automatic dirty tracking
 * and unsaved changes warnings. Loads current HTTP configuration on mount,
 * validates on field changes, and saves via REST PUT.
 */
export function useHttpForm(options: UseHttpFormOptions = {}) {
  const toast = useToast();
  const { onCancel } = options;

  const machine = useMemo(() => createHttpFormMachine(), []);

  const form = useForm(machine, {
    actions: {
      onCancel: () => onCancel?.(),
    },
    services: {
      save: async (ctx: FormContext<HttpConfiguration>) => {
        try {
          const payload = httpConfigToRest(ctx.data);
          await restClient.put(ENDPOINTS.HTTP, payload);
          toast.success('HTTP settings saved');
        } catch (err) {
          const message = humanizeHttpError(err);
          toast.error(message);
          throw new Error(message);
        }
      },
    },
  });

  return form;
}
