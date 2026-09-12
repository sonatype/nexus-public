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
import { render, screen } from '@testing-library/react';

import ExtJS from '../../../interface/ExtJS';
import { restClient } from '../../../interface/api/rest-client';

// Mock ExtJS module
jest.mock('../../../interface/ExtJS', () => ({
  useState: jest.fn(),
  useUser: jest.fn(),
  state: jest.fn(),
  showErrorMessage: jest.fn(),
}));

// Mock rest-client module
jest.mock('../../../interface/api/rest-client', () => ({
  restClient: {
    get: jest.fn(),
  },
}));

// Mock Wizard component
jest.mock('../Wizard', () => {
  return function MockWizard(): JSX.Element {
    return <div data-testid="wizard-stub" />;
  };
});

// Mock WizardContext module - capture props via a passthrough
let capturedWizardProviderProps: {
  fetchSteps?: () => Promise<unknown[]>;
  verify?: () => Promise<void>;
  onDone?: () => void;
  children?: React.ReactNode;
} = {};

jest.mock('../WizardContext', () => ({
  WizardProvider: function MockWizardProvider({
    fetchSteps,
    verify,
    onDone,
    children,
  }: {
    fetchSteps: () => Promise<unknown[]>;
    verify?: () => Promise<void>;
    onDone?: () => void;
    children: React.ReactNode;
  }): JSX.Element {
    capturedWizardProviderProps = { fetchSteps, verify, onDone, children };
    return <>{children}</>;
  },
}));

// Import the component AFTER mocks are set up
// eslint-disable-next-line @typescript-eslint/no-require-imports
const OnboardingWizardMount = require('../OnboardingWizardMount').default;

describe('OnboardingWizardMount', () => {
  let consoleErrorSpy: jest.SpyInstance;
  let consoleWarnSpy: jest.SpyInstance;

  beforeEach(() => {
    jest.clearAllMocks();
    capturedWizardProviderProps = {};

    consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
  });

  afterEach(() => {
    consoleErrorSpy.mockRestore();
    consoleWarnSpy.mockRestore();
  });

  describe('rendering conditions', () => {
    it('renders null when feature flag is off', () => {
      (ExtJS.useState as jest.Mock).mockReturnValue(false);
      (ExtJS.useUser as jest.Mock).mockReturnValue({ administrator: true });

      const { container } = render(<OnboardingWizardMount />);

      expect(container.firstChild).toBeNull();
      expect(screen.queryByTestId('wizard-stub')).not.toBeInTheDocument();
      expect(consoleErrorSpy).not.toHaveBeenCalled();
      expect(consoleWarnSpy).not.toHaveBeenCalled();
    });

    it('renders null when feature flag is on but onboarding.required is false', () => {
      (ExtJS.useState as jest.Mock)
        .mockReturnValueOnce(true) // featureEnabled
        .mockReturnValueOnce(false); // onboardingRequired
      (ExtJS.useUser as jest.Mock).mockReturnValue({ administrator: true });

      const { container } = render(<OnboardingWizardMount />);

      expect(container.firstChild).toBeNull();
      expect(screen.queryByTestId('wizard-stub')).not.toBeInTheDocument();
      expect(consoleErrorSpy).not.toHaveBeenCalled();
      expect(consoleWarnSpy).not.toHaveBeenCalled();
    });

    it('renders null when feature flag and onboarding.required are on but user is not admin', () => {
      (ExtJS.useState as jest.Mock)
        .mockReturnValueOnce(true) // featureEnabled
        .mockReturnValueOnce(true); // onboardingRequired
      (ExtJS.useUser as jest.Mock).mockReturnValue({ administrator: false });

      const { container } = render(<OnboardingWizardMount />);

      expect(container.firstChild).toBeNull();
      expect(screen.queryByTestId('wizard-stub')).not.toBeInTheDocument();
      expect(consoleErrorSpy).not.toHaveBeenCalled();
      expect(consoleWarnSpy).not.toHaveBeenCalled();
    });

    it('renders null when useUser returns undefined', () => {
      (ExtJS.useState as jest.Mock)
        .mockReturnValueOnce(true) // featureEnabled
        .mockReturnValueOnce(true); // onboardingRequired
      (ExtJS.useUser as jest.Mock).mockReturnValue(undefined);

      const { container } = render(<OnboardingWizardMount />);

      expect(container.firstChild).toBeNull();
      expect(screen.queryByTestId('wizard-stub')).not.toBeInTheDocument();
      expect(consoleErrorSpy).not.toHaveBeenCalled();
      expect(consoleWarnSpy).not.toHaveBeenCalled();
    });

    it('renders Wizard when feature flag, onboarding.required, and admin are all true', () => {
      (ExtJS.useState as jest.Mock)
        .mockReturnValueOnce(true) // featureEnabled
        .mockReturnValueOnce(true); // onboardingRequired
      (ExtJS.useUser as jest.Mock).mockReturnValue({ administrator: true });

      render(<OnboardingWizardMount />);

      expect(screen.getByTestId('wizard-stub')).toBeInTheDocument();
      expect(consoleErrorSpy).not.toHaveBeenCalled();
      expect(consoleWarnSpy).not.toHaveBeenCalled();
    });

    it('renders null when useUser returns object without administrator field', () => {
      (ExtJS.useState as jest.Mock)
        .mockReturnValueOnce(true) // featureEnabled
        .mockReturnValueOnce(true); // onboardingRequired
      (ExtJS.useUser as jest.Mock).mockReturnValue({ id: 'someUser' });

      const { container } = render(<OnboardingWizardMount />);

      expect(container.firstChild).toBeNull();
      expect(screen.queryByTestId('wizard-stub')).not.toBeInTheDocument();
      expect(consoleErrorSpy).not.toHaveBeenCalled();
      expect(consoleWarnSpy).not.toHaveBeenCalled();
    });
  });

  describe('WizardProvider props', () => {
    beforeEach(() => {
      (ExtJS.useState as jest.Mock)
        .mockReturnValueOnce(true) // featureEnabled
        .mockReturnValueOnce(true); // onboardingRequired
      (ExtJS.useUser as jest.Mock).mockReturnValue({ administrator: true });
    });

    it('provides fetchSteps that calls the correct endpoint', async () => {
      const mockSteps = [{ type: 'Foo' }];
      (restClient.get as jest.Mock).mockResolvedValueOnce(mockSteps);

      render(<OnboardingWizardMount />);

      // Trigger the fetch
      const result = await capturedWizardProviderProps.fetchSteps!();

      expect(restClient.get).toHaveBeenCalledWith(
        '/service/rest/internal/ui/onboarding'
      );
      expect(result).toEqual(mockSteps);
      expect(consoleErrorSpy).not.toHaveBeenCalled();
      expect(consoleWarnSpy).not.toHaveBeenCalled();
    });

    it('provides onDone that clears onboarding.required', () => {
      const mockSetValue = jest.fn();
      (ExtJS.state as jest.Mock).mockReturnValue({
        getValue: jest.fn(),
        setValue: mockSetValue,
      });

      render(<OnboardingWizardMount />);

      // Invoke onDone
      capturedWizardProviderProps.onDone!();

      expect(mockSetValue).toHaveBeenCalledWith('onboarding.required', false);
      expect(consoleErrorSpy).not.toHaveBeenCalled();
      expect(consoleWarnSpy).not.toHaveBeenCalled();
    });

    it('onDone guards against ExtJS.state() returning undefined', () => {
      (ExtJS.state as jest.Mock).mockReturnValue(undefined);

      render(<OnboardingWizardMount />);

      // Should not throw
      expect(() => capturedWizardProviderProps.onDone!()).not.toThrow();
      expect(consoleErrorSpy).not.toHaveBeenCalled();
      expect(consoleWarnSpy).not.toHaveBeenCalled();
    });
  });

  describe('verify service', () => {
    beforeEach(() => {
      (ExtJS.useState as jest.Mock)
        .mockReturnValueOnce(true) // featureEnabled
        .mockReturnValueOnce(true); // onboardingRequired
      (ExtJS.useUser as jest.Mock).mockReturnValue({ administrator: true });
    });

    it('provides verify that calls the onboarding endpoint and throws OnboardingStillRequired if steps remain', async () => {
      const mockSteps = [{ type: 'Foo' }];
      (restClient.get as jest.Mock).mockResolvedValueOnce(mockSteps);

      render(<OnboardingWizardMount />);

      // Trigger the verify
      try {
        await capturedWizardProviderProps.verify!();
        throw new Error('verify should have thrown');
      } catch (err) {
        expect((err as Error).message).toBe('Onboarding still required');
        // Machine relies on this name to classify errorKind as stillRequired
        // rather than network — verify the wire contract.
        expect((err as Error).name).toBe('OnboardingStillRequired');
      }
      expect(restClient.get).toHaveBeenCalledWith(
        '/service/rest/internal/ui/onboarding',
      );
      expect(consoleErrorSpy).not.toHaveBeenCalled();
      expect(consoleWarnSpy).not.toHaveBeenCalled();
    });

    it('verify resolves without error when no steps remain', async () => {
      (restClient.get as jest.Mock).mockResolvedValueOnce([]);

      render(<OnboardingWizardMount />);

      // Trigger the verify - should not throw
      await expect(capturedWizardProviderProps.verify!()).resolves.toBeUndefined();
      expect(restClient.get).toHaveBeenCalledWith(
        '/service/rest/internal/ui/onboarding',
      );
      expect(consoleErrorSpy).not.toHaveBeenCalled();
      expect(consoleWarnSpy).not.toHaveBeenCalled();
    });
  });

  describe('dismiss toast on close', () => {
    beforeEach(() => {
      sessionStorage.clear();
      (ExtJS.useState as jest.Mock)
        .mockReturnValueOnce(true) // featureEnabled
        .mockReturnValueOnce(true); // onboardingRequired
      (ExtJS.useUser as jest.Mock).mockReturnValue({ administrator: true });
      (ExtJS.state as jest.Mock).mockReturnValue({
        getValue: jest.fn(),
        setValue: jest.fn(),
      });
    });

    afterEach(() => {
      sessionStorage.clear();
    });

    it('shows the "setup not completed" toast when dismissed flag is set', () => {
      sessionStorage.setItem('onboarding.dismissed', 'true');

      render(<OnboardingWizardMount />);

      capturedWizardProviderProps.onDone!();

      expect(ExtJS.showErrorMessage).toHaveBeenCalledTimes(1);
      expect(ExtJS.showErrorMessage).toHaveBeenCalledWith(
        expect.stringContaining('Setup was not completed'),
      );
      // Flag is cleared so a subsequent unrelated re-open won't repeat the toast.
      expect(sessionStorage.getItem('onboarding.dismissed')).toBeNull();
      expect(consoleErrorSpy).not.toHaveBeenCalled();
      expect(consoleWarnSpy).not.toHaveBeenCalled();
    });

    it('does not show the toast when there is no dismissed flag', () => {
      render(<OnboardingWizardMount />);

      capturedWizardProviderProps.onDone!();

      expect(ExtJS.showErrorMessage).not.toHaveBeenCalled();
      expect(consoleErrorSpy).not.toHaveBeenCalled();
      expect(consoleWarnSpy).not.toHaveBeenCalled();
    });

    it('completes cleanup and clears state even if sessionStorage throws', () => {
      const mockSetValue = jest.fn();
      (ExtJS.state as jest.Mock).mockReturnValue({
        getValue: jest.fn(),
        setValue: mockSetValue,
      });
      const getItemSpy = jest
        .spyOn(Storage.prototype, 'getItem')
        .mockImplementation(() => {
          throw new Error('SecurityError');
        });

      render(<OnboardingWizardMount />);

      expect(() => capturedWizardProviderProps.onDone!()).not.toThrow();
      // The critical side effect (clearing onboarding.required) must still fire
      // even when sessionStorage is unavailable.
      expect(mockSetValue).toHaveBeenCalledWith('onboarding.required', false);

      getItemSpy.mockRestore();
    });
  });
});
