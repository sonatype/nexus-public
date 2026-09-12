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

import React, { useCallback } from 'react';

import ExtJS from '../../interface/ExtJS';
import { restClient } from '../../interface/api/rest-client';
import UIStrings from './UIStrings';
import Wizard from './Wizard';
import { WizardProvider } from './WizardContext';
import { ONBOARDING_STILL_REQUIRED_NAME } from './wizardMachine';
import type { OnboardingStep } from './types';

// Test-only override so the Sonatype Test Hub can supply a fake step list
// instead of the real REST response, letting developers walk through step
// UIs regardless of the instance's actual onboarding completion state.
const MOCK_STEPS_STORAGE_KEY = 'SONATYPE_TEST_ONBOARDING_STEPS';

function readMockOnboardingSteps(): OnboardingStep[] | null {
  try {
    const raw = localStorage.getItem(MOCK_STEPS_STORAGE_KEY);
    if (!raw) return null;
    const types = JSON.parse(raw);
    if (!Array.isArray(types) || types.length === 0) return null;
    return types.map((type: string) => ({ type }));
  } catch {
    return null;
  }
}

async function fetchOnboardingSteps(): Promise<OnboardingStep[]> {
  const mockSteps = readMockOnboardingSteps();
  if (mockSteps) {
    return mockSteps;
  }
  return restClient.get<OnboardingStep[]>('/service/rest/internal/ui/onboarding');
}

async function verifyOnboardingComplete(): Promise<void> {
  const remainingSteps = await restClient.get<OnboardingStep[]>(
    '/service/rest/internal/ui/onboarding'
  );
  if (remainingSteps.length > 0) {
    const err = new Error('Onboarding still required');
    // Signal to the wizard machine that this is a "not done yet" state rather
    // than a network/transport failure, so the UI shows the correct message.
    err.name = ONBOARDING_STILL_REQUIRED_NAME;
    throw err;
  }
}

export default function OnboardingWizardMount(): React.ReactElement | null {
  const featureEnabled = ExtJS.useState(
    () => ExtJS.state()?.getValue?.('nexus.react.onboarding.enabled', false) ?? false
  );

  const onboardingRequired = ExtJS.useState(
    () => ExtJS.state()?.getValue?.('onboarding.required', false) ?? false
  );

  const user = ExtJS.useUser();
  const isAdmin = !!user?.administrator;

  const handleDone = useCallback(() => {
    let wasDismissed: string | null = null;
    try {
      wasDismissed = sessionStorage.getItem('onboarding.dismissed');
      if (wasDismissed) {
        sessionStorage.removeItem('onboarding.dismissed');
      }
    } catch {
      // sessionStorage blocked (private browsing / iframe policy). Fall through;
      // we simply won't surface the "not completed" message, which is acceptable.
    }
    if (wasDismissed) {
      ExtJS.showErrorMessage(UIStrings.ONBOARDING_WIZARD.DISMISS_TOAST.MESSAGE);
    }
    ExtJS.state()?.setValue?.('onboarding.required', false);
  }, []);

  if (!featureEnabled || !onboardingRequired || !isAdmin) {
    return null;
  }

  return (
    <WizardProvider
      fetchSteps={fetchOnboardingSteps}
      verify={verifyOnboardingComplete}
      onDone={handleDone}
    >
      <Wizard />
    </WizardProvider>
  );
}
