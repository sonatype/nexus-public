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
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';

import AnonymousAccessStep from '../AnonymousAccessStep';
import { useWizard } from '../useWizard';
import { useAnonymousApi } from '../../preview/pages/settings/security/anonymous/useAnonymousApi';

jest.mock('../useWizard');
jest.mock('../../preview/pages/settings/security/anonymous/useAnonymousApi');

const mockedUseWizard = jest.mocked(useWizard);
const mockedUseAnonymousApi = jest.mocked(useAnonymousApi);

describe('AnonymousAccessStep', () => {
  const registerStep = jest.fn();
  const submit = jest.fn();
  let fetchSettings: jest.Mock;
  let saveSettings: jest.Mock;

  const mostRecentRegistration = () => {
    const lastCall = registerStep.mock.calls[registerStep.mock.calls.length - 1];
    return lastCall[0] as { valid: boolean; onSubmit: () => Promise<void> };
  };

  beforeEach(() => {
    jest.clearAllMocks();

    fetchSettings = jest.fn().mockResolvedValue({
      enabled: false,
      userId: 'anonymous',
      realmName: 'NexusAuthorizingRealm',
    });
    saveSettings = jest.fn().mockResolvedValue(undefined);

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
    });

    mockedUseAnonymousApi.mockReturnValue({
      loading: false,
      error: null,
      setError: jest.fn(),
      fetchRealmTypes: jest.fn(),
      fetchSettings,
      saveSettings,
    });
  });

  it('shouldRenderBothRadioButtons', async () => {
    render(<AnonymousAccessStep />);
    await waitFor(() => expect(fetchSettings).toHaveBeenCalled());

    expect(screen.getByTestId('onboarding-wizard__anonymous-access-enable')).toBeInTheDocument();
    expect(screen.getByTestId('onboarding-wizard__anonymous-access-disable')).toBeInTheDocument();
    expect(screen.getByLabelText(/Enable anonymous access/)).toBeInTheDocument();
    expect(screen.getByLabelText(/Disable anonymous access/)).toBeInTheDocument();
  });

  it('shouldRenderOptionDescriptionsInsideCards', async () => {
    // Card-style options carry a short secondary description under each title
    // so users can grasp the choice without reading the full paragraph above.
    render(<AnonymousAccessStep />);
    await waitFor(() => expect(fetchSettings).toHaveBeenCalled());

    expect(
      screen.getByText('Allow anonymous users to read from repositories')
    ).toBeInTheDocument();
    expect(
      screen.getByText('Require authentication for all repository access')
    ).toBeInTheDocument();
  });

  it('shouldMarkSelectedCardWithDataSelectedAttribute', async () => {
    // The selected option's label carries data-selected="true" so the card can
    // render its highlighted (green) border via CSS without extra JS.
    render(<AnonymousAccessStep />);
    await waitFor(() => expect(fetchSettings).toHaveBeenCalled());

    const enableLabel = screen
      .getByTestId('onboarding-wizard__anonymous-access-enable')
      .closest('label');
    const disableLabel = screen
      .getByTestId('onboarding-wizard__anonymous-access-disable')
      .closest('label');

    expect(disableLabel).toHaveAttribute('data-selected', 'true');
    expect(enableLabel).toHaveAttribute('data-selected', 'false');

    fireEvent.click(screen.getByTestId('onboarding-wizard__anonymous-access-enable'));

    await waitFor(() => expect(enableLabel).toHaveAttribute('data-selected', 'true'));
    expect(disableLabel).toHaveAttribute('data-selected', 'false');
  });

  it('shouldRenderTitleAndDescription', async () => {
    render(<AnonymousAccessStep />);
    await waitFor(() => expect(fetchSettings).toHaveBeenCalled());

    expect(screen.getByText('Configure Anonymous Access')).toBeInTheDocument();
    expect(
      screen.getByText(/consider the security implications for your organization/i)
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /More information/i })).toHaveAttribute(
      'href',
      'https://links.sonatype.com/products/nexus/anonymous-access/docs'
    );
  });

  it('shouldRenderDescriptionInsideCalloutWithDefinitionList', async () => {
    // Hybrid — the description block is wrapped in a Radix Callout (Option
    // C container) with a top-left info icon, and internally uses a
    // semantic <dl>/<dt>/<dd> (Option A structure). Each definition reads
    // as a complete sentence so the term and explanation stand on their
    // own without prose-level connective words.
    render(<AnonymousAccessStep />);
    await waitFor(() => expect(fetchSettings).toHaveBeenCalled());

    const callout = screen.getByTestId('onboarding-wizard__anonymous-access-description-callout');
    expect(callout).toHaveAttribute('id', 'onboarding-wizard__anonymous-access-description');

    const list = screen.getByTestId('onboarding-wizard__anonymous-access-description-list');
    expect(list.tagName).toBe('DL');
    expect(callout.contains(list)).toBe(true);

    const terms = list.querySelectorAll('dt');
    const definitions = list.querySelectorAll('dd');
    expect(terms).toHaveLength(2);
    expect(definitions).toHaveLength(2);
    expect(terms[0]).toHaveTextContent('Enable anonymous access');
    expect(terms[1]).toHaveTextContent('Disable anonymous access');
    expect(definitions[0].textContent).toMatch(/^By default, users can search/);
    expect(definitions[0].textContent).toMatch(
      /consider the security implications for your organization/i
    );
    expect(definitions[1].textContent).toMatch(/^Choose with care because it/);
    expect(definitions[1].textContent).toMatch(/will require credentials for all/i);

    // The security-warning phrases are emphasized via <em> so screen
    // readers announce the stress and sighted users see the italics.
    // Disable narrows the emphasis to just the two words "require
    // credentials" — the surrounding "will" and "for all" render normally.
    const enableEmphasis = definitions[0].querySelector('em');
    const disableEmphasis = definitions[1].querySelector('em');
    expect(enableEmphasis).toHaveTextContent(
      'Please consider the security implications for your organization.'
    );
    expect(disableEmphasis).toHaveTextContent('require credentials');
    expect(disableEmphasis?.textContent).not.toMatch(/will/);
    expect(disableEmphasis?.textContent).not.toMatch(/for all/);

    expect(callout.querySelector('a')).toHaveAttribute(
      'href',
      'https://links.sonatype.com/products/nexus/anonymous-access/docs'
    );
  });

  it('shouldAssociateDescriptionWithFieldsetForScreenReaders', async () => {
    // Screen reader users must hear the security-implications context when
    // they land on the radio group. aria-describedby on the fieldset links
    // it to the descriptive paragraphs above the radios.
    render(<AnonymousAccessStep />);
    await waitFor(() => expect(fetchSettings).toHaveBeenCalled());

    const group = screen.getByRole('group');
    const describedById = group.getAttribute('aria-describedby');
    expect(describedById).toBeTruthy();
    const describedBy = document.getElementById(describedById as string);
    expect(describedBy).not.toBeNull();
    expect(describedBy?.textContent).toMatch(
      /consider the security implications for your organization/i
    );
  });

  it('shouldDefaultToDisabledOnInitialRender', async () => {
    // Fetch never resolves — the initial state must already have Disabled
    // selected. This is the UX-convention default confirmed on 2026-07-10.
    fetchSettings.mockReturnValue(new Promise(() => {}));

    render(<AnonymousAccessStep />);
    await waitFor(() => expect(registerStep).toHaveBeenCalled());

    expect(screen.getByTestId('onboarding-wizard__anonymous-access-disable')).toBeChecked();
    expect(screen.getByTestId('onboarding-wizard__anonymous-access-enable')).not.toBeChecked();
  });

  it('shouldMarkStepValidFromInitialRender', async () => {
    // Because a radio is always selected, Next is always enabled — no
    // interaction required to unlock progression.
    fetchSettings.mockReturnValue(new Promise(() => {}));

    render(<AnonymousAccessStep />);
    await waitFor(() => expect(registerStep).toHaveBeenCalled());

    expect(mostRecentRegistration().valid).toBe(true);
  });

  it('shouldSwitchToEnableOnUserSelection', async () => {
    fetchSettings.mockReturnValue(new Promise(() => {}));
    render(<AnonymousAccessStep />);
    await waitFor(() => expect(registerStep).toHaveBeenCalled());

    fireEvent.click(screen.getByTestId('onboarding-wizard__anonymous-access-enable'));

    await waitFor(() =>
      expect(screen.getByTestId('onboarding-wizard__anonymous-access-enable')).toBeChecked()
    );
    expect(screen.getByTestId('onboarding-wizard__anonymous-access-disable')).not.toBeChecked();
    expect(mostRecentRegistration().valid).toBe(true);
  });

  it('shouldOverrideDefaultWhenFetchReturnsEnabled', async () => {
    // Re-entering the wizard on an install where anonymous is currently
    // enabled must flip the pre-selection to Enable (AC #4 respected).
    fetchSettings.mockResolvedValue({
      enabled: true,
      userId: 'anonymous',
      realmName: 'NexusAuthorizingRealm',
    });

    render(<AnonymousAccessStep />);

    await waitFor(() =>
      expect(screen.getByTestId('onboarding-wizard__anonymous-access-enable')).toBeChecked()
    );
    expect(screen.getByTestId('onboarding-wizard__anonymous-access-disable')).not.toBeChecked();
  });

  it('shouldKeepDefaultWhenFetchReturnsDisabled', async () => {
    fetchSettings.mockResolvedValue({
      enabled: false,
      userId: 'anonymous',
      realmName: 'NexusAuthorizingRealm',
    });

    render(<AnonymousAccessStep />);

    await waitFor(() => expect(fetchSettings).toHaveBeenCalled());
    expect(screen.getByTestId('onboarding-wizard__anonymous-access-disable')).toBeChecked();
    expect(screen.getByTestId('onboarding-wizard__anonymous-access-enable')).not.toBeChecked();
  });

  it('shouldKeepDefaultOnFetchFailure', async () => {
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    fetchSettings.mockRejectedValue(new Error('nope'));

    render(<AnonymousAccessStep />);

    await waitFor(() => expect(fetchSettings).toHaveBeenCalled());
    // Default (Disabled) stands; step remains valid so the user is not blocked.
    expect(screen.getByTestId('onboarding-wizard__anonymous-access-disable')).toBeChecked();
    expect(screen.getByTestId('onboarding-wizard__anonymous-access-enable')).not.toBeChecked();
    expect(mostRecentRegistration().valid).toBe(true);
    // Failure is surfaced to devs via console.warn even though UX is unaffected.
    await waitFor(() => expect(warnSpy).toHaveBeenCalled());
    warnSpy.mockRestore();
  });

  it('shouldCallSaveSettingsWithEnabledPayloadWhenEnableSelected', async () => {
    fetchSettings.mockReturnValue(new Promise(() => {}));
    render(<AnonymousAccessStep />);
    await waitFor(() => expect(registerStep).toHaveBeenCalled());

    fireEvent.click(screen.getByTestId('onboarding-wizard__anonymous-access-enable'));
    await waitFor(() =>
      expect(screen.getByTestId('onboarding-wizard__anonymous-access-enable')).toBeChecked()
    );

    await act(async () => {
      await mostRecentRegistration().onSubmit();
    });

    expect(saveSettings).toHaveBeenCalledWith({
      userId: 'anonymous',
      realmName: 'NexusAuthorizingRealm',
      enabled: true,
    });
  });

  it('shouldCallSaveSettingsWithDisabledPayloadFromDefaultSelection', async () => {
    // No user interaction — accepting the pre-selected Disabled default must
    // still submit the correct payload.
    fetchSettings.mockReturnValue(new Promise(() => {}));
    render(<AnonymousAccessStep />);
    await waitFor(() => expect(registerStep).toHaveBeenCalled());

    await act(async () => {
      await mostRecentRegistration().onSubmit();
    });

    expect(saveSettings).toHaveBeenCalledWith({
      userId: 'anonymous',
      realmName: 'NexusAuthorizingRealm',
      enabled: false,
    });
  });

  it('shouldFocusFirstRadioOnMount', async () => {
    render(<AnonymousAccessStep />);
    await waitFor(() => expect(fetchSettings).toHaveBeenCalled());

    expect(screen.getByTestId('onboarding-wizard__anonymous-access-enable')).toHaveFocus();
  });

  it('shouldSubmitWizardOnEnterKey', async () => {
    fetchSettings.mockReturnValue(new Promise(() => {}));
    render(<AnonymousAccessStep />);
    await waitFor(() => expect(registerStep).toHaveBeenCalled());

    const form = screen.getByTestId('onboarding-wizard__anonymous-access').querySelector('form');
    expect(form).not.toBeNull();
    fireEvent.submit(form as HTMLFormElement);

    expect(submit).toHaveBeenCalledTimes(1);
  });
});
