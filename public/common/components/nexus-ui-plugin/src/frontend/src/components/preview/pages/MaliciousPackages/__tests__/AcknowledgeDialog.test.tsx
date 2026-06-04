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
import { Theme } from '@radix-ui/themes';

import { AcknowledgeDialog } from '../AcknowledgeDialog';
import type { MaliciousFinding } from '../types';

function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

function makeFinding(overrides: Partial<MaliciousFinding> = {}): MaliciousFinding {
  return {
    id: 1,
    repositoryName: 'npm-proxy',
    assetId: 'asset-1',
    path: '/some/path',
    format: 'npm',
    recordedTime: null,
    deletedTime: null,
    deletedBy: null,
    deletionMethod: null,
    acknowledgedAt: null,
    acknowledgedBy: null,
    acknowledgedReason: null,
    firstDetectedAt: '2026-03-15T10:00:00Z',
    hash: null,
    createdBy: null,
    createdByIp: null,
    componentName: 'evil-package',
    componentVersion: '1.0.0',
    componentFormat: 'npm',
    threatLevel: 10,
    threatSummary: 'Malware detected',
    threatReference: null,
    policyName: null,
    ...overrides,
  };
}

describe('AcknowledgeDialog', () => {
  const defaultProps = {
    open: true,
    finding: makeFinding(),
    onConfirm: jest.fn(),
    onCancel: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders dialog title with component name', () => {
    renderWithTheme(<AcknowledgeDialog {...defaultProps} />);

    expect(screen.getByText(/Acknowledge & Accept — evil-package/)).toBeInTheDocument();
  });

  it('shows path when componentName is null', () => {
    const finding = makeFinding({ componentName: null });
    renderWithTheme(<AcknowledgeDialog {...defaultProps} finding={finding} />);

    expect(screen.getByText(/Acknowledge & Accept — \/some\/path/)).toBeInTheDocument();
  });

  it('renders reason textarea', () => {
    renderWithTheme(<AcknowledgeDialog {...defaultProps} />);

    expect(screen.getByLabelText('Reason (required)')).toBeInTheDocument();
  });

  it('renders duration selector with default of 30 days', () => {
    renderWithTheme(<AcknowledgeDialog {...defaultProps} />);

    expect(screen.getByText('Accept risk for')).toBeInTheDocument();
    expect(screen.getByText('30 days')).toBeInTheDocument();
  });

  it('disables confirm button when reason is empty', () => {
    renderWithTheme(<AcknowledgeDialog {...defaultProps} />);

    const confirmButton = screen.getByRole('button', { name: /Acknowledge & Accept/ });
    expect(confirmButton).toBeDisabled();
  });

  it('enables confirm button when reason is provided', async () => {
    renderWithTheme(<AcknowledgeDialog {...defaultProps} />);

    await userEvent.type(screen.getByLabelText('Reason (required)'), 'False positive');

    const confirmButton = screen.getByRole('button', { name: /Acknowledge & Accept/ });
    expect(confirmButton).not.toBeDisabled();
  });

  it('calls onConfirm with reason and default duration when confirmed', async () => {
    renderWithTheme(<AcknowledgeDialog {...defaultProps} />);

    await userEvent.type(screen.getByLabelText('Reason (required)'), 'False positive');
    await userEvent.click(screen.getByRole('button', { name: /Acknowledge & Accept/ }));

    expect(defaultProps.onConfirm).toHaveBeenCalledWith('False positive', '30d');
  });

  it('calls onCancel when cancel button is clicked', async () => {
    renderWithTheme(<AcknowledgeDialog {...defaultProps} />);

    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(defaultProps.onCancel).toHaveBeenCalledTimes(1);
  });

  it('does not render dialog content when closed', () => {
    renderWithTheme(<AcknowledgeDialog {...defaultProps} open={false} />);

    expect(screen.queryByText(/Acknowledge & Accept/)).not.toBeInTheDocument();
  });
});
