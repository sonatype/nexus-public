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
import { useForm, type FieldProps } from '../../../../../../interface/form';
import { createOAuth2FormMachine } from './oauth2FormMachine';
import { OAuth2Config } from './types';

/**
 * Presentation-facing API for the OAuth2 settings page.
 *
 * Note: OAuth2 intentionally has NO success toast — successful save is signaled
 * only by the form returning to a pristine (disabled-save) state, matching the
 * legacy page.
 */
export interface UseOAuth2FormResult {
  data: OAuth2Config;
  isLoading: boolean;
  isSaving: boolean;
  isPristine: boolean;
  hasValidationErrors: boolean;
  loadError: string | null;
  saveError: string | null;
  /** Props for binding a text/password/textarea field to the machine. */
  field: (name: keyof OAuth2Config) => FieldProps;
  /** Submit the form (validates, then saves when valid). */
  submit: () => void;
  /** Discard changes, resetting to the last-loaded values. */
  reset: () => void;
}

/**
 * Integration hook wiring the OAuth2 form machine to React. The machine owns
 * load/save/validation; this hook only projects state and exposes commands.
 */
export function useOAuth2Form(): UseOAuth2FormResult {
  const machine = useMemo(() => createOAuth2FormMachine(), []);
  const form = useForm<OAuth2Config>(machine);

  return {
    data: form.data,
    isLoading: form.isLoading,
    isSaving: form.isSaving,
    isPristine: form.isPristine,
    hasValidationErrors: form.hasValidationErrors,
    loadError: form.loadError,
    saveError: form.saveError,
    field: (name) => form.field(name as string),
    submit: form.submit,
    reset: form.reset,
  };
}

export default useOAuth2Form;
