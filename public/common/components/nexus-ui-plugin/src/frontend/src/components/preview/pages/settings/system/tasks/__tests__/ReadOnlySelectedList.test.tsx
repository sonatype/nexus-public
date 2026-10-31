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
import { render, screen, within } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { ReadOnlySelectedList } from '../ReadOnlySelectedList';

const renderWithTheme = (ui: React.ReactElement) => render(<Theme>{ui}</Theme>);

describe('ReadOnlySelectedList', () => {
  it('renders label, help text, the Selected header, and each value', () => {
    renderWithTheme(
      <ReadOnlySelectedList label="Blob store" helpText="Select the blob stores" values={['default', 'other']} testId="input-blobstoreName" />
    );
    expect(screen.getByText('Blob store')).toBeInTheDocument();
    expect(screen.getByText('Select the blob stores')).toBeInTheDocument();
    expect(screen.getByText('Selected')).toBeInTheDocument();
    const box = screen.getByTestId('input-blobstoreName');
    expect(within(box).getByText('default')).toBeInTheDocument();
    expect(within(box).getByText('other')).toBeInTheDocument();
  });

  it('renders an empty box (no items, no crash) when values is empty', () => {
    renderWithTheme(<ReadOnlySelectedList label="Repository" values={[]} testId="input-repositoryName" />);
    expect(screen.getByText('Selected')).toBeInTheDocument();
    expect(screen.getByTestId('input-repositoryName')).toBeInTheDocument();
  });

  it('renders emptyText when values is empty and emptyText is provided', () => {
    renderWithTheme(
      <ReadOnlySelectedList label="Blob store" values={[]} emptyText="All Blob Stores selected" testId="input-blobstoreName" />
    );
    expect(screen.getByText('All Blob Stores selected')).toBeInTheDocument();
  });

  it('does not render emptyText when values are present', () => {
    renderWithTheme(
      <ReadOnlySelectedList label="Blob store" values={['default']} emptyText="All Blob Stores selected" testId="input-blobstoreName" />
    );
    expect(screen.queryByText('All Blob Stores selected')).not.toBeInTheDocument();
  });

  it('does not render an Available column or any transfer buttons', () => {
    renderWithTheme(<ReadOnlySelectedList label="Blob store" values={['default']} testId="input-blobstoreName" />);
    expect(screen.queryByText('Available')).not.toBeInTheDocument();
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });
});
