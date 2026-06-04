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
import { HelmResultRow } from '../HelmResultRow';
import type { HelmResult } from '../helm.types';

const fullResult: HelmResult = {
  id: 'helm:nginx-ingress',
  name: 'nginx-ingress',
  displayName: 'nginx-ingress',
  latestVersion: '4.9.0',
  appVersion: '3.4.0',
  versionsCount: 87,
  description: 'NGINX Ingress Controller for Kubernetes',
  icon: 'https://example.com/nginx-icon.svg',
  repositoriesCount: 2,
  lastUpdated: '2024-01-20T10:30:00Z',
};

const minimalResult: HelmResult = {
  id: 'helm:minimal',
  name: 'minimal',
  displayName: 'minimal',
  latestVersion: '1.0.0',
  versionsCount: 1,
  repositoriesCount: 1,
  lastUpdated: '2024-01-01T00:00:00Z',
};

describe('HelmResultRow', () => {
  it('renders chart name and description when present', () => {
    render(
      <table>
        <tbody>
          <HelmResultRow result={fullResult} onSelect={jest.fn()} />
        </tbody>
      </table>
    );

    expect(screen.getByText('nginx-ingress')).toBeInTheDocument();
    expect(screen.getByText(/NGINX Ingress Controller/)).toBeInTheDocument();
  });

  it('renders icon when present', () => {
    const { container } = render(
      <table>
        <tbody>
          <HelmResultRow result={fullResult} onSelect={jest.fn()} />
        </tbody>
      </table>
    );

    const img = container.querySelector('img');
    expect(img).toBeInTheDocument();
    expect(img).toHaveAttribute('src', fullResult.icon);
  });

  it('renders appVersion when present', () => {
    render(
      <table>
        <tbody>
          <HelmResultRow result={fullResult} onSelect={jest.fn()} />
        </tbody>
      </table>
    );

    expect(screen.getByText('3.4.0')).toBeInTheDocument();
  });

  it('shows dash for appVersion when absent', () => {
    render(
      <table>
        <tbody>
          <HelmResultRow result={minimalResult} onSelect={jest.fn()} />
        </tbody>
      </table>
    );

    expect(screen.getByText('-')).toBeInTheDocument();
  });

  it('does not render icon when absent', () => {
    const { container } = render(
      <table>
        <tbody>
          <HelmResultRow result={minimalResult} onSelect={jest.fn()} />
        </tbody>
      </table>
    );

    expect(container.querySelector('img')).not.toBeInTheDocument();
  });

  it('does not render description when absent', () => {
    render(
      <table>
        <tbody>
          <HelmResultRow result={minimalResult} onSelect={jest.fn()} />
        </tbody>
      </table>
    );

    expect(screen.queryByText(/Controller/)).not.toBeInTheDocument();
  });

  it('truncates description longer than 60 characters', () => {
    const longDesc = 'A'.repeat(61);
    render(
      <table>
        <tbody>
          <HelmResultRow result={{ ...fullResult, description: longDesc }} onSelect={jest.fn()} />
        </tbody>
      </table>
    );

    expect(screen.getByText(`${'A'.repeat(60)}...`)).toBeInTheDocument();
  });

  it('calls onSelect when clicked', async () => {
    const onSelect = jest.fn();
    render(
      <table>
        <tbody>
          <HelmResultRow result={fullResult} onSelect={onSelect} />
        </tbody>
      </table>
    );

    const row = screen.getByRole('button', { name: /view details for nginx-ingress/i });
    await userEvent.click(row);

    expect(onSelect).toHaveBeenCalledWith('helm:nginx-ingress');
  });

  it('calls onSelect when Enter key pressed', () => {
    const onSelect = jest.fn();
    render(
      <table>
        <tbody>
          <HelmResultRow result={fullResult} onSelect={onSelect} />
        </tbody>
      </table>
    );

    const row = screen.getByRole('button', { name: /view details for nginx-ingress/i });
    row.focus();
    fireEvent.keyDown(row, { key: 'Enter' });

    expect(onSelect).toHaveBeenCalledWith('helm:nginx-ingress');
  });

  it('calls onSelect when Space key pressed', () => {
    const onSelect = jest.fn();
    render(
      <table>
        <tbody>
          <HelmResultRow result={fullResult} onSelect={onSelect} />
        </tbody>
      </table>
    );

    const row = screen.getByRole('button', { name: /view details for nginx-ingress/i });
    row.focus();
    fireEvent.keyDown(row, { key: ' ' });

    expect(onSelect).toHaveBeenCalledWith('helm:nginx-ingress');
  });
});
