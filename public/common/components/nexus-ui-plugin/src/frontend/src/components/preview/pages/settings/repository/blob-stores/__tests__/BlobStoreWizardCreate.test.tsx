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
import '@testing-library/jest-dom';
import { BlobStoreWizardCreate } from '../BlobStoreWizardCreate';

jest.mock('../useBlobStores', () => ({
  useBlobStore: () => ({
    save: jest.fn(),
  }),
  useBlobStoreTypes: () => ({
    types: [
      { id: 'file', name: 'File' },
      { id: 's3', name: 'S3' },
      { id: 'group', name: 'Group' },
    ],
    quotaTypes: [{ id: 'spaceUsedQuota', name: 'Space Used' }],
    loading: false,
  }),
  useS3DropdownValues: () => ({
    values: { regions: [] },
    loading: false,
  }),
  useGroupableBlobStores: () => ({
    blobStores: [],
    loading: false,
  }),
}));

const renderWithTheme = (ui: React.ReactElement) =>
  render(<Theme>{ui}</Theme>);

async function goToBasicConfigStep() {
  renderWithTheme(<BlobStoreWizardCreate onBack={jest.fn()} />);
  const fileCard = screen.getByText('File').closest('button');
  if (!fileCard) throw new Error('File blob store option not found');
  await userEvent.click(fileCard);
  const nextButton = screen.getByRole('button', { name: /Continue/i });
  await userEvent.click(nextButton);
}

describe('BlobStoreWizardCreate', () => {
  it('renders the wizard with step 0 (type selector)', () => {
    renderWithTheme(<BlobStoreWizardCreate onBack={jest.fn()} />);

    expect(screen.getByText('Create Blob Store')).toBeInTheDocument();
    expect(screen.getByText('Select Type')).toBeInTheDocument();
    expect(screen.getByText('Choose storage type')).toBeInTheDocument();
    expect(screen.getByText('File')).toBeInTheDocument();
  });

  it('has step indicator for the wizard', () => {
    renderWithTheme(<BlobStoreWizardCreate onBack={jest.fn()} />);

    expect(screen.getByTestId('blob-store-wizard-steps')).toBeInTheDocument();
  });

  it('shows Continue button on step 0 (type selection)', () => {
    renderWithTheme(<BlobStoreWizardCreate onBack={jest.fn()} />);

    expect(screen.getByRole('button', { name: /Continue/i })).toBeInTheDocument();
  });

  it('disables Next on Basic Config when path is root slash', async () => {
    await goToBasicConfigStep();

    const nameInput = screen.getByLabelText(/name/i);
    const pathInput = screen.getByLabelText(/path/i);

    await userEvent.type(nameInput, 'my-blob-store');
    await userEvent.type(pathInput, '/');

    const nextButton = screen.getByRole('button', { name: /Continue/i });
    expect(nextButton).toBeDisabled();
  });

  it('disables Next on Basic Config when path is empty', async () => {
    await goToBasicConfigStep();

    const nameInput = screen.getByLabelText(/name/i);
    await userEvent.type(nameInput, 'my-blob-store');

    const nextButton = screen.getByRole('button', { name: /Continue/i });
    expect(nextButton).toBeDisabled();
  });

  it('enables Next on Basic Config when path is valid', async () => {
    await goToBasicConfigStep();

    const nameInput = screen.getByLabelText(/name/i);
    const pathInput = screen.getByLabelText(/path/i);

    await userEvent.type(nameInput, 'my-blob-store');
    await userEvent.type(pathInput, '/tmp/blob-store');

    const nextButton = screen.getByRole('button', { name: /Continue/i });
    expect(nextButton).toBeEnabled();
  });
});
