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
import { render, screen, } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { MavenFacet } from '../MavenFacet';
import { RepositoryFormData } from '../../types';

const mockGetValue = jest.fn((key: string) => {
  if (key === 'isCloud') return false;
  return false;
});

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    useState: (fn: () => any) => fn(),
    state: () => ({
      getValue: mockGetValue,
    }),
  },
}));

const defaultFormData: RepositoryFormData = {
  name: 'test-repo',
  format: 'maven2',
  type: 'hosted',
  online: true,
  storage: { blobStoreName: 'default', strictContentTypeValidation: true },
  maven: { versionPolicy: 'RELEASE', layoutPolicy: 'STRICT', contentDisposition: 'ATTACHMENT' },
};

function renderFacet(props: Partial<React.ComponentProps<typeof MavenFacet>> = {}) {
  const defaultProps = {
    formData: defaultFormData,
    onNestedChange: jest.fn(),
    errors: {},
    isEdit: false,
  };
  return render(
    <Theme>
      <MavenFacet {...defaultProps} {...props} />
    </Theme>
  );
}

describe('MavenFacet', () => {
  beforeEach(() => {
    mockGetValue.mockImplementation((key: string) => {
      if (key === 'isCloud') return false;
      return false;
    });
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('renders Maven 2 section title', () => {
    renderFacet();
    expect(screen.getByText('Maven 2')).toBeInTheDocument();
  });

  it('displays version policy, layout policy, and content disposition', () => {
    renderFacet();
    expect(screen.getByText('Version Policy')).toBeInTheDocument();
    expect(screen.getByText('Layout Policy')).toBeInTheDocument();
    expect(screen.getByText('Content Disposition')).toBeInTheDocument();
  });

  it('renders with help text for version policy', () => {
    renderFacet();
    expect(screen.getByText(/controls what type of artifacts/i)).toBeInTheDocument();
  });

  it('renders with help text for layout policy', () => {
    renderFacet();
    expect(screen.getByText(/validates that all paths/i)).toBeInTheDocument();
  });

  it('renders with default values when maven config is undefined', () => {
    const formData = { ...defaultFormData, maven: undefined };
    renderFacet({ formData });
    // Should show defaults
    expect(screen.getByText('Version Policy')).toBeInTheDocument();
  });

  it('does not show phishing warning when content disposition is ATTACHMENT', () => {
    renderFacet();
    expect(screen.queryByText(/phishing/i)).not.toBeInTheDocument();
  });

  it('shows phishing warning when content disposition is INLINE', () => {
    const formData = {
      ...defaultFormData,
      maven: { ...defaultFormData.maven, contentDisposition: 'INLINE' },
    };
    renderFacet({ formData });
    expect(screen.getByText(/phishing/i)).toBeInTheDocument();
  });

  it('hides Content Disposition when isCloud is true', () => {
    mockGetValue.mockImplementation((key: string) => {
      if (key === 'isCloud') return true;
      return false;
    });
    renderFacet();
    expect(screen.queryByText('Content Disposition')).not.toBeInTheDocument();
    expect(screen.queryByText(/phishing/i)).not.toBeInTheDocument();
    // Version and Layout Policy should still be visible
    expect(screen.getByText('Version Policy')).toBeInTheDocument();
    expect(screen.getByText('Layout Policy')).toBeInTheDocument();
  });

  it('does not show phishing warning when isCloud is true even with INLINE disposition', () => {
    mockGetValue.mockImplementation((key: string) => {
      if (key === 'isCloud') return true;
      return false;
    });
    const formData = {
      ...defaultFormData,
      maven: { ...defaultFormData.maven, contentDisposition: 'INLINE' },
    };
    renderFacet({ formData });
    expect(screen.queryByText(/phishing/i)).not.toBeInTheDocument();
  });
});
