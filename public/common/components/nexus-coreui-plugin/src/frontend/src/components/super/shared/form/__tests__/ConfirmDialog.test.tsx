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
import { ConfirmDialog } from '../ConfirmDialog';

function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

describe('ConfirmDialog', () => {
  const defaultProps = {
    open: true,
    onOpenChange: jest.fn(),
    title: 'Test Title',
    message: 'Test message',
    onConfirm: jest.fn(),
  };

  beforeEach(() => jest.clearAllMocks());

  it('renders dialog when open', () => {
    renderWithTheme(<ConfirmDialog {...defaultProps} />);
    expect(screen.getByRole('alertdialog')).toBeInTheDocument();
    expect(screen.getByText('Test Title')).toBeInTheDocument();
    expect(screen.getByText('Test message')).toBeInTheDocument();
  });

  it('does not render when closed', () => {
    renderWithTheme(<ConfirmDialog {...defaultProps} open={false} />);
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
  });

  it('calls onConfirm when confirm button clicked', () => {
    renderWithTheme(<ConfirmDialog {...defaultProps} confirmLabel="Delete" />);
    fireEvent.click(screen.getByText('Delete'));
    expect(defaultProps.onConfirm).toHaveBeenCalledTimes(1);
  });

  it('uses default button labels', () => {
    renderWithTheme(<ConfirmDialog {...defaultProps} />);
    expect(screen.getByText('Confirm')).toBeInTheDocument();
    expect(screen.getByText('Cancel')).toBeInTheDocument();
  });

  it('uses custom button labels', () => {
    renderWithTheme(<ConfirmDialog {...defaultProps} confirmLabel="Yes" cancelLabel="No" />);
    expect(screen.getByText('Yes')).toBeInTheDocument();
    expect(screen.getByText('No')).toBeInTheDocument();
  });

  it('disables buttons when loading', () => {
    renderWithTheme(<ConfirmDialog {...defaultProps} loading={true} confirmLabel="Save" cancelLabel="Cancel" />);
    expect(screen.getByText('Cancel').closest('button')).toBeDisabled();
    expect(screen.getByText('Save').closest('button')).toBeDisabled();
  });

  it('renders children content', () => {
    renderWithTheme(
      <ConfirmDialog {...defaultProps}>
        <p data-testid="extra-content">Extra warning</p>
      </ConfirmDialog>
    );
    expect(screen.getByTestId('extra-content')).toBeInTheDocument();
  });

  it('accepts ReactNode as message', () => {
    renderWithTheme(
      <ConfirmDialog {...defaultProps} message={<span data-testid="jsx-msg">Bold warning</span>} />
    );
    expect(screen.getByTestId('jsx-msg')).toBeInTheDocument();
  });

  it('renders warning variant icon styling', () => {
    renderWithTheme(<ConfirmDialog {...defaultProps} variant="warning" />);
    expect(screen.getByRole('alertdialog')).toBeInTheDocument();
  });

  describe('button layout and variant (M-4, N-7)', () => {
    it('cancel button uses variant="surface" not variant="soft" (M-4)', () => {
      renderWithTheme(<ConfirmDialog {...defaultProps} />);
      const cancelBtn = screen.getByText('Cancel').closest('button');
      // Radix Button with variant="surface" applies rt-variant-surface class
      expect(cancelBtn).toHaveClass('rt-variant-surface');
      expect(cancelBtn).not.toHaveClass('rt-variant-soft');
    });

    it('button order in DOM matches visual order for accessibility (N-7)', () => {
      renderWithTheme(<ConfirmDialog {...defaultProps} confirmLabel="Delete" cancelLabel="Cancel" />);
      const buttons = screen.getAllByRole('button');
      const deleteIdx = buttons.findIndex((b) => b.textContent === 'Delete');
      const cancelIdx = buttons.findIndex((b) => b.textContent === 'Cancel');
      // DOM order should match visual order: Cancel (left) then Delete (right)
      expect(cancelIdx).toBeLessThan(deleteIdx);
    });
  });

  describe('testId props', () => {
    it('sets data-testid on cancel button from cancelTestId', () => {
      renderWithTheme(<ConfirmDialog {...defaultProps} cancelTestId="my-cancel" />);
      expect(screen.getByTestId('my-cancel')).toBeInTheDocument();
      expect(screen.getByTestId('my-cancel')).toHaveTextContent('Cancel');
    });

    it('sets data-testid on confirm button from confirmTestId', () => {
      renderWithTheme(<ConfirmDialog {...defaultProps} confirmTestId="my-confirm" />);
      expect(screen.getByTestId('my-confirm')).toBeInTheDocument();
      expect(screen.getByTestId('my-confirm')).toHaveTextContent('Confirm');
    });

    it('derives cancel and confirm testIds from testId prop', () => {
      renderWithTheme(<ConfirmDialog {...defaultProps} testId="delete-user-dialog" />);
      expect(screen.getByTestId('delete-user-dialog-cancel')).toBeInTheDocument();
      expect(screen.getByTestId('delete-user-dialog-confirm')).toBeInTheDocument();
    });

    it('explicit cancelTestId/confirmTestId take precedence over derived testId', () => {
      renderWithTheme(
        <ConfirmDialog
          {...defaultProps}
          testId="base-dialog"
          cancelTestId="explicit-cancel"
          confirmTestId="explicit-confirm"
        />
      );
      expect(screen.getByTestId('explicit-cancel')).toBeInTheDocument();
      expect(screen.getByTestId('explicit-confirm')).toBeInTheDocument();
      expect(screen.queryByTestId('base-dialog-cancel')).not.toBeInTheDocument();
    });

    it('renders without testId props without error', () => {
      renderWithTheme(<ConfirmDialog {...defaultProps} />);
      expect(screen.getByText('Cancel')).toBeInTheDocument();
      expect(screen.getByText('Confirm')).toBeInTheDocument();
    });
  });
});
