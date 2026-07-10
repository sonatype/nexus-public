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

import { NegativeCacheFacet } from '../NegativeCacheFacet';
import { RepositoryFormData } from '../../types';
import UIStrings from '../../../../../../../../constants/pages/admin/repository/RepositoriesStrings';

const defaultFormData: RepositoryFormData = {
  name: 'test-repo',
  format: 'maven2',
  type: 'proxy',
  storage: { blobStoreName: 'default', strictContentTypeValidation: true },
  negativeCache: {
    enabled: true,
    timeToLive: 1440,
  },
};

function renderFacet(props: Partial<React.ComponentProps<typeof NegativeCacheFacet>> = {}) {
  const defaultProps = {
    formData: defaultFormData,
    onChange: jest.fn(),
    onNestedChange: jest.fn(),
    errors: {},
  };
  return render(
    <Theme>
      <NegativeCacheFacet {...defaultProps} {...props} />
    </Theme>
  );
}

describe('NegativeCacheFacet', () => {
  describe('Label alignment with classic UI (issue 4)', () => {
    it('uses "Negative Cache TTL (Minutes)" label matching classic UI', () => {
      renderFacet();
      expect(screen.getByText(UIStrings.NEGATIVE_CACHE.TTL.label)).toBeInTheDocument();
      expect(UIStrings.NEGATIVE_CACHE.TTL.label).toBe('Negative Cache TTL (Minutes)');
    });
  });

  describe('TTL field disabled when negative cache is unchecked (issue 3)', () => {
    it('enables TTL field when negative cache is enabled', () => {
      renderFacet({
        formData: { ...defaultFormData, negativeCache: { enabled: true, timeToLive: 1440 } },
      });
      const ttlInput = screen.getByTestId('input-negativeCache-timeToLive');
      expect(ttlInput).not.toBeDisabled();
    });

    it('disables TTL field when negative cache is disabled', () => {
      renderFacet({
        formData: { ...defaultFormData, negativeCache: { enabled: false, timeToLive: 1440 } },
      });
      const ttlInput = screen.getByTestId('input-negativeCache-timeToLive');
      expect(ttlInput).toBeDisabled();
    });

    it('calls onNestedChange with enabled=false when checkbox is unchecked', () => {
      const mockOnNestedChange = jest.fn();
      renderFacet({ onNestedChange: mockOnNestedChange });

      const checkbox = screen.getByTestId('checkbox-negativeCache-enabled');
      fireEvent.click(checkbox);

      expect(mockOnNestedChange).toHaveBeenCalledWith('negativeCache', { enabled: false });
    });
  });
});
