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

import { useMemo, useCallback, useRef, useEffect } from 'react';
import { useForm } from '../../../../../../interface/form';
import { useToast } from '../../../../shared';
import { createLdapFormMachine } from './ldapFormMachine';
import { LdapServer, LdapFormData, LdapSchemaTemplate } from './types';

export interface UseLdapFormOptions {
  serverId?: string;
  server?: LdapServer | null;
  existingNames?: string[];
  onSave?: (data: LdapFormData) => Promise<void>;
  onCancel: () => void;
  createServer: (data: LdapFormData) => Promise<LdapServer>;
  updateServer: (data: LdapFormData) => Promise<LdapServer>;
}

export interface UseLdapFormReturn {
  form: ReturnType<typeof useForm>;
  server: LdapServer | null;
  isCreate: boolean;
  applyTemplate: (template: LdapSchemaTemplate) => void;
  changeProtocol: (protocol: string) => void;
  confirmPasswordAndSubmit: (password: string) => void;
}

/**
 * Custom hook for managing LdapForm state and logic.
 *
 * Uses XState form machine for state management with automatic dirty tracking
 * and unsaved changes warnings. The machine loads the LDAP server being edited
 * (if serverId provided) and reference data.
 *
 * This hook also handles save operations and toast notifications.
 */
export function useLdapForm({
  serverId,
  server,
  existingNames,
  onSave,
  onCancel,
  createServer,
  updateServer,
}: UseLdapFormOptions): UseLdapFormReturn {
  const toast = useToast();
  const isCreate = !(serverId || server);

  // Mutable ref so the machine's validate action always sees the latest list
  // without needing to recreate the machine (which would reset form state).
  const existingNamesRef = useRef<string[]>(existingNames ?? []);
  useEffect(() => {
    existingNamesRef.current = existingNames ?? [];
  }, [existingNames]);

  // Create the form machine - memoized based on serverId and server.
  // existingNamesRef intentionally omitted from deps: it's a stable object
  // whose .current is kept up-to-date by the effect above.
  const machine = useMemo(
    () => createLdapFormMachine(serverId, server, existingNamesRef),
    // eslint-disable-next-line react-hooks/exhaustive-deps -- stable ref, kept current by effect above
    [serverId, server]
  );

  // Use the form machine with action/service overrides
  const form = useForm(machine, {
    actions: {
      onCancel: onCancel,
    },
    services: {
      save: async (ctx: { data: LdapFormData; server: LdapServer | null }) => {
        // createServer/updateServer call setError on the shared useLdapApi instance (owned by
        // LdapPage) when they fail, so the error surfaces via the `error` prop passed to
        // LdapForm → WizardForm. Re-throwing here keeps the XState machine in the error state.
        if (isCreate) {
          await createServer(ctx.data);
          toast.success(`LDAP server "${ctx.data.name}" created successfully`);
        } else {
          await updateServer(ctx.data);
          toast.success(`LDAP server "${ctx.data.name}" updated successfully`);
        }
        if (onSave) {
          await onSave(ctx.data);
        }
        onCancel();
      },
    },
  });

  // Access the raw state to get the extended context
  const context = (form.state as { context: { server: LdapServer | null } }).context;
  const loadedServer = context.server;

  /**
   * Apply a schema template to user/group mapping fields
   */
  const applyTemplate = useCallback(
    (template: LdapSchemaTemplate) => {
      form.send({ type: 'APPLY_TEMPLATE', template } as any);
    },
    [form]
  );

  /**
   * Change the LDAP protocol with automatic port update
   */
  const changeProtocol = useCallback(
    (protocol: string) => {
      form.send({ type: 'PROTOCOL_CHANGE', value: protocol } as any);
    },
    [form]
  );

  /**
   * Confirm a re-entered password (edit mode) and submit in the same
   * synchronous transition, avoiding a setTimeout-based race between the
   * UPDATE and SUBMIT events.
   */
  const confirmPasswordAndSubmit = useCallback(
    (password: string) => {
      form.send({ type: 'CONFIRM_PASSWORD_AND_SUBMIT', value: password } as any);
    },
    [form]
  );

  return {
    form,
    server: loadedServer,
    isCreate,
    applyTemplate,
    changeProtocol,
    confirmPasswordAndSubmit,
  };
}
