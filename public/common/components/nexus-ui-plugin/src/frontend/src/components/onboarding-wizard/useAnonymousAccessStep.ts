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

import { useCallback, useEffect, useState } from 'react';

import { useAnonymousApi } from '../preview/pages/settings/security/anonymous/useAnonymousApi';
import { useWizard } from './useWizard';

const ANONYMOUS_USER_ID = 'anonymous';
const ANONYMOUS_REALM_NAME = 'NexusAuthorizingRealm';
const DEFAULT_SECURE_DISABLED = false;

export interface UseAnonymousAccessStepResult {
  enabled: boolean;
  onSelect: (value: boolean) => void;
  onFormSubmit: (event: React.FormEvent) => void;
}

export function useAnonymousAccessStep(): UseAnonymousAccessStepResult {
  const { registerStep, submit } = useWizard();
  const { fetchSettings, saveSettings } = useAnonymousApi();
  const [enabled, setEnabled] = useState<boolean>(DEFAULT_SECURE_DISABLED);

  useEffect(() => {
    let cancelled = false;
    fetchSettings()
      .then((settings) => {
        if (!cancelled) {
          setEnabled(settings.enabled);
        }
      })
      .catch((error) => {
        console.warn('[AnonymousAccessStep] Failed to fetch anonymous settings', error);
      });
    return () => {
      cancelled = true;
    };
  }, [fetchSettings]);

  const onSelect = useCallback((value: boolean) => {
    setEnabled(value);
  }, []);

  const onSubmit = useCallback(async () => {
    await saveSettings({
      userId: ANONYMOUS_USER_ID,
      realmName: ANONYMOUS_REALM_NAME,
      enabled,
    });
  }, [saveSettings, enabled]);

  useEffect(() => {
    registerStep({ valid: true, onSubmit });
  }, [registerStep, onSubmit]);

  const onFormSubmit = useCallback(
    (event: React.FormEvent) => {
      event.preventDefault();
      submit();
    },
    [submit]
  );

  return { enabled, onSelect, onFormSubmit };
}
