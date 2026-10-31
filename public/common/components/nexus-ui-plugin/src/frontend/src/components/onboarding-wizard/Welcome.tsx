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

import React, { useEffect } from 'react';

import ExtJS from '../../interface/ExtJS';
import { useWizard } from './useWizard';
import UIStrings from './UIStrings';

import './Welcome.scss';

function getWelcomeLogoUrl(): string {
  try {
    return ExtJS.urlOf('static/rapture/resources/favicon.svg');
  } catch {
    return '/static/rapture/resources/favicon.svg';
  }
}

export default function Welcome(): JSX.Element {
  const { registerStep, getStarted } = useWizard();
  const { LOGO_ALT, TITLE, DESCRIPTION } = UIStrings.ONBOARDING_WIZARD.WELCOME;

  useEffect(() => {
    registerStep({ valid: true, onSubmit: () => getStarted() });
  }, [registerStep, getStarted]);

  return (
    <div className="onboarding-wizard__welcome">
      <img
        src={getWelcomeLogoUrl()}
        alt={LOGO_ALT}
        className="onboarding-wizard__welcome-logo"
        width={64}
        height={64}
        decoding="async"
      />
      <h1 className="onboarding-wizard__welcome-title">{TITLE}</h1>
      <p className="onboarding-wizard__welcome-description">{DESCRIPTION}</p>
    </div>
  );
}
