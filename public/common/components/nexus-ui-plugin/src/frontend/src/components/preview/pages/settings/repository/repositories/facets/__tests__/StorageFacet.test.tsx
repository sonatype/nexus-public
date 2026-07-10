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
import '@testing-library/jest-dom';
import { Theme } from '@radix-ui/themes';

import { StorageFacet } from '../../facets/StorageFacet';

const BLOB_STORE_SELECT_TESTID = 'select-storage-blobStoreName';
const CONTENT_VALIDATION_TESTID = 'checkbox-storage-strictContentTypeValidation';
const STORAGE_HEADING = 'Storage';

const mockBlobStores = [
  { name: 'default' },
  { name: 'secondary' },
];

const defaultFormData = {
  name: 'test-repo',
  type: 'hosted' as const,
  format: 'maven2',
  online: true,
  storage: {
    blobStoreName: 'default',
    strictContentTypeValidation: true,
  },
};

function renderStorageFacet(props: Partial<Parameters<typeof StorageFacet>[0]> = {}) {
  const defaultProps = {
    formData: defaultFormData,
    onChange: jest.fn(),
    onNestedChange: jest.fn(),
    errors: undefined,
    isEdit: false,
    isCloud: false,
    blobStores: mockBlobStores,
  };

  return render(
    <Theme>
      <StorageFacet {...defaultProps} {...props} />
    </Theme>
  );
}

describe('StorageFacet', () => {
  describe('self-hosted distribution (isCloud=false)', () => {
    it('shouldRenderBlobStoreDropdown', () => {
      renderStorageFacet({ isCloud: false });

      expect(screen.getByTestId(BLOB_STORE_SELECT_TESTID)).toBeInTheDocument();
    });

    it('shouldRenderStorageHeading', () => {
      renderStorageFacet({ isCloud: false });

      expect(screen.getByText(STORAGE_HEADING)).toBeInTheDocument();
    });

    it('shouldRenderContentValidationCheckbox', () => {
      renderStorageFacet({ isCloud: false });

      expect(screen.getByTestId(CONTENT_VALIDATION_TESTID)).toBeInTheDocument();
    });

    it('shouldDisableBlobStoreWhenEditing', () => {
      renderStorageFacet({ isCloud: false, isEdit: true });

      const trigger = screen.getByTestId(BLOB_STORE_SELECT_TESTID);
      expect(trigger).toBeDisabled();
    });
  });

  describe('cloud distribution (isCloud=true)', () => {
    it('shouldNotRenderBlobStoreDropdown', () => {
      renderStorageFacet({ isCloud: true });

      expect(screen.queryByTestId(BLOB_STORE_SELECT_TESTID)).not.toBeInTheDocument();
    });

    it('shouldRenderStorageHeading', () => {
      renderStorageFacet({ isCloud: true });

      expect(screen.getByText(STORAGE_HEADING)).toBeInTheDocument();
    });

    it('shouldRenderContentValidationCheckbox', () => {
      renderStorageFacet({ isCloud: true });

      expect(screen.getByTestId(CONTENT_VALIDATION_TESTID)).toBeInTheDocument();
    });

    it('shouldNotRenderBlobStoreHelpText', () => {
      renderStorageFacet({ isCloud: true });

      expect(screen.queryByText('Select the blob store used to store repository contents')).not.toBeInTheDocument();
    });
  });

  describe('strict content type validation by format', () => {
    it('shouldRenderContentValidationForMaven2Format', () => {
      renderStorageFacet({ formData: { ...defaultFormData, format: 'maven2' } });

      expect(screen.getByTestId(CONTENT_VALIDATION_TESTID)).toBeInTheDocument();
    });

    it('shouldRenderContentValidationForNpmFormat', () => {
      renderStorageFacet({ formData: { ...defaultFormData, format: 'npm' } });

      expect(screen.getByTestId(CONTENT_VALIDATION_TESTID)).toBeInTheDocument();
    });

    it('shouldRenderContentValidationForTerraformFormat', () => {
      // The Terraform backend honors strictContentTypeValidation (see
      // TerraformContentValidator.determineContentType), so the toggle must stay
      // available — matching classic UI behavior across formats.
      renderStorageFacet({ formData: { ...defaultFormData, format: 'terraform' } });

      expect(screen.getByTestId(CONTENT_VALIDATION_TESTID)).toBeInTheDocument();
    });
  });
});
