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
  oci: { v1Enabled: false, forceBasicAuth: true, pathEnabled: true },
  cosign: { enforcement: 'NONE' },
};

/** Connector-based routing: the only state in which the port/subdomain inputs are offered. */
const connectorRoutedFormData: RepositoryFormData = {
  ...defaultFormData,
  oci: {
    v1Enabled: false,
    forceBasicAuth: true,
    pathEnabled: false,
    httpPort: 8082,
    httpsPort: 8083,
    subdomain: null,
  },
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

  // The connector fields belong to connector-based routing only. The fixture seeds
  // pathEnabled: true (the create default), so they are hidden until routing is switched over —
  // mirroring DockerFacet. See the 'connector fields' describe block below.
  it('renders HTTP and HTTPS connector fields when path-based routing is off', () => {
    renderFacet({ formData: connectorRoutedFormData });
    expect(screen.getByText('HTTP Connector')).toBeInTheDocument();
    expect(screen.getByText('HTTPS Connector')).toBeInTheDocument();
  });

  it('renders subdomain field when path-based routing is off', () => {
    renderFacet({ formData: connectorRoutedFormData });
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

    // Fixture seeds forceBasicAuth: true (the REST model default), so toggling clears it.
    expect(onNestedChange).toHaveBeenCalledWith('oci', { forceBasicAuth: false });
  });

  it('defaults Force Basic Authentication to checked when the oci block is absent', () => {
    // The REST model default is true and cloud rejects false; an unchecked default
    // would silently send forceBasicAuth: false on create.
    renderFacet({ formData: { ...defaultFormData, oci: undefined } });
    expect(screen.getByRole('checkbox', { name: /force basic/i })).toBeChecked();
  });

  it('calls onNestedChange when path routing is toggled', () => {
    const onNestedChange = jest.fn();
    renderFacet({ onNestedChange });

    const checkbox = screen.getByRole('checkbox', { name: /path-based routing/i });
    fireEvent.click(checkbox);

    expect(onNestedChange).toHaveBeenCalledWith('oci', { pathEnabled: false });
  });

  // Regression cover for NEXUS-54771. The connector inputs used to render alongside the
  // path-routing checkbox regardless of its state, while validateRepository skipped the port
  // range check whenever pathEnabled was true. Because pathEnabled defaults to true, the default
  // state accepted an out-of-range port client-side and failed with a server 400. Hiding the
  // inputs (and clearing the values on toggle) is what makes that skip sound.
  describe('connector fields follow path-based routing', () => {
    it('hides the connector inputs when path-based routing is enabled', () => {
      renderFacet();

      expect(screen.getByRole('checkbox', { name: /path-based routing/i })).toBeChecked();
      expect(screen.queryByText('HTTP Connector')).not.toBeInTheDocument();
      expect(screen.queryByText('HTTPS Connector')).not.toBeInTheDocument();
      expect(screen.queryByText('Subdomain')).not.toBeInTheDocument();
    });

    it('shows the connector inputs when path-based routing is disabled', () => {
      renderFacet({ formData: connectorRoutedFormData });

      expect(screen.getByRole('checkbox', { name: /path-based routing/i })).not.toBeChecked();
      expect(screen.getByText('HTTP Connector')).toBeInTheDocument();
      expect(screen.getByText('HTTPS Connector')).toBeInTheDocument();
      expect(screen.getByText('Subdomain')).toBeInTheDocument();
    });

    it('clears the connector values when switching to path-based routing', () => {
      const onNestedChange = jest.fn();
      renderFacet({ formData: connectorRoutedFormData, onNestedChange });

      fireEvent.click(screen.getByRole('checkbox', { name: /path-based routing/i }));

      // A port left behind would stay bound after save, and would also bypass the port-range
      // check that validateRepository skips under path routing.
      expect(onNestedChange).toHaveBeenCalledWith('oci', {
        pathEnabled: true,
        httpPort: null,
        httpsPort: null,
        subdomain: null,
      });
    });
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

  describe('cloud (isCloud=true)', () => {
    // The backend converter rejects httpPort, httpsPort, a non-empty subdomain and
    // forceBasicAuth=false on cloud, so the UI must not offer any of them.
    it('hides the connector and subdomain fields', () => {
      renderFacet({ isCloud: true });
      expect(screen.queryByText('HTTP Connector')).not.toBeInTheDocument();
      expect(screen.queryByText('HTTPS Connector')).not.toBeInTheDocument();
      expect(screen.queryByText('Subdomain')).not.toBeInTheDocument();
      expect(screen.queryByText('Enable Path-Based Routing')).not.toBeInTheDocument();
    });

    it('still renders Force Basic Authentication, but locked on', () => {
      renderFacet({ isCloud: true });
      const checkbox = screen.getByRole('checkbox', { name: /force basic/i });
      expect(checkbox).toBeChecked();
      expect(checkbox).toBeDisabled();
    });

    it('forces path-based routing and clears connector fields', () => {
      const onNestedChange = jest.fn();
      renderFacet({
        isCloud: true,
        onNestedChange,
        formData: {
          ...defaultFormData,
          oci: {
            v1Enabled: false,
            forceBasicAuth: false,
            pathEnabled: false,
            httpPort: 8082,
            httpsPort: 8083,
            subdomain: 'oci-hosted',
          },
        },
      });
      expect(onNestedChange).toHaveBeenCalledWith('oci', {
        pathEnabled: true,
        forceBasicAuth: true,
        httpPort: null,
        httpsPort: null,
        subdomain: null,
      });
    });

    it('does not fight the user once cloud-safe values are already set', () => {
      const onNestedChange = jest.fn();
      renderFacet({
        isCloud: true,
        onNestedChange,
        formData: {
          ...defaultFormData,
          oci: {
            v1Enabled: false,
            forceBasicAuth: true,
            pathEnabled: true,
            httpPort: null,
            httpsPort: null,
            subdomain: null,
          },
        },
      });
      expect(onNestedChange).not.toHaveBeenCalled();
    });

    it('keeps the connector fields on self-hosted', () => {
      // Connector-routed fixture: on self-hosted the fields are offered, unlike cloud where the
      // whole block is withheld regardless of the routing mode.
      renderFacet({ isCloud: false, formData: connectorRoutedFormData });
      expect(screen.getByText('HTTP Connector')).toBeInTheDocument();
      expect(screen.getByText('Subdomain')).toBeInTheDocument();
      expect(screen.getByRole('checkbox', { name: /force basic/i })).toBeEnabled();
    });

    it('withholds the routing checkbox on cloud even under connector routing', () => {
      // pathEnabled=false must not surface the connector inputs on cloud: the converter rejects
      // ports and subdomains there, and the effect below forces path routing back on.
      renderFacet({ isCloud: true, formData: connectorRoutedFormData });
      expect(screen.queryByText('Enable Path-Based Routing')).not.toBeInTheDocument();
      expect(screen.queryByText('HTTP Connector')).not.toBeInTheDocument();
      expect(screen.queryByText('Subdomain')).not.toBeInTheDocument();
    });
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
        cosign: undefined,
      };
      renderFacet({ formData: formDataWithoutCosign });
      expect(screen.getByText('Cosign Keyless Policy')).toBeInTheDocument();
      expect(screen.queryByText('Identity Regex')).not.toBeInTheDocument();
    });

    // The server rejects KEYLESS unconditionally (HTTP 400, verified live), so offering it made
    // every such save fail and persist nothing. It must not be selectable.
    // SettingsSelect renders a Radix combobox whose options mount only while it is open, so
    // open it and assert on the listbox contents.
    describe('enforcement mode options', () => {
      // JSDOM does not implement Element.scrollIntoView; Radix Select calls it when the listbox
      // opens. Same shim as TablePagination.test.tsx.
      let originalScrollIntoView: typeof Element.prototype.scrollIntoView;
      beforeEach(() => {
        originalScrollIntoView = Element.prototype.scrollIntoView;
        Element.prototype.scrollIntoView = jest.fn();
      });
      afterEach(() => {
        Element.prototype.scrollIntoView = originalScrollIntoView;
      });

      it('does not offer Keyless as a selectable enforcement mode', async () => {
        renderFacet();
        fireEvent.click(screen.getByRole('combobox', { name: /enforcement mode/i }));

        const options = await screen.findAllByRole('option');
        const labels = options.map((o) => o.textContent);
        expect(labels).toEqual(['Off (no cosign enforcement)']);
        expect(labels.join(' ')).not.toMatch(/keyless/i);
      });
    });

    describe('legacy persisted KEYLESS policy', () => {
      const legacyKeyless: RepositoryFormData = {
        ...defaultFormData,
        cosign: {
          enforcement: 'KEYLESS',
          identityRegex: '^https://github\\.com/acme/.*$',
          issuerRegex: '^https://token\\.example$',
        },
      };

      // A repository saved before the server-side rejection landed still reports KEYLESS on read.
      // Opening its form must not crash and must not hide the stored policy.
      it('loads without crashing and discloses the stored policy read-only', () => {
        renderFacet({ formData: legacyKeyless });
        expect(screen.getByTestId('oci-cosign-legacy-keyless')).toBeInTheDocument();
        expect(screen.getByText(/keyless \(not available in this release\)/i)).toBeInTheDocument();
        expect(
          screen.getByTestId('oci-cosign-legacy-keyless').textContent
        ).toMatch(/signature verification is/i);
      });

      // Saving downgrades to NONE (the server refuses KEYLESS), so the operator has to be told
      // before they save rather than discovering it afterwards.
      it('warns that saving will reset enforcement to Off', () => {
        renderFacet({ formData: legacyKeyless });
        expect(screen.getByText(/saving this form will reset enforcement to off/i))
          .toBeInTheDocument();
      });

      it('surfaces the stored identity and issuer regexes as informational text', () => {
        renderFacet({ formData: legacyKeyless });
        expect(screen.getByText('^https://github\\.com/acme/.*$')).toBeInTheDocument();
        expect(screen.getByText('^https://token\\.example$')).toBeInTheDocument();
      });

      // The select cannot represent KEYLESS (it is not an option), so it falls back to NONE
      // rather than rendering an out-of-range value.
      it('falls back to NONE in the select without rewriting form state', () => {
        const onNestedChange = jest.fn();
        renderFacet({ formData: legacyKeyless, onNestedChange });
        // Merely opening the form must not silently patch the stored policy.
        expect(onNestedChange).not.toHaveBeenCalledWith('cosign', expect.anything());
      });

      it('does not show the legacy notice for a NONE policy', () => {
        renderFacet();
        expect(screen.queryByTestId('oci-cosign-legacy-keyless')).not.toBeInTheDocument();
      });
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

    it('renders the indexUrl field when CUSTOM index is selected', () => {
      renderFacet({
        repoType: 'proxy',
        formData: {
          ...proxyFormData,
          ociProxy: { indexType: 'CUSTOM' },
        },
      });
      expect(screen.getByText('OCI Registry Index URL')).toBeInTheDocument();
    });

    // The requirement itself now lives in validateRepository so it can block Save; the facet's
    // job is only to render whatever error the machine reports. Rendering the message locally
    // (as this test previously asserted) showed it without gating advancement.
    it('marks the index URL field required under CUSTOM mode', () => {
      renderFacet({
        repoType: 'proxy',
        formData: {
          ...proxyFormData,
          ociProxy: { indexType: 'CUSTOM', indexUrl: '' },
        },
      });
      expect(screen.getByText('OCI Registry Index URL')).toBeInTheDocument();
      expect(
        screen.queryByText(/index url is required when custom index is selected/i)
      ).not.toBeInTheDocument();
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
          ociProxy: { cacheForeignLayers: true, foreignLayerUrlWhitelist: ['.*'] },
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
      expect(onNestedChange).toHaveBeenCalledWith(
        'ociProxy',
        expect.objectContaining({
          cacheForeignLayers: true,
          foreignLayerUrlWhitelist: ['.*'],
        })
      );
    });

    it('switching back to REGISTRY clears the custom index URL', () => {
      const onNestedChange = jest.fn();
      renderFacet({
        repoType: 'proxy',
        onNestedChange,
        formData: {
          ...proxyFormData,
          ociProxy: {
            indexType: 'CUSTOM',
            indexUrl: 'https://idx.example.com',
          },
        },
      });
      // Click the REGISTRY radio
      const registryLabel = screen.getByText(/use proxy registry/i);
      fireEvent.click(registryLabel);
      expect(onNestedChange).toHaveBeenCalledWith(
        'ociProxy',
        expect.objectContaining({
          indexType: 'REGISTRY',
          indexUrl: null,
        })
      );
    });

    // The OCI backend has no truststore-for-index concept at any layer, so a control implying it
    // governs certificate trust must not be rendered.
    it('does not render a truststore toggle for index access', () => {
      renderFacet({
        repoType: 'proxy',
        formData: {
          ...proxyFormData,
          ociProxy: { indexType: 'CUSTOM', indexUrl: 'https://idx.example.com' },
        },
      });
      expect(screen.queryByRole('checkbox', { name: /nexus truststore/i })).not.toBeInTheDocument();
    });

    // The indexUrl requirement is enforced by validateRepository (flat key), which also blocks
    // Save. Previously the facet rendered the message locally at render time, so it appeared
    // without ever gating advancement.
    it('renders the indexUrl error from the flat validation key', () => {
      renderFacet({
        repoType: 'proxy',
        formData: {
          ...proxyFormData,
          ociProxy: { indexType: 'CUSTOM', indexUrl: '' },
        },
        errors: { 'ociProxy.indexUrl': 'Index URL is required when Custom index is selected' },
      });
      expect(
        screen.getByText(/index url is required when custom index is selected/i)
      ).toBeInTheDocument();
    });
  });

  // Production only ever passes the flat dot-notation map (see resolveError.ts). Reading these
  // nested-only would render no message at all under the form machine.
  describe('validation message rendering from flat keys', () => {
    it.each([
      ['oci.httpPort', 'HTTP port out of range'],
      ['oci.httpsPort', 'HTTPS port out of range'],
      ['oci.subdomain', 'Subdomain is invalid'],
    ])('renders %s from the flat errors map', (key, message) => {
      // Connector-routed: these three fields only exist under connector-based routing, which is
      // also the only state in which validateRepository produces port errors for them.
      renderFacet({ formData: connectorRoutedFormData, errors: { [key]: message } });
      expect(screen.getByText(message)).toBeInTheDocument();
    });

    it.each([
      ['oci.httpPort', 'nested http port message', { oci: { httpPort: 'nested http port message' } }],
      ['oci.subdomain', 'nested subdomain message', { oci: { subdomain: 'nested subdomain message' } }],
    ])('still renders %s from the nested shape', (_key, message, errors) => {
      renderFacet({ formData: connectorRoutedFormData, errors: errors as never });
      expect(screen.getByText(message)).toBeInTheDocument();
    });
  });
});
