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

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useMachine } from '@xstate/react';
import { ExtJS } from '../../../../../../interface/ExtJS';
import { useForm, type FormContext } from '../../../../../../interface/form';
import { useToast } from '../../../../shared';
import { createIqServerFormMachine, saveIqSettings, toUpdatePayload } from './iqServerFormMachine';
import { createIqConnectionMachine, fetchCapabilities } from './iqConnectionMachine';
import { isValidUrl } from './iqServerUtils';
import { validateProperties } from './propertyList';
import { IqServerFormData, IqProperty, PASSWORD_PLACEHOLDER } from './types';

export function useIqServerForm() {
  const toast = useToast();
  const canUpdate = ExtJS.checkPermission('nexus:settings:update');
  const isCloud = ExtJS.state?.()?.getValue?.('isCloud', false) ?? false;

  const formMachine = useMemo(() => createIqServerFormMachine(), []);
  const connectionMachine = useMemo(() => createIqConnectionMachine(), []);

  const form = useForm<IqServerFormData>(formMachine, {
    services: {
      save: async (ctx: FormContext<IqServerFormData>) => {
        const saved = await saveIqSettings(ctx.data);
        toast.success('IQ Server configuration saved successfully');
        return saved;
      },
    },
  });

  const [connState, connSend] = useMachine(connectionMachine);
  const connectionStatus = (['testing', 'connected', 'failed'] as const).find((s) => connState.matches(s)) ?? 'idle';

  // Load initial capabilities and auto-test once, after the form finishes loading.
  // `form.data` in deps is safe: `bootstrapped.current = true` is set synchronously before any
  // async work, so subsequent effect invocations hit the early-return and are no-ops.
  const bootstrapped = useRef(false);
  useEffect(() => {
    if (form.isLoading || bootstrapped.current) return;
    bootstrapped.current = true;
    fetchCapabilities().then((capabilities) => connSend({ type: 'SET_CAPABILITIES', capabilities }));
    const s = form.data;
    if (s.enabled && s.url?.trim()) {
      connSend({ type: 'AUTO_TEST', settings: toUpdatePayload(s) });
    }
  }, [form.isLoading, form.data, connSend]);

  const handleFieldChange = useCallback(
    (name: string, value: unknown) => {
      form.send({ type: 'UPDATE', name, value } as never);
      connSend({ type: 'RESET_CONNECTION' });
    },
    [form, connSend],
  );

  // URL change has extra side effects (mirrors today): clear a placeholder password when
  // the URL actually changes, and disable the trust-store toggle for non-https URLs.
  const handleUrlChange = useCallback(
    (value: string) => {
      form.send({ type: 'UPDATE', name: 'url', value } as never);
      if (form.data.password === PASSWORD_PLACEHOLDER && form.data.url !== value) {
        form.send({ type: 'UPDATE', name: 'password', value: '' } as never);
      }
      if (!value?.startsWith('https://')) {
        form.send({ type: 'UPDATE', name: 'useTrustStoreForUrl', value: false } as never);
      }
      connSend({ type: 'RESET_CONNECTION' });
    },
    [form, connSend],
  );

  const verify = useCallback(
    () => connSend({ type: 'TEST', settings: toUpdatePayload(form.data) }),
    [connSend, form.data],
  );

  // On successful save, tell the connection machine to show the "Saved" message.
  // Relies on isPristine/saveError flipping in the same onSaveSuccess/setSaveError action as
  // isSaving, so they're guaranteed to land in the same render as the isSaving->false transition.
  const prevSaving = useRef(false);
  useEffect(() => {
    if (prevSaving.current && !form.isSaving && form.isPristine && !form.saveError) {
      connSend({ type: 'SAVED' });
    }
    prevSaving.current = form.isSaving;
  }, [form.isSaving, form.isPristine, form.saveError, connSend]);

  const pristineSettings = form.state.context.pristineData;
  const canOpenDashboard = pristineSettings.enabled && isValidUrl(pristineSettings.url);

  // Local UI-only state: "show all row errors" after a blocked save, a clear-all confirm
  // banner, and a one-time dismiss for the dropped-lines-on-load warning.
  const [showAllValidation, setShowAllValidation] = useState(false);
  const [showClearAllConfirm, setShowClearAllConfirm] = useState(false);
  const [dismissedDroppedWarning, setDismissedDroppedWarning] = useState(false);

  const properties = form.data.properties;

  const { validations: propertyValidations, hasBlockingErrors: hasPropertyErrors } = useMemo(
    () => validateProperties(properties),
    [properties],
  );

  const setProperties = useCallback(
    (next: IqProperty[]) => form.send({ type: 'UPDATE', name: 'properties', value: next } as never),
    [form],
  );

  const requestClearAllProperties = useCallback(() => setShowClearAllConfirm(true), []);
  const cancelClearAllProperties = useCallback(() => setShowClearAllConfirm(false), []);
  const confirmClearAllProperties = useCallback(() => {
    setProperties([]);
    setShowClearAllConfirm(false);
  }, [setProperties]);

  const dismissPropertiesDroppedWarning = useCallback(() => setDismissedDroppedWarning(true), []);
  const propertiesDroppedLineCount = form.data.propertiesDroppedLineCount;
  const showPropertiesDroppedWarning = propertiesDroppedLineCount > 0 && !dismissedDroppedWarning;

  const submit = useCallback(() => {
    if (hasPropertyErrors) {
      setShowAllValidation(true);
      return;
    }
    setShowAllValidation(false);
    form.submit();
  }, [form, hasPropertyErrors]);

  const reset = useCallback(() => {
    form.reset();
    setShowAllValidation(false);
    setShowClearAllConfirm(false);
  }, [form]);

  const clearSaveError = useCallback(() => form.send({ type: 'CLEAR_SAVE_ERROR' } as never), [form]);

  return {
    ...form,
    handleFieldChange,
    verify,
    handleUrlChange,
    connectionStatus,
    connectionMessage: connState.context.message,
    verificationResult: connState.context.verificationResult,
    capabilities: connState.context.capabilities,
    isCloud,
    canUpdate,
    canOpenDashboard,
    dashboardUrl: pristineSettings.url,
    submit,
    reset,
    clearSaveError,
    properties,
    setProperties,
    propertyValidations,
    hasPropertyErrors,
    showAllValidation,
    showClearAllConfirm,
    requestClearAllProperties,
    confirmClearAllProperties,
    cancelClearAllProperties,
    propertiesDroppedLineCount,
    showPropertiesDroppedWarning,
    dismissPropertiesDroppedWarning,
  };
}
