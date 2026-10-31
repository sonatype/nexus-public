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
import { ExtJS } from '../../../../../../interface/ExtJS';
import { useForm } from '../../../../../../interface/form';
import { useToast } from '../../../../shared';
import { createSamlFormMachine, DEFAULT_CONFIG, toSamlPayload, SamlFormContext } from './samlFormMachine';
import {
  fetchSamlConfiguration,
  saveSamlConfiguration,
  deleteSamlConfiguration,
  getSamlMetadataUrl,
} from './samlApi';
import { SamlConfiguration } from './types';

/**
 * Returns the default SAML configuration with a computed default Entity ID
 * (absolute metadata URL, preserving the Nexus context path). Lives in the hook
 * (ViewModel) because it depends on the ExtJS/browser environment; the machine
 * stays environment-free.
 */
function getDefaultConfigWithEntityId(): SamlConfiguration {
  const relativeUrl = ExtJS.urlOf('/service/rest/v1/security/saml/metadata');
  const absoluteUrl = new URL(relativeUrl, window.location.href).toString();
  return { ...DEFAULT_CONFIG, entityId: absoluteUrl };
}

export interface UseSamlFormResult {
  config: SamlConfiguration;
  isConfigured: boolean;
  /** Initial configuration load in progress (drives the full-page spinner). */
  isInitialLoading: boolean;
  /** A save or delete operation is in progress. */
  isBusy: boolean;
  isPristine: boolean;
  isConfirmingDelete: boolean;
  /** Page-level error banner (load/save). Delete errors are shown in the dialog. */
  error: string | null;
  /** Delete error, surfaced inside the confirmation dialog (not the page banner). */
  deleteError: string | null;
  fieldError: (name: keyof SamlConfiguration) => string | undefined;
  handleFieldChange: (name: keyof SamlConfiguration, value: string | boolean | null) => void;
  handleSave: () => void;
  handleDiscard: () => void;
  requestDelete: () => void;
  confirmDelete: () => void;
  cancelDelete: () => void;
  clearError: () => void;
  getAbsoluteMetadataUrl: () => string;
  copyMetadataUrl: () => void;
}

/**
 * Integration hook wiring the SAML form machine to React. Injects the API
 * services (load with 404→unconfigured handling, save with trimming/tri-state
 * conversion, delete returning the reset default) plus success toasts.
 */
export function useSamlForm(): UseSamlFormResult {
  const toast = useToast();
  const machine = useMemo(() => createSamlFormMachine(), []);

  const form = useForm<SamlConfiguration>(machine, {
    services: {
      load: async () => {
        // A 404 (no configuration yet) resolves as unconfigured; any real error
        // propagates so the machine's loading.onError path surfaces it while
        // keeping the form editable.
        const cfg = await fetchSamlConfiguration();
        if (cfg) {
          return { data: cfg, isConfigured: true };
        }
        return { data: getDefaultConfigWithEntityId(), isConfigured: false };
      },
      save: async (ctx) => {
        await saveSamlConfiguration(toSamlPayload(ctx.data));
        toast.success('SAML configuration saved successfully');
      },
      delete: async () => {
        await deleteSamlConfiguration();
        toast.success('SAML configuration deleted successfully');
        return getDefaultConfigWithEntityId();
      },
    },
  });

  const ctx = form.state.context as SamlFormContext;
  const touched = form.touched as Record<string, boolean>;

  const getAbsoluteMetadataUrl = (): string =>
    new URL(ExtJS.urlOf(getSamlMetadataUrl()), window.location.href).toString();

  return {
    config: form.data,
    isConfigured: ctx.isConfigured,
    isInitialLoading: form.isLoading,
    isBusy: form.isSaving || form.isDeleting,
    isPristine: form.isPristine,
    isConfirmingDelete: form.isConfirmingDelete || form.isDeleting,
    // Delete errors are surfaced inside the confirmation dialog, so they are
    // intentionally excluded from the page-level banner.
    error: form.loadError ?? form.saveError,
    deleteError: form.deleteError,
    fieldError: (name) => (touched[name as string] ? form.validationErrors[name as string] ?? undefined : undefined),
    handleFieldChange: (name, value) => form.send({ type: 'UPDATE', name: name as string, value }),
    handleSave: form.submit,
    handleDiscard: form.reset,
    requestDelete: form.requestDelete,
    confirmDelete: form.confirmDelete,
    cancelDelete: form.cancelDelete,
    // CLEAR_ERROR is a machine-specific event handled via the form machine's
    // `on` config; it is not part of the base FormEvent union, hence the cast.
    clearError: () => form.send({ type: 'CLEAR_ERROR' } as never),
    getAbsoluteMetadataUrl,
    copyMetadataUrl: () => {
      navigator.clipboard.writeText(getAbsoluteMetadataUrl());
      toast.success('Metadata URL copied to clipboard');
    },
  };
}

export default useSamlForm;
