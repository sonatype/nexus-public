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

import { ProxyFacet } from '../ProxyFacet';
import { RepositoryFormData } from '../../types';

const defaultFormData: RepositoryFormData = {
  name: 'test-repo',
  format: 'maven2',
  type: 'proxy',
  proxy: {
    remoteUrl: 'https://repo1.maven.org/maven2/',
    contentMaxAge: 1440,
    metadataMaxAge: 1440,
  },
};

function renderFacet(props: Partial<React.ComponentProps<typeof ProxyFacet>> = {}) {
  const defaultProps = {
    formData: defaultFormData,
    onChange: jest.fn(),
    onNestedChange: jest.fn(),
    errors: {},
    format: 'maven2',
  };
  return render(
    <Theme>
      <ProxyFacet {...defaultProps} {...props} />
    </Theme>
  );
}

describe('ProxyFacet', () => {
  it('renders Remote Storage field', () => {
    renderFacet();
    const remoteUrlInput = screen.getByDisplayValue('https://repo1.maven.org/maven2/');
    expect(remoteUrlInput).toBeInTheDocument();
    expect(remoteUrlInput).toHaveAttribute('name', 'proxy-remoteUrl');
  });

  it('renders Maximum Component Age field', () => {
    renderFacet();
    expect(screen.getByLabelText(/Maximum Component Age/i)).toBeInTheDocument();
  });

  it('renders Maximum Metadata Age field', () => {
    renderFacet();
    expect(screen.getByLabelText(/Maximum Metadata Age/i)).toBeInTheDocument();
  });

  it('calls onNestedChange when Remote URL is changed', () => {
    const mockOnNestedChange = jest.fn();
    renderFacet({ onNestedChange: mockOnNestedChange });

    const remoteUrlInput = screen.getByDisplayValue('https://repo1.maven.org/maven2/');
    fireEvent.change(remoteUrlInput, { target: { value: 'https://new-url.example.com/' } });

    expect(mockOnNestedChange).toHaveBeenCalledWith('proxy', {
      remoteUrl: 'https://new-url.example.com/',
    });
  });

  it('shows correct placeholder for npm format', () => {
    const formData = {
      ...defaultFormData,
      proxy: { ...defaultFormData.proxy!, remoteUrl: '' },
    };
    renderFacet({ formData, format: 'npm' });

    expect(screen.getByText(/https:\/\/registry\.npmjs\.org/)).toBeInTheDocument();
  });

  it('shows correct placeholder for pypi format', () => {
    const formData = {
      ...defaultFormData,
      proxy: { ...defaultFormData.proxy!, remoteUrl: '' },
    };
    renderFacet({ formData, format: 'pypi' });

    expect(screen.getByText(/https:\/\/pypi\.org/)).toBeInTheDocument();
  });
});
