/*
 * Copyright (c) 2008-present Sonatype, Inc.
 *
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/pro/attributions
 * Sonatype and Sonatype Nexus are trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Foundation.
 * M2Eclipse is a trademark of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { BulkDeleteConfirmationModal } from '../BulkDeleteConfirmationModal';

const renderModal = (props = {}) =>
  render(
    <Theme>
      <BulkDeleteConfirmationModal
        isOpen={true}
        onClose={jest.fn()}
        onConfirm={jest.fn()}
        count={3}
        {...props}
      />
    </Theme>
  );

describe('BulkDeleteConfirmationModal', () => {
  describe('rendering', () => {
    it('renders the delete confirmation dialog', () => {
      renderModal();
      expect(screen.getByTestId('bulk-delete-modal')).toBeInTheDocument();
    });

    it('displays the count of entries to delete', () => {
      renderModal({ count: 5 });
      expect(screen.getByText('Delete 5 IP Addresses?')).toBeInTheDocument();
    });

    it('displays count=1 with singular form', () => {
      renderModal({ count: 1 });
      expect(screen.getByText('Delete 1 IP Address?')).toBeInTheDocument();
    });
  });

  describe('actions', () => {
    it('calls onConfirm when confirm button is clicked', () => {
      const onConfirm = jest.fn();
      renderModal({ onConfirm });
      fireEvent.click(screen.getByTestId('bulk-delete-confirm-button'));
      expect(onConfirm).toHaveBeenCalled();
    });

    it('calls onClose when cancel button is clicked', () => {
      const onClose = jest.fn();
      renderModal({ onClose });
      fireEvent.click(screen.getByTestId('bulk-delete-cancel-button'));
      expect(onClose).toHaveBeenCalled();
    });
  });
});
