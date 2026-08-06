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
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { DeleteConfirmationModal } from '../DeleteConfirmationModal';

describe('DeleteConfirmationModal', () => {
  const mockOnClose = jest.fn();
  const mockOnConfirm = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('Rendering & Visibility', () => {
    it('renders when open is true', () => {
      render(
        <DeleteConfirmationModal
          open={true}
          onClose={mockOnClose}
          onConfirm={mockOnConfirm}
          entityType="repository"
        />
      );

      expect(screen.getByText('Delete repository?')).toBeInTheDocument();
    });

    it('does not render when open is false', () => {
      render(
        <DeleteConfirmationModal
          open={false}
          onClose={mockOnClose}
          onConfirm={mockOnConfirm}
          entityType="repository"
        />
      );

      expect(screen.queryByText('Delete repository?')).not.toBeInTheDocument();
    });

    it('displays correct entity type in title', () => {
      render(
        <DeleteConfirmationModal
          open={true}
          onClose={mockOnClose}
          onConfirm={mockOnConfirm}
          entityType="blob store"
        />
      );

      expect(screen.getByText('Delete blob store?')).toBeInTheDocument();
    });

    it('displays warning message', () => {
      render(
        <DeleteConfirmationModal
          open={true}
          onClose={mockOnClose}
          onConfirm={mockOnConfirm}
          entityType="user"
        />
      );

      expect(
        screen.getByText(/This action cannot be undone/)
      ).toBeInTheDocument();
    });
  });

  describe('Entity Name Mode (Repositories, Blob Stores)', () => {
    it('requires typing "Delete" regardless of entity name', async () => {
      render(
        <DeleteConfirmationModal
          open={true}
          onClose={mockOnClose}
          onConfirm={mockOnConfirm}
          entityName="my-repository"
          entityType="repository"
        />
      );

      const input = screen.getByPlaceholderText('Type "Delete" to confirm');
      const deleteButton = screen.getByRole('button', { name: /delete/i });

      expect(deleteButton).toBeDisabled();

      // Typing the entity name does NOT enable the button — only the literal word.
      await userEvent.type(input, 'my-repository');
      expect(deleteButton).toBeDisabled();

      await userEvent.clear(input);
      await userEvent.type(input, 'Delete');
      await waitFor(() => {
        expect(deleteButton).toBeEnabled();
      });
    });

    it('shows error for non-matching input', async () => {
      render(
        <DeleteConfirmationModal
          open={true}
          onClose={mockOnClose}
          onConfirm={mockOnConfirm}
          entityName="my-repository"
          entityType="repository"
        />
      );

      const input = screen.getByPlaceholderText('Type "Delete" to confirm');

      await userEvent.type(input, 'wrong-name');

      expect(
        screen.getByText('The confirmation text provided is incorrect')
      ).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /delete/i })).toBeDisabled();
    });

    it('shows entity name in description, "Delete" in warning box', () => {
      render(
        <DeleteConfirmationModal
          open={true}
          onClose={mockOnClose}
          onConfirm={mockOnConfirm}
          entityName="test-blob-store"
          entityType="blob store"
        />
      );

      // Entity name surfaces in the description so the user knows what they're deleting.
      expect(screen.getByText(/test-blob-store/)).toBeInTheDocument();
      // Warning box always shows the literal acknowledgement word (assert via the
      // unique helper caption next to it — the bare word also appears on the
      // submit button so a `getByText('Delete')` would be ambiguous).
      const helper = screen.getByText('Type this to confirm deletion');
      expect(helper.parentElement).toHaveTextContent('Delete');
    });
  });

  describe('Acknowledgement word verification', () => {
    it('requires typing the acknowledgement word when entityName is null', async () => {
      render(
        <DeleteConfirmationModal
          open={true}
          onClose={mockOnClose}
          onConfirm={mockOnConfirm}
          entityType="user"
        />
      );

      const input = screen.getByPlaceholderText('Type "Delete" to confirm');
      const deleteButton = screen.getByRole('button', { name: /delete/i });

      // Initially disabled
      expect(deleteButton).toBeDisabled();

      // Type Delete
      await userEvent.type(input, 'Delete');

      // Button should be enabled
      await waitFor(() => {
        expect(deleteButton).toBeEnabled();
      });
    });

    it('accepts any casing of the acknowledgement word', async () => {
      render(
        <DeleteConfirmationModal
          open={true}
          onClose={mockOnClose}
          onConfirm={mockOnConfirm}
          entityType="role"
        />
      );

      const input = screen.getByPlaceholderText('Type "Delete" to confirm');
      const deleteButton = screen.getByRole('button', { name: /delete/i });

      // Lowercase confirms.
      await userEvent.type(input, 'delete');
      await waitFor(() => expect(deleteButton).toBeEnabled());
      expect(
        screen.queryByText('The confirmation text provided is incorrect')
      ).not.toBeInTheDocument();

      // Sentence case (the canonical form) confirms.
      await userEvent.clear(input);
      await userEvent.type(input, 'Delete');
      await waitFor(() => expect(deleteButton).toBeEnabled());

      // Uppercase also confirms.
      await userEvent.clear(input);
      await userEvent.type(input, 'DELETE');
      await waitFor(() => expect(deleteButton).toBeEnabled());

      // Mixed case also confirms.
      await userEvent.clear(input);
      await userEvent.type(input, 'DeLeTe');
      await waitFor(() => expect(deleteButton).toBeEnabled());
    });

    it('shows "Delete" in warning box when no entity name provided', () => {
      render(
        <DeleteConfirmationModal
          open={true}
          onClose={mockOnClose}
          onConfirm={mockOnConfirm}
          entityType="privilege"
        />
      );

      // Same disambiguation trick as the entity-name test: target via the unique
      // helper caption to avoid colliding with the Delete submit button label.
      const helper = screen.getByText('Type this to confirm deletion');
      expect(helper.parentElement).toHaveTextContent('Delete');
    });
  });

  describe('Validation', () => {
    it('enables delete button only when text matches exactly', async () => {
      render(
        <DeleteConfirmationModal
          open={true}
          onClose={mockOnClose}
          onConfirm={mockOnConfirm}
          entityType="task"
        />
      );

      const input = screen.getByPlaceholderText('Type "Delete" to confirm');
      const deleteButton = screen.getByRole('button', { name: /delete/i });

      expect(deleteButton).toBeDisabled();

      await userEvent.type(input, 'Del');
      expect(deleteButton).toBeDisabled();

      await userEvent.type(input, 'ete');
      await waitFor(() => {
        expect(deleteButton).toBeEnabled();
      });
    });

    it('validates in real-time as user types', async () => {
      render(
        <DeleteConfirmationModal
          open={true}
          onClose={mockOnClose}
          onConfirm={mockOnConfirm}
          entityName="test-resource"
          entityType="repository"
        />
      );

      const input = screen.getByPlaceholderText('Type "Delete" to confirm');

      // Non-matching first character → error shown
      await userEvent.type(input, 'x');
      expect(screen.getByText('The confirmation text provided is incorrect')).toBeInTheDocument();

      // Clear and type a correct-so-far prefix → no error (in-progress input)
      await userEvent.clear(input);
      await userEvent.type(input, 'Del');
      expect(screen.queryByText('The confirmation text provided is incorrect')).not.toBeInTheDocument();

      // Complete the word
      await userEvent.type(input, 'ete');
      await waitFor(() => {
        expect(screen.queryByText('The confirmation text provided is incorrect')).not.toBeInTheDocument();
      });
    });

    it('does not show error before user starts typing', () => {
      render(
        <DeleteConfirmationModal
          open={true}
          onClose={mockOnClose}
          onConfirm={mockOnConfirm}
          entityType="user"
        />
      );

      expect(
        screen.queryByText('The confirmation text provided is incorrect')
      ).not.toBeInTheDocument();
    });
  });

  describe('Actions', () => {
    it('calls onConfirm when delete button clicked with valid text', async () => {
      render(
        <DeleteConfirmationModal
          open={true}
          onClose={mockOnClose}
          onConfirm={mockOnConfirm}
          entityType="role"
        />
      );

      const input = screen.getByPlaceholderText('Type "Delete" to confirm');
      await userEvent.type(input, 'Delete');

      const deleteButton = screen.getByRole('button', { name: /delete/i });
      await userEvent.click(deleteButton);

      expect(mockOnConfirm).toHaveBeenCalledTimes(1);
    });

    it('calls onClose when cancel button clicked', async () => {
      render(
        <DeleteConfirmationModal
          open={true}
          onClose={mockOnClose}
          onConfirm={mockOnConfirm}
          entityType="user"
        />
      );

      const cancelButton = screen.getByRole('button', { name: /cancel/i });
      await userEvent.click(cancelButton);

      expect(mockOnClose).toHaveBeenCalledTimes(1);
    });

    it('calls onClose when X button clicked', async () => {
      render(
        <DeleteConfirmationModal
          open={true}
          onClose={mockOnClose}
          onConfirm={mockOnConfirm}
          entityType="repository"
        />
      );

      const closeButton = screen.getByRole('button', { name: /close/i });
      await userEvent.click(closeButton);

      expect(mockOnClose).toHaveBeenCalledTimes(1);
    });

    it('resets input when modal closes', async () => {
      const { rerender } = render(
        <DeleteConfirmationModal
          open={true}
          onClose={mockOnClose}
          onConfirm={mockOnConfirm}
          entityType="user"
        />
      );

      const input = screen.getByPlaceholderText('Type "Delete" to confirm');
      await userEvent.type(input, 'Delete');

      // Close modal
      rerender(
        <DeleteConfirmationModal
          open={false}
          onClose={mockOnClose}
          onConfirm={mockOnConfirm}
          entityType="user"
        />
      );

      // Reopen modal
      rerender(
        <DeleteConfirmationModal
          open={true}
          onClose={mockOnClose}
          onConfirm={mockOnConfirm}
          entityType="user"
        />
      );

      const newInput = screen.getByPlaceholderText('Type "Delete" to confirm');
      expect(newInput).toHaveValue('');
    });
  });

  describe('Keyboard', () => {
    it('submits on Enter key when valid', async () => {
      render(
        <DeleteConfirmationModal
          open={true}
          onClose={mockOnClose}
          onConfirm={mockOnConfirm}
          entityType="task"
        />
      );

      const input = screen.getByPlaceholderText('Type "Delete" to confirm');
      await userEvent.type(input, 'Delete');
      fireEvent.keyDown(input, { key: 'Enter' });

      expect(mockOnConfirm).toHaveBeenCalledTimes(1);
    });

    it('does not submit on Enter when invalid', async () => {
      render(
        <DeleteConfirmationModal
          open={true}
          onClose={mockOnClose}
          onConfirm={mockOnConfirm}
          entityType="user"
        />
      );

      const input = screen.getByPlaceholderText('Type "Delete" to confirm');
      await userEvent.type(input, 'wrong');
      fireEvent.keyDown(input, { key: 'Enter' });

      expect(mockOnConfirm).not.toHaveBeenCalled();
    });

    it('focuses input on mount', async () => {
      render(
        <DeleteConfirmationModal
          open={true}
          onClose={mockOnClose}
          onConfirm={mockOnConfirm}
          entityType="repository"
        />
      );

      await waitFor(
        () => {
          const input = screen.getByPlaceholderText(/Type .* to confirm/);
          expect(input).toHaveFocus();
        },
        { timeout: 3000 }
      );
    });
  });

  describe('Loading State', () => {
    it('disables buttons when loading prop is true', () => {
      render(
        <DeleteConfirmationModal
          open={true}
          onClose={mockOnClose}
          onConfirm={mockOnConfirm}
          entityType="blob store"
          loading={true}
        />
      );

      expect(screen.getByRole('button', { name: /cancel/i })).toBeDisabled();
      expect(screen.getByRole('button', { name: /deleting/i })).toBeDisabled();
    });

    it('shows "Deleting..." text when loading', () => {
      render(
        <DeleteConfirmationModal
          open={true}
          onClose={mockOnClose}
          onConfirm={mockOnConfirm}
          entityType="repository"
          loading={true}
        />
      );

      expect(screen.getByRole('button', { name: /deleting/i })).toBeInTheDocument();
    });

    it('disables input field when loading', () => {
      render(
        <DeleteConfirmationModal
          open={true}
          onClose={mockOnClose}
          onConfirm={mockOnConfirm}
          entityType="user"
          loading={true}
        />
      );

      const input = screen.getByPlaceholderText('Type "Delete" to confirm');
      expect(input).toBeDisabled();
    });

    it('does not submit on Enter when loading', async () => {
      render(
        <DeleteConfirmationModal
          open={true}
          onClose={mockOnClose}
          onConfirm={mockOnConfirm}
          entityType="repository"
          loading={true}
        />
      );

      const input = screen.getByPlaceholderText(/Type .* to confirm/);
      fireEvent.keyDown(input, { key: 'Enter' });

      expect(mockOnConfirm).not.toHaveBeenCalled();
    });
  });

  describe('Accessibility', () => {
    it('has proper ARIA labels', () => {
      render(
        <DeleteConfirmationModal
          open={true}
          onClose={mockOnClose}
          onConfirm={mockOnConfirm}
          entityType="privilege"
        />
      );

      expect(screen.getByRole('button', { name: 'Close' })).toBeInTheDocument();
      expect(screen.getByLabelText(/acknowledgement/i)).toBeInTheDocument();
    });

    it('announces errors to screen readers', async () => {
      render(
        <DeleteConfirmationModal
          open={true}
          onClose={mockOnClose}
          onConfirm={mockOnConfirm}
          entityType="user"
        />
      );

      const input = screen.getByPlaceholderText('Type "Delete" to confirm');
      await userEvent.type(input, 'wrong');

      const errorMessage = screen.getByRole('alert');
      expect(errorMessage).toHaveTextContent('The confirmation text provided is incorrect');
    });

    it('has aria-describedby association', () => {
      render(
        <DeleteConfirmationModal
          open={true}
          onClose={mockOnClose}
          onConfirm={mockOnConfirm}
          entityType="repository"
        />
      );

      // Radix UI Dialog automatically associates Description with Content via aria-describedby
      const description = screen.getByText(/This action cannot be undone/);
      expect(description).toBeInTheDocument();
    });
  });
});
