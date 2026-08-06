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

import Welcome from '../Welcome';
import { useWizard } from '../useWizard';

jest.mock('../useWizard');
const mockedUseWizard = jest.mocked(useWizard);

describe('Welcome', () => {
  const registerStep = jest.fn();
  const getStarted = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockedUseWizard.mockReturnValue({
      state: 'stepReady',
      steps: [],
      currentIndex: -1,
      currentStep: null,
      errorMessage: null,
      isCurrentStepValid: true,
      registerStep,
      getStarted,
      submit: jest.fn(),
      skip: jest.fn(),
      finish: jest.fn(),
    });
  });

  it('renders the logo with alt text', () => {
    render(<Welcome />);
    const logo = screen.getByAltText('Sonatype Nexus Repository');
    expect(logo).toBeInTheDocument();
    expect(logo.tagName).toBe('IMG');
    expect(logo).toHaveAttribute('src', expect.stringContaining('favicon.svg'));
  });

  it('renders the title text', () => {
    render(<Welcome />);
    expect(screen.getByText(/Let's Get You Set Up/i)).toBeInTheDocument();
  });

  it('renders the description text', () => {
    render(<Welcome />);
    expect(
      screen.getByText(/This wizard will guide you through a few quick steps/i)
    ).toBeInTheDocument();
  });

  it('registers a valid step on mount', () => {
    render(<Welcome />);
    expect(registerStep).toHaveBeenCalledWith({
      valid: true,
      onSubmit: expect.any(Function),
    });
  });

  it('calls getStarted when onSubmit is invoked', () => {
    render(<Welcome />);
    const registration = registerStep.mock.calls[0][0];
    registration.onSubmit();
    expect(getStarted).toHaveBeenCalledTimes(1);
  });

  it('does not render a button', () => {
    render(<Welcome />);
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });
});
