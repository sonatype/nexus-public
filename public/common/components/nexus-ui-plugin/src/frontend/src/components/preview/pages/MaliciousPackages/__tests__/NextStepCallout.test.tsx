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

import { NextStepCallout } from '../NextStepCallout';

function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

const defaultProps = {
  pendingCount: 5,
  unprotectedRepoCount: 3,
  onAction: jest.fn(),
  onSkip: jest.fn(),
};

describe('NextStepCallout', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('returns null when phase is null', () => {
    const { container } = renderWithTheme(
      <NextStepCallout {...defaultProps} phase={null} />
    );
    expect(container.children[0]?.children.length ?? 0).toBe(0);
  });

  it('renders ALERT phase message and buttons', () => {
    renderWithTheme(<NextStepCallout {...defaultProps} phase="ALERT" />);

    expect(screen.getByText(/Run Malicious Packages tasks/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Run Scans' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Skip' })).toBeInTheDocument();
  });

  it('renders TRIAGE phase with pending count', () => {
    renderWithTheme(<NextStepCallout {...defaultProps} phase="TRIAGE" pendingCount={12} />);

    expect(screen.getByText(/12 repos still have unscanned malware/)).toBeInTheDocument();
  });

  it('renders CONTAINMENT phase with unprotected repo count', () => {
    renderWithTheme(<NextStepCallout {...defaultProps} phase="CONTAINMENT" unprotectedRepoCount={7} />);

    expect(screen.getByText(/7 repos lack quarantine protection/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Configure in Protect' })).toBeInTheDocument();
  });

  it('renders ERADICATION phase without skip button', () => {
    renderWithTheme(<NextStepCallout {...defaultProps} phase="ERADICATION" />);

    expect(screen.getByText(/Delete or acknowledge each finding/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Delete All Pending' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Skip' })).not.toBeInTheDocument();
  });

  it('renders RECOVERY phase', () => {
    renderWithTheme(<NextStepCallout {...defaultProps} phase="RECOVERY" />);

    expect(screen.getByText(/Re-scan affected repos/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Re-Scan' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Skip Verification' })).toBeInTheDocument();
  });

  it('renders POST_INCIDENT phase without skip button', () => {
    renderWithTheme(<NextStepCallout {...defaultProps} phase="POST_INCIDENT" />);

    expect(screen.getByText(/Export your report/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Export CSV' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Skip' })).not.toBeInTheDocument();
  });

  it('calls onAction when action button is clicked', async () => {
    renderWithTheme(<NextStepCallout {...defaultProps} phase="ALERT" />);

    await userEvent.click(screen.getByRole('button', { name: 'Run Scans' }));
    expect(defaultProps.onAction).toHaveBeenCalledTimes(1);
  });

  it('calls onSkip when skip button is clicked', async () => {
    renderWithTheme(<NextStepCallout {...defaultProps} phase="ALERT" />);

    await userEvent.click(screen.getByRole('button', { name: 'Skip' }));
    expect(defaultProps.onSkip).toHaveBeenCalledTimes(1);
  });
});
