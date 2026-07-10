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
import { OciFacet } from '../OciFacet';
import { RepositoryFormData } from '../../types';

const defaultFormData: RepositoryFormData = {
  name: 'test-oci',
  format: 'oci',
  recipe: 'oci-hosted',
  type: 'hosted',
  online: true,
  storage: { blobStoreName: 'default', strictContentTypeValidation: true },
  oci: { forceBasicAuth: false, pathEnabled: true, cosign: { enforcement: 'NONE' } },
};

function renderFacet(props: Partial<React.ComponentProps<typeof OciFacet>> = {}) {
  const defaultProps = {
    formData: defaultFormData,
    onNestedChange: jest.fn(),
    errors: {},
    repoType: 'hosted' as const,
  };
  return render(
    <Theme>
      <OciFacet {...defaultProps} {...props} />
    </Theme>
  );
}

describe('OciFacet', () => {
  it('renders OCI Registry Connectors section', () => {
    renderFacet();
    expect(screen.getByText('OCI Registry Connectors')).toBeInTheDocument();
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

  it('renders path-based routing checkbox', () => {
    renderFacet();
    expect(screen.getByText('Enable Path-Based Routing')).toBeInTheDocument();
  });

  it('renders force basic auth checkbox', () => {
    renderFacet();
    expect(screen.getByText('Force Basic Authentication')).toBeInTheDocument();
  });

  it('does NOT render Docker V1 API toggle (OCI is v2 only)', () => {
    renderFacet();
    expect(screen.queryByText(/docker v1 api/i)).not.toBeInTheDocument();
  });

  it('does NOT render Docker Index section even for proxy repos', () => {
    const proxyFormData: RepositoryFormData = {
      ...defaultFormData,
      type: 'proxy',
    };
    renderFacet({ repoType: 'proxy', formData: proxyFormData });
    expect(screen.queryByText(/docker index/i)).not.toBeInTheDocument();
  });

  it('calls onNestedChange when force auth is toggled', () => {
    const onNestedChange = jest.fn();
    renderFacet({ onNestedChange });

    const checkbox = screen.getByRole('checkbox', { name: /force basic/i });
    fireEvent.click(checkbox);

    expect(onNestedChange).toHaveBeenCalledWith('oci', { forceBasicAuth: true });
  });

  it('calls onNestedChange when path routing is toggled', () => {
    const onNestedChange = jest.fn();
    renderFacet({ onNestedChange });

    const checkbox = screen.getByRole('checkbox', { name: /path-based routing/i });
    fireEvent.click(checkbox);

    expect(onNestedChange).toHaveBeenCalledWith('oci', { pathEnabled: false });
  });

  it('renders for proxy and group repo types without error', () => {
    const { rerender } = renderFacet({ repoType: 'proxy' });
    expect(screen.getByText('OCI Registry Connectors')).toBeInTheDocument();

    rerender(
      <Theme>
        <OciFacet
          formData={defaultFormData}
          onNestedChange={jest.fn()}
          errors={{}}
          repoType="group"
        />
      </Theme>
    );
    expect(screen.getByText('OCI Registry Connectors')).toBeInTheDocument();
  });

  describe('Cosign Keyless Policy', () => {
    it('renders the cosign policy section', () => {
      renderFacet();
      expect(screen.getByText('Cosign Keyless Policy')).toBeInTheDocument();
      expect(screen.getByText('Enforcement mode')).toBeInTheDocument();
    });

    it('hides identity and issuer regex fields when enforcement is NONE', () => {
      renderFacet();
      expect(screen.queryByText('Identity Regex')).not.toBeInTheDocument();
      expect(screen.queryByText('Issuer Regex')).not.toBeInTheDocument();
    });

    it('defaults enforcement to NONE when cosign block is missing', () => {
      const formDataWithoutCosign: RepositoryFormData = {
        ...defaultFormData,
        oci: { forceBasicAuth: false, pathEnabled: true },
      };
      renderFacet({ formData: formDataWithoutCosign });
      expect(screen.getByText('Cosign Keyless Policy')).toBeInTheDocument();
      expect(screen.queryByText('Identity Regex')).not.toBeInTheDocument();
    });

    it('shows identity and issuer regex fields when enforcement is KEYLESS', () => {
      const keylessFormData: RepositoryFormData = {
        ...defaultFormData,
        oci: {
          forceBasicAuth: false,
          pathEnabled: true,
          cosign: { enforcement: 'KEYLESS', identityRegex: '.*', issuerRegex: '.*' },
        },
      };
      renderFacet({ formData: keylessFormData });
      expect(screen.getByText('Identity Regex')).toBeInTheDocument();
      expect(screen.getByText('Issuer Regex')).toBeInTheDocument();
    });

    it('renders validation errors for missing regexes under KEYLESS', () => {
      const keylessMissing: RepositoryFormData = {
        ...defaultFormData,
        oci: {
          forceBasicAuth: false,
          pathEnabled: true,
          cosign: { enforcement: 'KEYLESS' },
        },
      };
      renderFacet({ formData: keylessMissing });
      expect(
        screen.getByText(/identity regex is required when keyless enforcement is enabled/i)
      ).toBeInTheDocument();
      expect(
        screen.getByText(/issuer regex is required when keyless enforcement is enabled/i)
      ).toBeInTheDocument();
    });

    it('does not render validation errors when regexes are provided', () => {
      const keylessFilled: RepositoryFormData = {
        ...defaultFormData,
        oci: {
          forceBasicAuth: false,
          pathEnabled: true,
          cosign: { enforcement: 'KEYLESS', identityRegex: 'a', issuerRegex: 'b' },
        },
      };
      renderFacet({ formData: keylessFilled });
      expect(
        screen.queryByText(/identity regex is required when keyless enforcement is enabled/i)
      ).not.toBeInTheDocument();
    });
  });

  describe('OCI Proxy section (repoType=proxy)', () => {
    const proxyFormData: RepositoryFormData = {
      ...defaultFormData,
      type: 'proxy',
      recipe: 'oci-proxy',
    };

    it('does not render the OCI Proxy section for hosted repos', () => {
      renderFacet();
      expect(screen.queryByText('OCI Proxy')).not.toBeInTheDocument();
      expect(screen.queryByText('OCI Registry Index')).not.toBeInTheDocument();
    });

    it('does not render the OCI Proxy section for group repos', () => {
      renderFacet({ repoType: 'group' });
      expect(screen.queryByText('OCI Proxy')).not.toBeInTheDocument();
    });

    it('renders the OCI Proxy section for proxy repos', () => {
      renderFacet({ repoType: 'proxy', formData: proxyFormData });
      expect(screen.getByText('OCI Proxy')).toBeInTheDocument();
      expect(screen.getByText('OCI Registry Index')).toBeInTheDocument();
      expect(screen.getByText(/use proxy registry/i)).toBeInTheDocument();
      expect(screen.getByText(/custom index/i)).toBeInTheDocument();
    });

    it('hides the indexUrl field when index source is REGISTRY', () => {
      renderFacet({ repoType: 'proxy', formData: proxyFormData });
      expect(screen.queryByText('OCI Registry Index URL')).not.toBeInTheDocument();
    });

    it('renders the indexUrl + truststore fields when CUSTOM index is selected', () => {
      renderFacet({
        repoType: 'proxy',
        formData: {
          ...proxyFormData,
          oci: {
            ...proxyFormData.oci!,
            ociProxy: { indexType: 'CUSTOM' },
          },
        },
      });
      expect(screen.getByText('OCI Registry Index URL')).toBeInTheDocument();
      expect(
        screen.getByText(/use the nexus truststore for https index access/i)
      ).toBeInTheDocument();
    });

    it('flags missing index URL as a required-field error under CUSTOM mode', () => {
      renderFacet({
        repoType: 'proxy',
        formData: {
          ...proxyFormData,
          oci: {
            ...proxyFormData.oci!,
            ociProxy: { indexType: 'CUSTOM', indexUrl: '' },
          },
        },
      });
      expect(
        screen.getByText(/index url is required when custom index is selected/i)
      ).toBeInTheDocument();
    });

    it('renders the foreign-layer caching toggle', () => {
      renderFacet({ repoType: 'proxy', formData: proxyFormData });
      expect(screen.getByText('Foreign Layer Caching')).toBeInTheDocument();
    });

    it('shows the allow-list rows only when caching is enabled', () => {
      renderFacet({ repoType: 'proxy', formData: proxyFormData });
      expect(screen.queryByText('Foreign Layer Allowed URLs')).not.toBeInTheDocument();

      const { rerender } = renderFacet({
        repoType: 'proxy',
        formData: {
          ...proxyFormData,
          oci: {
            ...proxyFormData.oci!,
            ociProxy: { cacheForeignLayers: true, foreignLayerUrlWhitelist: ['.*'] },
          },
        },
      });
      expect(screen.getAllByText('Foreign Layer Allowed URLs').length).toBeGreaterThan(0);
      // The seeded ".*" row is rendered as the value of the input.
      const rows = screen.getAllByDisplayValue('.*');
      expect(rows.length).toBeGreaterThan(0);
      rerender(
        <Theme>
          <OciFacet
            formData={proxyFormData}
            onNestedChange={jest.fn()}
            errors={{}}
            repoType="proxy"
          />
        </Theme>
      );
    });

    it('seeds a default ".*" row when caching is first enabled with no rows', () => {
      const onNestedChange = jest.fn();
      renderFacet({ repoType: 'proxy', formData: proxyFormData, onNestedChange });
      const cacheToggle = screen.getByRole('checkbox', {
        name: /foreign layer caching/i,
      });
      fireEvent.click(cacheToggle);
      expect(onNestedChange).toHaveBeenCalledWith('oci', {
        ociProxy: expect.objectContaining({
          cacheForeignLayers: true,
          foreignLayerUrlWhitelist: ['.*'],
        }),
      });
    });

    it('switching back to REGISTRY clears the custom index URL', () => {
      const onNestedChange = jest.fn();
      renderFacet({
        repoType: 'proxy',
        onNestedChange,
        formData: {
          ...proxyFormData,
          oci: {
            ...proxyFormData.oci!,
            ociProxy: {
              indexType: 'CUSTOM',
              indexUrl: 'https://idx.example.com',
              useTrustStoreForIndexAccess: true,
            },
          },
        },
      });
      // Click the REGISTRY radio
      const registryLabel = screen.getByText(/use proxy registry/i);
      fireEvent.click(registryLabel);
      expect(onNestedChange).toHaveBeenCalledWith(
        'oci',
        expect.objectContaining({
          ociProxy: expect.objectContaining({
            indexType: 'REGISTRY',
            indexUrl: null,
            useTrustStoreForIndexAccess: false,
          }),
        })
      );
    });
  });
});
