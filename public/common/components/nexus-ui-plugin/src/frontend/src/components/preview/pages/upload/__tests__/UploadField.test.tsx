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

import { UploadField } from '../UploadField';

const FIELD_NAME = 'filename';

const renderWithTheme = (ui) => render(<Theme>{ui}</Theme>);

describe('UploadField (shared asset field)', () => {
  const mockOnChange = jest.fn();

  const stringField = {
    name: FIELD_NAME,
    type: 'STRING',
    displayName: 'Filename',
    helpText: 'The file name',
    optional: true,
  };

  beforeEach(() => {
    mockOnChange.mockClear();
  });

  describe('STRING type', () => {
    it('renders a text input with its label and help text', () => {
      renderWithTheme(<UploadField field={stringField} value="" onChange={mockOnChange} />);

      expect(screen.getByText('Filename')).toBeInTheDocument();
      expect(screen.getByText('The file name')).toBeInTheDocument();
      expect(screen.getByTestId(`input-${FIELD_NAME}`)).toBeInTheDocument();
    });

    it('forwards the placeholder prop to the input', () => {
      renderWithTheme(
        <UploadField
          field={stringField}
          value=""
          onChange={mockOnChange}
          placeholder="my-file.txt"
        />
      );

      expect(screen.getByTestId(`input-${FIELD_NAME}`)).toHaveAttribute('placeholder', 'my-file.txt');
    });

    it('falls back to the field name when displayName is absent', () => {
      renderWithTheme(
        <UploadField field={{ name: FIELD_NAME, type: 'STRING' }} value="" onChange={mockOnChange} />
      );

      expect(screen.getByText(FIELD_NAME)).toBeInTheDocument();
    });

    it('calls onChange when the value changes', () => {
      renderWithTheme(<UploadField field={stringField} value="" onChange={mockOnChange} />);

      fireEvent.change(screen.getByTestId(`input-${FIELD_NAME}`), { target: { value: 'renamed.txt' } });

      expect(mockOnChange.mock.calls[0][0]).toBe('renamed.txt');
    });

    it('calls onBlur when provided', () => {
      const mockOnBlur = jest.fn();
      renderWithTheme(
        <UploadField field={stringField} value="" onChange={mockOnChange} onBlur={mockOnBlur} />
      );

      fireEvent.blur(screen.getByTestId(`input-${FIELD_NAME}`));

      expect(mockOnBlur).toHaveBeenCalled();
    });

    it('displays an error message', () => {
      renderWithTheme(
        <UploadField field={stringField} value="" onChange={mockOnChange} error="Invalid name" />
      );

      expect(screen.getByText('Invalid name')).toBeInTheDocument();
    });
  });

  describe('BOOLEAN type', () => {
    const booleanField = {
      name: 'flag',
      type: 'BOOLEAN',
      displayName: 'Flag',
      helpText: 'Toggle it',
      optional: true,
    };

    it('renders a checkbox with its label', () => {
      renderWithTheme(<UploadField field={booleanField} value={false} onChange={mockOnChange} />);

      expect(screen.getByText('Flag')).toBeInTheDocument();
      expect(screen.getByTestId('checkbox-flag')).toBeInTheDocument();
    });

    it('calls onChange with the boolean value', () => {
      renderWithTheme(<UploadField field={booleanField} value={false} onChange={mockOnChange} />);

      fireEvent.click(screen.getByTestId('checkbox-flag'));

      expect(mockOnChange.mock.calls[0][0]).toBe(true);
    });
  });

  describe('SELECT type', () => {
    const selectField = {
      name: 'choice',
      type: 'SELECT',
      displayName: 'Choice',
      optional: false,
      selectOptions: ['a', 'b'],
    };

    it('renders a select with its label', () => {
      renderWithTheme(<UploadField field={selectField} value="" onChange={mockOnChange} />);

      expect(screen.getByText('Choice')).toBeInTheDocument();
      expect(screen.getByTestId('select-choice')).toBeInTheDocument();
    });
  });
});
