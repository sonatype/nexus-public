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
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { Theme } from '@radix-ui/themes';
import { ToastProvider } from '../../../shared';

import { NuGetApiTokenPage } from '../NuGetApiTokenPage';

// ---------------------------------------------------------------------------
// Mocks
// ---------------------------------------------------------------------------

const mockRequestAuthenticationToken = jest.fn();

jest.mock('../../../../../interface/ExtJS', () => ({
  ExtJS: {
    requestAuthenticationToken: (...args: unknown[]) => mockRequestAuthenticationToken(...args),
    useUser: jest.fn(() => ({ id: 'testuser', userId: 'testuser' })),
  },
}));

jest.mock('axios', () => ({
  get: jest.fn(),
  delete: jest.fn(),
}));

const mockAxios = jest.requireMock('axios');

Object.defineProperty(navigator, 'clipboard', {
  value: { writeText: jest.fn().mockResolvedValue(undefined) },
  configurable: true,
});

function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme><ToastProvider>{children}</ToastProvider></Theme>;
}

function renderPage() {
  return render(<NuGetApiTokenPage />, { wrapper: TestWrapper });
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe('NuGetApiTokenPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders page header and key card', () => {
    renderPage();
    expect(screen.getByTestId('nuget-api-token-page')).toBeInTheDocument();
    expect(screen.getByText('NuGet API Key')).toBeInTheDocument();
    expect(screen.getByTestId('nuget-key-card')).toBeInTheDocument();
  });

  it('renders Access and Reset buttons', () => {
    renderPage();
    expect(screen.getByTestId('access-api-key-btn')).toBeInTheDocument();
    expect(screen.getByTestId('reset-api-key-btn')).toBeInTheDocument();
  });

  it('shows usage instructions with {API_KEY} placeholder before reveal', () => {
    renderPage();
    expect(screen.getByTestId('usage-instructions-card')).toBeInTheDocument();
    const dotnetPreview = screen.getByTestId('dotnet-cmd-preview');
    expect(dotnetPreview.textContent).toContain('{API_KEY}');
    const nugetPreview = screen.getByTestId('nuget-cmd-preview');
    expect(nugetPreview.textContent).toContain('{API_KEY}');
  });

  it('shows user ID in dotnet command preview', () => {
    renderPage();
    const dotnetPreview = screen.getByTestId('dotnet-cmd-preview');
    expect(dotnetPreview.textContent).toContain('testuser');
  });

  it('Access API Key calls auth then GET and shows reveal modal', async () => {
    mockRequestAuthenticationToken.mockResolvedValue('authtok');
    mockAxios.get.mockResolvedValue({ data: { apiKey: 'my-api-key-abc123' } });

    renderPage();

    fireEvent.click(screen.getByTestId('access-api-key-btn'));

    await waitFor(() => {
      expect(screen.getByTestId('nuget-reveal-dialog')).toBeInTheDocument();
    });

    expect(mockRequestAuthenticationToken).toHaveBeenCalled();
    expect(screen.getByText('my-api-key-abc123')).toBeInTheDocument();
  });

  it('copy button in reveal modal calls clipboard.writeText', async () => {
    mockRequestAuthenticationToken.mockResolvedValue('authok');
    mockAxios.get.mockResolvedValue({ data: { apiKey: 'testkey' } });

    renderPage();
    fireEvent.click(screen.getByTestId('access-api-key-btn'));

    await waitFor(() => expect(screen.getByTestId('nuget-reveal-dialog')).toBeInTheDocument());

    const copyBtn = screen.getByTestId('copy-btn-your-api-key');
    fireEvent.click(copyBtn);

    expect(navigator.clipboard.writeText).toHaveBeenCalledWith('testkey');
  });

  it('closing modal hides the key', async () => {
    mockRequestAuthenticationToken.mockResolvedValue('tok');
    mockAxios.get.mockResolvedValue({ data: { apiKey: 'secretkey' } });

    renderPage();
    fireEvent.click(screen.getByTestId('access-api-key-btn'));
    await waitFor(() => expect(screen.getByTestId('nuget-reveal-dialog')).toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: 'Close' }));

    await waitFor(() => {
      expect(screen.queryByTestId('nuget-reveal-dialog')).not.toBeInTheDocument();
    });
  });

  it('shows ConfirmDialog when Reset API Key is clicked', async () => {
    renderPage();
    fireEvent.click(screen.getByTestId('reset-api-key-btn'));

    await waitFor(() => {
      expect(screen.getByTestId('reset-nuget-key-dialog-confirm')).toBeInTheDocument();
    });
  });

  it('calls DELETE API after reset confirmation', async () => {
    mockRequestAuthenticationToken.mockResolvedValue('delauthok');
    mockAxios.delete.mockResolvedValue({});

    renderPage();
    fireEvent.click(screen.getByTestId('reset-api-key-btn'));
    await waitFor(() => expect(screen.getByTestId('reset-nuget-key-dialog-confirm')).toBeInTheDocument());

    fireEvent.click(screen.getByTestId('reset-nuget-key-dialog-confirm'));

    await waitFor(() => {
      expect(mockAxios.delete).toHaveBeenCalledWith(
        expect.stringContaining('nuget-api-key?authToken=')
      );
    });
  });
});
