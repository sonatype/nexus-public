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
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';

import { ContentSelectorForm } from '../ContentSelectorForm';
import { ContentSelector } from '../types';
import { ToastProvider } from '../../../../../shared/Toast';

// Mock child components
jest.mock('../ContentSelectorPreview', () => ({
  ContentSelectorPreview: () => <div data-testid="content-selector-preview">Preview</div>,
}));

jest.mock('../CSELEditor', () => {
  const React = require('react');
  return {
    CSELEditor: ({
      value,
      onChange,
      onValidationChange,
      placeholder,
    }: {
      value: string;
      onChange: (value: string) => void;
      onValidationChange: (result: any) => void;
      placeholder?: string;
    }) => {
      // Call onValidationChange to report valid state when there's content
      React.useEffect(() => {
        onValidationChange({ hasBlockingErrors: false, messages: [] });
      }, [value, onValidationChange]);

      return (
        <textarea
          data-testid="csel-editor"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
        />
      );
    },
  };
});

jest.mock('../ExpressionPreview', () => ({
  ExpressionPreview: ({ expression }: { expression: string }) => (
    <div data-testid="expression-preview">Expression: {expression}</div>
  ),
}));

// Mock the API hook used internally by the form
// Variables used in mock must have 'mock' prefix for hoisting
const mockCreateContentSelector = jest.fn().mockResolvedValue({});
const mockUpdateContentSelector = jest.fn().mockResolvedValue({});
const mockDeleteContentSelector = jest.fn().mockResolvedValue(undefined);

jest.mock('../useContentSelectorsApi', () => ({
  useContentSelectorsApi: () => ({
    loading: false,
    error: null,
    setError: jest.fn(),
    fetchContentSelectors: jest.fn().mockResolvedValue([]),
    fetchContentSelector: jest.fn().mockResolvedValue(null),
    fetchRepositories: jest.fn().mockResolvedValue([]),
    createContentSelector: mockCreateContentSelector,
    updateContentSelector: mockUpdateContentSelector,
    deleteContentSelector: mockDeleteContentSelector,
    previewContentSelector: jest.fn().mockResolvedValue([]),
    fetchPrivilegesForSelector: jest.fn().mockResolvedValue([]),
  }),
}));

// Mock nexus-ui-plugin for the form machine
jest.mock('@sonatype/nexus-ui-plugin', () => {
  const actual = jest.requireActual('@sonatype/nexus-ui-plugin');
  return {
    ...actual,
    createFormMachine: actual.createFormMachine,
    useForm: actual.useForm,
    ENDPOINTS: {
      CONTENT_SELECTORS: '/service/rest/v1/security/content-selectors',
    },
    restClient: {
      get: jest.fn().mockResolvedValue(null),
      post: jest.fn().mockResolvedValue({}),
      put: jest.fn().mockResolvedValue({}),
      delete: jest.fn().mockResolvedValue(undefined),
    },
  };
});

// Mock the shared module
jest.mock('../../../../../shared', () => ({
  useToast: () => ({
    success: jest.fn(),
    error: jest.fn(),
    info: jest.fn(),
  }),
  useUnsavedChangesWarning: jest.fn(),
  clearDirtyState: jest.fn(),
}));

// Wrapper component for Radix Theme and Toast context
function TestWrapper({ children }: { children: React.ReactNode }) {
  return (
    <Theme>
      <ToastProvider>{children}</ToastProvider>
    </Theme>
  );
}

describe('ContentSelectorForm', () => {
  const mockOnCancel = jest.fn();
  const mockOnComplete = jest.fn();

  const mockSelector: ContentSelector = {
    name: 'test-selector',
    type: 'csel',
    description: 'Test description',
    expression: 'format == "maven2"',
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('create mode', () => {
    it('renders empty form in create mode', async () => {
      render(
        <ContentSelectorForm
          isCreate={true}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByText('Selector Settings')).toBeInTheDocument();
      });
      expect(screen.getByLabelText(/Name/i)).toBeInTheDocument();
      expect(screen.getByTestId('csel-editor')).toBeInTheDocument();
    });

    it('shows Create button in create mode', async () => {
      render(
        <ContentSelectorForm
          isCreate={true}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Create/i })).toBeInTheDocument();
      });
    });

    it('allows editing name in create mode', async () => {
      render(
        <ContentSelectorForm
          isCreate={true}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByLabelText(/Name/i)).toBeInTheDocument();
      });

      const nameInput = screen.getByLabelText(/Name/i);
      await userEvent.type(nameInput, 'new-selector');

      expect(nameInput).toHaveValue('new-selector');
    });
  });

  describe('edit mode', () => {
    it('renders form with selector data in edit mode', async () => {
      render(
        <ContentSelectorForm
          selector={mockSelector}
          isCreate={false}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByText('test-selector')).toBeInTheDocument();
      });
    });

    it('shows Save button in edit mode', async () => {
      render(
        <ContentSelectorForm
          selector={mockSelector}
          isCreate={false}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Save/i })).toBeInTheDocument();
      });
    });

    it('displays name as read-only in edit mode', async () => {
      render(
        <ContentSelectorForm
          selector={mockSelector}
          isCreate={false}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        // Name should be displayed as text, not input
        expect(screen.getByText('test-selector')).toBeInTheDocument();
      });
    });
  });

  describe('form submission', () => {
    it('validates name is required before submission', async () => {
      render(
        <ContentSelectorForm
          isCreate={true}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByTestId('csel-editor')).toBeInTheDocument();
      });

      // Fill in expression but not name
      const expressionInput = screen.getByTestId('csel-editor');
      await userEvent.type(expressionInput, 'format == "npm"');

      // Submit
      const submitButton = screen.getByRole('button', { name: /Create/i });
      await userEvent.click(submitButton);

      // Validation should prevent save
      expect(mockCreateContentSelector).not.toHaveBeenCalled();
    });

    it('validates expression is required before submission', async () => {
      render(
        <ContentSelectorForm
          isCreate={true}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByLabelText(/Name/i)).toBeInTheDocument();
      });

      // Fill in name but not expression
      const nameInput = screen.getByLabelText(/Name/i);
      await userEvent.type(nameInput, 'new-selector');

      // Submit
      const submitButton = screen.getByRole('button', { name: /Create/i });
      await userEvent.click(submitButton);

      // Validation should prevent save
      expect(mockCreateContentSelector).not.toHaveBeenCalled();
    });
  });

  describe('delete functionality', () => {
    it('shows delete button when canDelete is true', async () => {
      render(
        <ContentSelectorForm
          selector={mockSelector}
          isCreate={false}
          canDelete={true}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Delete/i })).toBeInTheDocument();
      });
    });

    it('hides delete button when canDelete is false', async () => {
      render(
        <ContentSelectorForm
          selector={mockSelector}
          isCreate={false}
          canDelete={false}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByText('test-selector')).toBeInTheDocument();
      });

      expect(screen.queryByRole('button', { name: /Delete/i })).not.toBeInTheDocument();
    });

    it('suppresses unsaved changes indicator during delete flow', async () => {
      render(
        <ContentSelectorForm
          selector={mockSelector}
          isCreate={false}
          canDelete={true}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Delete/i })).toBeInTheDocument();
      });

      const deleteButton = screen.getByRole('button', { name: /Delete/i });
      await userEvent.click(deleteButton);

      await waitFor(() => {
        expect(screen.getByText(/Delete Content Selector/i)).toBeInTheDocument();
      });

      const form = screen.getByTestId('content-selector-form');
      expect(form).toHaveAttribute('data-pristine', 'true');
    });

    it('shows confirmation when delete button is clicked', async () => {
      render(
        <ContentSelectorForm
          selector={mockSelector}
          isCreate={false}
          canDelete={true}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Delete/i })).toBeInTheDocument();
      });

      const deleteButton = screen.getByRole('button', { name: /Delete/i });
      await userEvent.click(deleteButton);

      await waitFor(() => {
        expect(screen.getByText(/Delete Content Selector/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Delete/i })).toBeInTheDocument();
      });
    });
  });

  describe('loading state', () => {
    it('shows loading message when loading is true', async () => {
      render(
        <ContentSelectorForm
          isCreate={true}
          onCancel={mockOnCancel}
          loading={true}
        />,
        { wrapper: TestWrapper }
      );

      // The machine also shows loading while it loads - check for either
      await waitFor(() => {
        const loadingText = screen.queryByText('Loading form...') || screen.queryByText('Loading...');
        expect(loadingText).toBeInTheDocument();
      });
    });
  });

  describe('error display', () => {
    it('displays error message when error prop is provided', async () => {
      render(
        <ContentSelectorForm
          isCreate={true}
          onCancel={mockOnCancel}
          error="Failed to save content selector"
        />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByText('Failed to save content selector')).toBeInTheDocument();
      });
    });
  });

  describe('example expressions', () => {
    it('renders example expressions section', async () => {
      render(
        <ContentSelectorForm
          isCreate={true}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByText('Example Expressions')).toBeInTheDocument();
        expect(screen.getByText('Click to use as starting point')).toBeInTheDocument();
      });
    });
  });

  describe('CSEL reference documentation', () => {
    it('renders collapsible reference section', async () => {
      render(
        <ContentSelectorForm
          isCreate={true}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByText('CSEL Reference Documentation')).toBeInTheDocument();
      });
    });

    it('expands reference documentation when clicked', async () => {
      render(
        <ContentSelectorForm
          isCreate={true}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByText('CSEL Reference Documentation')).toBeInTheDocument();
      });

      const referenceToggle = screen.getByText('CSEL Reference Documentation');
      await userEvent.click(referenceToggle);

      await waitFor(() => {
        expect(screen.getByText('Available Attributes')).toBeInTheDocument();
      });
    });
  });

  describe('expression preview', () => {
    it('renders expression preview component', async () => {
      render(
        <ContentSelectorForm
          selector={mockSelector}
          isCreate={false}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByTestId('expression-preview')).toBeInTheDocument();
      });
    });
  });

  describe('content selector preview', () => {
    it('shows preview when expression is valid', async () => {
      render(
        <ContentSelectorForm
          selector={mockSelector}
          isCreate={false}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByTestId('content-selector-preview')).toBeInTheDocument();
      });
    });
  });

  describe('form data-testid attributes', () => {
    it('has correct data-testid on form', async () => {
      render(
        <ContentSelectorForm
          isCreate={true}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByTestId('content-selector-form')).toBeInTheDocument();
      });
    });

    it('has correct data-mode attribute in create mode', async () => {
      render(
        <ContentSelectorForm
          isCreate={true}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByTestId('content-selector-form')).toHaveAttribute(
          'data-mode',
          'create'
        );
      });
    });

    it('has correct data-mode attribute in edit mode', async () => {
      render(
        <ContentSelectorForm
          selector={mockSelector}
          isCreate={false}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      await waitFor(() => {
        expect(screen.getByTestId('content-selector-form')).toHaveAttribute(
          'data-mode',
          'edit'
        );
      });
    });
  });
});
