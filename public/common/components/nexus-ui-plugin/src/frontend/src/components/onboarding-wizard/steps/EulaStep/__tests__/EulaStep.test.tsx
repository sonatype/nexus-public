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
import userEvent from '@testing-library/user-event';

import EulaStep from '../EulaStep';
import { useEulaStep } from '../useEulaStep';
import ExtJS from '../../../../../interface/ExtJS';
import UIStrings from '../../../UIStrings';

jest.mock('../useEulaStep');
jest.mock('../../../../../interface/ExtJS', () => ({
  __esModule: true,
  default: {
    ceLicenseUrl: jest.fn(() => '/CE-LICENSE.html'),
  },
}));

const mockedUseEulaStep = jest.mocked(useEulaStep);
const mockedCeLicenseUrl = ExtJS.ceLicenseUrl as jest.Mock;

const { COMMUNITY_EULA } = UIStrings.ONBOARDING_WIZARD;

describe('EulaStep', () => {
  const defaultHookResult = {
    accepted: false,
    loading: false,
    success: false,
    error: null,
    errorKind: null,
    onAcceptChange: jest.fn(),
    onRetry: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
    mockedUseEulaStep.mockReturnValue(defaultHookResult);
  });

  describe('rendering', () => {
    it('renders without crashing', () => {
      render(<EulaStep />);
      expect(screen.getByTestId('eula-step')).toBeInTheDocument();
    });

    it('renders the title', () => {
      render(<EulaStep />);
      expect(screen.getByText(COMMUNITY_EULA.TITLE)).toBeInTheDocument();
    });

    it('renders the subtitle', () => {
      render(<EulaStep />);
      expect(screen.getByTestId('eula-step__subtitle')).toHaveTextContent(
        COMMUNITY_EULA.SUBTITLE
      );
    });

    it('renders the Read Full License Agreement link opening ceLicenseUrl in a new tab', () => {
      mockedCeLicenseUrl.mockReturnValue('/CE-LICENSE.html');
      render(<EulaStep />);
      const link = screen.getByTestId('eula-step__read-full-license');
      expect(link).toBeInTheDocument();
      expect(link).toHaveTextContent(COMMUNITY_EULA.READ_FULL_LICENSE_LABEL);
      expect(link).toHaveAttribute('href', '/CE-LICENSE.html');
      expect(link).toHaveAttribute('target', '_blank');
      expect(link).toHaveAttribute('rel', 'noopener noreferrer');
    });

    it('places the Read Full License Agreement link between the iframe and the checkbox', () => {
      render(<EulaStep />);
      const iframe = screen.getByTestId('eula-step__license-iframe');
      const link = screen.getByTestId('eula-step__read-full-license');
      const checkbox = screen.getByTestId('eula-step__accept-checkbox');

      // eslint-disable-next-line no-bitwise
      expect(
        iframe.compareDocumentPosition(link) & Node.DOCUMENT_POSITION_FOLLOWING
      ).toBeTruthy();
      // eslint-disable-next-line no-bitwise
      expect(
        link.compareDocumentPosition(checkbox) & Node.DOCUMENT_POSITION_FOLLOWING
      ).toBeTruthy();
    });

    it('renders the license iframe when not loading', () => {
      render(<EulaStep />);
      const iframe = screen.getByTestId('eula-step__license-iframe');
      expect(iframe).toBeInTheDocument();
      expect(iframe).toHaveAttribute('src', '/CE-LICENSE.html');
      expect(iframe).toHaveAttribute('title', COMMUNITY_EULA.IFRAME_TITLE);
      // Sandbox intentionally omitted — matches LicenseAgreementModal and
      // the license HTML is served from the same origin already.
      expect(iframe).not.toHaveAttribute('sandbox');
    });

    it('resolves the iframe src through ExtJS.ceLicenseUrl', () => {
      mockedCeLicenseUrl.mockReturnValueOnce('http://nexus.example.com/CE-LICENSE.html');
      render(<EulaStep />);
      const iframe = screen.getByTestId('eula-step__license-iframe');
      expect(iframe).toHaveAttribute('src', 'http://nexus.example.com/CE-LICENSE.html');
      expect(mockedCeLicenseUrl).toHaveBeenCalled();
    });

    it('renders the checkbox with correct label', () => {
      render(<EulaStep />);
      expect(screen.getByText(COMMUNITY_EULA.CHECKBOX_LABEL)).toBeInTheDocument();
      expect(screen.getByTestId('eula-step__accept-checkbox')).toBeInTheDocument();
    });

    it('shows loading spinner when loading is true', () => {
      mockedUseEulaStep.mockReturnValue({
        ...defaultHookResult,
        loading: true,
      });
      render(<EulaStep />);
      expect(screen.getByTestId('eula-step__loading')).toBeInTheDocument();
      expect(screen.queryByTestId('eula-step__license-iframe')).not.toBeInTheDocument();
    });

    it('does not show loading spinner when loading is false', () => {
      render(<EulaStep />);
      expect(screen.queryByTestId('eula-step__loading')).not.toBeInTheDocument();
      expect(screen.getByTestId('eula-step__license-iframe')).toBeInTheDocument();
    });
  });

  describe('checkbox state', () => {
    it('renders unchecked checkbox when accepted is false', () => {
      render(<EulaStep />);
      const checkbox = screen.getByTestId('eula-step__accept-checkbox');
      expect(checkbox).not.toBeChecked();
    });

    it('renders checked checkbox when accepted is true', () => {
      mockedUseEulaStep.mockReturnValue({
        ...defaultHookResult,
        accepted: true,
      });
      render(<EulaStep />);
      const checkbox = screen.getByTestId('eula-step__accept-checkbox');
      expect(checkbox).toBeChecked();
    });
  });

  describe('error handling', () => {
    it('renders error banner when a fetch error is present', () => {
      mockedUseEulaStep.mockReturnValue({
        ...defaultHookResult,
        error: 'Something went wrong',
        errorKind: 'fetch',
      });
      render(<EulaStep />);
      const errorBanner = screen.getByTestId('eula-step__error');
      expect(errorBanner).toBeInTheDocument();
      expect(errorBanner).toHaveTextContent('Something went wrong');
    });

    it('renders error banner when a submit error is present', () => {
      mockedUseEulaStep.mockReturnValue({
        ...defaultHookResult,
        error: 'Submit failed',
        errorKind: 'submit',
      });
      render(<EulaStep />);
      expect(screen.getByTestId('eula-step__error')).toHaveTextContent('Submit failed');
    });

    it('does not render error banner when error is null', () => {
      render(<EulaStep />);
      expect(screen.queryByTestId('eula-step__error')).not.toBeInTheDocument();
    });

    it('renders retry button for fetch errors', () => {
      mockedUseEulaStep.mockReturnValue({
        ...defaultHookResult,
        error: 'Failed to load',
        errorKind: 'fetch',
      });
      render(<EulaStep />);
      const button = screen.getByTestId('eula-step__retry-button');
      expect(button).toBeInTheDocument();
      expect(button).toHaveTextContent(UIStrings.ONBOARDING_WIZARD.ACTIONS.RETRY);
    });

    it('does NOT render retry button for submit errors (Next retries the POST)', () => {
      mockedUseEulaStep.mockReturnValue({
        ...defaultHookResult,
        error: 'Submit failed',
        errorKind: 'submit',
      });
      render(<EulaStep />);
      expect(screen.queryByTestId('eula-step__retry-button')).not.toBeInTheDocument();
    });

    it('calls onRetry when retry button is clicked', async () => {
      const onRetry = jest.fn();
      mockedUseEulaStep.mockReturnValue({
        ...defaultHookResult,
        error: 'Failed to load',
        errorKind: 'fetch',
        onRetry,
      });
      render(<EulaStep />);
      const retryButton = screen.getByTestId('eula-step__retry-button');
      retryButton.click();
      expect(onRetry).toHaveBeenCalledTimes(1);
    });
  });

  describe('success indicator', () => {
    it('renders the success callout when success is true', () => {
      mockedUseEulaStep.mockReturnValue({
        ...defaultHookResult,
        success: true,
      });
      render(<EulaStep />);
      const callout = screen.getByTestId('eula-step__success');
      expect(callout).toBeInTheDocument();
      expect(callout).toHaveTextContent(COMMUNITY_EULA.SUCCESS_MESSAGE);
    });

    it('does not render the success callout when success is false', () => {
      render(<EulaStep />);
      expect(screen.queryByTestId('eula-step__success')).not.toBeInTheDocument();
    });
  });

  describe('checkbox interaction', () => {
    it('calls onAcceptChange with true when checkbox is checked', async () => {
      const onAcceptChange = jest.fn();
      mockedUseEulaStep.mockReturnValue({
        ...defaultHookResult,
        onAcceptChange,
      });
      render(<EulaStep />);
      const checkbox = screen.getByTestId('eula-step__accept-checkbox');
      checkbox.click();
      expect(onAcceptChange).toHaveBeenCalledWith(true);
    });

    it('calls onAcceptChange with false when checkbox is unchecked', async () => {
      const onAcceptChange = jest.fn();
      mockedUseEulaStep.mockReturnValue({
        ...defaultHookResult,
        accepted: true,
        onAcceptChange,
      });
      render(<EulaStep />);
      const checkbox = screen.getByTestId('eula-step__accept-checkbox');
      checkbox.click();
      expect(onAcceptChange).toHaveBeenCalledWith(false);
    });
  });

  describe('tab order (AC #18)', () => {
    // Within the step's own DOM the iframe must come before the checkbox in
    // tab order. The wizard chrome renders the Next button after the step body
    // (see Wizard.tsx), so checkbox → Next is guaranteed by DOM order at the
    // parent level and is covered separately in the wizard tests.
    it('tabs from the license iframe through the read-full-license link to the acceptance checkbox', () => {
      render(<EulaStep />);
      const iframe = screen.getByTestId('eula-step__license-iframe');
      const link = screen.getByTestId('eula-step__read-full-license');
      const checkbox = screen.getByTestId('eula-step__accept-checkbox');

      iframe.focus();
      expect(document.activeElement).toBe(iframe);

      userEvent.tab();
      expect(document.activeElement).toBe(link);

      userEvent.tab();
      expect(document.activeElement).toBe(checkbox);
    });
  });
});
