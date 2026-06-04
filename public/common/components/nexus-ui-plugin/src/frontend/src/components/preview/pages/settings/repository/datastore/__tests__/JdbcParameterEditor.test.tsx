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

import { JdbcParameterEditor, JdbcParameter, ParameterValidation, isKnownParameter } from '../JdbcParameterEditor';

function renderComponent(props: Partial<React.ComponentProps<typeof JdbcParameterEditor>> = {}) {
  const defaultProps = {
    parameters: [],
    onChange: jest.fn(),
  };

  return render(
    <Theme>
      <JdbcParameterEditor {...defaultProps} {...props} />
    </Theme>
  );
}

const getUser = () => {
  return typeof (userEvent as any).setup === 'function' ? (userEvent as any).setup() : userEvent;
};

describe('JdbcParameterEditor', () => {
  describe('Empty State', () => {
    it('displays empty state when no parameters', () => {
      renderComponent({ parameters: [] });

      expect(screen.getByText(/no advanced parameters configured/i)).toBeInTheDocument();
    });

    it('shows add parameter button', () => {
      renderComponent({ parameters: [] });

      expect(screen.getByRole('button', { name: /add parameter/i })).toBeInTheDocument();
    });
  });

  describe('Parameter Display', () => {
    const mockParameters: JdbcParameter[] = [
      { id: '1', name: 'socketTimeout', value: '30000', isDefault: false, isCustom: true },
      { id: '2', name: 'connectTimeout', value: '5000', isDefault: false, isCustom: true },
    ];

    it('displays parameter rows', () => {
      renderComponent({ parameters: mockParameters });

      expect(screen.getByDisplayValue('socketTimeout')).toBeInTheDocument();
      expect(screen.getByDisplayValue('30000')).toBeInTheDocument();
      expect(screen.getByDisplayValue('connectTimeout')).toBeInTheDocument();
      expect(screen.getByDisplayValue('5000')).toBeInTheDocument();
    });

    it('shows Custom badge for custom parameters', () => {
      renderComponent({ parameters: mockParameters });

      const badges = screen.getAllByText('Custom');
      expect(badges).toHaveLength(2);
    });

    it('shows Default badge for default parameters', () => {
      const defaultParams: JdbcParameter[] = [
        { id: '1', name: 'defaultParam', value: 'value', isDefault: true, isCustom: false },
      ];

      renderComponent({ parameters: defaultParams });

      expect(screen.getByText('Default')).toBeInTheDocument();
    });

    it('shows description for known parameters', () => {
      renderComponent({ parameters: mockParameters });

      // Description format: "Time to wait for socket read operations (ms). Default: 0"
      expect(screen.getByText(/time to wait for socket read/i)).toBeInTheDocument();
    });

    it('shows warning for unknown parameters', () => {
      const unknownParams: JdbcParameter[] = [
        { id: '1', name: 'unknownParam', value: 'value', isDefault: false, isCustom: true },
      ];

      renderComponent({ parameters: unknownParams });

      // Multiple matches may exist (help text + row warning), so use getAllByText
      const warnings = screen.getAllByText(/unknown parameter/i);
      expect(warnings.length).toBeGreaterThan(0);
    });
  });

  describe('Adding Parameters', () => {
    it('calls onChange with new parameter when Add Parameter is clicked', async () => {
      const user = getUser();
      const mockOnChange = jest.fn();
      
      renderComponent({ parameters: [], onChange: mockOnChange });

      await user.click(screen.getByRole('button', { name: /add parameter/i }));

      expect(mockOnChange).toHaveBeenCalledWith(
        expect.arrayContaining([
          expect.objectContaining({
            name: '',
            value: '',
            isDefault: false,
            isCustom: true,
          }),
        ])
      );
    });
  });

  describe('Removing Parameters', () => {
    it('calls onChange without removed parameter when remove is clicked', async () => {
      const user = getUser();
      const mockOnChange = jest.fn();
      const mockParameters: JdbcParameter[] = [
        { id: '1', name: 'socketTimeout', value: '30000', isDefault: false, isCustom: true },
        { id: '2', name: 'connectTimeout', value: '5000', isDefault: false, isCustom: true },
      ];

      renderComponent({ parameters: mockParameters, onChange: mockOnChange });

      const removeButtons = screen.getAllByRole('button', { name: /remove parameter/i });
      await user.click(removeButtons[0]);

      expect(mockOnChange).toHaveBeenCalledWith([
        expect.objectContaining({ name: 'connectTimeout' }),
      ]);
    });

    it('does not show remove button for default read-only parameters', () => {
      const defaultParams: JdbcParameter[] = [
        { id: '1', name: 'defaultParam', value: 'value', isDefault: true, isCustom: false },
      ];

      renderComponent({ parameters: defaultParams });

      expect(screen.queryByRole('button', { name: /remove parameter/i })).not.toBeInTheDocument();
    });
  });

  describe('Editing Parameters', () => {
    it('calls onChange when parameter name is edited', async () => {
      const user = getUser();
      const mockOnChange = jest.fn();
      const mockParameters: JdbcParameter[] = [
        { id: '1', name: 'socketTimeout', value: '30000', isDefault: false, isCustom: true },
      ];

      renderComponent({ parameters: mockParameters, onChange: mockOnChange });

      const nameInput = screen.getByDisplayValue('socketTimeout');
      await user.clear(nameInput);
      await user.type(nameInput, 'newName');

      // onChange is called for each character typed
      expect(mockOnChange).toHaveBeenCalled();
    });

    it('calls onChange when parameter value is edited', async () => {
      const user = getUser();
      const mockOnChange = jest.fn();
      const mockParameters: JdbcParameter[] = [
        { id: '1', name: 'socketTimeout', value: '30000', isDefault: false, isCustom: true },
      ];

      renderComponent({ parameters: mockParameters, onChange: mockOnChange });

      const valueInput = screen.getByDisplayValue('30000');
      await user.clear(valueInput);
      await user.type(valueInput, '60000');

      expect(mockOnChange).toHaveBeenCalled();
    });

    it('disables editing for default read-only parameters', () => {
      const defaultParams: JdbcParameter[] = [
        { id: '1', name: 'defaultParam', value: 'value', isDefault: true, isCustom: false },
      ];

      renderComponent({ parameters: defaultParams });

      const nameInput = screen.getByDisplayValue('defaultParam');
      expect(nameInput).toBeDisabled();
    });
  });

  describe('Validation Display', () => {
    it('displays error messages only when showAllValidation is true (after save attempt)', () => {
      const params: JdbcParameter[] = [
        { id: '1', name: '', value: '', isDefault: false, isCustom: true },
      ];
      const validations: ParameterValidation[] = [
        { id: '1', error: 'Parameter name is required' },
      ];

      renderComponent({ parameters: params, validations, showAllValidation: true });

      expect(screen.getByText('Parameter name is required')).toBeInTheDocument();
    });

    it('does not display error messages before save attempt (showAllValidation is false)', () => {
      const params: JdbcParameter[] = [
        { id: '1', name: '', value: '', isDefault: false, isCustom: true },
      ];
      const validations: ParameterValidation[] = [
        { id: '1', error: 'Parameter name is required' },
      ];

      renderComponent({ parameters: params, validations, showAllValidation: false });

      // Errors should NOT show until user tries to save
      expect(screen.queryByText('Parameter name is required')).not.toBeInTheDocument();
    });

    it('shows required field indicators (red asterisks) in header', () => {
      const params: JdbcParameter[] = [
        { id: '1', name: 'test', value: 'value', isDefault: false, isCustom: true },
      ];

      renderComponent({ parameters: params });

      // Header should show asterisks for required fields
      expect(screen.getByText('Parameter Name')).toBeInTheDocument();
      expect(screen.getByText('Value')).toBeInTheDocument();
      // Red asterisks indicate required
      expect(screen.getAllByText('*')).toHaveLength(2);
    });

    it('displays warning messages', () => {
      const params: JdbcParameter[] = [
        { id: '1', name: 'unknownParam', value: 'value', isDefault: false, isCustom: true },
      ];
      const validations: ParameterValidation[] = [
        { id: '1', warning: 'Unknown parameter warning' },
      ];

      renderComponent({ parameters: params, validations });

      expect(screen.getByText('Unknown parameter warning')).toBeInTheDocument();
    });
  });

  describe('Reset Functionality', () => {
    it('shows reset button when onReset is provided and custom params exist', () => {
      const params: JdbcParameter[] = [
        { id: '1', name: 'socketTimeout', value: '30000', isDefault: false, isCustom: true },
      ];

      renderComponent({ parameters: params, onReset: jest.fn() });

      expect(screen.getByRole('button', { name: /reset to defaults/i })).toBeInTheDocument();
    });

    it('hides reset button when no custom parameters exist', () => {
      const params: JdbcParameter[] = [
        { id: '1', name: 'defaultParam', value: 'value', isDefault: true, isCustom: false },
      ];

      renderComponent({ parameters: params, onReset: jest.fn() });

      expect(screen.queryByRole('button', { name: /reset to defaults/i })).not.toBeInTheDocument();
    });

    it('calls onReset when reset button is clicked', async () => {
      const user = getUser();
      const mockOnReset = jest.fn();
      const params: JdbcParameter[] = [
        { id: '1', name: 'socketTimeout', value: '30000', isDefault: false, isCustom: true },
      ];

      renderComponent({ parameters: params, onReset: mockOnReset });

      await user.click(screen.getByRole('button', { name: /reset to defaults/i }));

      expect(mockOnReset).toHaveBeenCalled();
    });
  });

  describe('Disabled State', () => {
    it('disables all inputs when disabled prop is true', () => {
      const params: JdbcParameter[] = [
        { id: '1', name: 'socketTimeout', value: '30000', isDefault: false, isCustom: true },
      ];

      renderComponent({ parameters: params, disabled: true });

      const nameInput = screen.getByDisplayValue('socketTimeout');
      const valueInput = screen.getByDisplayValue('30000');

      expect(nameInput).toBeDisabled();
      expect(valueInput).toBeDisabled();
    });

    it('disables add button when disabled prop is true', () => {
      renderComponent({ parameters: [], disabled: true });

      expect(screen.getByRole('button', { name: /add parameter/i })).toBeDisabled();
    });
  });
});

describe('isKnownParameter', () => {
  it('returns true for known parameters', () => {
    expect(isKnownParameter('socketTimeout')).toBe(true);
    expect(isKnownParameter('connectTimeout')).toBe(true);
    expect(isKnownParameter('ssl')).toBe(true);
    expect(isKnownParameter('sslmode')).toBe(true);
  });

  it('returns false for unknown parameters', () => {
    expect(isKnownParameter('unknownParam')).toBe(false);
    expect(isKnownParameter('randomSetting')).toBe(false);
  });

  it('is case-insensitive for better UX', () => {
    expect(isKnownParameter('SOCKETTIMEOUT')).toBe(true);
    expect(isKnownParameter('SocketTimeout')).toBe(true);
    expect(isKnownParameter('sockettimeout')).toBe(true);
  });
});

