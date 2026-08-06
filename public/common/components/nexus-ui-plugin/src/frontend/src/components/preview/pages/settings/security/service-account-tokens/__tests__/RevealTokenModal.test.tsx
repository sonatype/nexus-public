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
import { render, screen, fireEvent, act } from '@testing-library/react';
import '@testing-library/jest-dom';

import { RevealTokenModal } from '../RevealTokenModal';
import { SERVICE_ACCOUNT_TOKENS_STRINGS } from '../strings';

const LABELS = SERVICE_ACCOUNT_TOKENS_STRINGS.REVEAL_MODAL;

describe('RevealTokenModal', () => {
  const defaultProps = {
    open: true,
    token: 'sat.test-token-abc123',
    onClose: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  describe('rendering', () => {
    it('renders when open', () => {
      render(<RevealTokenModal {...defaultProps} />);
      expect(screen.getByTestId('sat-reveal-modal')).toBeInTheDocument();
    });

    it('does not render when closed', () => {
      render(<RevealTokenModal {...defaultProps} open={false} />);
      expect(screen.queryByTestId('sat-reveal-modal')).not.toBeInTheDocument();
    });

    it('displays the token value', () => {
      render(<RevealTokenModal {...defaultProps} />);
      expect(screen.getByTestId('sat-token-value')).toHaveTextContent('sat.test-token-abc123');
    });
  });

  describe('countdown', () => {
    it('shows countdown text aligned with the Done button', () => {
      render(<RevealTokenModal {...defaultProps} />);

      const countdown = screen.getByTestId('sat-reveal-countdown');
      expect(countdown).toHaveTextContent(LABELS.AUTO_CLOSE_NOTICE(60));
    });

    it('countdown decrements over time', () => {
      render(<RevealTokenModal {...defaultProps} />);

      // Initial state
      expect(screen.getByText(LABELS.AUTO_CLOSE_NOTICE(60))).toBeInTheDocument();

      // Advance 1 second
      act(() => {
        jest.advanceTimersByTime(1000);
      });

      expect(screen.getByText(LABELS.AUTO_CLOSE_NOTICE(59))).toBeInTheDocument();
    });

    it('calls onClose when countdown reaches 0', () => {
      const onClose = jest.fn();
      render(<RevealTokenModal {...defaultProps} onClose={onClose} />);

      // Fast-forward 60 seconds
      act(() => {
        jest.advanceTimersByTime(60000);
      });

      expect(onClose).toHaveBeenCalled();
    });
  });

  describe('copy functionality', () => {
    it('has a copy button', () => {
      render(<RevealTokenModal {...defaultProps} />);
      expect(screen.getByTestId('sat-token-copy')).toBeInTheDocument();
    });

    it('copies token to clipboard when copy button is clicked', async () => {
      const mockClipboard = {
        writeText: jest.fn().mockResolvedValue(undefined),
      };
      Object.assign(navigator, { clipboard: mockClipboard });

      render(<RevealTokenModal {...defaultProps} />);

      const copyButton = screen.getByTestId('sat-token-copy');
      fireEvent.click(copyButton);

      expect(mockClipboard.writeText).toHaveBeenCalledWith('sat.test-token-abc123');
    });
  });

  describe('copy announcement for screen readers', () => {
    it('has a visually hidden status region for copy announcement', () => {
      render(<RevealTokenModal {...defaultProps} />);

      // There should be a span with role="status" for the SR announcement
      const statusRegion = document.querySelector('[role="status"]');
      expect(statusRegion).toBeInTheDocument();
    });

    it('announces copy success text after copying', async () => {
      const mockClipboard = {
        writeText: jest.fn().mockResolvedValue(undefined),
      };
      Object.assign(navigator, { clipboard: mockClipboard });

      render(<RevealTokenModal {...defaultProps} />);

      // Initially, the status should be empty
      const statusRegion = document.querySelector('[role="status"]');
      expect(statusRegion).toHaveTextContent('');

      // Click copy
      const copyButton = screen.getByTestId('sat-token-copy');
      fireEvent.click(copyButton);

      // Wait for clipboard and state update
      await act(async () => {
        await Promise.resolve();
      });

      // Now it should announce the copy success
      expect(statusRegion).toHaveTextContent(LABELS.COPY_ANNOUNCEMENT);
    });
  });

  describe('warning callout', () => {
    it('has role="alert" on the warning callout', () => {
      render(<RevealTokenModal {...defaultProps} />);

      const callout = screen.getByRole('alert');
      expect(callout).toBeInTheDocument();
    });

    it('contains the warning message in the callout, countdown rendered alongside the Done button', () => {
      render(<RevealTokenModal {...defaultProps} />);

      const callout = screen.getByRole('alert');
      expect(callout).toHaveTextContent(LABELS.WARNING);
      expect(screen.getByTestId('sat-reveal-countdown')).toHaveTextContent(LABELS.AUTO_CLOSE_NOTICE(60));
    });
  });

  describe('accessibility', () => {
    it('has Dialog.Title for screen readers', () => {
      render(<RevealTokenModal {...defaultProps} />);
      expect(screen.getByText(LABELS.TITLE)).toBeInTheDocument();
    });

    it('has Dialog.Description for screen readers', () => {
      render(<RevealTokenModal {...defaultProps} />);
      // The description is in a VisuallyHidden component
      expect(screen.getByText(LABELS.DIALOG_DESCRIPTION)).toBeInTheDocument();
    });
  });
});
