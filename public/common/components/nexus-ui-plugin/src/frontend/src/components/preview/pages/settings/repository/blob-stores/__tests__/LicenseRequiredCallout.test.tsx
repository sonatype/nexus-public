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
import { LicenseRequiredCallout } from '../LicenseRequiredCallout';

describe('LicenseRequiredCallout', () => {
  it('renders the Pre-Signed URL message as an amber surface Radix callout', () => {
    render(<LicenseRequiredCallout feature="presignedUrl" />);

    const callout = screen.getByTestId('license-callout-presigned-url');
    expect(callout).toHaveTextContent('Pre-Signed URL requires a Pro license.');
    expect(callout).toHaveClass('rt-CalloutRoot', 'rt-variant-surface');
    expect(callout).toHaveAttribute('data-accent-color', 'amber');
  });

  it('renders the Global Distribution message', () => {
    render(<LicenseRequiredCallout feature="globalDistribution" />);

    expect(screen.getByTestId('license-callout-global-distribution')).toHaveTextContent(
      'CloudFront and S3 Transfer Acceleration require an Enterprise license.'
    );
    const link = screen.getByRole('link', { name: /learn more/i });
    expect(link).toHaveAttribute('href', 'https://links.sonatype.com/products/nxrm3/enterprise-license');
    expect(link).toHaveAttribute('data-analytics-id', 'nxrm-blobstore-global-distribution-license-learn-more-link');
  });

  it('links Learn more to the edition page in a new tab and says so to screen readers', () => {
    render(<LicenseRequiredCallout feature="presignedUrl" />);

    const link = screen.getByRole('link', { name: /learn more/i });
    expect(link).toHaveAttribute('href', 'https://links.sonatype.com/products/repository-pro');
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
    expect(link).toHaveAccessibleName('Learn more (opens in a new tab)');
    expect(link).toHaveAttribute('data-analytics-id', 'nxrm-blobstore-presigned-url-license-learn-more-link');
  });

  it('applies the bottom margin prop', () => {
    render(<LicenseRequiredCallout feature="globalDistribution" mb="4" />);

    expect(screen.getByTestId('license-callout-global-distribution')).toHaveClass('rt-r-mb-4');
  });

  it('renders the Global Distribution settings page message', () => {
    render(<LicenseRequiredCallout feature="globalDistributionSettings" />);

    expect(screen.getByTestId('license-callout-global-distribution-settings')).toHaveTextContent(
      'Global Distribution requires an Enterprise license.'
    );
    const link = screen.getByRole('link', { name: /learn more/i });
    expect(link).toHaveAttribute('href', 'https://links.sonatype.com/products/nxrm3/enterprise-license');
    expect(link).toHaveAttribute('data-analytics-id', 'nxrm-global-distribution-license-learn-more-link');
  });

  it('renders the repository form Global Distribution message', () => {
    render(<LicenseRequiredCallout feature="repositoryGlobalDistribution" />);

    expect(screen.getByTestId('license-callout-repository-global-distribution')).toHaveTextContent(
      'Global Distribution requires an Enterprise license.'
    );
    const link = screen.getByRole('link', { name: /learn more/i });
    expect(link).toHaveAttribute('href', 'https://links.sonatype.com/products/nxrm3/enterprise-license');
    expect(link).toHaveAttribute('data-analytics-id', 'nxrm-repository-global-distribution-license-learn-more-link');
  });
});
