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

const mockGet = jest.fn();
const mockPut = jest.fn();
jest.mock('../../../../../../../interface/api', () => ({
  restClient: {
    get: (...args: unknown[]) => mockGet(...args),
    put: (...args: unknown[]) => mockPut(...args),
  },
  parseApiError: jest.fn((err: unknown) => ({ message: err instanceof Error ? err.message : 'Error' })),
}));

import { RepositoryFirewallStep } from '../RepositoryFirewallStep';
import { __resetPccsFormatsCacheForTests } from '../../../../../shared/security/useFirewallEnable';

function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

describe('RepositoryFirewallStep', () => {
  const onComplete = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    __resetPccsFormatsCacheForTests();
    mockGet.mockResolvedValue([
      { format: 'npm', pccsModeSupported: true },
      { format: 'pypi', pccsModeSupported: true },
    ]);
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

  it('does NOT render the PCCS button for non-PCCS formats (maven2)', () => {
    renderWithTheme(
      <RepositoryFirewallStep
        repositoryName="maven2-proxy-1"
        hasFirewallLicense={true}
        format="maven2"
        onComplete={onComplete}
      />
    );
    expect(screen.queryByRole('button', { name: 'PCCS' })).not.toBeInTheDocument();
  });

  it('does NOT render PCCS when format prop is omitted', () => {
    renderWithTheme(
      <RepositoryFirewallStep
        repositoryName="npm-proxy-1"
        hasFirewallLicense={true}
        onComplete={onComplete}
      />
    );
    expect(screen.queryByRole('button', { name: 'PCCS' })).not.toBeInTheDocument();
  });

  it('renders the PCCS button for npm proxies', async () => {
    renderWithTheme(
      <RepositoryFirewallStep
        repositoryName="npm-proxy-1"
        hasFirewallLicense={true}
        format="npm"
        onComplete={onComplete}
      />
    );
    expect(await screen.findByRole('button', { name: 'PCCS' })).toBeInTheDocument();
  });

  it('renders the PCCS button for pypi proxies', async () => {
    renderWithTheme(
      <RepositoryFirewallStep
        repositoryName="pypi-proxy-1"
        hasFirewallLicense={true}
        format="pypi"
        onComplete={onComplete}
      />
    );
    expect(await screen.findByRole('button', { name: 'PCCS' })).toBeInTheDocument();
  });

  it('deferred mode: clicking PCCS calls onChoice("pccs") without API calls', async () => {
    const onChoice = jest.fn();

    renderWithTheme(
      <RepositoryFirewallStep
        mode="deferred"
        value="none"
        onChoice={onChoice}
        hasFirewallLicense={true}
        format="npm"
      />
    );

    const pccsBtn = await screen.findByRole('button', { name: 'PCCS' });
    fireEvent.click(pccsBtn);
    expect(onChoice).toHaveBeenCalledWith('pccs');
    // No PUT side-effect in deferred mode
    expect(mockPut).not.toHaveBeenCalled();
  });

  it('shows the deferred PCCS confirmation summary when value="pccs"', async () => {
    renderWithTheme(
      <RepositoryFirewallStep
        mode="deferred"
        value="pccs"
        onChoice={jest.fn()}
        hasFirewallLicense={true}
        format="npm"
      />
    );
    expect(await screen.findByText('Firewall will be enabled in PCCS mode')).toBeInTheDocument();
  });
});
