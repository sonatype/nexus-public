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
import { render, screen } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import '@testing-library/jest-dom';

import { UploadFieldRenderer } from '../UploadFieldRenderer';

// Wrap components with Theme provider
const renderWithTheme = (ui) => {
  return render(<Theme>{ui}</Theme>);
};

describe('UploadFieldRenderer', () => {
  const mockOnChange = jest.fn();

  const baseMavenDefinition = {
    format: 'maven2',
    uiUpload: true,
    multipleUpload: true,
    componentFields: [
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
        name: 'generate-pom',
        type: 'BOOLEAN',
        displayName: 'Generate POM',
        optional: true,
        group: 'Component coordinates',
      },
    ],
  };

  const baseNpmDefinition = {
    format: 'npm',
    uiUpload: true,
    multipleUpload: false,
    componentFields: [
      {
        name: 'name',
        type: 'STRING',
        displayName: 'Package Name',
        optional: false,
      },
      {
        name: 'version',
        type: 'STRING',
        displayName: 'Version',
        optional: false,
      },
    ],
  };

  const baseRawDefinition = {
    format: 'raw',
    uiUpload: true,
    multipleUpload: true,
    componentFields: [
      {
        name: 'directory',
        type: 'STRING',
        displayName: 'Directory',
        optional: false,
      },
      {
        name: 'filename',
        type: 'STRING',
        displayName: 'Filename',
        optional: true,
      },
    ],
  };

  const genericDefinition = {
    format: 'docker',
    uiUpload: true,
    multipleUpload: false,
    componentFields: [
      {
        name: 'imageName',
        type: 'STRING',
        displayName: 'Image Name',
        optional: false,
        group: 'Docker',
      },
      {
        name: 'tag',
        type: 'STRING',
        displayName: 'Tag',
        optional: true,
        group: 'Docker',
      },
    ],
  };

  beforeEach(() => {
    mockOnChange.mockClear();
  });

  describe('Format Detection', () => {
    it('renders Maven fields for maven2 format', () => {
      renderWithTheme(
        <UploadFieldRenderer
          format="maven2"
          definition={baseMavenDefinition}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByTestId('upload-field-renderer')).toBeInTheDocument();
      expect(screen.getByTestId('maven-upload-fields')).toBeInTheDocument();
    });

    it('renders npm fields for npm format', () => {
      renderWithTheme(
        <UploadFieldRenderer
          format="npm"
          definition={baseNpmDefinition}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByTestId('upload-field-renderer')).toBeInTheDocument();
      expect(screen.getByTestId('npm-upload-fields')).toBeInTheDocument();
    });

    it('renders raw fields for raw format', () => {
      renderWithTheme(
        <UploadFieldRenderer
          format="raw"
          definition={baseRawDefinition}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByTestId('upload-field-renderer')).toBeInTheDocument();
      expect(screen.getByTestId('raw-upload-fields')).toBeInTheDocument();
    });

    it('renders generic fields for unknown formats', () => {
      renderWithTheme(
        <UploadFieldRenderer
          format="docker"
          definition={genericDefinition}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByTestId('upload-field-renderer')).toBeInTheDocument();
      expect(screen.getByTestId('generic-upload-fields')).toBeInTheDocument();
    });

    it('is case-insensitive for format detection', () => {
      renderWithTheme(
        <UploadFieldRenderer
          format="MAVEN2"
          definition={baseMavenDefinition}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByTestId('maven-upload-fields')).toBeInTheDocument();
    });
  });

  describe('Empty States', () => {
    it('renders empty when definition is null', () => {
      renderWithTheme(
        <UploadFieldRenderer
          format="maven2"
          definition={null}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByTestId('upload-field-renderer-empty')).toBeInTheDocument();
    });

    it('renders empty when componentFields is empty', () => {
      renderWithTheme(
        <UploadFieldRenderer
          format="maven2"
          definition={{ ...baseMavenDefinition, componentFields: [] }}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByTestId('upload-field-renderer-empty')).toBeInTheDocument();
    });
  });

  describe('Props Passing', () => {
    it('passes disabledFields to format component', () => {
      const disabledFields = new Set(['groupId']);

      renderWithTheme(
        <UploadFieldRenderer
          format="maven2"
          definition={baseMavenDefinition}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
          disabledFields={disabledFields}
        />
      );

      const groupIdInput = screen.getByTestId('input-groupId');
      expect(groupIdInput).toBeDisabled();
    });

    it('passes hasPomFile to Maven component', () => {
      renderWithTheme(
        <UploadFieldRenderer
          format="maven2"
          definition={baseMavenDefinition}
          values={{}}
          onChange={mockOnChange}
          errors={{}}
          hasPomFile={true}
        />
      );

      // When POM file is present, coordinate fields should be disabled
      expect(screen.getByTestId('input-groupId')).toBeDisabled();
      expect(screen.getByTestId('input-artifactId')).toBeDisabled();
      expect(screen.getByTestId('input-version')).toBeDisabled();
    });

    it('passes errors to format component', () => {
      renderWithTheme(
        <UploadFieldRenderer
          format="maven2"
          definition={baseMavenDefinition}
          values={{}}
          onChange={mockOnChange}
          errors={{ groupId: 'Group ID is required' }}
        />
      );

      expect(screen.getByText('Group ID is required')).toBeInTheDocument();
    });

    it('passes values to format component', () => {
      renderWithTheme(
        <UploadFieldRenderer
          format="maven2"
          definition={baseMavenDefinition}
          values={{ groupId: 'com.example' }}
          onChange={mockOnChange}
          errors={{}}
        />
      );

      expect(screen.getByTestId('input-groupId')).toHaveValue('com.example');
    });
  });
});

