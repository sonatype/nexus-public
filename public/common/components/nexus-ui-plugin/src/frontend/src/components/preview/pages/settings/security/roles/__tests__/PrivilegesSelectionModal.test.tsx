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
import { Theme } from '@radix-ui/themes';
import { PrivilegesSelectionModal } from '../PrivilegesSelectionModal';
import { PrivilegeReference } from '../types';

const mockAvailablePrivileges: PrivilegeReference[] = [
  { id: 'nx-all', name: 'nx-all' },
  { id: 'nx-search-read', name: 'nx-search-read' },
  { id: 'nx-repository-view-*', name: 'nx-repository-view-*' },
  { id: 'nx-admin', name: 'nx-admin' },
  { id: 'nx-repository-admin', name: 'nx-repository-admin' },
];

const renderWithTheme = (component: React.ReactNode) => {
  return render(<Theme>{component}</Theme>);
};

describe('PrivilegesSelectionModal', () => {
  const mockOnClose = jest.fn();
  const mockOnSave = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('Modal Visibility', () => {
    it('should render modal when isOpen is true', () => {
      renderWithTheme(
        <PrivilegesSelectionModal
          isOpen={true}
          onClose={mockOnClose}
          availablePrivileges={mockAvailablePrivileges}
          selectedPrivileges={[]}
          onSave={mockOnSave}
        />
      );

      expect(screen.getByText('Select Privileges')).toBeInTheDocument();
      expect(screen.getByText('Select privileges to grant to this role. Double-click or use arrows to move items.')).toBeInTheDocument();
    });

    it('should not render modal when isOpen is false', () => {
      renderWithTheme(
        <PrivilegesSelectionModal
          isOpen={false}
          onClose={mockOnClose}
          availablePrivileges={mockAvailablePrivileges}
          selectedPrivileges={[]}
          onSave={mockOnSave}
        />
      );

      expect(screen.queryByText('Select Privileges')).not.toBeInTheDocument();
    });
  });

  describe('Modal Actions', () => {
    it('should call onClose when Cancel button is clicked', () => {
      renderWithTheme(
        <PrivilegesSelectionModal
          isOpen={true}
          onClose={mockOnClose}
          availablePrivileges={mockAvailablePrivileges}
          selectedPrivileges={[]}
          onSave={mockOnSave}
        />
      );

      fireEvent.click(screen.getByText('Cancel'));
      expect(mockOnClose).toHaveBeenCalled();
    });

    it('should call onClose when X button is clicked', () => {
      renderWithTheme(
        <PrivilegesSelectionModal
          isOpen={true}
          onClose={mockOnClose}
          availablePrivileges={mockAvailablePrivileges}
          selectedPrivileges={[]}
          onSave={mockOnSave}
        />
      );

      const closeButton = screen.getByLabelText('Close');
      fireEvent.click(closeButton);
      expect(mockOnClose).toHaveBeenCalled();
    });

    it('should call onSave with selection and close modal when Apply Selection is clicked', () => {
      const selectedPrivileges = ['nx-search-read', 'nx-admin'];

      renderWithTheme(
        <PrivilegesSelectionModal
          isOpen={true}
          onClose={mockOnClose}
          availablePrivileges={mockAvailablePrivileges}
          selectedPrivileges={selectedPrivileges}
          onSave={mockOnSave}
        />
      );

      fireEvent.click(screen.getByText('Apply Selection'));

      expect(mockOnSave).toHaveBeenCalledWith(selectedPrivileges);
      expect(mockOnClose).toHaveBeenCalled();
    });
  });

  describe('Selection State', () => {
    it('should display available privileges', () => {
      renderWithTheme(
        <PrivilegesSelectionModal
          isOpen={true}
          onClose={mockOnClose}
          availablePrivileges={mockAvailablePrivileges}
          selectedPrivileges={[]}
          onSave={mockOnSave}
        />
      );

      expect(screen.getByText('Available Privileges')).toBeInTheDocument();
    });

    it('should display selected privileges label', () => {
      renderWithTheme(
        <PrivilegesSelectionModal
          isOpen={true}
          onClose={mockOnClose}
          availablePrivileges={mockAvailablePrivileges}
          selectedPrivileges={[]}
          onSave={mockOnSave}
        />
      );

      expect(screen.getByText('Selected Privileges')).toBeInTheDocument();
    });

    it('should reset selection when modal reopens', async () => {
      const { rerender } = renderWithTheme(
        <PrivilegesSelectionModal
          isOpen={true}
          onClose={mockOnClose}
          availablePrivileges={mockAvailablePrivileges}
          selectedPrivileges={['nx-search-read']}
          onSave={mockOnSave}
        />
      );

      // Close modal
      rerender(
        <Theme>
          <PrivilegesSelectionModal
            isOpen={false}
            onClose={mockOnClose}
            availablePrivileges={mockAvailablePrivileges}
            selectedPrivileges={['nx-search-read']}
            onSave={mockOnSave}
          />
        </Theme>
      );

      // Reopen with different selection
      rerender(
        <Theme>
          <PrivilegesSelectionModal
            isOpen={true}
            onClose={mockOnClose}
            availablePrivileges={mockAvailablePrivileges}
            selectedPrivileges={['nx-admin', 'nx-all']}
            onSave={mockOnSave}
          />
        </Theme>
      );

      // Click Apply - should save the new selection
      fireEvent.click(screen.getByText('Apply Selection'));
      expect(mockOnSave).toHaveBeenCalledWith(['nx-admin', 'nx-all']);
    });

    it('should reset to original selection when Cancel is clicked', () => {
      const originalSelection = ['nx-search-read'];

      renderWithTheme(
        <PrivilegesSelectionModal
          isOpen={true}
          onClose={mockOnClose}
          availablePrivileges={mockAvailablePrivileges}
          selectedPrivileges={originalSelection}
          onSave={mockOnSave}
        />
      );

      fireEvent.click(screen.getByText('Cancel'));

      expect(mockOnSave).not.toHaveBeenCalled();
      expect(mockOnClose).toHaveBeenCalled();
    });
  });

  describe('Loading State', () => {
    it('should show loading indicator when loading is true', () => {
      renderWithTheme(
        <PrivilegesSelectionModal
          isOpen={true}
          onClose={mockOnClose}
          availablePrivileges={mockAvailablePrivileges}
          selectedPrivileges={[]}
          onSave={mockOnSave}
          loading={true}
        />
      );

      expect(screen.getByText('Loading privileges...')).toBeInTheDocument();
    });

    it('should disable Apply Selection button when loading', () => {
      renderWithTheme(
        <PrivilegesSelectionModal
          isOpen={true}
          onClose={mockOnClose}
          availablePrivileges={mockAvailablePrivileges}
          selectedPrivileges={[]}
          onSave={mockOnSave}
          loading={true}
        />
      );

      // Use getByRole to get the actual button, not the text span inside it
      const applyButton = screen.getByRole('button', { name: 'Apply Selection' });
      expect(applyButton).toBeDisabled();
    });

    it('should not show transfer list when loading', () => {
      renderWithTheme(
        <PrivilegesSelectionModal
          isOpen={true}
          onClose={mockOnClose}
          availablePrivileges={mockAvailablePrivileges}
          selectedPrivileges={[]}
          onSave={mockOnSave}
          loading={true}
        />
      );

      expect(screen.queryByText('Available Privileges')).not.toBeInTheDocument();
    });

    it('should show transfer list when not loading', () => {
      renderWithTheme(
        <PrivilegesSelectionModal
          isOpen={true}
          onClose={mockOnClose}
          availablePrivileges={mockAvailablePrivileges}
          selectedPrivileges={[]}
          onSave={mockOnSave}
          loading={false}
        />
      );

      expect(screen.getByText('Available Privileges')).toBeInTheDocument();
    });
  });

  describe('Empty States', () => {
    it('should handle empty available privileges', () => {
      renderWithTheme(
        <PrivilegesSelectionModal
          isOpen={true}
          onClose={mockOnClose}
          availablePrivileges={[]}
          selectedPrivileges={[]}
          onSave={mockOnSave}
        />
      );

      expect(screen.getByText('Select Privileges')).toBeInTheDocument();
    });

    it('should handle all privileges selected', () => {
      const allPrivilegeIds = mockAvailablePrivileges.map(p => p.id);

      renderWithTheme(
        <PrivilegesSelectionModal
          isOpen={true}
          onClose={mockOnClose}
          availablePrivileges={mockAvailablePrivileges}
          selectedPrivileges={allPrivilegeIds}
          onSave={mockOnSave}
        />
      );

      fireEvent.click(screen.getByText('Apply Selection'));
      expect(mockOnSave).toHaveBeenCalledWith(allPrivilegeIds);
    });
  });
});
