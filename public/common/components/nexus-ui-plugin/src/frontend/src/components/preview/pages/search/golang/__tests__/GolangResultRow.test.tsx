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
import { GolangResultRow } from '../GolangResultRow';
import type { GolangResult } from '../golang.types';

const fullResult: GolangResult = {
  id: 'go:github.com/gin-gonic/gin',
  module: 'github.com/gin-gonic/gin',
  latestVersion: 'v1.9.1',
  versionsCount: 42,
  description: 'Gin is a HTTP web framework written in Go',
  license: 'MIT',
  repositoriesCount: 1,
  lastUpdated: '2024-01-15T12:00:00Z',
};

const minimalResult: GolangResult = {
  id: 'go:golang.org/x/net',
  module: 'golang.org/x/net',
  latestVersion: 'v0.20.0',
  versionsCount: 5,
  repositoriesCount: 1,
  lastUpdated: '2024-01-01T00:00:00Z',
};

describe('GolangResultRow', () => {
  it('renders module path', () => {
    render(
      <table>
        <tbody>
          <GolangResultRow result={fullResult} onSelect={jest.fn()} />
        </tbody>
      </table>
    );

    expect(screen.getByText('github.com/gin-gonic/gin')).toBeInTheDocument();
  });

  it('renders description when present', () => {
    render(
      <table>
        <tbody>
          <GolangResultRow result={fullResult} onSelect={jest.fn()} />
        </tbody>
      </table>
    );

    expect(screen.getByText(/Gin is a HTTP web framework/)).toBeInTheDocument();
  });

  it('does not render description when absent', () => {
    render(
      <table>
        <tbody>
          <GolangResultRow result={minimalResult} onSelect={jest.fn()} />
        </tbody>
      </table>
    );

    expect(screen.queryByText(/web framework/)).not.toBeInTheDocument();
  });

  it('renders license when present', () => {
    render(
      <table>
        <tbody>
          <GolangResultRow result={fullResult} onSelect={jest.fn()} />
        </tbody>
      </table>
    );

    expect(screen.getByText('MIT')).toBeInTheDocument();
  });

  it('shows dash for license when absent', () => {
    render(
      <table>
        <tbody>
          <GolangResultRow result={minimalResult} onSelect={jest.fn()} />
        </tbody>
      </table>
    );

    expect(screen.getByText('-')).toBeInTheDocument();
  });

  it('renders version and versionsCount', () => {
    render(
      <table>
        <tbody>
          <GolangResultRow result={fullResult} onSelect={jest.fn()} />
        </tbody>
      </table>
    );

    expect(screen.getByText('v1.9.1')).toBeInTheDocument();
    expect(screen.getByText('42')).toBeInTheDocument();
  });

  it('calls onSelect when clicked', async () => {
    const onSelect = jest.fn();
    render(
      <table>
        <tbody>
          <GolangResultRow result={fullResult} onSelect={onSelect} />
        </tbody>
      </table>
    );

    const row = screen.getByRole('button', { name: /view details for github.com\/gin-gonic\/gin/i });
    await userEvent.click(row);

    expect(onSelect).toHaveBeenCalledWith('go:github.com/gin-gonic/gin');
  });

  it('calls onSelect when Enter key pressed', () => {
    const onSelect = jest.fn();
    render(
      <table>
        <tbody>
          <GolangResultRow result={fullResult} onSelect={onSelect} />
        </tbody>
      </table>
    );

    const row = screen.getByRole('button', { name: /view details for github.com\/gin-gonic\/gin/i });
    row.focus();
    fireEvent.keyDown(row, { key: 'Enter' });

    expect(onSelect).toHaveBeenCalledWith('go:github.com/gin-gonic/gin');
  });

  it('calls onSelect when Space key pressed', () => {
    const onSelect = jest.fn();
    render(
      <table>
        <tbody>
          <GolangResultRow result={fullResult} onSelect={onSelect} />
        </tbody>
      </table>
    );

    const row = screen.getByRole('button', { name: /view details for github.com\/gin-gonic\/gin/i });
    row.focus();
    fireEvent.keyDown(row, { key: ' ' });

    expect(onSelect).toHaveBeenCalledWith('go:github.com/gin-gonic/gin');
  });
});
