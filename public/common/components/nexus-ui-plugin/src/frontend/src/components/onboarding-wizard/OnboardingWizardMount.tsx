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
import Wizard from './Wizard';
import { WizardProvider } from './WizardContext';
import type { OnboardingStep } from './types';

async function fetchOnboardingSteps(): Promise<OnboardingStep[]> {
  return restClient.get<OnboardingStep[]>('/service/rest/internal/ui/onboarding');
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
    ExtJS.state()?.setValue?.('onboarding.required', false);
  }, []);

  if (!featureEnabled || !onboardingRequired || !isAdmin) {
    return null;
  }

  return (
    <WizardProvider
      fetchSteps={fetchOnboardingSteps}
      onDone={handleDone}
    >
      <Wizard />
    </WizardProvider>
  );
}
