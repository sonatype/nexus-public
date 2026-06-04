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
import { DockerFacet, createRoutingModeChangeHandler, ROUTING_PATH_BASED, ROUTING_CONNECTORS } from '../DockerFacet';
import { RepositoryFormData } from '../../types';

const CONNECTOR_FORM_DATA: RepositoryFormData = {
  name: 'test-docker',
  format: 'docker',
  type: 'hosted',
  online: true,
  storage: { blobStoreName: 'default', strictContentTypeValidation: true },
  docker: { forceBasicAuth: false, v1Enabled: false, pathEnabled: false },
};

const PATH_BASED_FORM_DATA: RepositoryFormData = {
  name: 'test-docker',
  format: 'docker',
  type: 'hosted',
  online: true,
  storage: { blobStoreName: 'default', strictContentTypeValidation: true },
  docker: { forceBasicAuth: false, v1Enabled: false, pathEnabled: true },
};

const DEFAULT_FORM_DATA: RepositoryFormData = {
  name: 'test-docker',
  format: 'docker',
  type: 'hosted',
  online: true,
  storage: { blobStoreName: 'default', strictContentTypeValidation: true },
  docker: { forceBasicAuth: false, v1Enabled: false },
};

const PROXY_FORM_DATA: RepositoryFormData = {
  ...CONNECTOR_FORM_DATA,
  type: 'proxy',
  dockerProxy: { indexType: 'HUB', cacheForeignLayers: false, foreignLayerUrlWhitelist: [] },
};

function renderFacet(props: Partial<React.ComponentProps<typeof DockerFacet>> = {}) {
  const defaultProps = {
    formData: CONNECTOR_FORM_DATA,
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

  it('renders routing mode select', () => {
    renderFacet();
    expect(screen.getByText('Routing Mode')).toBeInTheDocument();
  });

  it('shows connector fields when pathEnabled is false', () => {
    renderFacet({ formData: CONNECTOR_FORM_DATA });
    expect(screen.getByText('HTTP Connector')).toBeInTheDocument();
    expect(screen.getByText('HTTPS Connector')).toBeInTheDocument();
    expect(screen.getByText('Subdomain')).toBeInTheDocument();
  });

  it('hides connector fields when pathEnabled is true', () => {
    renderFacet({ formData: PATH_BASED_FORM_DATA });
    expect(screen.queryByText('HTTP Connector')).not.toBeInTheDocument();
    expect(screen.queryByText('HTTPS Connector')).not.toBeInTheDocument();
    expect(screen.queryByText('Subdomain')).not.toBeInTheDocument();
  });

  it('defaults to connectors when pathEnabled is undefined', () => {
    renderFacet({ formData: DEFAULT_FORM_DATA });
    expect(screen.getByText('HTTP Connector')).toBeInTheDocument();
    expect(screen.getByText('HTTPS Connector')).toBeInTheDocument();
    expect(screen.getByText('Subdomain')).toBeInTheDocument();
  });

  it('always shows force basic auth regardless of routing mode', () => {
    renderFacet({ formData: PATH_BASED_FORM_DATA });
    expect(screen.getByText('Force Basic Authentication')).toBeInTheDocument();
  });

  it('always shows V1 API regardless of routing mode', () => {
    renderFacet({ formData: PATH_BASED_FORM_DATA });
    expect(screen.getByText('Enable Docker V1 API')).toBeInTheDocument();
  });

  it('does not show Docker Index section for hosted repos', () => {
    renderFacet({ repoType: 'hosted' });
    expect(screen.queryByText('Docker Index')).not.toBeInTheDocument();
  });

  it('shows Docker Index section for proxy repos', () => {
    renderFacet({ repoType: 'proxy', formData: PROXY_FORM_DATA });
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
    renderFacet({ repoType: 'proxy', formData: PROXY_FORM_DATA });
    expect(screen.getByText(/download and cache foreign layers/i)).toBeInTheDocument();
  });

  it('displays port validation error when provided', () => {
    const errors = { 'docker.httpPort': 'Port must be between 1 and 65535' };
    renderFacet({ errors });
    expect(screen.getByText('Port must be between 1 and 65535')).toBeInTheDocument();
  });

  describe('createRoutingModeChangeHandler', () => {
    it('clears connector values when switching to path-based', () => {
      const onNestedChange = jest.fn();
      const handler = createRoutingModeChangeHandler(onNestedChange);

      handler(ROUTING_PATH_BASED);

      expect(onNestedChange).toHaveBeenCalledWith('docker', {
        pathEnabled: true,
        httpPort: null,
        httpsPort: null,
        subdomain: null,
      });
    });

    it('sets pathEnabled false when switching to connectors', () => {
      const onNestedChange = jest.fn();
      const handler = createRoutingModeChangeHandler(onNestedChange);

      handler(ROUTING_CONNECTORS);

      expect(onNestedChange).toHaveBeenCalledWith('docker', { pathEnabled: false });
    });

    it('does not clear connector values when switching to connectors', () => {
      const onNestedChange = jest.fn();
      const handler = createRoutingModeChangeHandler(onNestedChange);

      handler(ROUTING_CONNECTORS);

      expect(onNestedChange).not.toHaveBeenCalledWith('docker', expect.objectContaining({
        httpPort: null,
      }));
    });
  });
});
