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

import { NpmUploadFields } from '../components/fields/NpmUploadFields';

// Wrap components with Theme provider
const renderWithTheme = (ui) => {
  return render(<Theme>{ui}</Theme>);
};

describe('NpmUploadFields', () => {
  const mockOnChange = jest.fn();

  const npmFieldsByGroup = {
    'Package Details': [
      {
        name: 'name',
        type: 'STRING',
        displayName: 'Package Name',
        helpText: 'npm package name',
        optional: false,
        group: 'Package Details',
      },
      {
        name: 'version',
        type: 'STRING',
        displayName: 'Version',
        helpText: 'Semver version',
        optional: false,
        group: 'Package Details',
      },
      {
        name: 'tag',
        type: 'STRING',
        displayName: 'Tag',
        optional: true,
        group: 'Package Details',
      },
    ],
  };

  const emptyFieldsByGroup = {};

  beforeEach(() => {
    mockOnChange.mockClear();
  });

  describe('Rendering', () => {
    it('renders npm upload fields', () => {
      renderWithTheme(
        <NpmUploadFields
          fieldsByGroup={npmFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByTestId('npm-upload-fields')).toBeInTheDocument();
    });

    it('renders header with npm Package Details title', () => {
      renderWithTheme(
        <NpmUploadFields
          fieldsByGroup={npmFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByText('npm Package Details')).toBeInTheDocument();
    });

    it('renders package name field', () => {
      renderWithTheme(
        <NpmUploadFields
          fieldsByGroup={npmFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByTestId('input-npm-package-name')).toBeInTheDocument();
      expect(screen.getByText('Package Name')).toBeInTheDocument();
    });

    it('renders version field', () => {
      renderWithTheme(
        <NpmUploadFields
          fieldsByGroup={npmFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByTestId('input-npm-version')).toBeInTheDocument();
    });

    it('renders other fields in separate section', () => {
      renderWithTheme(
        <NpmUploadFields
          fieldsByGroup={npmFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByText('Tag')).toBeInTheDocument();
    });
  });

  describe('Empty State', () => {
    it('renders minimal UI when no fields defined', () => {
      renderWithTheme(
        <NpmUploadFields
          fieldsByGroup={emptyFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByTestId('npm-upload-fields')).toBeInTheDocument();
      expect(screen.getByText('npm Package')).toBeInTheDocument();
      expect(
        screen.getByText(/Upload your npm package tarball/)
      ).toBeInTheDocument();
    });
  });

  describe('Values', () => {
    it('displays current values in fields', () => {
      renderWithTheme(
        <NpmUploadFields
          fieldsByGroup={npmFieldsByGroup}
          values={{
            name: '@scope/my-package',
            version: '2.0.0',
          }}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByTestId('input-npm-package-name')).toHaveValue(
        '@scope/my-package'
      );
      expect(screen.getByTestId('input-npm-version')).toHaveValue('2.0.0');
    });

    it('calls onChange when package name changes', () => {
      renderWithTheme(
        <NpmUploadFields
          fieldsByGroup={npmFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      const input = screen.getByTestId('input-npm-package-name');
      fireEvent.change(input, { target: { value: '@newscope/new-package' } });

      expect(mockOnChange).toHaveBeenCalledWith('name', '@newscope/new-package');
    });

    it('calls onChange when version changes', () => {
      renderWithTheme(
        <NpmUploadFields
          fieldsByGroup={npmFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      const input = screen.getByTestId('input-npm-version');
      fireEvent.change(input, { target: { value: '3.0.0' } });

      expect(mockOnChange).toHaveBeenCalledWith('version', '3.0.0');
    });
  });

  describe('Errors', () => {
    it('displays error for package name', () => {
      renderWithTheme(
        <NpmUploadFields
          fieldsByGroup={npmFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{ name: 'Package name is required' }}
        />
      );

      expect(screen.getByText('Package name is required')).toBeInTheDocument();
    });

    it('displays error for version', () => {
      renderWithTheme(
        <NpmUploadFields
          fieldsByGroup={npmFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{ version: 'Version is required' }}
        />
      );

      expect(screen.getByText('Version is required')).toBeInTheDocument();
    });
  });

  describe('Disabled Fields', () => {
    it('disables package name when in disabledFields', () => {
      renderWithTheme(
        <NpmUploadFields
          fieldsByGroup={npmFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
          disabledFields={new Set(['name'])}
        />
      );

      expect(screen.getByTestId('input-npm-package-name')).toBeDisabled();
    });

    it('disables version when in disabledFields', () => {
      renderWithTheme(
        <NpmUploadFields
          fieldsByGroup={npmFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
          disabledFields={new Set(['version'])}
        />
      );

      expect(screen.getByTestId('input-npm-version')).toBeDisabled();
    });
  });

  describe('Help Text', () => {
    it('displays help text for fields', () => {
      renderWithTheme(
        <NpmUploadFields
          fieldsByGroup={npmFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(
        screen.getByText(/The npm package name/)
      ).toBeInTheDocument();
      expect(
        screen.getByText(/The semver version/)
      ).toBeInTheDocument();
    });
  });
});

