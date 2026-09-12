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
import { ExternalLink } from 'lucide-react';

import { useWizard } from '../../useWizard';
import UIStrings from '../../UIStrings';

import './CommunityDiscoverStep.scss';

/**
 * Generates the discover link with UTM parameters for analytics tracking.
 */
function getDiscoverLink(): string {
  const params = {
    utm_medium: 'product',
    utm_source: 'nexus_repo_community',
    utm_campaign: 'repo_community_usage',
  };

  return `https://links.sonatype.com/products/nxrm3/discover-community-edition?${new URLSearchParams(params).toString()}`;
}

/**
 * CommunityDiscoverStep displays information about Community Edition features.
 * Display-only — always valid; the wizard chrome's Next button advances.
 */
export default function CommunityDiscoverStep(): JSX.Element {
  const { registerStep, actionButtonRef } = useWizard();
  const { TITLE, SUBTITLE, LEARN_MORE_LABEL, BENEFITS_LIST } =
    UIStrings.ONBOARDING_WIZARD.COMMUNITY_DISCOVER;

  // Register step as always valid on mount. onSubmit is a stable no-op:
  // the wizard chrome dispatches STEP_ADVANCED once it resolves, and depending
  // on the wizard's own submit callback would create a re-render loop
  // (submit's identity changes each time we register).
  useEffect(() => {
    registerStep({ valid: true, onSubmit: () => {} });
  }, [registerStep]);

  // No form fields — focus the wizard's action button so keyboard users can
  // advance without an extra Tab.
  useEffect(() => {
    actionButtonRef.current?.focus();
  }, [actionButtonRef]);

  return (
    <div className="community-discover-step" data-testid="community-discover-step">
      <h2 className="community-discover-step__title">{TITLE}</h2>
      <p className="community-discover-step__subtitle">{SUBTITLE}</p>
      <ul className="community-discover-step__benefits-list">
        {BENEFITS_LIST.map((benefit) => (
          <li key={benefit.label} className="community-discover-step__benefit-item">
            <strong className="community-discover-step__benefit-label">{benefit.label}</strong>
            {' — '}
            <span className="community-discover-step__benefit-description">
              {benefit.description}
            </span>
          </li>
        ))}
      </ul>
      <a
        href={getDiscoverLink()}
        target="_blank"
        rel="noopener noreferrer"
        className="community-discover-step__learn-more-link"
        data-testid="community-discover-step__learn-more-link"
      >
        {LEARN_MORE_LABEL}
        <ExternalLink size={14} aria-hidden="true" />
      </a>
    </div>
  );
}
