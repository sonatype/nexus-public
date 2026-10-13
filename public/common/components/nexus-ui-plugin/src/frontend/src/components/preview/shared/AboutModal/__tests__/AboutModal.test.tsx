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

jest.mock('../../../../../interface/ExtJS', () => ({
  ExtJS: {
    urlOf: (path: string) => `https://testurl${path}`,
  },
}));

import { AboutModal } from '../AboutModal';

const LEGAL_MARKER = 'terms and conditions';
const COPYRIGHT_DOCUMENT = `<!DOCTYPE html>
<html>
<head><title>Copyright</title><style>body { font-size: 14px; }</style></head>
<body>
  <p>
    All use of and access to Sonatype Nexus Repository Professional is governed by these
    <a href="http://links.sonatype.com/products/nxrm/tos/pro" target="_blank" rel="noopener">${LEGAL_MARKER}</a>.
  </p>
<p>Copyright &copy; 2008-Present, Sonatype, Inc. All rights reserved.</p>
</body>
</html>`;

const SELF_HOSTED_PROPS = {
  open: true,
  onOpenChange: jest.fn(),
  isCloud: false,
  version: '3.97.0-SNAPSHOT',
  edition: 'PRO',
  buildRevision: 'c282006bacf',
  buildTimestamp: '2026-09-10T12:00:00Z',
};

function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

function mockFetchOnce(response: Partial<Response>) {
  global.fetch = jest.fn().mockResolvedValue(response) as unknown as typeof fetch;
}

describe('AboutModal', () => {
  const originalFetch = global.fetch;

  beforeEach(() => {
    mockFetchOnce({ ok: true, text: () => Promise.resolve(COPYRIGHT_DOCUMENT) });
  });

  afterEach(() => {
    global.fetch = originalFetch;
    jest.clearAllMocks();
  });

  it('renders the modal title when open', async () => {
    renderWithTheme(<AboutModal {...SELF_HOSTED_PROPS} />);
    expect(screen.getByText('About Sonatype Nexus Repository')).toBeInTheDocument();
    await screen.findByTestId('about-modal-legal');
  });

  it('renders version, edition, build revision and build timestamp on self-hosted', async () => {
    renderWithTheme(<AboutModal {...SELF_HOSTED_PROPS} />);

    const info = screen.getByTestId('about-modal-info');
    expect(info).toHaveTextContent('Version');
    expect(info).toHaveTextContent('3.97.0-SNAPSHOT');
    expect(info).toHaveTextContent('Edition');
    expect(info).toHaveTextContent('PRO');
    expect(info).toHaveTextContent('Build Revision');
    expect(info).toHaveTextContent('c282006bacf');
    expect(info).toHaveTextContent('Build Timestamp');
    expect(info).toHaveTextContent('2026-09-10T12:00:00Z');
    await screen.findByTestId('about-modal-legal');
  });

  it('omits the version and build info block on cloud', async () => {
    renderWithTheme(<AboutModal {...SELF_HOSTED_PROPS} isCloud={true} />);
    expect(screen.queryByTestId('about-modal-info')).not.toBeInTheDocument();
    await screen.findByTestId('about-modal-legal');
  });

  it('fetches COPYRIGHT.html and injects the legal body verbatim', async () => {
    renderWithTheme(<AboutModal {...SELF_HOSTED_PROPS} />);

    const legal = await screen.findByTestId('about-modal-legal');
    expect(legal).toHaveTextContent(LEGAL_MARKER);

    const link = legal.querySelector('a');
    expect(link).toHaveAttribute('href', 'http://links.sonatype.com/products/nxrm/tos/pro');
    expect(link).toHaveAttribute('target', '_blank');
  });

  it('fetches the legal document on both self-hosted and cloud', async () => {
    renderWithTheme(<AboutModal {...SELF_HOSTED_PROPS} isCloud={true} />);
    const legal = await screen.findByTestId('about-modal-legal');
    expect(legal).toHaveTextContent(LEGAL_MARKER);
  });

  it('does not fetch when the modal is closed', () => {
    renderWithTheme(<AboutModal {...SELF_HOSTED_PROPS} open={false} />);
    expect(global.fetch).not.toHaveBeenCalled();
  });

  it('shows an error with a retry action when the fetch fails, then recovers', async () => {
    global.fetch = jest
      .fn()
      .mockResolvedValueOnce({ ok: false, status: 500 })
      .mockResolvedValueOnce({ ok: true, text: () => Promise.resolve(COPYRIGHT_DOCUMENT) }) as unknown as typeof fetch;

    renderWithTheme(<AboutModal {...SELF_HOSTED_PROPS} />);

    const error = await screen.findByTestId('about-modal-error');
    expect(error).toHaveTextContent('Unable to load legal information.');

    fireEvent.click(screen.getByText('Retry'));

    const legal = await screen.findByTestId('about-modal-legal');
    expect(legal).toHaveTextContent(LEGAL_MARKER);
  });

  it('calls onOpenChange(false) when the Close button is clicked', async () => {
    const onOpenChange = jest.fn();
    renderWithTheme(<AboutModal {...SELF_HOSTED_PROPS} onOpenChange={onOpenChange} />);

    fireEvent.click(screen.getByTestId('about-modal-close'));

    await waitFor(() => expect(onOpenChange).toHaveBeenCalledWith(false));
  });
});
