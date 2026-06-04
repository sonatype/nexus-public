/*
 * Copyright (c) 2008-present Sonatype, Inc.
 *
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/pro/attributions
 * Sonatype and Sonatype Nexus are trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Foundation.
 * M2Eclipse is a trademark of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { BulkImportModal } from '../BulkImportModal';

const renderModal = (props = {}) =>
  render(
    <Theme>
      <BulkImportModal isOpen={true} onClose={jest.fn()} onImport={jest.fn()} {...props} />
    </Theme>
  );

describe('BulkImportModal', () => {
  describe('initial state', () => {
    it('renders the dialog title', () => {
      renderModal();
      expect(screen.getByRole('heading', { name: 'Import IP Addresses' })).toBeInTheDocument();
    });

    it('shows file input area', () => {
      renderModal();
      expect(screen.getByTestId('file-input')).toBeInTheDocument();
    });

    it('submit button is initially disabled', () => {
      renderModal();
      expect(screen.getByTestId('import-submit-button')).toBeDisabled();
    });

    it('shows download sample file link', () => {
      renderModal();
      expect(screen.getByTestId('download-sample-button')).toBeInTheDocument();
    });
  });

  describe('file selection', () => {
    it('enables submit button when a valid CSV file is selected', async () => {
      renderModal();
      const csvContent = '192.168.1.1,Office\n10.0.0.0/24,Internal';
      const file = new File([csvContent], 'ips.csv', { type: 'text/csv' });
      const input = screen.getByTestId('file-input');

      Object.defineProperty(input, 'files', { value: [file], configurable: true });
      fireEvent.change(input, { target: { files: [file] } });

      await waitFor(() => {
        expect(screen.getByTestId('import-submit-button')).not.toBeDisabled();
      });
    });

    it('shows file name after file is selected', async () => {
      renderModal();
      const file = new File(['ip1\nip2'], 'my-ips.csv', { type: 'text/csv' });
      const input = screen.getByTestId('file-input');

      Object.defineProperty(input, 'files', { value: [file], configurable: true });
      fireEvent.change(input, { target: { files: [file] } });

      await waitFor(() => {
        expect(screen.getByText('my-ips.csv')).toBeInTheDocument();
      });
    });

    it('rejects files larger than 1 MB and shows error', async () => {
      renderModal();
      const largeContent = 'x'.repeat(1_100_000);
      const file = new File([largeContent], 'large.csv', { type: 'text/csv' });
      const input = screen.getByTestId('file-input');

      Object.defineProperty(input, 'files', { value: [file], configurable: true });
      fireEvent.change(input, { target: { files: [file] } });

      await waitFor(() => {
        expect(screen.getByText(/too large/i)).toBeInTheDocument();
      });
    });

    it('rejects non-CSV file type', async () => {
      renderModal();
      const file = new File(['data'], 'doc.pdf', { type: 'application/pdf' });
      const input = screen.getByTestId('file-input');

      Object.defineProperty(input, 'files', { value: [file], configurable: true });
      fireEvent.change(input, { target: { files: [file] } });

      await waitFor(() => {
        expect(screen.getByText(/invalid file type/i)).toBeInTheDocument();
      });
    });
  });

  describe('submission', () => {
    it('calls onImport with the selected file on submit', async () => {
      const onImport = jest.fn();
      renderModal({ onImport });

      const csvContent = '192.168.1.1,Office\n10.0.0.0/24,Internal';
      const file = new File([csvContent], 'ips.csv', { type: 'text/csv' });
      const input = screen.getByTestId('file-input');

      Object.defineProperty(input, 'files', { value: [file], configurable: true });
      fireEvent.change(input, { target: { files: [file] } });

      await waitFor(() => {
        expect(screen.getByTestId('import-submit-button')).not.toBeDisabled();
      });

      fireEvent.click(screen.getByTestId('import-submit-button'));

      expect(onImport).toHaveBeenCalledWith(file);
    });

    it('calls onClose when cancel button is clicked', () => {
      const onClose = jest.fn();
      renderModal({ onClose });
      fireEvent.click(screen.getByTestId('import-cancel-button'));
      expect(onClose).toHaveBeenCalled();
    });
  });
});
