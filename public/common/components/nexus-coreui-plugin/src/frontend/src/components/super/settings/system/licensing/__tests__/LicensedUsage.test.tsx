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

import { LicensedUsage } from '../LicensedUsage';

// Mock scrollToUsageCenter
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  scrollToUsageCenter: jest.fn(),
}));

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

describe('LicensedUsage', () => {
  it('renders usage limits', () => {
    render(
      <LicensedUsage maxRepoRequests="1,000,000" maxRepoComponents="50,000" />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText('Licensed Usage')).toBeInTheDocument();
    expect(screen.getByText('1,000,000')).toBeInTheDocument();
    expect(screen.getByText('50,000')).toBeInTheDocument();
  });

  it('displays field labels', () => {
    render(
      <LicensedUsage maxRepoRequests="1,000,000" maxRepoComponents="50,000" />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText('Requests Per Month')).toBeInTheDocument();
    expect(screen.getByText('Total Components')).toBeInTheDocument();
  });

  it('displays description text', () => {
    render(
      <LicensedUsage maxRepoRequests="1,000,000" maxRepoComponents="50,000" />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText(/Your license is based on the total components stored and monthly requests/)).toBeInTheDocument();
    expect(screen.getByText(/Track your current consumption on the/)).toBeInTheDocument();
  });

  it('displays Usage Center link', () => {
    render(
      <LicensedUsage maxRepoRequests="1,000,000" maxRepoComponents="50,000" />,
      { wrapper: TestWrapper }
    );

    const usageCenterLink = screen.getByText('Usage Center');
    expect(usageCenterLink).toBeInTheDocument();
    expect(usageCenterLink.closest('a')).toHaveAttribute('href', '#');
  });

  it('calls scrollToUsageCenter when Usage Center link is clicked', () => {
    const { scrollToUsageCenter } = require('@sonatype/nexus-ui-plugin');

    render(
      <LicensedUsage maxRepoRequests="1,000,000" maxRepoComponents="50,000" />,
      { wrapper: TestWrapper }
    );

    const usageCenterLink = screen.getByText('Usage Center');
    fireEvent.click(usageCenterLink);

    expect(scrollToUsageCenter).toHaveBeenCalled();
  });

  it('displays Contact us link', () => {
    render(
      <LicensedUsage maxRepoRequests="1,000,000" maxRepoComponents="50,000" />,
      { wrapper: TestWrapper }
    );

    const contactLink = screen.getByText('Contact us');
    expect(contactLink).toBeInTheDocument();
    expect(contactLink.closest('a')).toHaveAttribute(
      'href',
      'http://links.sonatype.com/products/nexus/pro/store?utm_medium=product&utm_source=nexus_repository&utm_campaign=repo_pricing_expansion'
    );
    expect(contactLink.closest('a')).toHaveAttribute('target', '_blank');
    expect(contactLink.closest('a')).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('displays contact link description', () => {
    render(
      <LicensedUsage maxRepoRequests="1,000,000" maxRepoComponents="50,000" />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText(/for additional capacity/)).toBeInTheDocument();
  });

  it('handles large numbers correctly', () => {
    render(
      <LicensedUsage maxRepoRequests="10,000,000,000" maxRepoComponents="1,000,000,000" />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText('10,000,000,000')).toBeInTheDocument();
    expect(screen.getByText('1,000,000,000')).toBeInTheDocument();
  });

  it('handles small numbers correctly', () => {
    render(
      <LicensedUsage maxRepoRequests="100" maxRepoComponents="50" />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText('100')).toBeInTheDocument();
    expect(screen.getByText('50')).toBeInTheDocument();
  });

  it('handles zero values', () => {
    render(
      <LicensedUsage maxRepoRequests="0" maxRepoComponents="0" />,
      { wrapper: TestWrapper }
    );

    // Both fields show "0", so check both are present
    const zeros = screen.getAllByText('0');
    expect(zeros.length).toBe(2);
  });
});

