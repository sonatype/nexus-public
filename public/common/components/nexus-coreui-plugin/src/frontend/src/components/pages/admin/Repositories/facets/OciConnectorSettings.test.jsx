/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import React from 'react';
import {render, screen} from '@testing-library/react';

import OciConnectorSettings from './OciConnectorSettings';
import UIStrings from '../../../../../constants/UIStrings';

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    isProEdition: jest.fn().mockReturnValue(true)
  },
  FormUtils: {
    checkboxProps: jest.fn(() => ({isChecked: false})),
    handleUpdate: jest.fn(() => jest.fn()),
    fieldProps: jest.fn(() => ({value: ''})),
    selectProps: jest.fn((name, state) => {
      // Walk the dotted path so the cosign.enforcement select renders the right option.
      const path = name.split('.');
      let value = state?.context?.data;
      for (const segment of path) {
        value = value?.[segment];
        if (value === undefined) break;
      }
      return {value: value ?? ''};
    })
  }
}));

const {CONNECTORS, COSIGN} = UIStrings.REPOSITORIES.EDITOR.OCI;

describe('OciConnectorSettings', () => {
  const mockSend = jest.fn();

  const makeParentMachine = (overrides = {}) => [
    {
      context: {
        data: {
          name: 'my-oci-repo',
          oci: {
            httpPort: null,
            httpsPort: null,
            forceBasicAuth: false,
            pathEnabled: true,
            subdomain: null,
            cosign: {enforcement: 'NONE'},
            ...overrides
          }
        }
      }
    },
    mockSend
  ];

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders the OCI Registry Connectors caption', () => {
    render(<OciConnectorSettings parentMachine={makeParentMachine()} />);
    expect(screen.getByText(CONNECTORS.CAPTION)).toBeInTheDocument();
  });

  it('renders HTTP and HTTPS connector fields', () => {
    render(<OciConnectorSettings parentMachine={makeParentMachine()} />);
    expect(screen.getByText(CONNECTORS.HTTP.LABEL)).toBeInTheDocument();
    expect(screen.getByText(CONNECTORS.HTTPS.LABEL)).toBeInTheDocument();
  });

  it('renders path-based routing fieldset label', () => {
    render(<OciConnectorSettings parentMachine={makeParentMachine()} />);
    expect(screen.getByText(CONNECTORS.PATH_ENABLED.LABEL)).toBeInTheDocument();
  });

  it('renders force basic auth fieldset label', () => {
    render(<OciConnectorSettings parentMachine={makeParentMachine()} />);
    expect(screen.getByText(CONNECTORS.FORCE_BASIC_AUTH.LABEL)).toBeInTheDocument();
  });

  // NEXUS-53064 B2: anonymous-pull warning banner.
  it('renders an anonymous-pull warning banner under the Force Basic Auth checkbox', () => {
    render(<OciConnectorSettings parentMachine={makeParentMachine()} />);
    expect(screen.getByText(CONNECTORS.FORCE_BASIC_AUTH.WARNING)).toBeInTheDocument();
  });

  it('renders subdomain field when running pro edition', () => {
    render(<OciConnectorSettings parentMachine={makeParentMachine()} />);
    expect(screen.getByText(CONNECTORS.SUBDOMAIN.LABEL)).toBeInTheDocument();
  });

  it('does not render a Docker V1 API toggle (OCI is v2 only)', () => {
    render(<OciConnectorSettings parentMachine={makeParentMachine()} />);
    expect(screen.queryByText(/docker v1 api/i)).not.toBeInTheDocument();
  });

  describe('Cosign Keyless Policy', () => {
    it('renders the cosign caption and enforcement dropdown', () => {
      render(<OciConnectorSettings parentMachine={makeParentMachine()} />);
      expect(screen.getByText(COSIGN.CAPTION)).toBeInTheDocument();
      expect(screen.getByText(COSIGN.ENFORCEMENT.LABEL)).toBeInTheDocument();
    });

    // NEXUS-53064 / UX P0-1: KEYLESS option is hidden until a real verifier
    //   ships. The stub fails open at upload AND pull time, so exposing the
    //   option would mislead admins about supply-chain enforcement.
    it('hides the KEYLESS dropdown option', () => {
      render(<OciConnectorSettings parentMachine={makeParentMachine()} />);
      expect(screen.getByRole('option', {name: COSIGN.ENFORCEMENT.OPTIONS.NONE})).toBeInTheDocument();
      expect(
        screen.queryByRole('option', {name: COSIGN.ENFORCEMENT.OPTIONS.KEYLESS})
      ).not.toBeInTheDocument();
    });

    it('does not render identity or issuer regex inputs (KEYLESS path is hidden)', () => {
      render(<OciConnectorSettings parentMachine={makeParentMachine()} />);
      expect(screen.queryByText(COSIGN.IDENTITY_REGEX.LABEL)).not.toBeInTheDocument();
      expect(screen.queryByText(COSIGN.ISSUER_REGEX.LABEL)).not.toBeInTheDocument();
    });

    it('does not render the keyless stub warning banner', () => {
      render(<OciConnectorSettings parentMachine={makeParentMachine()} />);
      expect(screen.queryByText(COSIGN.KEYLESS_STUB_WARNING)).not.toBeInTheDocument();
    });

    it('does not surface KEYLESS even when stale state is set on the machine', () => {
      // Defence in depth: even if a saved repository carries an old KEYLESS
      // value (e.g. from a database upgrade), the UI must not render the
      // regex inputs or stub banner because the verifier still fails open.
      render(
        <OciConnectorSettings
          parentMachine={makeParentMachine({cosign: {enforcement: 'KEYLESS'}})}
        />
      );
      expect(screen.queryByText(COSIGN.IDENTITY_REGEX.LABEL)).not.toBeInTheDocument();
      expect(screen.queryByText(COSIGN.ISSUER_REGEX.LABEL)).not.toBeInTheDocument();
      expect(screen.queryByText(COSIGN.KEYLESS_STUB_WARNING)).not.toBeInTheDocument();
    });

    it('renders even when cosign block is missing', () => {
      render(
        <OciConnectorSettings
          parentMachine={makeParentMachine({cosign: undefined})}
        />
      );
      expect(screen.getByText(COSIGN.CAPTION)).toBeInTheDocument();
      expect(screen.queryByText(COSIGN.IDENTITY_REGEX.LABEL)).not.toBeInTheDocument();
    });
  });
});
