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

import { SupportRequestPage } from '../SupportRequestPage';

// Mock ExtJS
jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    checkPermission: jest.fn(),
    isProEdition: jest.fn(),
    state: jest.fn(() => ({
      getValue: jest.fn(),
    })),
  },
}));

import { ExtJS } from '../../../../../../../interface/ExtJS';

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

describe('SupportRequestPage', () => {
  let windowOpenSpy: jest.SpyInstance;
  const mockCheckPermission = ExtJS.checkPermission as jest.Mock;
  const mockIsProEdition = ExtJS.isProEdition as jest.Mock;

  beforeEach(() => {
    windowOpenSpy = jest.spyOn(window, 'open').mockImplementation(() => null);
    // Default: Pro Edition with permission
    mockCheckPermission.mockReturnValue(true);
    mockIsProEdition.mockReturnValue(true);
  });

  afterEach(() => {
    windowOpenSpy.mockRestore();
    jest.clearAllMocks();
  });

  describe('Pro Edition with permission', () => {
    it('renders the page header', () => {
      render(<SupportRequestPage />, { wrapper: TestWrapper });

      expect(screen.getByText('Support Request')).toBeInTheDocument();
      expect(screen.getByText('Submit a support request to Sonatype')).toBeInTheDocument();
    });

    it('renders the page with testid', () => {
      render(<SupportRequestPage />, { wrapper: TestWrapper });

      expect(screen.getByTestId('support-request-page')).toBeInTheDocument();
      expect(screen.getByTestId('support-request-page')).toHaveAttribute('data-edition', 'pro');
    });

    it('renders description text', () => {
      render(<SupportRequestPage />, { wrapper: TestWrapper });

      expect(screen.getByText(/Please include a complete description of your problem/)).toBeInTheDocument();
    });

    it('renders support ZIP recommendation callout', () => {
      render(<SupportRequestPage />, { wrapper: TestWrapper });

      expect(screen.getByText(/Attaching a support ZIP to your request/)).toBeInTheDocument();
      expect(screen.getByTestId('support-request-supportzip-link')).toBeInTheDocument();
    });

    it('renders submit button', () => {
      render(<SupportRequestPage />, { wrapper: TestWrapper });

      expect(screen.getByTestId('support-request-submit-button')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Submit Support Request/i })).toBeInTheDocument();
    });

    it('opens external support URL when submit button is clicked', () => {
      render(<SupportRequestPage />, { wrapper: TestWrapper });

      const submitButton = screen.getByTestId('support-request-submit-button');
      fireEvent.click(submitButton);

      expect(windowOpenSpy).toHaveBeenCalledWith(
        'https://links.sonatype.com/products/nexus/pro/support-request',
        '_blank',
        'noopener,noreferrer'
      );
    });

    it('navigates to Support ZIP page when link is clicked', () => {
      const originalHash = window.location.hash;
      
      render(<SupportRequestPage />, { wrapper: TestWrapper });

      const supportZipLink = screen.getByTestId('support-request-supportzip-link');
      fireEvent.click(supportZipLink);

      // Should navigate internally
      expect(windowOpenSpy).not.toHaveBeenCalled();

      window.location.hash = originalHash;
    });

    it('applies custom className', () => {
      const { container } = render(<SupportRequestPage className="custom-class" />, { wrapper: TestWrapper });

      const root = container.querySelector('.support-request-page');
      expect(root).toHaveClass('support-request-page', 'custom-class');
    });
  });

  describe('OSS Edition', () => {
    beforeEach(() => {
      mockIsProEdition.mockReturnValue(false);
    });

    it('shows Pro Edition warning', () => {
      render(<SupportRequestPage />, { wrapper: TestWrapper });

      expect(screen.getByTestId('support-request-pro-only-warning')).toBeInTheDocument();
      expect(screen.getByText(/only available in Nexus Repository Pro Edition/)).toBeInTheDocument();
    });

    it('does not show submit button', () => {
      render(<SupportRequestPage />, { wrapper: TestWrapper });

      expect(screen.queryByTestId('support-request-submit-button')).not.toBeInTheDocument();
    });

    it('has data-edition="oss" attribute', () => {
      render(<SupportRequestPage />, { wrapper: TestWrapper });

      expect(screen.getByTestId('support-request-page')).toHaveAttribute('data-edition', 'oss');
    });
  });

  describe('Pro Edition without permission', () => {
    beforeEach(() => {
      mockCheckPermission.mockReturnValue(false);
      mockIsProEdition.mockReturnValue(true);
    });

    it('shows permission warning', () => {
      render(<SupportRequestPage />, { wrapper: TestWrapper });

      expect(screen.getByTestId('support-request-permission-warning')).toBeInTheDocument();
      expect(screen.getByText(/do not have permission/)).toBeInTheDocument();
    });

    it('does not show submit button', () => {
      render(<SupportRequestPage />, { wrapper: TestWrapper });

      expect(screen.queryByTestId('support-request-submit-button')).not.toBeInTheDocument();
    });

    it('has data-permission="denied" attribute', () => {
      render(<SupportRequestPage />, { wrapper: TestWrapper });

      expect(screen.getByTestId('support-request-page')).toHaveAttribute('data-permission', 'denied');
    });
  });
});

