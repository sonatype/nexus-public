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
import '@testing-library/jest-dom';
import { Theme } from '@radix-ui/themes';

import { StorageFacet } from '../../facets/StorageFacet';

const mockIsEnterprise = jest.fn(() => true);
jest.mock('../../../../../../shared/hooks/useIsEnterpriseEdition', () => ({
  useIsEnterpriseEdition: () => mockIsEnterprise(),
}));

const mockCheckPermission = jest.fn((..._args: unknown[]) => true);
jest.mock('../../../../../../../../interface/ExtJS', () => ({
  ...jest.requireActual('../../../../../../../../interface/ExtJS'),
  ExtJS: {
    checkPermission: (...args: unknown[]) => mockCheckPermission(...args),
    usePermission: (getValue: () => boolean) => getValue(),
    useUser: () => ({ id: 'admin' }),
  },
}));

const mockCdnDefault = jest.fn(() => false);
jest.mock('../../repositoryFormMachine', () => ({
  ...jest.requireActual('../../repositoryFormMachine'),
  getCdnDefaultForNewRepositories: () => mockCdnDefault(),
}));

beforeAll(() => {
  Element.prototype.hasPointerCapture = jest.fn(() => false);
  Element.prototype.setPointerCapture = jest.fn();
  Element.prototype.releasePointerCapture = jest.fn();
  Element.prototype.scrollIntoView = jest.fn();
});

const BLOB_STORE_SELECT_TESTID = 'select-storage-blobStoreName';
const CONTENT_VALIDATION_TESTID = 'checkbox-storage-strictContentTypeValidation';
const GLOBAL_DISTRIBUTION_TESTID = 'checkbox-global-distribution';
const STORAGE_HEADING = 'Storage';

const mockBlobStores = [
  { name: 'default', type: 'S3', globalDistributionEnabled: true },
  { name: 'secondary', type: 'File' },
  { name: 's3-plain', type: 'S3' },
  { name: 'azure-store', type: 'Azure Cloud Storage' },
];

const noCdnBlobStores = [{ name: 'default' }, { name: 'secondary' }];

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

function withGlobalDistribution(globalDistribution: boolean) {
  return {
    ...defaultFormData,
    storage: { ...defaultFormData.storage, globalDistribution },
  };
}

function withBlobStore(blobStoreName: string) {
  return { ...defaultFormData, storage: { blobStoreName, globalDistribution: false } };
}

async function selectBlobStore(name: string) {
  await userEvent.click(screen.getByTestId(BLOB_STORE_SELECT_TESTID));
  await userEvent.click(screen.getByRole('option', { name }));
}

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
  beforeEach(() => {
    mockIsEnterprise.mockReturnValue(true);
    mockCheckPermission.mockReturnValue(true);
    mockCdnDefault.mockReturnValue(false);
  });

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
  describe('global distribution', () => {
    it('shouldNotRenderGlobalDistributionOnEnterpriseWhenFieldIsAbsent', () => {
      renderStorageFacet({ formData: defaultFormData });

      expect(screen.queryByTestId('repository-global-distribution')).not.toBeInTheDocument();
    });

    it('shouldCheckGlobalDistributionCheckboxWhenEnabled', () => {
      renderStorageFacet({
        formData: withGlobalDistribution(true),
      });

      expect(screen.getByTestId(GLOBAL_DISTRIBUTION_TESTID)).toBeChecked();
    });

    it('shouldPropagateGlobalDistributionChange', () => {
      const onNestedChange = jest.fn();
      renderStorageFacet({
        formData: withGlobalDistribution(false),
        onNestedChange,
      });

      fireEvent.click(screen.getByTestId(GLOBAL_DISTRIBUTION_TESTID));

      expect(onNestedChange).toHaveBeenCalledWith('storage', { globalDistribution: true });
    });

    it('shouldClearGlobalDistributionWhenBlobStoreLacksCdn', () => {
      const onNestedChange = jest.fn();
      renderStorageFacet({
        formData: withGlobalDistribution(true),
        blobStores: noCdnBlobStores,
        onNestedChange,
      });

      expect(onNestedChange).toHaveBeenCalledWith('storage', { globalDistribution: false });
    });

    it('shouldNotClearGlobalDistributionBeforeBlobStoresLoad', () => {
      const onNestedChange = jest.fn();
      renderStorageFacet({
        formData: withGlobalDistribution(true),
        blobStores: [],
        onNestedChange,
      });

      expect(onNestedChange).not.toHaveBeenCalled();
    });

    it('shouldRenderAvailableGlobalDistributionWithoutPillOnEnterprise', () => {
      renderStorageFacet({ formData: withGlobalDistribution(false) });

      expect(screen.getByTestId(GLOBAL_DISTRIBUTION_TESTID)).toBeEnabled();
      expect(screen.queryByTestId('edition-pill-enterprise')).not.toBeInTheDocument();
    });

    it('shouldShowEnabledCalloutWithBlobStoreWhenChecked', () => {
      renderStorageFacet({ formData: withGlobalDistribution(true) });

      expect(screen.getByTestId('global-distribution-enabled-callout')).toHaveTextContent(
        'Served from blob store: default'
      );
    });

    it('shouldRenderBlobStoreOffStateWhenBlobStoreLacksCdn', () => {
      renderStorageFacet({ formData: withBlobStore('s3-plain') });

      expect(screen.getByTestId(GLOBAL_DISTRIBUTION_TESTID)).toBeDisabled();
      expect(screen.getByTestId('global-distribution-blob-store-off-callout')).toHaveTextContent(
        'not enabled for the "s3-plain" blob store.'
      );
      expect(screen.queryByTestId('global-distribution-not-s3-callout')).not.toBeInTheDocument();
    });

    it('shouldLinkConfigureBlobStoreToTheS3EditPageForAnS3BlobStoreWithoutCdn', () => {
      renderStorageFacet({ formData: withBlobStore('s3-plain') });

      expect(screen.getByRole('link', { name: /configure blob store/i })).toHaveAttribute(
        'href',
        '#preview/admin/repository/blobstores/s3/s3-plain'
      );
      expect(mockCheckPermission).toHaveBeenCalledWith('nexus:blobstores:read');
    });

    it.each(['secondary', 'azure-store'])(
      'shouldShowTheBlueInformationalCalloutForANonS3BlobStore (%s)',
      (blobStoreName) => {
        renderStorageFacet({ formData: withBlobStore(blobStoreName) });

        expect(screen.getByTestId(GLOBAL_DISTRIBUTION_TESTID)).toBeDisabled();
        expect(screen.queryByTestId('global-distribution-blob-store-off-callout')).not.toBeInTheDocument();
        expect(screen.getByTestId('global-distribution-not-s3-callout')).toHaveTextContent(
          'Global Distribution is only available for S3 blob stores.'
        );
        expect(screen.queryByRole('link', { name: /configure blob store/i })).not.toBeInTheDocument();
      }
    );

    it('shouldHideConfigureBlobStoreLinkWithoutBlobStoreReadPermission', () => {
      mockCheckPermission.mockReturnValue(false);
      renderStorageFacet({ formData: withBlobStore('s3-plain') });

      expect(screen.getByTestId('global-distribution-blob-store-off-callout')).toBeInTheDocument();
      expect(screen.queryByRole('link', { name: /configure blob store/i })).not.toBeInTheDocument();
      expect(mockCheckPermission).toHaveBeenCalledWith('nexus:blobstores:read');
    });

    it('shouldRenderDisabledCheckboxWithoutCalloutBeforeABlobStoreIsSelected', () => {
      renderStorageFacet({
        formData: { ...withGlobalDistribution(false), storage: { blobStoreName: '', globalDistribution: false } },
      });

      expect(screen.getByTestId(GLOBAL_DISTRIBUTION_TESTID)).toBeDisabled();
      expect(screen.queryByTestId('global-distribution-blob-store-off-callout')).not.toBeInTheDocument();
    });

    it('shouldRenderNothingWhenTheBlobStoreListCannotBeChecked', () => {
      const onNestedChange = jest.fn();
      renderStorageFacet({ formData: withGlobalDistribution(true), blobStores: [], onNestedChange });

      expect(screen.queryByTestId('repository-global-distribution')).not.toBeInTheDocument();
      expect(onNestedChange).not.toHaveBeenCalled();
    });

    it('shouldRenderNothingWhenTheSelectedBlobStoreIsNotListed', () => {
      renderStorageFacet({ formData: withBlobStore('unlisted') });

      expect(screen.queryByTestId('repository-global-distribution')).not.toBeInTheDocument();
    });

    it('shouldRenderNothingBeforeABlobStoreIsSelectedWhenTheBlobStoreListIsEmpty', () => {
      renderStorageFacet({ formData: withBlobStore(''), blobStores: [] });

      expect(screen.queryByTestId('repository-global-distribution')).not.toBeInTheDocument();
    });

    it('shouldRenderAvailableStateInCloudWithoutBlobStores', () => {
      renderStorageFacet({ formData: withGlobalDistribution(true), isCloud: true, blobStores: [] });

      expect(screen.getByTestId(GLOBAL_DISTRIBUTION_TESTID)).toBeEnabled();
      expect(screen.getByTestId('global-distribution-enabled-callout')).not.toHaveTextContent('Served from blob store');
      expect(screen.queryByTestId('global-distribution-blob-store-off-callout')).not.toBeInTheDocument();
    });

    it('shouldClearGlobalDistributionWhenSwitchingToABlobStoreWithoutCdn', async () => {
      const onNestedChange = jest.fn();
      renderStorageFacet({ formData: withGlobalDistribution(true), onNestedChange });

      await selectBlobStore('secondary');

      expect(onNestedChange).toHaveBeenCalledWith('storage', { blobStoreName: 'secondary', globalDistribution: false });
    });

    it.each([true, false])(
      'shouldReseedGlobalDistributionFromTheCdnDefaultWhenSwitchingToACdnBlobStore (default=%s)',
      async (cdnDefault) => {
        mockCdnDefault.mockReturnValue(cdnDefault);
        const onNestedChange = jest.fn();
        renderStorageFacet({ formData: withBlobStore('secondary'), onNestedChange });

        await selectBlobStore('default');

        expect(onNestedChange).toHaveBeenCalledWith('storage', {
          blobStoreName: 'default',
          globalDistribution: cdnDefault,
        });
      }
    );

    describe('without an Enterprise license', () => {
      beforeEach(() => mockIsEnterprise.mockReturnValue(false));

      it.each([false, true])('shouldShowPillAndLicenseCalloutOnly (isCloud=%s)', (isCloud) => {
        renderStorageFacet({ formData: defaultFormData, isCloud });

        expect(screen.getByRole('heading', { name: 'Global Distribution' })).toBeInTheDocument();
        expect(screen.getByTestId('edition-pill-enterprise')).toBeInTheDocument();
        expect(screen.getByTestId('license-callout-repository-global-distribution')).toBeInTheDocument();
        expect(screen.queryByTestId(GLOBAL_DISTRIBUTION_TESTID)).not.toBeInTheDocument();
      });

      it('shouldNotTouchTheFormValue', () => {
        const onNestedChange = jest.fn();
        renderStorageFacet({ formData: withGlobalDistribution(true), blobStores: noCdnBlobStores, onNestedChange });

        expect(onNestedChange).not.toHaveBeenCalled();
        expect(screen.queryByTestId(GLOBAL_DISTRIBUTION_TESTID)).not.toBeInTheDocument();
      });

      it('shouldOnlyChangeTheBlobStoreNameWhenSwitchingBlobStores', async () => {
        mockCdnDefault.mockReturnValue(true);
        const onNestedChange = jest.fn();
        renderStorageFacet({ formData: withBlobStore('secondary'), blobStores: mockBlobStores, onNestedChange });

        await selectBlobStore('default');

        expect(onNestedChange).toHaveBeenCalledWith('storage', { blobStoreName: 'default' });
      });
    });

    it('shouldRenderGlobalDistributionBeforeStrictContentValidation', () => {
      renderStorageFacet({ formData: withGlobalDistribution(false) });

      const block = screen.getByTestId('repository-global-distribution');
      const strict = screen.getByTestId(CONTENT_VALIDATION_TESTID);
      expect(block.compareDocumentPosition(strict) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    });
  });
});
