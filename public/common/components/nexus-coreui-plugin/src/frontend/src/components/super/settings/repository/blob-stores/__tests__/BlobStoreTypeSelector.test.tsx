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
import { BlobStoreTypeSelector } from '../BlobStoreTypeSelector';
import { BLOB_STORE_TYPE_IDS } from '../blobStoreFormMachine';

const renderWithTheme = (ui: React.ReactElement) =>
  render(<Theme>{ui}</Theme>);

describe('BlobStoreTypeSelector', () => {
  it('renders all blob store type options', () => {
    renderWithTheme(
      <BlobStoreTypeSelector selectedType={null} onSelect={jest.fn()} />
    );

    expect(screen.getByText('File')).toBeInTheDocument();
    expect(screen.getByText('Amazon S3')).toBeInTheDocument();
    expect(screen.getByText('Azure Blob')).toBeInTheDocument();
    expect(screen.getByText('Google Cloud')).toBeInTheDocument();
    expect(screen.getByText('Group')).toBeInTheDocument();
  });

  it('calls onSelect when a type is clicked', () => {
    const onSelect = jest.fn();
    renderWithTheme(
      <BlobStoreTypeSelector selectedType={null} onSelect={onSelect} />
    );

    fireEvent.click(screen.getByText('File'));
    expect(onSelect).toHaveBeenCalledWith(BLOB_STORE_TYPE_IDS.FILE);

    fireEvent.click(screen.getByText('Amazon S3'));
    expect(onSelect).toHaveBeenCalledWith(BLOB_STORE_TYPE_IDS.S3);
  });

  it('shows selected state for chosen type', () => {
    renderWithTheme(
      <BlobStoreTypeSelector
        selectedType={BLOB_STORE_TYPE_IDS.FILE}
        onSelect={jest.fn()}
      />
    );

    const fileCard = screen.getByText('File').closest('button');
    expect(fileCard).toHaveAttribute('aria-selected', 'true');
  });

  it('does not call onSelect when disabled', () => {
    const onSelect = jest.fn();
    renderWithTheme(
      <BlobStoreTypeSelector selectedType={null} onSelect={onSelect} disabled />
    );

    fireEvent.click(screen.getByText('File'));
    expect(onSelect).not.toHaveBeenCalled();
  });
});
