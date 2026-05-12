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

import { GenericUploadFields, UploadField } from '../components/fields/GenericUploadFields';

// Wrap components with Theme provider
const renderWithTheme = (ui) => {
  return render(<Theme>{ui}</Theme>);
};

describe('UploadField', () => {
  const mockOnChange = jest.fn();

  const stringField = {
    name: 'testField',
    type: 'STRING',
    displayName: 'Test Field',
    helpText: 'This is help text',
    optional: false,
  };

  const booleanField = {
    name: 'checkField',
    type: 'BOOLEAN',
    displayName: 'Check Field',
    helpText: 'Boolean help text',
    optional: true,
  };

  const selectField = {
    name: 'selectField',
    type: 'SELECT',
    displayName: 'Select Field',
    helpText: 'Select help text',
    optional: false,
    selectOptions: ['option1', 'option2', 'option3'],
  };

  beforeEach(() => {
    mockOnChange.mockClear();
  });

  describe('STRING type', () => {
    it('renders text input with label', () => {
      renderWithTheme(
        <UploadField field={stringField} value="" onChange={mockOnChange} />
      );

      expect(screen.getByText('Test Field')).toBeInTheDocument();
      expect(screen.getByTestId('input-testField')).toBeInTheDocument();
    });

    it('displays help text', () => {
      renderWithTheme(
        <UploadField field={stringField} value="" onChange={mockOnChange} />
      );

      expect(screen.getByText('This is help text')).toBeInTheDocument();
    });

    it('shows required indicator for required fields', () => {
      renderWithTheme(
        <UploadField field={stringField} value="" onChange={mockOnChange} />
      );

      expect(screen.getByText('*')).toBeInTheDocument();
    });

    it('does not show required indicator for optional fields', () => {
      renderWithTheme(
        <UploadField
          field={{ ...stringField, optional: true }}
          value=""
          onChange={mockOnChange}
        />
      );

      expect(screen.queryByText('*')).not.toBeInTheDocument();
    });

    it('calls onChange with new value', () => {
      renderWithTheme(
        <UploadField field={stringField} value="" onChange={mockOnChange} />
      );

      const input = screen.getByTestId('input-testField');
      fireEvent.change(input, { target: { value: 'new value' } });

      expect(mockOnChange).toHaveBeenCalledWith('new value');
    });

    it('displays error message', () => {
      renderWithTheme(
        <UploadField
          field={stringField}
          value=""
          onChange={mockOnChange}
          error="This field is required"
        />
      );

      expect(screen.getByText('This field is required')).toBeInTheDocument();
    });

    it('disables input when disabled', () => {
      renderWithTheme(
        <UploadField
          field={stringField}
          value=""
          onChange={mockOnChange}
          disabled
        />
      );

      expect(screen.getByTestId('input-testField')).toBeDisabled();
    });
  });

  describe('BOOLEAN type', () => {
    it('renders checkbox with label', () => {
      renderWithTheme(
        <UploadField field={booleanField} value={false} onChange={mockOnChange} />
      );

      expect(screen.getByText('Check Field')).toBeInTheDocument();
      expect(screen.getByTestId('checkbox-checkField')).toBeInTheDocument();
    });

    it('displays help text', () => {
      renderWithTheme(
        <UploadField field={booleanField} value={false} onChange={mockOnChange} />
      );

      expect(screen.getByText('Boolean help text')).toBeInTheDocument();
    });

    it('calls onChange with boolean value', () => {
      renderWithTheme(
        <UploadField field={booleanField} value={false} onChange={mockOnChange} />
      );

      const checkbox = screen.getByTestId('checkbox-checkField');
      fireEvent.click(checkbox);

      expect(mockOnChange).toHaveBeenCalledWith(true);
    });

    it('reflects checked state', () => {
      renderWithTheme(
        <UploadField field={booleanField} value={true} onChange={mockOnChange} />
      );

      const checkbox = screen.getByTestId('checkbox-checkField');
      expect(checkbox).toBeChecked();
    });
  });

  describe('SELECT type', () => {
    it('renders select with label', () => {
      renderWithTheme(
        <UploadField field={selectField} value="" onChange={mockOnChange} />
      );

      expect(screen.getByText('Select Field')).toBeInTheDocument();
      expect(screen.getByTestId('select-selectField')).toBeInTheDocument();
    });

    it('displays help text', () => {
      renderWithTheme(
        <UploadField field={selectField} value="" onChange={mockOnChange} />
      );

      expect(screen.getByText('Select help text')).toBeInTheDocument();
    });

    it('shows required indicator', () => {
      renderWithTheme(
        <UploadField field={selectField} value="" onChange={mockOnChange} />
      );

      expect(screen.getByText('*')).toBeInTheDocument();
    });
  });
});

describe('GenericUploadFields', () => {
  const mockOnChange = jest.fn();

  const fieldsByGroup = {
    'Group A': [
      {
        name: 'field1',
        type: 'STRING',
        displayName: 'Field 1',
        optional: false,
        group: 'Group A',
      },
      {
        name: 'field2',
        type: 'STRING',
        displayName: 'Field 2',
        optional: true,
        group: 'Group A',
      },
    ],
    'Group B': [
      {
        name: 'field3',
        type: 'BOOLEAN',
        displayName: 'Field 3',
        optional: true,
        group: 'Group B',
      },
    ],
  };

  beforeEach(() => {
    mockOnChange.mockClear();
  });

  it('renders all field groups', () => {
    renderWithTheme(
      <GenericUploadFields
        fieldsByGroup={fieldsByGroup}
        values={{}}
        onChange={mockOnChange}
        errors={{}}
      />
    );

    expect(screen.getByTestId('generic-upload-fields')).toBeInTheDocument();
    expect(screen.getByText('Group A')).toBeInTheDocument();
    expect(screen.getByText('Group B')).toBeInTheDocument();
  });

  it('renders all fields within groups', () => {
    renderWithTheme(
      <GenericUploadFields
        fieldsByGroup={fieldsByGroup}
        values={{}}
        onChange={mockOnChange}
        errors={{}}
      />
    );

    expect(screen.getByText('Field 1')).toBeInTheDocument();
    expect(screen.getByText('Field 2')).toBeInTheDocument();
    expect(screen.getByText('Field 3')).toBeInTheDocument();
  });

  it('passes values to fields', () => {
    renderWithTheme(
      <GenericUploadFields
        fieldsByGroup={fieldsByGroup}
        values={{ field1: 'test value' }}
        onChange={mockOnChange}
        errors={{}}
      />
    );

    expect(screen.getByTestId('input-field1')).toHaveValue('test value');
  });

  it('passes errors to fields', () => {
    renderWithTheme(
      <GenericUploadFields
        fieldsByGroup={fieldsByGroup}
        values={{}}
        onChange={mockOnChange}
        errors={{ field1: 'Error message' }}
      />
    );

    expect(screen.getByText('Error message')).toBeInTheDocument();
  });

  it('disables specified fields', () => {
    renderWithTheme(
      <GenericUploadFields
        fieldsByGroup={fieldsByGroup}
        values={{}}
        onChange={mockOnChange}
        errors={{}}
        disabledFields={new Set(['field1'])}
      />
    );

    expect(screen.getByTestId('input-field1')).toBeDisabled();
    expect(screen.getByTestId('input-field2')).not.toBeDisabled();
  });

  it('calls onChange with field name and value', () => {
    renderWithTheme(
      <GenericUploadFields
        fieldsByGroup={fieldsByGroup}
        values={{}}
        onChange={mockOnChange}
        errors={{}}
      />
    );

    const input = screen.getByTestId('input-field1');
    fireEvent.change(input, { target: { value: 'new value' } });

    expect(mockOnChange).toHaveBeenCalledWith('field1', 'new value');
  });

  it('renders empty when no field groups', () => {
    const { container } = renderWithTheme(
      <GenericUploadFields
        fieldsByGroup={{}}
        values={{}}
        onChange={mockOnChange}
        errors={{}}
      />
    );

    expect(container.firstChild).toBeEmptyDOMElement();
  });

  it('renders field group headings', () => {
    renderWithTheme(
      <GenericUploadFields
        fieldsByGroup={fieldsByGroup}
        values={{}}
        onChange={mockOnChange}
        errors={{}}
      />
    );

    expect(screen.getByText('Group A')).toBeInTheDocument();
    expect(screen.getByText('Group B')).toBeInTheDocument();
  });
});

