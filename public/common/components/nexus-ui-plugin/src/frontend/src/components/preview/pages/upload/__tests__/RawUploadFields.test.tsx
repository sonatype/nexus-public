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

const DIRECTORY_TESTID = 'input-raw-directory';
const DIRECTORY_FIELD = 'directory';
const DIRECTORY_LABEL = 'Directory';
const DIRECTORY_PLACEHOLDER = '/path/to/files/';
// Fallback copy reconciled to the backend DIRECTORY_HELP_TEXT constant.
const FALLBACK_DIRECTORY_HELP = 'Destination for uploaded files (e.g. /path/to/files/)';
const BACKEND_DIRECTORY_HELP = 'Backend-supplied directory guidance';

// Wrap components with Theme provider
const renderWithTheme = (ui) => {
  return render(<Theme>{ui}</Theme>);
};

describe('RawUploadFields', () => {
  const mockOnChange = jest.fn();

  // directory is the only raw componentField; filename is a per-file assetField, never present here.
  const rawFieldsByGroup = {
    'File Details': [
      {
        name: DIRECTORY_FIELD,
        type: 'STRING',
        displayName: DIRECTORY_LABEL,
        helpText: BACKEND_DIRECTORY_HELP,
        optional: false,
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

  // Directory metadata without displayName/helpText, so the form falls back to its own strings.
  const rawFieldsNoMetadata = {
    'File Details': [
      {
        name: DIRECTORY_FIELD,
        type: 'STRING',
        optional: false,
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

      expect(screen.getByTestId(DIRECTORY_TESTID)).toBeInTheDocument();
      expect(screen.getByText(DIRECTORY_LABEL)).toBeInTheDocument();
    });

    it('does not render a top-level filename field', () => {
      renderWithTheme(
        <RawUploadFields
          fieldsByGroup={rawFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.queryByTestId('input-raw-filename')).not.toBeInTheDocument();
      expect(screen.queryByText('Filename')).not.toBeInTheDocument();
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
    it('renders directory field when definition is empty', () => {
      renderWithTheme(
        <RawUploadFields
          fieldsByGroup={emptyFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByTestId(DIRECTORY_TESTID)).toBeInTheDocument();
    });

    it('does not render a filename field when definition is empty', () => {
      renderWithTheme(
        <RawUploadFields
          fieldsByGroup={emptyFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.queryByTestId('input-raw-filename')).not.toBeInTheDocument();
    });
  });

  describe('Values', () => {
    it('displays current directory value', () => {
      renderWithTheme(
        <RawUploadFields
          fieldsByGroup={rawFieldsByGroup}
          values={{ directory: '/path/to/files' }}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByTestId(DIRECTORY_TESTID)).toHaveValue('/path/to/files');
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

      const input = screen.getByTestId(DIRECTORY_TESTID);
      fireEvent.change(input, { target: { value: '/new/path' } });

      expect(mockOnChange).toHaveBeenCalledWith(DIRECTORY_FIELD, '/new/path');
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
  });

  describe('Disabled Fields', () => {
    it('disables directory when in disabledFields', () => {
      renderWithTheme(
        <RawUploadFields
          fieldsByGroup={rawFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
          disabledFields={new Set([DIRECTORY_FIELD])}
        />
      );

      expect(screen.getByTestId(DIRECTORY_TESTID)).toBeDisabled();
    });
  });

  describe('Help Text', () => {
    it('displays backend-supplied directory help text when available', () => {
      renderWithTheme(
        <RawUploadFields
          fieldsByGroup={rawFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByText(BACKEND_DIRECTORY_HELP)).toBeInTheDocument();
    });

    it('falls back to reconciled directory help text when metadata omits it', () => {
      renderWithTheme(
        <RawUploadFields
          fieldsByGroup={rawFieldsNoMetadata}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByText(FALLBACK_DIRECTORY_HELP)).toBeInTheDocument();
    });
  });

  describe('Labels', () => {
    it('falls back to default directory label when metadata omits displayName', () => {
      renderWithTheme(
        <RawUploadFields
          fieldsByGroup={rawFieldsNoMetadata}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByText(DIRECTORY_LABEL)).toBeInTheDocument();
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

      expect(screen.getByTestId(DIRECTORY_TESTID)).toHaveAttribute(
        'placeholder',
        DIRECTORY_PLACEHOLDER
      );
    });
  });
});
