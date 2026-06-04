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
import { render, screen, fireEvent } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import '@testing-library/jest-dom';

import { RepositoryFirewallStep } from '../RepositoryFirewallStep';

jest.mock('../../../../../../../interface/api', () => ({
  restClient: { put: jest.fn() },
  parseApiError: jest.fn((err: unknown) => ({ message: err instanceof Error ? err.message : 'Error' })),
}));

function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

describe('RepositoryFirewallStep', () => {
  const onComplete = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders None, Audit, Quarantine when hasFirewallLicense', () => {
    renderWithTheme(
      <RepositoryFirewallStep
        repositoryName="maven2-proxy-1"
        hasFirewallLicense={true}
        onComplete={onComplete}
      />
    );

    expect(screen.getByText('Enable Repository Firewall')).toBeInTheDocument();
    expect(screen.getByText(/Protect this proxy repository by enabling Firewall/)).toBeInTheDocument();
    expect(screen.getByText('Skip')).toBeInTheDocument();
    expect(screen.getByText('Audit')).toBeInTheDocument();
    expect(screen.getByText('Quarantine')).toBeInTheDocument();
  });

  it('shows cross-sell when hasFirewallLicense is false', () => {
    renderWithTheme(
      <RepositoryFirewallStep
        repositoryName="maven2-proxy-1"
        hasFirewallLicense={false}
        onComplete={onComplete}
      />
    );

    expect(screen.getByText('Enable Repository Firewall')).toBeInTheDocument();
    expect(screen.getByText('Learn more')).toBeInTheDocument();
    expect(screen.getByText('Contact sales')).toBeInTheDocument();
  });

  it('calls onComplete when None is clicked (opens confirm, then completes)', async () => {
    renderWithTheme(
      <RepositoryFirewallStep
        repositoryName="maven2-proxy-1"
        hasFirewallLicense={true}
        onComplete={onComplete}
      />
    );

    fireEvent.click(screen.getByRole('button', { name: 'Skip' }));
    const continueBtn = await screen.findByRole('button', { name: /Continue Anyway/i });
    fireEvent.click(continueBtn);
    expect(onComplete).toHaveBeenCalledTimes(1);
  });

  it('deferred mode: calls onChoice with level, no API calls', () => {
    const onChoice = jest.fn();

    renderWithTheme(
      <RepositoryFirewallStep
        mode="deferred"
        value="none"
        onChoice={onChoice}
        hasFirewallLicense={true}
      />
    );

    fireEvent.click(screen.getByRole('button', { name: 'Quarantine' }));
    expect(onChoice).toHaveBeenCalledWith('quarantine');
  });
});
