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
import { GenericResultRow } from '../GenericResultRow';
import type { GenericResult } from '../generic.types';

const mavenResult: GenericResult = {
  id: 'maven:commons-lang3:3.14.0',
  format: 'maven2',
  repository: 'maven-central',
  group: 'org.apache.commons',
  name: 'commons-lang3',
  version: '3.14.0',
  displayName: 'commons-lang3',
  assets: [],
};

const npmResultNoGroup: GenericResult = {
  id: 'npm:lodash:4.17.21',
  format: 'npm',
  repository: 'npm-proxy',
  group: null,
  name: 'lodash',
  version: '4.17.21',
  displayName: 'lodash',
  assets: [],
};

const unknownFormatResult: GenericResult = {
  id: 'custom:lib:1.0.0',
  format: 'custom-format',
  repository: 'custom-repo',
  group: null,
  name: 'lib',
  version: '1.0.0',
  displayName: 'lib',
  assets: [],
};

describe('GenericResultRow', () => {
  it('renders display name', () => {
    render(
      <table>
        <tbody>
          <GenericResultRow result={mavenResult} onSelect={jest.fn()} />
        </tbody>
      </table>
    );

    expect(screen.getByText('commons-lang3')).toBeInTheDocument();
  });

  it('renders group when present', () => {
    render(
      <table>
        <tbody>
          <GenericResultRow result={mavenResult} onSelect={jest.fn()} />
        </tbody>
      </table>
    );

    expect(screen.getByText('org.apache.commons')).toBeInTheDocument();
  });

  it('does not render group when null', () => {
    render(
      <table>
        <tbody>
          <GenericResultRow result={npmResultNoGroup} onSelect={jest.fn()} />
        </tbody>
      </table>
    );

    expect(screen.queryByText(/apache/)).not.toBeInTheDocument();
  });

  it('renders known format label from FORMAT_CONFIG', () => {
    render(
      <table>
        <tbody>
          <GenericResultRow result={mavenResult} onSelect={jest.fn()} />
        </tbody>
      </table>
    );

    expect(screen.getByText('Maven')).toBeInTheDocument();
  });

  it('renders raw format name for unknown format', () => {
    render(
      <table>
        <tbody>
          <GenericResultRow result={unknownFormatResult} onSelect={jest.fn()} />
        </tbody>
      </table>
    );

    expect(screen.getByText('custom-format')).toBeInTheDocument();
  });

  it('renders version and repository', () => {
    render(
      <table>
        <tbody>
          <GenericResultRow result={mavenResult} onSelect={jest.fn()} />
        </tbody>
      </table>
    );

    expect(screen.getByText('3.14.0')).toBeInTheDocument();
    expect(screen.getByText('maven-central')).toBeInTheDocument();
  });

  it('calls onSelect when clicked', async () => {
    const onSelect = jest.fn();
    render(
      <table>
        <tbody>
          <GenericResultRow result={mavenResult} onSelect={onSelect} />
        </tbody>
      </table>
    );

    const row = screen.getByRole('button', { name: /view details for commons-lang3/i });
    await userEvent.click(row);

    expect(onSelect).toHaveBeenCalledWith('maven:commons-lang3:3.14.0');
  });

  it('calls onSelect when Enter key pressed', () => {
    const onSelect = jest.fn();
    render(
      <table>
        <tbody>
          <GenericResultRow result={mavenResult} onSelect={onSelect} />
        </tbody>
      </table>
    );

    const row = screen.getByRole('button', { name: /view details for commons-lang3/i });
    row.focus();
    fireEvent.keyDown(row, { key: 'Enter' });

    expect(onSelect).toHaveBeenCalledWith('maven:commons-lang3:3.14.0');
  });

  it('calls onSelect when Space key pressed', () => {
    const onSelect = jest.fn();
    render(
      <table>
        <tbody>
          <GenericResultRow result={mavenResult} onSelect={onSelect} />
        </tbody>
      </table>
    );

    const row = screen.getByRole('button', { name: /view details for commons-lang3/i });
    row.focus();
    fireEvent.keyDown(row, { key: ' ' });

    expect(onSelect).toHaveBeenCalledWith('maven:commons-lang3:3.14.0');
  });
});
