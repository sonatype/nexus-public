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
import { Theme } from '@radix-ui/themes';
import '@testing-library/jest-dom';

// Mock @sonatype/nexus-ui-plugin BEFORE importing components that use it
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    showSuccessMessage: jest.fn(),
    showErrorMessage: jest.fn(),
  },
}));

// Mock useToast
const mockToastError = jest.fn();
const mockToastSuccess = jest.fn();
jest.mock('../../../../../components/shared', () => ({
  useToast: () => ({
    error: mockToastError,
    success: mockToastSuccess,
  }),
}));

import { CopyUrlButton } from '../CopyUrlButton';
import { ACTION_STRINGS } from '../actions.types';

// Get the mocked module
const { ExtJS } = jest.requireMock('@sonatype/nexus-ui-plugin');

// Helper to wrap components with Radix Theme
function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

// Mock clipboard API
const mockWriteText = jest.fn();
Object.defineProperty(navigator, 'clipboard', {
  value: {
    writeText: mockWriteText,
  },
  writable: true,
});

describe('CopyUrlButton', () => {
  const testUrl = 'https://example.com/repository/artifact.jar';

  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();
    mockWriteText.mockResolvedValue(undefined);
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  describe('rendering', () => {
    it('renders copy button', () => {
      renderWithTheme(<CopyUrlButton url={testUrl} />);
      const button = screen.getByRole('button');
      expect(button).toBeInTheDocument();
    });

    it('has correct aria-label with default text', () => {
      renderWithTheme(<CopyUrlButton url={testUrl} />);
      const button = screen.getByRole('button');
      expect(button).toHaveAttribute('aria-label', ACTION_STRINGS.copyUrl.tooltipText);
    });

    it('has correct aria-label with custom tooltip text', () => {
      const customTooltip = 'Copy download URL';
      renderWithTheme(<CopyUrlButton url={testUrl} tooltipText={customTooltip} />);
      const button = screen.getByRole('button');
      expect(button).toHaveAttribute('aria-label', customTooltip);
    });

    it('applies custom className', () => {
      renderWithTheme(<CopyUrlButton url={testUrl} className="custom-class" />);
      const button = screen.getByRole('button');
      expect(button).toHaveClass('custom-class');
    });

    it('renders disabled when disabled prop is true', () => {
      renderWithTheme(<CopyUrlButton url={testUrl} disabled />);
      const button = screen.getByRole('button');
      expect(button).toBeDisabled();
    });
  });

  describe('copy functionality', () => {
    it('copies URL to clipboard when clicked', async () => {
      renderWithTheme(<CopyUrlButton url={testUrl} />);
      const button = screen.getByRole('button');

      await act(async () => {
        fireEvent.click(button);
      });

      expect(mockWriteText).toHaveBeenCalledWith(testUrl);
    });

    it('shows success message via toast', async () => {
      renderWithTheme(<CopyUrlButton url={testUrl} />);
      const button = screen.getByRole('button');

      await act(async () => {
        fireEvent.click(button);
      });

      expect(mockToastSuccess).toHaveBeenCalledWith(ACTION_STRINGS.copyUrl.successMessage);
    });

    it('shows custom success message when provided', async () => {
      const customMessage = 'Link copied!';
      renderWithTheme(<CopyUrlButton url={testUrl} successMessage={customMessage} />);
      const button = screen.getByRole('button');

      await act(async () => {
        fireEvent.click(button);
      });

      expect(mockToastSuccess).toHaveBeenCalledWith(customMessage);
    });

    it('stops event propagation', async () => {
      const parentClickHandler = jest.fn();
      render(
        <Theme>
          <div onClick={parentClickHandler}>
            <CopyUrlButton url={testUrl} />
          </div>
        </Theme>
      );
      const button = screen.getByRole('button');

      await act(async () => {
        fireEvent.click(button);
      });

      expect(parentClickHandler).not.toHaveBeenCalled();
    });

    it('does not copy when disabled', async () => {
      renderWithTheme(<CopyUrlButton url={testUrl} disabled />);
      const button = screen.getByRole('button');

      await act(async () => {
        fireEvent.click(button);
      });

      expect(mockWriteText).not.toHaveBeenCalled();
    });

    it('does not copy when URL is empty', async () => {
      renderWithTheme(<CopyUrlButton url="" />);
      const button = screen.getByRole('button');

      await act(async () => {
        fireEvent.click(button);
      });

      expect(mockWriteText).not.toHaveBeenCalled();
    });
  });

  describe('visual feedback', () => {
    it('shows copied state after successful copy', async () => {
      renderWithTheme(<CopyUrlButton url={testUrl} />);
      const button = screen.getByRole('button');

      await act(async () => {
        fireEvent.click(button);
      });

      expect(button).toHaveClass('nxrm-copy-url-button--copied');
    });

    it('resets copied state after timeout', async () => {
      renderWithTheme(<CopyUrlButton url={testUrl} />);
      const button = screen.getByRole('button');

      await act(async () => {
        fireEvent.click(button);
      });

      expect(button).toHaveClass('nxrm-copy-url-button--copied');

      // Fast-forward timers
      await act(async () => {
        jest.advanceTimersByTime(2000);
      });

      expect(button).not.toHaveClass('nxrm-copy-url-button--copied');
    });

    it('changes button color to green when copied', async () => {
      renderWithTheme(<CopyUrlButton url={testUrl} />);
      const button = screen.getByRole('button');

      // Before click, should not have green color class
      expect(button).not.toHaveClass('nxrm-copy-url-button--copied');

      await act(async () => {
        fireEvent.click(button);
      });

      // After click, should have copied class
      expect(button).toHaveClass('nxrm-copy-url-button--copied');
    });
  });

  describe('error handling', () => {
    it('shows error message when clipboard fails', async () => {
      mockWriteText.mockRejectedValueOnce(new Error('Clipboard access denied'));

      renderWithTheme(<CopyUrlButton url={testUrl} />);
      const button = screen.getByRole('button');

      await act(async () => {
        fireEvent.click(button);
      });

      expect(mockToastError).toHaveBeenCalledWith(ACTION_STRINGS.copyUrl.errorMessage);
    });

    it('does not show success state when clipboard fails', async () => {
      mockWriteText.mockRejectedValueOnce(new Error('Clipboard access denied'));

      renderWithTheme(<CopyUrlButton url={testUrl} />);
      const button = screen.getByRole('button');

      await act(async () => {
        fireEvent.click(button);
      });

      expect(button).not.toHaveClass('nxrm-copy-url-button--copied');
    });
  });

  describe('rapid clicking', () => {
    it('handles multiple rapid clicks correctly', async () => {
      renderWithTheme(<CopyUrlButton url={testUrl} />);
      const button = screen.getByRole('button');

      // Click multiple times rapidly
      await act(async () => {
        fireEvent.click(button);
      });
      await act(async () => {
        fireEvent.click(button);
      });
      await act(async () => {
        fireEvent.click(button);
      });

      // Should still work correctly
      expect(mockWriteText).toHaveBeenCalledTimes(3);
      expect(button).toHaveClass('nxrm-copy-url-button--copied');
    });
  });

  describe('cleanup', () => {
    it('cleans up timeout on unmount', async () => {
      const { unmount } = renderWithTheme(<CopyUrlButton url={testUrl} />);
      const button = screen.getByRole('button');

      await act(async () => {
        fireEvent.click(button);
      });

      // Unmount before timeout fires
      unmount();

      // Should not throw any errors when advancing timers after unmount
      await act(async () => {
        jest.advanceTimersByTime(2000);
      });
    });
  });
});

