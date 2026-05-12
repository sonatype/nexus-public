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

import { SupportZipResponse } from '../SupportZipResponse';
import { SupportZipResponseData } from '../types';

// Mock Support ZIP API
jest.mock('../useSupportZipApi', () => ({
  useSupportZipApi: () => ({
    getDownloadUrl: (file: string) => `service/rest/wonderland/download/${file}`,
  }),
}));

// Mock ExtJS
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    urlOf: jest.fn((url) => `http://localhost:8081/${url}`),
    downloadUrl: jest.fn(),
  },
}));

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

describe('SupportZipResponse', () => {
  const mockResponse: SupportZipResponseData = {
    file: '/path/to/support.zip',
    name: 'support-2024-01-15.zip',
    size: '15 MB',
    truncated: false,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders success header', () => {
    render(<SupportZipResponse response={mockResponse} />, { wrapper: TestWrapper });

    expect(screen.getByText('Support ZIP Created')).toBeInTheDocument();
  });

  it('displays file name', () => {
    render(<SupportZipResponse response={mockResponse} />, { wrapper: TestWrapper });

    expect(screen.getByText('support-2024-01-15.zip')).toBeInTheDocument();
  });

  it('displays file size', () => {
    render(<SupportZipResponse response={mockResponse} />, { wrapper: TestWrapper });

    expect(screen.getByText('15 MB')).toBeInTheDocument();
  });

  it('shows download button', () => {
    render(<SupportZipResponse response={mockResponse} />, { wrapper: TestWrapper });

    expect(screen.getByText('Download')).toBeInTheDocument();
  });

  it('downloads support zip when Download button is clicked', () => {
    const { ExtJS } = require('@sonatype/nexus-ui-plugin');
    render(<SupportZipResponse response={mockResponse} />, { wrapper: TestWrapper });

    fireEvent.click(screen.getByText('Download'));

    expect(ExtJS.downloadUrl).toHaveBeenCalledWith(
      'http://localhost:8081/service/rest/wonderland/download/support-2024-01-15.zip'
    );
  });

  it('shows truncation warning when file is truncated', () => {
    const truncatedResponse: SupportZipResponseData = {
      ...mockResponse,
      truncated: true,
    };

    render(<SupportZipResponse response={truncatedResponse} />, { wrapper: TestWrapper });

    expect(screen.getByText(/truncated due to size limits/)).toBeInTheDocument();
  });

  it('does not show truncation warning when file is not truncated', () => {
    render(<SupportZipResponse response={mockResponse} />, { wrapper: TestWrapper });

    expect(screen.queryByText(/truncated/)).not.toBeInTheDocument();
  });

  it('displays file path information', () => {
    render(<SupportZipResponse response={mockResponse} />, { wrapper: TestWrapper });

    expect(screen.getByText(/Path:/)).toBeInTheDocument();
    expect(screen.getByText('/path/to/support.zip')).toBeInTheDocument();
  });
});

