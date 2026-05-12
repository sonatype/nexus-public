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
import { useForm, ENDPOINTS, restClient } from '@sonatype/nexus-ui-plugin';
import type { FormContext } from '@sonatype/nexus-ui-plugin';
import { useToast } from '../../../../shared';
import { createEmailFormMachine, emailConfigToRest } from './emailFormMachine';
import { EmailConfiguration } from './types';

export interface UseEmailFormOptions {
  onCancel?: () => void;
}

/**
 * Custom hook for managing Email settings form state.
 *
 * Uses XState form machine for state management with automatic dirty tracking
 * and unsaved changes warnings. Loads current email configuration on mount,
 * validates on field changes, and saves via REST PUT.
 */
export function useEmailForm(options: UseEmailFormOptions = {}) {
  const toast = useToast();
  const { onCancel } = options;

  const machine = useMemo(() => createEmailFormMachine(), []);

  const form = useForm(machine, {
    actions: {
      onCancel: () => onCancel?.(),
    },
    services: {
      save: async (ctx: FormContext<EmailConfiguration>) => {
        try {
          const payload = emailConfigToRest(ctx.data);
          await restClient.put(ENDPOINTS.EMAIL, payload);
          toast.success('Email server settings saved');
        } catch (err) {
          toast.error(err instanceof Error ? err.message : 'Failed to save email settings');
          throw err;
        }
      },
    },
  });

  return form;
}
