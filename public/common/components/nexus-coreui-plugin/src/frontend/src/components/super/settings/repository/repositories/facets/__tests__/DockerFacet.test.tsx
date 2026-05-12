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
import { DockerFacet } from '../DockerFacet';
import { RepositoryFormData } from '../../types';

const defaultFormData: RepositoryFormData = {
  name: 'test-docker',
  format: 'docker',
  type: 'hosted',
  online: true,
  storage: { blobStoreName: 'default', strictContentTypeValidation: true },
  docker: { forceBasicAuth: false, v1Enabled: false },
};

const proxyFormData: RepositoryFormData = {
  ...defaultFormData,
  type: 'proxy',
  dockerProxy: { indexType: 'HUB', cacheForeignLayers: false },
};

function renderFacet(props: Partial<React.ComponentProps<typeof DockerFacet>> = {}) {
  const defaultProps = {
    formData: defaultFormData,
    onNestedChange: jest.fn(),
    errors: {},
    repoType: 'hosted' as const,
  };
  return render(
    <Theme>
      <DockerFacet {...defaultProps} {...props} />
    </Theme>
  );
}

describe('DockerFacet', () => {
  it('renders Docker Registry API Support section', () => {
    renderFacet();
    expect(screen.getByText('Docker Registry API Support')).toBeInTheDocument();
  });

  it('renders HTTP and HTTPS connector fields', () => {
    renderFacet();
    expect(screen.getByText('HTTP Connector')).toBeInTheDocument();
    expect(screen.getByText('HTTPS Connector')).toBeInTheDocument();
  });

  it('renders subdomain field', () => {
    renderFacet();
    expect(screen.getByText('Subdomain')).toBeInTheDocument();
  });

  it('renders force basic auth checkbox', () => {
    renderFacet();
    expect(screen.getByText('Force Basic Authentication')).toBeInTheDocument();
  });

  it('renders Docker V1 API checkbox', () => {
    renderFacet();
    expect(screen.getByText('Enable Docker V1 API')).toBeInTheDocument();
  });

  it('does NOT show Docker Index section for hosted repos', () => {
    renderFacet({ repoType: 'hosted' });
    expect(screen.queryByText('Docker Index')).not.toBeInTheDocument();
  });

  it('shows Docker Index section for proxy repos', () => {
    renderFacet({ repoType: 'proxy', formData: proxyFormData });
    expect(screen.getAllByText('Docker Index').length).toBeGreaterThanOrEqual(1);
  });

  it('calls onNestedChange when force auth is toggled', () => {
    const onNestedChange = jest.fn();
    renderFacet({ onNestedChange });

    const checkbox = screen.getByRole('checkbox', { name: /force basic/i });
    fireEvent.click(checkbox);

    expect(onNestedChange).toHaveBeenCalledWith('docker', { forceBasicAuth: true });
  });

  it('calls onNestedChange when V1 is toggled', () => {
    const onNestedChange = jest.fn();
    renderFacet({ onNestedChange });

    const checkbox = screen.getByRole('checkbox', { name: /docker v1/i });
    fireEvent.click(checkbox);

    expect(onNestedChange).toHaveBeenCalledWith('docker', { v1Enabled: true });
  });

  it('shows foreign layer caching for proxy repos', () => {
    renderFacet({ repoType: 'proxy', formData: proxyFormData });
    expect(screen.getByText(/download and cache foreign layers/i)).toBeInTheDocument();
  });
});
