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
import '@testing-library/jest-dom';
import { Theme } from '@radix-ui/themes';

import { RepositoryGlobalDistribution } from '../RepositoryGlobalDistribution';

const CHECKBOX_TESTID = 'checkbox-global-distribution';

function renderBlock(props: Partial<Parameters<typeof RepositoryGlobalDistribution>[0]> = {}) {
  return render(
    <Theme>
      <RepositoryGlobalDistribution mode="available" checked={false} onChange={jest.fn()} {...props} />
    </Theme>
  );
}

describe('RepositoryGlobalDistribution', () => {
  it.each(['unlicensed', 'blobStoreOff', 'available'] as const)('renders the Global Distribution heading (%s)', (mode) => {
    renderBlock({ mode });

    expect(screen.getByRole('heading', { name: 'Global Distribution' })).toBeInTheDocument();
  });

  describe('unlicensed', () => {
    it('shows the Enterprise pill and the license callout without a checkbox', () => {
      renderBlock({ mode: 'unlicensed' });

      expect(screen.getByTestId('edition-pill-enterprise')).toHaveTextContent('Enterprise');
      expect(screen.getByTestId('license-callout-repository-global-distribution')).toHaveTextContent(
        'Global Distribution requires an Enterprise license.'
      );
      expect(screen.queryByTestId(CHECKBOX_TESTID)).not.toBeInTheDocument();
    });
  });

  describe('available', () => {
    it('shows no Enterprise pill and no license callout', () => {
      renderBlock({ mode: 'available' });

      expect(screen.queryByTestId('edition-pill-enterprise')).not.toBeInTheDocument();
      expect(screen.queryByTestId('license-callout-repository-global-distribution')).not.toBeInTheDocument();
    });

    it('renders an enabled unchecked checkbox and no callout when disabled', () => {
      renderBlock({ mode: 'available', checked: false });

      expect(screen.getByTestId(CHECKBOX_TESTID)).not.toBeChecked();
      expect(screen.getByTestId(CHECKBOX_TESTID)).toBeEnabled();
      expect(screen.getByText('Enable Global Distribution')).toBeInTheDocument();
      expect(screen.getByText('Serve repository content through CloudFront edge locations')).toBeInTheDocument();
      expect(screen.queryByTestId('global-distribution-enabled-callout')).not.toBeInTheDocument();
    });

    it('tags the checkbox with its analytics id', () => {
      renderBlock({ mode: 'available' });

      expect(screen.getByTestId(CHECKBOX_TESTID)).toHaveAttribute(
        'data-analytics-id',
        'nxrm-repository-global-distribution-checkbox'
      );
    });

    it('shows the green enabled callout with the serving blob store when checked', () => {
      renderBlock({ mode: 'available', checked: true, blobStoreName: 'cdn-store' });

      const callout = screen.getByTestId('global-distribution-enabled-callout');
      expect(callout).toHaveAttribute('data-accent-color', 'green');
      expect(callout).toHaveTextContent('Global Distribution is enabled');
      expect(callout).toHaveTextContent('Served from blob store: cdn-store');
    });

    it('omits the serving blob store line when no blob store name is given (cloud)', () => {
      renderBlock({ mode: 'available', checked: true });

      expect(screen.getByTestId('global-distribution-enabled-callout')).not.toHaveTextContent('Served from blob store');
    });

    it('propagates checkbox changes', () => {
      const onChange = jest.fn();
      renderBlock({ mode: 'available', checked: false, onChange });

      fireEvent.click(screen.getByTestId(CHECKBOX_TESTID));

      expect(onChange).toHaveBeenCalledWith(true, expect.anything());
    });
  });

  describe('blobStoreOff', () => {
    it('renders a disabled unchecked checkbox and the orange blob store callout', () => {
      renderBlock({
        mode: 'blobStoreOff',
        checked: true,
        blobStoreName: 'plain-store',
        blobStoreHref: '#preview/admin/repository/blobstores/s3/plain-store',
        isS3BlobStore: true,
      });

      expect(screen.getByTestId(CHECKBOX_TESTID)).toBeDisabled();
      expect(screen.getByTestId(CHECKBOX_TESTID)).not.toBeChecked();
      const callout = screen.getByTestId('global-distribution-blob-store-off-callout');
      expect(callout).toHaveAttribute('data-accent-color', 'orange');
      expect(callout).toHaveTextContent(
        'Global Distribution is unavailable because it is not enabled for the "plain-store" blob store.'
      );
    });

    it('describes the disabled checkbox with the blob store callout message', () => {
      renderBlock({ mode: 'blobStoreOff', blobStoreName: 'plain-store', isS3BlobStore: true });

      const describedBy = screen.getByTestId(CHECKBOX_TESTID).getAttribute('aria-describedby') ?? '';
      expect(describedBy.split(' ')).toContain('global-distribution-blob-store-off-message');
      expect(screen.getByTestId(CHECKBOX_TESTID)).toHaveAccessibleDescription(
        expect.stringContaining('not enabled for the "plain-store" blob store.')
      );
    });

    it('links Configure Blob Store to the blob store edit page in a new tab', () => {
      renderBlock({
        mode: 'blobStoreOff',
        blobStoreName: 'plain-store',
        blobStoreHref: '#preview/admin/repository/blobstores/s3/plain-store',
        isS3BlobStore: true,
      });

      const link = screen.getByRole('link', { name: 'Configure Blob Store (opens in a new tab)' });
      expect(link).toHaveAttribute('href', '#preview/admin/repository/blobstores/s3/plain-store');
      expect(link).toHaveAttribute('target', '_blank');
      expect(link).toHaveAttribute('rel', 'noopener noreferrer');
      expect(link).toHaveAttribute(
        'data-analytics-id',
        'nxrm-repository-global-distribution-configure-blob-store-link'
      );
    });

    it('omits the Configure Blob Store link without an href', () => {
      renderBlock({ mode: 'blobStoreOff', blobStoreName: 'plain-store', isS3BlobStore: true });

      expect(screen.queryByRole('link', { name: /configure blob store/i })).not.toBeInTheDocument();
    });

    it('omits the callout when no blob store is selected yet', () => {
      renderBlock({ mode: 'blobStoreOff', isS3BlobStore: true });

      expect(screen.getByTestId(CHECKBOX_TESTID)).toBeDisabled();
      expect(screen.queryByTestId('global-distribution-blob-store-off-callout')).not.toBeInTheDocument();
      expect(screen.getByTestId(CHECKBOX_TESTID).getAttribute('aria-describedby')).not.toContain(
        'global-distribution-blob-store-off-message'
      );
    });
  });

  describe('blobStoreOff, non-S3 blob store', () => {
    it('renders a disabled unchecked checkbox and the blue informational callout instead of the orange warning', () => {
      renderBlock({
        mode: 'blobStoreOff',
        checked: true,
        blobStoreName: 'file-store',
        isS3BlobStore: false,
      });

      expect(screen.getByTestId(CHECKBOX_TESTID)).toBeDisabled();
      expect(screen.getByTestId(CHECKBOX_TESTID)).not.toBeChecked();
      expect(screen.queryByTestId('global-distribution-blob-store-off-callout')).not.toBeInTheDocument();

      const callout = screen.getByTestId('global-distribution-not-s3-callout');
      expect(callout).toHaveAttribute('data-accent-color', 'blue');
      expect(callout).toHaveTextContent('Global Distribution is only available for S3 blob stores.');
    });

    it('describes the disabled checkbox with the informational message', () => {
      renderBlock({ mode: 'blobStoreOff', blobStoreName: 'file-store', isS3BlobStore: false });

      const describedBy = screen.getByTestId(CHECKBOX_TESTID).getAttribute('aria-describedby') ?? '';
      expect(describedBy.split(' ')).toContain('global-distribution-not-s3-message');
      expect(screen.getByTestId(CHECKBOX_TESTID)).toHaveAccessibleDescription(
        expect.stringContaining('Global Distribution is only available for S3 blob stores.')
      );
    });

    it('never shows a Configure Blob Store link, even with an href', () => {
      renderBlock({
        mode: 'blobStoreOff',
        blobStoreName: 'file-store',
        isS3BlobStore: false,
        blobStoreHref: '#preview/admin/repository/blobstores/file/file-store',
      });

      expect(screen.queryByRole('link', { name: /configure blob store/i })).not.toBeInTheDocument();
    });

    it('omits the callout when no blob store is selected yet, even if isS3BlobStore is false', () => {
      renderBlock({ mode: 'blobStoreOff', isS3BlobStore: false });

      expect(screen.getByTestId(CHECKBOX_TESTID)).toBeDisabled();
      expect(screen.queryByTestId('global-distribution-not-s3-callout')).not.toBeInTheDocument();
    });
  });
});
