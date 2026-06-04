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

import { RawUploadFields } from '../components/fields/RawUploadFields';

// Wrap components with Theme provider
const renderWithTheme = (ui) => {
  return render(<Theme>{ui}</Theme>);
};

describe('RawUploadFields', () => {
  const mockOnChange = jest.fn();

  const rawFieldsByGroup = {
    'File Details': [
      {
        name: 'directory',
        type: 'STRING',
        displayName: 'Directory',
        helpText: 'Target directory path',
        optional: false,
        group: 'File Details',
      },
      {
        name: 'filename',
        type: 'STRING',
        displayName: 'Filename',
        helpText: 'Optional filename override',
        optional: true,
        group: 'File Details',
      },
      {
        name: 'customField',
        type: 'STRING',
        displayName: 'Custom Field',
        optional: true,
        group: 'File Details',
      },
    ],
  };

  const emptyFieldsByGroup = {};

  beforeEach(() => {
    mockOnChange.mockClear();
  });

  describe('Rendering', () => {
    it('renders raw upload fields', () => {
      renderWithTheme(
        <RawUploadFields
          fieldsByGroup={rawFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByTestId('raw-upload-fields')).toBeInTheDocument();
    });

    it('renders header with Raw File Details title', () => {
      renderWithTheme(
        <RawUploadFields
          fieldsByGroup={rawFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByText('Raw File Details')).toBeInTheDocument();
    });

    it('renders directory field', () => {
      renderWithTheme(
        <RawUploadFields
          fieldsByGroup={rawFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByTestId('input-raw-directory')).toBeInTheDocument();
      expect(screen.getByText('Directory')).toBeInTheDocument();
    });

    it('renders filename field', () => {
      renderWithTheme(
        <RawUploadFields
          fieldsByGroup={rawFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByTestId('input-raw-filename')).toBeInTheDocument();
      expect(screen.getByText('Filename')).toBeInTheDocument();
    });

    it('renders other fields in separate section', () => {
      renderWithTheme(
        <RawUploadFields
          fieldsByGroup={rawFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByText('Custom Field')).toBeInTheDocument();
    });
  });

  describe('Default Fields', () => {
    it('renders default directory field when not in definition', () => {
      renderWithTheme(
        <RawUploadFields
          fieldsByGroup={emptyFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByTestId('input-raw-directory')).toBeInTheDocument();
      expect(screen.getByTestId('input-raw-filename')).toBeInTheDocument();
    });
  });

  describe('Values', () => {
    it('displays current values in fields', () => {
      renderWithTheme(
        <RawUploadFields
          fieldsByGroup={rawFieldsByGroup}
          values={{
            directory: '/path/to/files',
            filename: 'custom-name.txt',
          }}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByTestId('input-raw-directory')).toHaveValue('/path/to/files');
      expect(screen.getByTestId('input-raw-filename')).toHaveValue('custom-name.txt');
    });

    it('calls onChange when directory changes', () => {
      renderWithTheme(
        <RawUploadFields
          fieldsByGroup={rawFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      const input = screen.getByTestId('input-raw-directory');
      fireEvent.change(input, { target: { value: '/new/path' } });

      expect(mockOnChange).toHaveBeenCalledWith('directory', '/new/path');
    });

    it('calls onChange when filename changes', () => {
      renderWithTheme(
        <RawUploadFields
          fieldsByGroup={rawFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      const input = screen.getByTestId('input-raw-filename');
      fireEvent.change(input, { target: { value: 'new-file.txt' } });

      expect(mockOnChange).toHaveBeenCalledWith('filename', 'new-file.txt');
    });
  });

  describe('Errors', () => {
    it('displays error for directory', () => {
      renderWithTheme(
        <RawUploadFields
          fieldsByGroup={rawFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{ directory: 'Directory is required' }}
        />
      );

      expect(screen.getByText('Directory is required')).toBeInTheDocument();
    });

    it('displays error for filename', () => {
      renderWithTheme(
        <RawUploadFields
          fieldsByGroup={rawFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{ filename: 'Invalid filename' }}
        />
      );

      expect(screen.getByText('Invalid filename')).toBeInTheDocument();
    });
  });

  describe('Disabled Fields', () => {
    it('disables directory when in disabledFields', () => {
      renderWithTheme(
        <RawUploadFields
          fieldsByGroup={rawFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
          disabledFields={new Set(['directory'])}
        />
      );

      expect(screen.getByTestId('input-raw-directory')).toBeDisabled();
    });

    it('disables filename when in disabledFields', () => {
      renderWithTheme(
        <RawUploadFields
          fieldsByGroup={rawFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
          disabledFields={new Set(['filename'])}
        />
      );

      expect(screen.getByTestId('input-raw-filename')).toBeDisabled();
    });
  });

  describe('Help Text', () => {
    it('displays help text for fields', () => {
      renderWithTheme(
        <RawUploadFields
          fieldsByGroup={rawFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(
        screen.getByText(/Target directory path/)
      ).toBeInTheDocument();
    });
  });

  describe('Placeholders', () => {
    it('shows directory placeholder', () => {
      renderWithTheme(
        <RawUploadFields
          fieldsByGroup={rawFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByTestId('input-raw-directory')).toHaveAttribute(
        'placeholder',
        '/path/to/directory'
      );
    });

    it('shows filename placeholder', () => {
      renderWithTheme(
        <RawUploadFields
          fieldsByGroup={rawFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByTestId('input-raw-filename')).toHaveAttribute(
        'placeholder',
        'filename.ext'
      );
    });
  });
});

