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

import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';

import CommunityDiscoverStep from '../CommunityDiscoverStep';
import { useWizard } from '../../../useWizard';

jest.mock('../../../useWizard');

const mockedUseWizard = jest.mocked(useWizard);

describe('CommunityDiscoverStep', () => {
  const registerStep = jest.fn();
  const submit = jest.fn();

  const mostRecentRegistration = () => {
    const lastCall = registerStep.mock.calls[registerStep.mock.calls.length - 1];
    return lastCall[0] as { valid: boolean; onSubmit: () => Promise<void> };
  };

  beforeEach(() => {
    jest.clearAllMocks();

    mockedUseWizard.mockReturnValue({
      state: 'stepReady',
      steps: [],
      currentIndex: 0,
      currentStep: null,
      errorMessage: null,
      isCurrentStepValid: false,
      registerStep,
      getStarted: jest.fn(),
      submit,
      skip: jest.fn(),
      finish: jest.fn(),
      actionButtonRef: { current: null },
    });
  });

  it('shouldRenderWithoutCrashing', () => {
    render(<CommunityDiscoverStep />);
    expect(screen.getByTestId('community-discover-step')).toBeInTheDocument();
  });

  it('shouldRenderTitleAndSubtitle', () => {
    render(<CommunityDiscoverStep />);

    expect(
      screen.getByText('Welcome to Nexus Repository Community Edition')
    ).toBeInTheDocument();
    expect(
      screen.getByText('Manage, store, and distribute software components.')
    ).toBeInTheDocument();
  });

  it('shouldRenderFourBenefitsAsLabelAndDescriptionPairs', () => {
    render(<CommunityDiscoverStep />);

    const benefits: Array<[string, string]> = [
      ['Repository formats', 'Support for Docker, Maven, npm, PyPI, NuGet, Helm, and more.'],
      ['Search and browse components', 'Find components across your repositories.'],
      [
        'Security and access management',
        'Integrate with LDAP and configure role-based access.',
      ],
      ['REST API and automation', 'Automate repository management and CI/CD workflows.'],
    ];

    benefits.forEach(([label, description]) => {
      expect(screen.getByText(label)).toBeInTheDocument();
      expect(screen.getByText(description)).toBeInTheDocument();
    });
  });

  it('shouldRenderLearnMoreLinkBelowBenefitsWithUtmParameters', () => {
    render(<CommunityDiscoverStep />);

    const learnMoreLink = screen.getByTestId('community-discover-step__learn-more-link');
    expect(learnMoreLink).toBeInTheDocument();
    expect(learnMoreLink).toHaveTextContent('Learn More');
    expect(learnMoreLink).toHaveAttribute('target', '_blank');
    expect(learnMoreLink).toHaveAttribute('rel', 'noopener noreferrer');

    const href = learnMoreLink.getAttribute('href');
    expect(href).toContain('https://links.sonatype.com/products/nxrm3/discover-community-edition');
    expect(href).toContain('utm_medium=product');
    expect(href).toContain('utm_source=nexus_repo_community');
    expect(href).toContain('utm_campaign=repo_community_usage');

    // AC #4: Learn More link is positioned below the benefits list in DOM order.
    const benefitsList = screen
      .getByTestId('community-discover-step')
      .querySelector('.community-discover-step__benefits-list');
    expect(benefitsList).not.toBeNull();
    // eslint-disable-next-line no-bitwise
    expect(
      benefitsList!.compareDocumentPosition(learnMoreLink) &
        Node.DOCUMENT_POSITION_FOLLOWING
    ).toBeTruthy();
  });

  it('shouldNotRenderIllustrationImage', () => {
    render(<CommunityDiscoverStep />);
    expect(screen.queryByTestId('community-discover-step__image')).not.toBeInTheDocument();
  });

  it('shouldRegisterAsValidOnMount', async () => {
    render(<CommunityDiscoverStep />);

    await waitFor(() => expect(registerStep).toHaveBeenCalled());

    const registration = mostRecentRegistration();
    expect(registration.valid).toBe(true);
    // onSubmit must be a stable no-op, not the wizard's own submit —
    // passing submit through re-triggers the registration effect and causes a
    // maximum-update-depth render loop.
    expect(typeof registration.onSubmit).toBe('function');
    expect(registration.onSubmit).not.toBe(submit);
    expect(registration.onSubmit()).toBeUndefined();
    expect(submit).not.toHaveBeenCalled();
  });

  it('shouldRegisterOnceAndNotLoopOnRerender', () => {
    const { rerender } = render(<CommunityDiscoverStep />);
    rerender(<CommunityDiscoverStep />);
    rerender(<CommunityDiscoverStep />);

    // Effect must run exactly once across re-renders. Any depend
    // wizard's submit callback would fire the effect on every render because
    // WizardContext memoizes submit against stepRegistration.
    expect(registerStep).toHaveBeenCalledTimes(1);
  });

  it('shouldFocusActionButtonOnMount', async () => {
    // AC #7: focus is set to the Next button when the step loads.
    const button = document.createElement('button');
    const focusSpy = jest.spyOn(button, 'focus');

    mockedUseWizard.mockReturnValue({
      state: 'stepReady',
      steps: [],
      currentIndex: 0,
      currentStep: null,
      errorMessage: null,
      isCurrentStepValid: false,
      registerStep,
      getStarted: jest.fn(),
      submit,
      skip: jest.fn(),
      finish: jest.fn(),
      actionButtonRef: { current: button },
    });

    render(<CommunityDiscoverStep />);

    await waitFor(() => expect(focusSpy).toHaveBeenCalledTimes(1));
  });
});
