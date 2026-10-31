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

import { useCallback, useEffect, useMemo, useRef } from 'react';
import { useMachine } from '@xstate/react';
import { waitFor } from 'xstate/lib/waitFor';
import {
  createUploadFormMachine,
  createInitialFormData,
  hasAnyValidationErrors,
  isFormDataDirty,
} from './uploadFormMachine';

import type {
  UseUploadFormResult,
  UploadComponentField,
  UploadFieldDefinition,
  UploadDefinitionExtended,
} from '../upload.types';
import { UPLOAD_FORM_STRINGS } from '../upload.types';

interface UseUploadFormProps {
  repositoryName: string;
  componentFields: UploadComponentField[];
  assetFields: UploadFieldDefinition[];
  multipleUpload: boolean;
  regexMap?: UploadDefinitionExtended['regexMap'] | null;
  disabledFields?: Set<string>;
}

/**
 * Hook to manage upload form state, validation, and submission.
 *
 * Features:
 * - Dynamic form data based on upload definition
 * - Multiple asset support
 * - Validation (required fields, unique assets)
 * - Regex-based field auto-population from filename
 * - Form submission with multipart/form-data
 *
 * @param props - Configuration from useUploadDefinition
 * @returns Form state and actions
 */
export function useUploadForm({
  repositoryName,
  componentFields,
  assetFields,
  multipleUpload,
  regexMap,
  disabledFields = new Set(),
}: UseUploadFormProps): UseUploadFormResult {
  // Build the machine from the current fields. NOTE: @xstate/react's useMachine
  // captures the machine on first mount only (via useConstant) — a new machine
  // reference on a later render is ignored. So this memo does NOT reset a still-
  // mounted form when componentFields/assetFields change; form reset on a
  // repository/field change relies on the caller REMOUNTING this hook. That is
  // currently guaranteed by UploadFormContainer, which gates <UploadForm> behind
  // useUploadDefinition's loading flag (flips true on repositoryName change),
  // unmounting the form before the new fields arrive. The deps only affect the
  // machine built on the next (re)mount.
  const machine = useMemo(
    () =>
      createUploadFormMachine({
        repositoryName,
        componentFields,
        assetFields,
        multipleUpload,
        regexMap,
        disabledFields,
      }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [componentFields, assetFields]
  );

  const [state, send, service] = useMachine(machine);

  // Sync mutable config without resetting form data. disabledFields is commonly
  // passed as a new Set instance per render, so compare contents rather than
  // relying on reference identity to avoid a send-triggered re-render loop.
  const syncedConfigRef = useRef({ repositoryName, multipleUpload, regexMap, disabledFields });
  useEffect(() => {
    const prev = syncedConfigRef.current;
    const disabledFieldsChanged =
      prev.disabledFields !== disabledFields &&
      (prev.disabledFields.size !== disabledFields.size ||
        [...disabledFields].some((field) => !prev.disabledFields.has(field)));

    if (
      prev.repositoryName !== repositoryName ||
      prev.multipleUpload !== multipleUpload ||
      prev.regexMap !== regexMap ||
      disabledFieldsChanged
    ) {
      syncedConfigRef.current = { repositoryName, multipleUpload, regexMap, disabledFields };
      send({
        type: 'SYNC_CONFIG',
        config: { repositoryName, multipleUpload, regexMap, disabledFields },
      });
    }
  }, [send, repositoryName, multipleUpload, regexMap, disabledFields]);

  const { formData, validationErrors, hasValidated, touchedFields } = state.context;

  // VALIDATE only triggers a synchronous assign (no invoke/always transition), so
  // the snapshot immediately after send() reflects the recomputed validationErrors.
  const validate = useCallback((): boolean => {
    send({ type: 'VALIDATE' });
    return !hasAnyValidationErrors(service.getSnapshot().context.validationErrors);
  }, [send, service]);

  const submit = useCallback(async () => {
    send({ type: 'SUBMIT' });
    // SUBMIT -> validating is a direct transition, and validating's `always` guard
    // resolves synchronously (XState v4 processes eventless transitions immediately),
    // so by the time send() returns the machine has already settled in either
    // 'editing' (validation failed) or 'submitting' (invoke started).
    const snap = service.getSnapshot();
    if (!snap.matches('submitting')) {
      return { success: false, error: 'Please fix validation errors' };
    }
    try {
      const settled = await waitFor(
        service,
        (s) =>
          s.matches('editing') &&
          (s.context.submitResult !== null || s.context.submitError !== null),
        { timeout: 30_000 }
      );
      const { submitResult, submitError } = settled.context;
      return submitResult
        ? { success: true, componentName: submitResult.componentName }
        : {
            success: false,
            error:
              submitError ?? `${UPLOAD_FORM_STRINGS.uploadError}: Upload failed`,
          };
    } catch {
      return { success: false, error: `${UPLOAD_FORM_STRINGS.uploadError}: Upload timed out` };
    }
  }, [send, service]);

  const isSubmitting = state.matches('submitting');
  const isValid = useMemo(
    () =>
      hasValidated ? !hasAnyValidationErrors(validationErrors) : true,
    [hasValidated, validationErrors]
  );

  const initialRef = useRef(state.context.initialFormData);
  const isDirty = useMemo(
    () => isFormDataDirty(formData, initialRef.current),
    [formData]
  );

  return {
    formData,
    validationErrors,
    isSubmitting,
    isValid,
    isDirty,
    touchedFields,
    setAssetFile: (index, file) => send({ type: 'SET_ASSET_FILE', index, file }),
    setAssetField: (index, field, value) =>
      send({ type: 'SET_ASSET_FIELD', index, field, value }),
    addAsset: () => send({ type: 'ADD_ASSET' }),
    removeAsset: (index) => send({ type: 'REMOVE_ASSET', index }),
    setComponentField: (field, value) =>
      send({ type: 'SET_COMPONENT_FIELD', field, value }),
    blurComponentField: (field) => send({ type: 'BLUR_COMPONENT_FIELD', field }),
    blurAssetField: (index, field) =>
      send({ type: 'BLUR_ASSET_FIELD', index, field }),
    validate,
    submit,
    reset: () => send({ type: 'RESET' }),
  };
}

export default useUploadForm;

// Re-export for backward compatibility (tests import createInitialFormData)
export { createInitialFormData };
