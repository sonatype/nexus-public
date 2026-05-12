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

import { LoggerForm } from '../LoggerForm';
import * as useLoggingConfigApiModule from '../useLoggingConfigApi';

// Mock the API hook
jest.mock('../useLoggingConfigApi');

// Mock useToast
jest.mock('../../../../../shared', () => ({
  ...jest.requireActual('../../../../../shared'),
  useToast: () => ({
    success: jest.fn(),
    error: jest.fn(),
    warning: jest.fn(),
    info: jest.fn(),
  }),
}));

const mockedUseLoggingConfigApi = useLoggingConfigApiModule.useLoggingConfigApi as jest.MockedFunction<
  typeof useLoggingConfigApiModule.useLoggingConfigApi
>;

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

describe('LoggerForm', () => {
  const mockOnSave = jest.fn();
  const mockOnCancel = jest.fn();
  const mockFetchLogger = jest.fn();
  const mockUpdateLogger = jest.fn();
  const mockResetLogger = jest.fn();
  const mockSetError = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockFetchLogger.mockResolvedValue({ name: 'org.sonatype', level: 'DEBUG', override: true });
    mockUpdateLogger.mockResolvedValue(undefined);
    mockResetLogger.mockResolvedValue(undefined);

    mockedUseLoggingConfigApi.mockReturnValue({
      loading: false,
      error: null,
      setError: mockSetError,
      fetchLoggers: jest.fn(),
      fetchLogger: mockFetchLogger,
      updateLogger: mockUpdateLogger,
      resetLogger: mockResetLogger,
      resetAllLoggers: jest.fn(),
    });
  });

  describe('Create Mode', () => {
    it('renders create form with empty fields', () => {
      render(
        <LoggerForm isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByLabelText(/Logger Name/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Logger Level/i)).toBeInTheDocument();
      expect(screen.getByText('Create Logger')).toBeInTheDocument();
    });

    it('allows editing logger name in create mode', () => {
      render(
        <LoggerForm isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} />,
        { wrapper: TestWrapper }
      );

      const nameInput = screen.getByLabelText(/Logger Name/i);
      expect(nameInput).not.toBeDisabled();

      fireEvent.change(nameInput, { target: { value: 'org.test' } });
      expect(nameInput).toHaveValue('org.test');
    });

    it('submits form with new logger', async () => {
      render(
        <LoggerForm isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} />,
        { wrapper: TestWrapper }
      );

      const nameInput = screen.getByLabelText(/Logger Name/i);
      fireEvent.change(nameInput, { target: { value: 'org.test' } });

      fireEvent.click(screen.getByText('Create Logger'));

      await waitFor(() => {
        expect(mockUpdateLogger).toHaveBeenCalledWith('org.test', 'INFO');
        expect(mockOnSave).toHaveBeenCalled();
      });
    });

    it('does not submit when form is pristine (no name entered)', () => {
      render(
        <LoggerForm isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} />,
        { wrapper: TestWrapper }
      );

      // The submit button exists but clicking it when pristine should not trigger submission
      // This is because SettingsForm wraps the form and prevents submission when pristine
      const submitButton = screen.getByRole('button', { name: /create logger/i });
      fireEvent.click(submitButton);

      // updateLogger should not be called when form is pristine
      expect(mockUpdateLogger).not.toHaveBeenCalled();
      expect(mockOnSave).not.toHaveBeenCalled();
    });
  });

  describe('Edit Mode', () => {
    it('renders loading state while fetching logger', () => {
      mockFetchLogger.mockImplementation(() => new Promise(() => {}));

      render(
        <LoggerForm loggerName="org.sonatype" onSave={mockOnSave} onCancel={mockOnCancel} />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByText('Loading logger...')).toBeInTheDocument();
    });

    it('loads existing logger data', async () => {
      render(
        <LoggerForm loggerName="org.sonatype" onSave={mockOnSave} onCancel={mockOnCancel} />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(mockFetchLogger).toHaveBeenCalledWith('org.sonatype');
      });
    });

    it('disables logger name in edit mode', async () => {
      render(
        <LoggerForm loggerName="org.sonatype" onSave={mockOnSave} onCancel={mockOnCancel} />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        const nameInput = screen.getByLabelText(/Logger Name/i);
        expect(nameInput).toBeDisabled();
      });
    });

    it('shows Delete button in edit mode', async () => {
      render(
        <LoggerForm loggerName="org.sonatype" onSave={mockOnSave} onCancel={mockOnCancel} onDelete={jest.fn()} />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByText('Delete')).toBeInTheDocument();
      });
    });

    it('does not show Delete button in create mode', () => {
      render(
        <LoggerForm isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} />,
        { wrapper: TestWrapper }
      );

      expect(screen.queryByText('Delete')).not.toBeInTheDocument();
    });

    it('calls onDelete when Delete is clicked', async () => {
      const mockOnDelete = jest.fn();
      render(
        <LoggerForm loggerName="org.sonatype" onSave={mockOnSave} onCancel={mockOnCancel} onDelete={mockOnDelete} />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByText('Delete')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText('Delete'));

      expect(mockOnDelete).toHaveBeenCalled();
    });
  });

  describe('Common Behavior', () => {
    it('calls onCancel when Cancel button is clicked', () => {
      render(
        <LoggerForm isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} />,
        { wrapper: TestWrapper }
      );

      // SettingsForm defaults cancelLabel to 'Discard'
      fireEvent.click(screen.getByText('Discard'));

      expect(mockOnCancel).toHaveBeenCalled();
    });

    it('displays error alert when update fails', async () => {
      mockedUseLoggingConfigApi.mockReturnValue({
        loading: false,
        error: 'Failed to update logger',
        setError: mockSetError,
        fetchLoggers: jest.fn(),
        fetchLogger: mockFetchLogger,
        updateLogger: mockUpdateLogger.mockRejectedValue(new Error('Failed')),
        resetLogger: mockResetLogger,
        resetAllLoggers: jest.fn(),
      });

      render(
        <LoggerForm isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByText('Failed to update logger')).toBeInTheDocument();
    });

    it('allows selecting different log levels', () => {
      render(
        <LoggerForm isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} />,
        { wrapper: TestWrapper }
      );

      const levelSelect = screen.getByLabelText(/Logger Level/i);
      expect(levelSelect).toBeInTheDocument();

      fireEvent.change(levelSelect, { target: { value: 'DEBUG' } });
      expect(levelSelect).toHaveValue('DEBUG');
    });

    it('shows Delete button in edit mode', async () => {
      render(
        <LoggerForm loggerName="org.test" onSave={mockOnSave} onCancel={mockOnCancel} onDelete={jest.fn()} />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByText('Delete')).toBeInTheDocument();
      });
      expect(screen.getByTestId('form-delete')).toBeInTheDocument();
    });

    it('does not show Delete button in create mode', () => {
      render(
        <LoggerForm isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} />,
        { wrapper: TestWrapper }
      );

      expect(screen.queryByText('Delete')).not.toBeInTheDocument();
    });

    it('calls onDelete when Delete is clicked', async () => {
      const mockOnDelete = jest.fn();
      render(
        <LoggerForm loggerName="org.test" onSave={mockOnSave} onCancel={mockOnCancel} onDelete={mockOnDelete} />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByText('Delete')).toBeInTheDocument();
      });
      fireEvent.click(screen.getByText('Delete'));

      expect(mockOnDelete).toHaveBeenCalled();
    });

    it('shows inheritance help text below level select', () => {
      render(
        <LoggerForm isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByText(/overrides the inherited level/i)).toBeInTheDocument();
    });
  });

  describe('testId selectors (E2E targeting)', () => {
    it('logger name input has data-testid input-loggerName', () => {
      render(
        <LoggerForm isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} />,
        { wrapper: TestWrapper }
      );
      expect(screen.getByTestId('input-loggerName')).toBeInTheDocument();
    });

    it('logger level select has data-testid select-loggerLevel', () => {
      render(
        <LoggerForm isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} />,
        { wrapper: TestWrapper }
      );
      expect(screen.getByTestId('select-loggerLevel')).toBeInTheDocument();
    });
  });
});


