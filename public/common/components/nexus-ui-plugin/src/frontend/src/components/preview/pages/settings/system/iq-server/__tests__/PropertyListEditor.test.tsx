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
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';

import { PropertyListEditor } from '../PropertyListEditor';
import { IqProperty, PropertyValidation } from '../types';

function renderComponent(props: Partial<React.ComponentProps<typeof PropertyListEditor>> = {}) {
  const defaultProps = { properties: [], onChange: jest.fn() };
  return render(
    <Theme>
      <PropertyListEditor {...defaultProps} {...props} />
    </Theme>
  );
}

const getUser = () => (typeof (userEvent as any).setup === 'function' ? (userEvent as any).setup() : userEvent);

describe('PropertyListEditor', () => {
  describe('Empty State', () => {
    it('displays empty state when no properties', () => {
      renderComponent({ properties: [] });
      expect(screen.getByText(/no properties configured/i)).toBeInTheDocument();
    });

    it('shows add parameter button', () => {
      renderComponent({ properties: [] });
      expect(screen.getByRole('button', { name: /add parameter/i })).toBeInTheDocument();
    });

    it('does not show Clear All when there are no properties', () => {
      renderComponent({ properties: [], onClearAll: jest.fn() });
      expect(screen.queryByRole('button', { name: /clear all/i })).not.toBeInTheDocument();
    });
  });

  describe('Property Display', () => {
    const mockProperties: IqProperty[] = [
      { id: '1', name: 'proxy.host', value: 'proxy.example.com' },
      { id: '2', name: 'proxy.port', value: '8080' },
    ];

    it('displays property rows', () => {
      renderComponent({ properties: mockProperties });
      expect(screen.getByDisplayValue('proxy.host')).toBeInTheDocument();
      expect(screen.getByDisplayValue('proxy.example.com')).toBeInTheDocument();
      expect(screen.getByDisplayValue('proxy.port')).toBeInTheDocument();
      expect(screen.getByDisplayValue('8080')).toBeInTheDocument();
    });
  });

  describe('Adding Properties', () => {
    it('calls onChange with a new blank row when Add Parameter is clicked', async () => {
      const user = getUser();
      const mockOnChange = jest.fn();
      renderComponent({ properties: [], onChange: mockOnChange });

      await user.click(screen.getByRole('button', { name: /add parameter/i }));

      expect(mockOnChange).toHaveBeenCalledWith(
        expect.arrayContaining([expect.objectContaining({ name: '', value: '' })])
      );
    });
  });

  describe('Removing Properties', () => {
    it('calls onChange without the removed property when remove is clicked', async () => {
      const user = getUser();
      const mockOnChange = jest.fn();
      const mockProperties: IqProperty[] = [
        { id: '1', name: 'proxy.host', value: 'x' },
        { id: '2', name: 'proxy.port', value: '8080' },
      ];
      renderComponent({ properties: mockProperties, onChange: mockOnChange });

      const removeButtons = screen.getAllByRole('button', { name: /remove property/i });
      await user.click(removeButtons[0]);

      expect(mockOnChange).toHaveBeenCalledWith([expect.objectContaining({ name: 'proxy.port' })]);
    });
  });

  describe('Editing Properties', () => {
    it('calls onChange when a property name is edited', async () => {
      const user = getUser();
      const mockOnChange = jest.fn();
      const mockProperties: IqProperty[] = [{ id: '1', name: 'proxy.host', value: 'x' }];
      renderComponent({ properties: mockProperties, onChange: mockOnChange });

      const nameInput = screen.getByDisplayValue('proxy.host');
      await user.clear(nameInput);
      await user.type(nameInput, 'newName');

      expect(mockOnChange).toHaveBeenCalled();
    });

    it('calls onChange when a property value is edited', async () => {
      const user = getUser();
      const mockOnChange = jest.fn();
      const mockProperties: IqProperty[] = [{ id: '1', name: 'proxy.host', value: 'x' }];
      renderComponent({ properties: mockProperties, onChange: mockOnChange });

      const valueInput = screen.getByDisplayValue('x');
      await user.clear(valueInput);
      await user.type(valueInput, 'y');

      expect(mockOnChange).toHaveBeenCalled();
    });
  });

  describe('Validation Display', () => {
    it('displays error messages only when showAllValidation is true', () => {
      const props: IqProperty[] = [{ id: '1', name: '', value: '' }];
      const validations: PropertyValidation[] = [{ id: '1', error: 'Parameter name is required' }];

      renderComponent({ properties: props, validations, showAllValidation: true });

      expect(screen.getByText('Parameter name is required')).toBeInTheDocument();
    });

    it('does not display error messages before save attempt', () => {
      const props: IqProperty[] = [{ id: '1', name: '', value: '' }];
      const validations: PropertyValidation[] = [{ id: '1', error: 'Parameter name is required' }];

      renderComponent({ properties: props, validations, showAllValidation: false });

      expect(screen.queryByText('Parameter name is required')).not.toBeInTheDocument();
    });

    it('shows required field indicators in the header', () => {
      renderComponent({ properties: [{ id: '1', name: 'a', value: 'b' }] });
      expect(screen.getByText('Name')).toBeInTheDocument();
      expect(screen.getByText('Value')).toBeInTheDocument();
      expect(screen.getAllByText('*')).toHaveLength(2);
    });
  });

  describe('Pasting a list into the name field', () => {
    function pasteText(input: HTMLElement, text: string) {
      fireEvent.paste(input, { clipboardData: { getData: () => text } } as unknown as ClipboardEvent);
    }

    it('splits a single "name=value" paste into the row\'s name and value', () => {
      const mockOnChange = jest.fn();
      const props: IqProperty[] = [{ id: '1', name: '', value: '' }];
      renderComponent({ properties: props, onChange: mockOnChange });

      pasteText(screen.getByPlaceholderText('Property name...'), 'proxy.host=proxy.example.com');

      expect(mockOnChange).toHaveBeenCalledWith([
        expect.objectContaining({ id: '1', name: 'proxy.host', value: 'proxy.example.com' }),
      ]);
    });

    it('expands a multi-line paste into additional rows', () => {
      const mockOnChange = jest.fn();
      const props: IqProperty[] = [{ id: '1', name: '', value: '' }];
      renderComponent({ properties: props, onChange: mockOnChange });

      pasteText(screen.getByPlaceholderText('Property name...'), 'a=1\nb=2\nc=3');

      const result = mockOnChange.mock.calls[0][0];
      expect(result).toHaveLength(3);
      expect(result[0]).toEqual(expect.objectContaining({ id: '1', name: 'a', value: '1' }));
      expect(result[1]).toEqual(expect.objectContaining({ name: 'b', value: '2' }));
      expect(result[2]).toEqual(expect.objectContaining({ name: 'c', value: '3' }));
    });

    it('does not intercept a paste with no "=" sign', () => {
      const mockOnChange = jest.fn();
      const props: IqProperty[] = [{ id: '1', name: '', value: '' }];
      renderComponent({ properties: props, onChange: mockOnChange });

      pasteText(screen.getByPlaceholderText('Property name...'), 'justtext');

      expect(mockOnChange).not.toHaveBeenCalled();
    });
  });

  describe('Clear All', () => {
    it('shows Clear All when properties exist and onClearAll is provided', () => {
      renderComponent({ properties: [{ id: '1', name: 'a', value: 'b' }], onClearAll: jest.fn() });
      expect(screen.getByRole('button', { name: /clear all/i })).toBeInTheDocument();
    });

    it('calls onClearAll when clicked', async () => {
      const user = getUser();
      const mockOnClearAll = jest.fn();
      renderComponent({ properties: [{ id: '1', name: 'a', value: 'b' }], onClearAll: mockOnClearAll });

      await user.click(screen.getByRole('button', { name: /clear all/i }));

      expect(mockOnClearAll).toHaveBeenCalled();
    });
  });

  describe('Disabled State', () => {
    it('disables all inputs when disabled prop is true', () => {
      const props: IqProperty[] = [{ id: '1', name: 'a', value: 'b' }];
      renderComponent({ properties: props, disabled: true });

      expect(screen.getByDisplayValue('a')).toBeDisabled();
      expect(screen.getByDisplayValue('b')).toBeDisabled();
    });

    it('disables add button when disabled prop is true', () => {
      renderComponent({ properties: [], disabled: true });
      expect(screen.getByRole('button', { name: /add parameter/i })).toBeDisabled();
    });
  });
});
