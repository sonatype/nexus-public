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
import { RoutingRuleForm } from '../RoutingRuleForm';
import { useRoutingRulesForm } from '../useRoutingRulesForm';

// Mock hooks
jest.mock('../useRoutingRulesForm');

const mockUseRoutingRulesForm = useRoutingRulesForm as jest.MockedFunction<typeof useRoutingRulesForm>;

function createMockRoutingForm(data: any = {}) {
  return {
    field: jest.fn((name: string) => {
      const value = data[name];
      return { name, value: value != null ? String(value) : '', onChange: jest.fn(), onBlur: jest.fn(), error: undefined };
    }),
    data,
    isPristine: true,
    isSaving: false,
    isLoading: false,
    isDeleting: false,
    saveError: null,
    validationErrors: {},
    state: { matches: jest.fn(() => false), context: { data } },
    send: jest.fn(),
  } as any;
}

// Mock child components - using require and createElement to avoid JSX/TypeScript issues in mock factories
jest.mock('../RoutingRuleMatcher', () => {
  const React = require('react');
  return {
    RoutingRuleMatcher: function MockMatcher(props) {
      return React.createElement('div', { 'data-testid': 'routing-rule-matcher' },
        React.createElement('input', {
          'data-testid': 'matcher-input',
          value: props.matchers[0] || '',
          onChange: function(e) { props.onChange([e.target.value]); }
        }),
        props.error && React.createElement('span', { 'data-testid': 'matcher-error' }, props.error)
      );
    },
  };
});

const mockRule = {
  id: '1',
  name: 'test-rule',
  description: 'Test Description',
  mode: 'BLOCK' as const,
  matchers: ['.*-sources\\.jar'],
  assignedRepositoryCount: 0,
  assignedRepositoryNames: [],
};

const mockRuleWithRepos = {
  ...mockRule,
  assignedRepositoryCount: 2,
  assignedRepositoryNames: ['maven-central', 'maven-snapshots'],
};

function TestWrapper({ children }) {
  return <Theme>{children}</Theme>;
}

describe('RoutingRuleForm', () => {
  const mockOnSave = jest.fn();
  const mockOnCancel = jest.fn();
  const mockOnDelete = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockUseRoutingRulesForm.mockImplementation(({ rule }: any) => {
      const formData = rule ? {
        name: rule.name, description: rule.description || '', mode: rule.mode || 'BLOCK', matchers: rule.matchers || [''],
      } : { name: '', description: '', mode: 'BLOCK', matchers: [''] };
      return {
        form: createMockRoutingForm(formData),
        routingRule: rule || null,
        isCreate: !rule,
        canDelete: rule ? (rule.assignedRepositoryCount ?? 0) === 0 : false,
        matchers: formData.matchers,
        handleMatchersChange: jest.fn(),
        handleModeChange: jest.fn(),
      } as any;
    });
  });

  describe('Create Mode', () => {
    it('should render empty form in create mode', () => {
      render(
        <RoutingRuleForm
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByLabelText(/name/i)).toHaveValue('');
      expect(screen.getByLabelText(/description/i)).toHaveValue('');
    });

    it('should allow editing name in create mode', () => {
      render(
        <RoutingRuleForm
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      const nameInput = screen.getByLabelText(/name/i);
      // In create mode, name should be editable (not disabled/readonly)
      expect(nameInput).not.toBeDisabled();
      // Verify the hook's field was called for the name
      const formMock = mockUseRoutingRulesForm.mock.results[0]?.value?.form;
      expect(formMock?.field).toHaveBeenCalledWith('name');
    });

    it('should show Create button in create mode', () => {
      render(
        <RoutingRuleForm
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByRole('button', { name: /create/i })).toBeInTheDocument();
    });

    it('should validate required fields', async () => {
      // Mock the hook to return validation errors for empty name
      mockUseRoutingRulesForm.mockReturnValue({
        form: {
          ...createMockRoutingForm({ name: '', description: '', mode: 'BLOCK', matchers: [''] }),
          validationErrors: { name: 'Name is required' },
          field: jest.fn((name: string) => ({
            name, value: '', error: name === 'name' ? 'Name is required' : undefined,
            onChange: jest.fn(), onBlur: jest.fn(),
          })),
        },
        routingRule: null,
        isCreate: true,
        canDelete: false,
        matchers: [''],
        handleMatchersChange: jest.fn(),
        handleModeChange: jest.fn(),
      } as any);

      render(
        <RoutingRuleForm isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByText(/name is required/i)).toBeInTheDocument();
    });

    it('should validate name pattern', async () => {
      // Mock the hook to return a pattern validation error
      mockUseRoutingRulesForm.mockReturnValue({
        form: {
          ...createMockRoutingForm({ name: '123-invalid', description: '', mode: 'BLOCK', matchers: ['.*'] }),
          validationErrors: { name: 'Must start with a letter' },
          field: jest.fn((name: string) => ({
            name, value: name === 'name' ? '123-invalid' : '',
            error: name === 'name' ? 'Must start with a letter' : undefined,
            onChange: jest.fn(), onBlur: jest.fn(),
          })),
        },
        routingRule: null,
        isCreate: true,
        canDelete: false,
        matchers: ['.*'],
        handleMatchersChange: jest.fn(),
        handleModeChange: jest.fn(),
      } as any);

      render(
        <RoutingRuleForm isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByText(/must start with a letter/i)).toBeInTheDocument();
    });

    it('should call form.send SUBMIT on create', async () => {
      const mockSend = jest.fn();
      mockUseRoutingRulesForm.mockReturnValue({
        form: {
          ...createMockRoutingForm({ name: 'new-rule', description: 'A description', mode: 'BLOCK', matchers: ['.*\\.jar'] }),
          isPristine: false,
          send: mockSend,
        },
        routingRule: null,
        isCreate: true,
        canDelete: false,
        matchers: ['.*\\.jar'],
        handleMatchersChange: jest.fn(),
        handleModeChange: jest.fn(),
      } as any);

      render(
        <RoutingRuleForm isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} />,
        { wrapper: TestWrapper }
      );

      fireEvent.click(screen.getByRole('button', { name: /create/i }));

      expect(mockSend).toHaveBeenCalledWith('SUBMIT');
    });
  });

  describe('Edit Mode', () => {
    it('should populate form with rule data', () => {
      render(
        <RoutingRuleForm
          rule={mockRule}
          isCreate={false}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          onDelete={mockOnDelete}
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByLabelText(/name/i)).toHaveValue('test-rule');
      expect(screen.getByLabelText(/description/i)).toHaveValue('Test Description');
    });

    it('should disable name field in edit mode', () => {
      render(
        <RoutingRuleForm
          rule={mockRule}
          isCreate={false}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          onDelete={mockOnDelete}
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByLabelText(/name/i)).toHaveAttribute('readonly');
    });

    it('should show Save button in edit mode', () => {
      render(
        <RoutingRuleForm
          rule={mockRule}
          isCreate={false}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          onDelete={mockOnDelete}
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByRole('button', { name: /save/i })).toBeInTheDocument();
    });

    it('should show Delete button when onDelete is provided', () => {
      render(
        <RoutingRuleForm
          rule={mockRule}
          isCreate={false}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          onDelete={mockOnDelete}
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByRole('button', { name: /delete/i })).toBeInTheDocument();
    });

    it('should disable Delete button when rule has assigned repositories', () => {
      render(
        <RoutingRuleForm
          rule={mockRuleWithRepos}
          isCreate={false}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          onDelete={mockOnDelete}
        />,
        { wrapper: TestWrapper }
      );

      const deleteButton = screen.getByRole('button', { name: /delete/i });
      expect(deleteButton).toBeDisabled();
    });

    it('should show warning when rule has assigned repositories', () => {
      render(
        <RoutingRuleForm
          rule={mockRuleWithRepos}
          isCreate={false}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          onDelete={mockOnDelete}
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByText(/assigned to 2 repositories/i)).toBeInTheDocument();
    });

    it('should call onDelete when delete button is clicked', () => {
      render(
        <RoutingRuleForm
          rule={mockRule}
          isCreate={false}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          onDelete={mockOnDelete}
        />,
        { wrapper: TestWrapper }
      );

      fireEvent.click(screen.getByRole('button', { name: /delete/i }));

      expect(mockOnDelete).toHaveBeenCalled();
    });

    it('should disable delete when rule has repositories', () => {
      render(
        <RoutingRuleForm
          rule={mockRuleWithRepos}
          isCreate={false}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          onDelete={mockOnDelete}
        />,
        { wrapper: TestWrapper }
      );

      const deleteButton = screen.getByRole('button', { name: /delete/i });
      expect(deleteButton).toBeDisabled();
    });
  });

  describe('Common Functionality', () => {
    it('should call onCancel when Cancel is clicked', () => {
      render(
        <RoutingRuleForm
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      fireEvent.click(screen.getByRole('button', { name: /cancel/i }));

      expect(mockOnCancel).toHaveBeenCalled();
    });

    it('should show loading state', () => {
      mockUseRoutingRulesForm.mockReturnValue({
        form: { ...createMockRoutingForm({ name: '', description: '', mode: 'BLOCK', matchers: [''] }), isLoading: true },
        routingRule: null,
        isCreate: true,
        canDelete: false,
        matchers: [''],
        handleMatchersChange: jest.fn(),
        handleModeChange: jest.fn(),
      } as any);

      render(
        <RoutingRuleForm isCreate={true} onSave={mockOnSave} onCancel={mockOnCancel} />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByText(/loading form/i)).toBeInTheDocument();
    });

    it('should show error message', () => {
      render(
        <RoutingRuleForm
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          error="Something went wrong"
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByText('Something went wrong')).toBeInTheDocument();
    });

    it('should show help section', () => {
      render(
        <RoutingRuleForm
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByText(/about routing rules/i)).toBeInTheDocument();
    });

    it('should allow changing mode', () => {
      render(
        <RoutingRuleForm
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      const modeSelect = screen.getByLabelText(/mode/i);
      fireEvent.change(modeSelect, { target: { value: 'ALLOW' } });

      expect(modeSelect).toHaveValue('ALLOW');
    });

    it('should call onCancel directly when form is pristine', () => {
      render(
        <RoutingRuleForm
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      const cancelButton = screen.getByText('Cancel');
      fireEvent.click(cancelButton);

      expect(mockOnCancel).toHaveBeenCalled();
    });

    it('should pass pristine state to SettingsForm for discard guard', () => {
      render(
        <RoutingRuleForm
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />,
        { wrapper: TestWrapper }
      );

      const form = screen.getByTestId('routing-rule-form');
      expect(form).toHaveAttribute('data-pristine', 'true');
    });
  });
});

