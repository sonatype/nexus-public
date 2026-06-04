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
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import { GAResultRow } from '../GAResultRow';
import type { GAResult } from '../../core';

jest.mock('../../../../shared', () => ({
  FormatBadge: ({ format }: { format: string }) => <span data-testid="format-badge">{format}</span>,
}));

const fullResult: GAResult = {
  gaId: 'maven:org.apache.commons:commons-lang3',
  format: 'maven2' as any,
  displayName: 'commons-lang3',
  namespace: 'org.apache.commons',
  latestVersion: '3.14.0',
  versionsCount: 42,
  repositoriesCount: 5,
  lastUpdated: '2024-01-20T10:30:00Z',
};

const noVersionResult: GAResult = {
  gaId: 'maven:org.example:lib',
  format: 'maven2' as any,
  displayName: 'lib',
  namespace: 'org.example',
  versionsCount: 1,
  repositoriesCount: 1,
  lastUpdated: '2024-01-01T00:00:00Z',
};

describe('GAResultRow', () => {
  it('renders display name', () => {
    render(<GAResultRow result={fullResult} onSelect={jest.fn()} />);

    expect(screen.getByText('commons-lang3')).toBeInTheDocument();
  });

  it('renders namespace', () => {
    render(<GAResultRow result={fullResult} onSelect={jest.fn()} />);

    expect(screen.getByText('org.apache.commons')).toBeInTheDocument();
  });

  it('renders latestVersion when present', () => {
    render(<GAResultRow result={fullResult} onSelect={jest.fn()} />);

    expect(screen.getByText(/3\.14\.0/)).toBeInTheDocument();
  });

  it('shows dash for latestVersion when absent', () => {
    render(<GAResultRow result={noVersionResult} onSelect={jest.fn()} />);

    expect(screen.getByText(/Latest: -/)).toBeInTheDocument();
  });

  it('renders format badge', () => {
    render(<GAResultRow result={fullResult} onSelect={jest.fn()} />);

    const badges = screen.getAllByTestId('format-badge');
    expect(badges.length).toBeGreaterThanOrEqual(1);
    expect(badges[0]).toHaveTextContent('maven2');
  });

  it('calls onSelect when clicked', async () => {
    const onSelect = jest.fn();
    render(<GAResultRow result={fullResult} onSelect={onSelect} />);

    const row = screen.getByRole('button', { name: /view details for commons-lang3/i });
    await userEvent.click(row);

    expect(onSelect).toHaveBeenCalledWith('maven:org.apache.commons:commons-lang3');
  });

  it('calls onSelect when Enter key pressed', () => {
    const onSelect = jest.fn();
    render(<GAResultRow result={fullResult} onSelect={onSelect} />);

    const row = screen.getByRole('button', { name: /view details for commons-lang3/i });
    row.focus();
    fireEvent.keyDown(row, { key: 'Enter' });

    expect(onSelect).toHaveBeenCalledWith('maven:org.apache.commons:commons-lang3');
  });

  it('calls onSelect when Space key pressed', () => {
    const onSelect = jest.fn();
    render(<GAResultRow result={fullResult} onSelect={onSelect} />);

    const row = screen.getByRole('button', { name: /view details for commons-lang3/i });
    row.focus();
    fireEvent.keyDown(row, { key: ' ' });

    expect(onSelect).toHaveBeenCalledWith('maven:org.apache.commons:commons-lang3');
  });
});
