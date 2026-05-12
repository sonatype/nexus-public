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

import { MavenUploadFields } from '../components/fields/MavenUploadFields';

// Wrap components with Theme provider
const renderWithTheme = (ui) => {
  return render(<Theme>{ui}</Theme>);
};

describe('MavenUploadFields', () => {
  const mockOnChange = jest.fn();

  const mavenFieldsByGroup = {
    'Component coordinates': [
      {
        name: 'groupId',
        type: 'STRING',
        displayName: 'Group ID',
        helpText: 'Maven group ID',
        optional: false,
        group: 'Component coordinates',
      },
      {
        name: 'artifactId',
        type: 'STRING',
        displayName: 'Artifact ID',
        optional: false,
        group: 'Component coordinates',
      },
      {
        name: 'version',
        type: 'STRING',
        displayName: 'Version',
        optional: false,
        group: 'Component coordinates',
      },
      {
        name: 'packaging',
        type: 'SELECT',
        displayName: 'Packaging',
        optional: true,
        selectOptions: ['jar', 'war', 'pom'],
        group: 'Component coordinates',
      },
      {
        name: 'extension',
        type: 'STRING',
        displayName: 'Extension',
        optional: true,
        group: 'Component coordinates',
      },
      {
        name: 'classifier',
        type: 'STRING',
        displayName: 'Classifier',
        optional: true,
        group: 'Component coordinates',
      },
      {
        name: 'generate-pom',
        type: 'BOOLEAN',
        displayName: 'Generate POM',
        optional: true,
        group: 'Component coordinates',
      },
    ],
  };

  beforeEach(() => {
    mockOnChange.mockClear();
  });

  describe('Rendering', () => {
    it('renders Maven upload fields', () => {
      renderWithTheme(
        <MavenUploadFields
          fieldsByGroup={mavenFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByTestId('maven-upload-fields')).toBeInTheDocument();
    });

    it('renders component coordinates header', () => {
      renderWithTheme(
        <MavenUploadFields
          fieldsByGroup={mavenFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByText('Component coordinates')).toBeInTheDocument();
    });

    it('renders all Maven GAV fields', () => {
      renderWithTheme(
        <MavenUploadFields
          fieldsByGroup={mavenFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByTestId('input-groupId')).toBeInTheDocument();
      expect(screen.getByTestId('input-artifactId')).toBeInTheDocument();
      expect(screen.getByTestId('input-version')).toBeInTheDocument();
      expect(screen.getByTestId('select-packaging')).toBeInTheDocument();
      expect(screen.getByTestId('input-extension')).toBeInTheDocument();
      expect(screen.getByTestId('input-classifier')).toBeInTheDocument();
      expect(screen.getByTestId('checkbox-generate-pom')).toBeInTheDocument();
    });

    it('renders field labels', () => {
      renderWithTheme(
        <MavenUploadFields
          fieldsByGroup={mavenFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByText('Group ID')).toBeInTheDocument();
      expect(screen.getByText('Artifact ID')).toBeInTheDocument();
      expect(screen.getByText('Version')).toBeInTheDocument();
      expect(screen.getByText('Packaging')).toBeInTheDocument();
    });
  });

  describe('Values', () => {
    it('displays current values in fields', () => {
      renderWithTheme(
        <MavenUploadFields
          fieldsByGroup={mavenFieldsByGroup}
          values={{
            groupId: 'com.example',
            artifactId: 'my-library',
            version: '1.0.0',
          }}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByTestId('input-groupId')).toHaveValue('com.example');
      expect(screen.getByTestId('input-artifactId')).toHaveValue('my-library');
      expect(screen.getByTestId('input-version')).toHaveValue('1.0.0');
    });

    it('calls onChange when field value changes', () => {
      renderWithTheme(
        <MavenUploadFields
          fieldsByGroup={mavenFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      const groupIdInput = screen.getByTestId('input-groupId');
      fireEvent.change(groupIdInput, { target: { value: 'com.newgroup' } });

      expect(mockOnChange).toHaveBeenCalledWith('groupId', 'com.newgroup');
    });
  });

  describe('Errors', () => {
    it('displays error messages for fields', () => {
      renderWithTheme(
        <MavenUploadFields
          fieldsByGroup={mavenFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{
            groupId: 'Group ID is required',
            artifactId: 'Artifact ID is required',
          }}
        />
      );

      expect(screen.getByText('Group ID is required')).toBeInTheDocument();
      expect(screen.getByText('Artifact ID is required')).toBeInTheDocument();
    });
  });

  describe('POM File Detection', () => {
    it('shows info message when POM file is present', () => {
      renderWithTheme(
        <MavenUploadFields
          fieldsByGroup={mavenFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
          hasPomFile={true}
        />
      );

      expect(
        screen.getByText(/Component coordinates will be extracted from the provided POM file/)
      ).toBeInTheDocument();
    });

    it('disables coordinate fields when POM file is present', () => {
      renderWithTheme(
        <MavenUploadFields
          fieldsByGroup={mavenFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
          hasPomFile={true}
        />
      );

      expect(screen.getByTestId('input-groupId')).toBeDisabled();
      expect(screen.getByTestId('input-artifactId')).toBeDisabled();
      expect(screen.getByTestId('input-version')).toBeDisabled();
    });

    it('does not disable extension and classifier when POM file is present', () => {
      renderWithTheme(
        <MavenUploadFields
          fieldsByGroup={mavenFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
          hasPomFile={true}
        />
      );

      expect(screen.getByTestId('input-extension')).not.toBeDisabled();
      expect(screen.getByTestId('input-classifier')).not.toBeDisabled();
    });
  });

  describe('Generate POM Checkbox', () => {
    it('renders generate POM checkbox', () => {
      renderWithTheme(
        <MavenUploadFields
          fieldsByGroup={mavenFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByTestId('checkbox-generate-pom')).toBeInTheDocument();
      expect(screen.getByText('Generate POM')).toBeInTheDocument();
    });

    it('calls onChange when checkbox is clicked', () => {
      renderWithTheme(
        <MavenUploadFields
          fieldsByGroup={mavenFieldsByGroup}
          values={{ 'generate-pom': false }}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      const checkbox = screen.getByTestId('checkbox-generate-pom');
      fireEvent.click(checkbox);

      expect(mockOnChange).toHaveBeenCalledWith('generate-pom', true);
    });

    it('reflects checked state', () => {
      renderWithTheme(
        <MavenUploadFields
          fieldsByGroup={mavenFieldsByGroup}
          values={{ 'generate-pom': true }}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      const checkbox = screen.getByTestId('checkbox-generate-pom');
      expect(checkbox).toBeChecked();
    });

    it('disables generate-pom when POM file is present', () => {
      renderWithTheme(
        <MavenUploadFields
          fieldsByGroup={mavenFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
          hasPomFile={true}
        />
      );

      expect(screen.getByTestId('checkbox-generate-pom')).toBeDisabled();
    });
  });

  describe('Packaging Field', () => {
    it('disables packaging when generate-pom is not checked and no POM file', () => {
      renderWithTheme(
        <MavenUploadFields
          fieldsByGroup={mavenFieldsByGroup}
          values={{ 'generate-pom': false }}
          onChange={mockOnChange}
          errors={{}}
          hasPomFile={false}
        />
      );

      expect(screen.getByTestId('select-packaging')).toBeDisabled();
    });

    it('enables packaging when generate-pom is checked', () => {
      renderWithTheme(
        <MavenUploadFields
          fieldsByGroup={mavenFieldsByGroup}
          values={{ 'generate-pom': true }}
          onChange={mockOnChange}
          errors={{}}
          hasPomFile={false}
        />
      );

      expect(screen.getByTestId('select-packaging')).not.toBeDisabled();
    });
  });

  describe('Disabled Fields', () => {
    it('respects disabledFields prop', () => {
      renderWithTheme(
        <MavenUploadFields
          fieldsByGroup={mavenFieldsByGroup}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
          disabledFields={new Set(['extension', 'classifier'])}
        />
      );

      expect(screen.getByTestId('input-extension')).toBeDisabled();
      expect(screen.getByTestId('input-classifier')).toBeDisabled();
    });
  });
});

