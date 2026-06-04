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
import { AptFacet } from '../AptFacet';
import { YumFacet } from '../YumFacet';
import { NugetFacet } from '../NugetFacet';
import { NpmFacet } from '../NpmFacet';
import { PyPiFacet } from '../PyPiFacet';
import { RawFacet } from '../RawFacet';
import { RepositoryFormData } from '../../types';

const baseFormData: RepositoryFormData = {
  name: 'test',
  format: 'raw',
  type: 'hosted',
  online: true,
  storage: { blobStoreName: 'default', strictContentTypeValidation: true },
};

describe('AptFacet', () => {
  it('renders APT Settings section', () => {
    render(
      <Theme>
        <AptFacet
          formData={{ ...baseFormData, format: 'apt', apt: { distribution: 'focal' } }}
          onNestedChange={jest.fn()}
          errors={{}}
          repoType="proxy"
        />
      </Theme>
    );
    expect(screen.getByText('APT Settings')).toBeInTheDocument();
    expect(screen.getByText('Distribution')).toBeInTheDocument();
  });

  it('shouldShowFlatAndEnforceDistributionForProxyOnly', () => {
    const { rerender } = render(
      <Theme>
        <AptFacet formData={{ ...baseFormData, format: 'apt' }} onNestedChange={jest.fn()} errors={{}} repoType="proxy" />
      </Theme>
    );
    expect(screen.getByText('Flat Repository')).toBeInTheDocument();
    expect(screen.getByText('Enforce Distribution')).toBeInTheDocument();

    rerender(
      <Theme>
        <AptFacet formData={{ ...baseFormData, format: 'apt' }} onNestedChange={jest.fn()} errors={{}} repoType="hosted" />
      </Theme>
    );
    expect(screen.queryByText('Flat Repository')).not.toBeInTheDocument();
    expect(screen.queryByText('Enforce Distribution')).not.toBeInTheDocument();
  });

  it('shouldShowSigningForHostedAndProxy', () => {
    const { rerender } = render(
      <Theme>
        <AptFacet formData={{ ...baseFormData, format: 'apt' }} onNestedChange={jest.fn()} errors={{}} repoType="hosted" />
      </Theme>
    );
    expect(screen.getByText('APT Signing')).toBeInTheDocument();

    rerender(
      <Theme>
        <AptFacet formData={{ ...baseFormData, format: 'apt' }} onNestedChange={jest.fn()} errors={{}} repoType="proxy" />
      </Theme>
    );
    expect(screen.getByText('APT Signing')).toBeInTheDocument();

    rerender(
      <Theme>
        <AptFacet formData={{ ...baseFormData, format: 'apt' }} onNestedChange={jest.fn()} errors={{}} repoType="group" />
      </Theme>
    );
    expect(screen.queryByText('APT Signing')).not.toBeInTheDocument();
  });

  it('calls onNestedChange when distribution changes', () => {
    const onNestedChange = jest.fn();
    render(
      <Theme>
        <AptFacet formData={{ ...baseFormData, format: 'apt' }} onNestedChange={onNestedChange} errors={{}} repoType="proxy" />
      </Theme>
    );
    const input = screen.getByPlaceholderText('e.g., bionic');
    fireEvent.change(input, { target: { value: 'jammy' } });
    expect(onNestedChange).toHaveBeenCalledWith('apt', { distribution: 'jammy' });
  });
});

describe('YumFacet', () => {
  it('shouldShowRepodataDepthAndDeployPolicyForHostedOnly', () => {
    const { rerender } = render(
      <Theme>
        <YumFacet formData={{ ...baseFormData, format: 'yum' }} onNestedChange={jest.fn()} errors={{}} repoType="hosted" />
      </Theme>
    );
    expect(screen.getByText('Yum Settings')).toBeInTheDocument();
    expect(screen.getByText('Repodata Depth')).toBeInTheDocument();
    expect(screen.getByText('Deploy Policy')).toBeInTheDocument();

    rerender(
      <Theme>
        <YumFacet formData={{ ...baseFormData, format: 'yum' }} onNestedChange={jest.fn()} errors={{}} repoType="proxy" />
      </Theme>
    );
    expect(screen.queryByText('Repodata Depth')).not.toBeInTheDocument();
    expect(screen.queryByText('Deploy Policy')).not.toBeInTheDocument();
  });

  it('shouldShowSigningFieldsForProxyAndGroupOnly', () => {
    const { rerender } = render(
      <Theme>
        <YumFacet formData={{ ...baseFormData, format: 'yum' }} onNestedChange={jest.fn()} errors={{}} repoType="proxy" />
      </Theme>
    );
    expect(screen.getByText('Signing Key')).toBeInTheDocument();
    expect(screen.getByText('Passphrase')).toBeInTheDocument();

    rerender(
      <Theme>
        <YumFacet formData={{ ...baseFormData, format: 'yum' }} onNestedChange={jest.fn()} errors={{}} repoType="group" />
      </Theme>
    );
    expect(screen.getByText('Signing Key')).toBeInTheDocument();
    expect(screen.getByText('Passphrase')).toBeInTheDocument();

    rerender(
      <Theme>
        <YumFacet formData={{ ...baseFormData, format: 'yum' }} onNestedChange={jest.fn()} errors={{}} repoType="hosted" />
      </Theme>
    );
    expect(screen.queryByText('Signing Key')).not.toBeInTheDocument();
    expect(screen.queryByText('Passphrase')).not.toBeInTheDocument();
  });

  it('shouldShowPassphraseValueWhenProvided', () => {
    const formDataWithSigning = {
      ...baseFormData,
      format: 'yum',
      yumSigning: { keypair: 'existing-gpg-key', passphrase: 'mypass' },
    };
    render(
      <Theme>
        <YumFacet formData={formDataWithSigning as any} onNestedChange={jest.fn()} errors={{}} repoType="proxy" />
      </Theme>
    );
    const passphraseInput = screen.getByTestId('password-yumSigning-passphrase');
    expect(passphraseInput).toHaveAttribute('value', 'mypass');
  });

  it('shouldShowEmptyPassphraseWhenNotProvided', () => {
    const formDataWithSigning = {
      ...baseFormData,
      format: 'yum',
      yumSigning: { keypair: 'existing-gpg-key', passphrase: null },
    };
    render(
      <Theme>
        <YumFacet formData={formDataWithSigning as any} onNestedChange={jest.fn()} errors={{}} repoType="proxy" />
      </Theme>
    );
    const passphraseInput = screen.getByTestId('password-yumSigning-passphrase');
    expect(passphraseInput).toHaveAttribute('value', '');
  });
});

describe('NugetFacet', () => {
  it('renders NuGet Settings section', () => {
    render(
      <Theme>
        <NugetFacet
          formData={{ ...baseFormData, format: 'nuget', nugetProxy: { queryCacheItemMaxAge: 3600, nugetVersion: 'V3' } }}
          onNestedChange={jest.fn()}
          errors={{}}
        />
      </Theme>
    );
    expect(screen.getByText('NuGet Settings')).toBeInTheDocument();
    expect(screen.getByText('Query Cache Item Max Age')).toBeInTheDocument();
    expect(screen.getByText('NuGet Protocol Version')).toBeInTheDocument();
  });

  it('displays validation error for query cache age', () => {
    render(
      <Theme>
        <NugetFacet
          formData={{ ...baseFormData, format: 'nuget', nugetProxy: { queryCacheItemMaxAge: -1, nugetVersion: 'V3' } }}
          onNestedChange={jest.fn()}
          errors={{ nugetProxy: { queryCacheItemMaxAge: 'Must be a positive number' } }}
        />
      </Theme>
    );
    expect(screen.getByText('Must be a positive number')).toBeInTheDocument();
  });

  it('calls onNestedChange when cache age changes', () => {
    const onNestedChange = jest.fn();
    render(
      <Theme>
        <NugetFacet
          formData={{ ...baseFormData, format: 'nuget', nugetProxy: { queryCacheItemMaxAge: 3600, nugetVersion: 'V3' } }}
          onNestedChange={onNestedChange}
          errors={{}}
        />
      </Theme>
    );
    const input = screen.getByDisplayValue('3600');
    fireEvent.change(input, { target: { value: '7200' } });
    expect(onNestedChange).toHaveBeenCalledWith('nugetProxy', { queryCacheItemMaxAge: 7200 });
  });
});

describe('NpmFacet', () => {
  it('renders npm Settings section when firewall features enabled', () => {
    render(
      <Theme>
        <NpmFacet formData={{ ...baseFormData, format: 'npm' }} onNestedChange={jest.fn()} showFirewallFeatures />
      </Theme>
    );
    expect(screen.getByText('npm Settings')).toBeInTheDocument();
    expect(screen.getByText('Filter component versions that fail Sonatype Repository Firewall policy')).toBeInTheDocument();
  });

  it('does not render when firewall features disabled', () => {
    render(
      <Theme>
        <NpmFacet formData={{ ...baseFormData, format: 'npm' }} onNestedChange={jest.fn()} />
      </Theme>
    );
    expect(screen.queryByText('npm Settings')).not.toBeInTheDocument();
  });

  it('calls onNestedChange when checkbox toggled', () => {
    const onNestedChange = jest.fn();
    render(
      <Theme>
        <NpmFacet formData={{ ...baseFormData, format: 'npm' }} onNestedChange={onNestedChange} showFirewallFeatures />
      </Theme>
    );
    fireEvent.click(screen.getByRole('checkbox'));
    expect(onNestedChange).toHaveBeenCalledWith('npm', { removeQuarantinedVersions: true });
  });
});

describe('PyPiFacet', () => {
  it('renders PyPI Settings section with Remote Index Path', () => {
    render(
      <Theme>
        <PyPiFacet formData={{ ...baseFormData, format: 'pypi' }} onNestedChange={jest.fn()} />
      </Theme>
    );
    expect(screen.getByText('PyPI Settings')).toBeInTheDocument();
    expect(screen.getByText('Remote Index Path')).toBeInTheDocument();
  });

  it('defaults Remote Index Path to /simple', () => {
    render(
      <Theme>
        <PyPiFacet formData={{ ...baseFormData, format: 'pypi' }} onNestedChange={jest.fn()} />
      </Theme>
    );
    expect(screen.getByDisplayValue('/simple')).toBeInTheDocument();
  });

  it('calls onNestedChange when Remote Index Path changes', () => {
    const onNestedChange = jest.fn();
    render(
      <Theme>
        <PyPiFacet formData={{ ...baseFormData, format: 'pypi' }} onNestedChange={onNestedChange} />
      </Theme>
    );
    fireEvent.change(screen.getByDisplayValue('/simple'), { target: { value: '' } });
    expect(onNestedChange).toHaveBeenCalledWith('pypi', { indexPath: '' });
  });

  it('shows firewall checkbox when showFirewallFeatures is true', () => {
    render(
      <Theme>
        <PyPiFacet formData={{ ...baseFormData, format: 'pypi' }} onNestedChange={jest.fn()} showFirewallFeatures />
      </Theme>
    );
    expect(screen.getByText('Filter component versions that fail Sonatype Repository Firewall policy')).toBeInTheDocument();
  });

  it('hides firewall checkbox when showFirewallFeatures is false', () => {
    render(
      <Theme>
        <PyPiFacet formData={{ ...baseFormData, format: 'pypi' }} onNestedChange={jest.fn()} />
      </Theme>
    );
    expect(screen.queryByText('Filter component versions that fail Sonatype Repository Firewall policy')).not.toBeInTheDocument();
  });
});

describe('RawFacet', () => {
  it('renders Raw Settings section', () => {
    render(
      <Theme>
        <RawFacet formData={{ ...baseFormData, format: 'raw' }} onNestedChange={jest.fn()} />
      </Theme>
    );
    expect(screen.getByText('Raw Settings')).toBeInTheDocument();
    expect(screen.getByText('Content Disposition')).toBeInTheDocument();
  });

  it('displays content disposition options', () => {
    render(
      <Theme>
        <RawFacet formData={{ ...baseFormData, format: 'raw', raw: { contentDisposition: 'ATTACHMENT' } }} onNestedChange={jest.fn()} />
      </Theme>
    );
    expect(screen.getByText(/content is displayed inline|downloaded as an attachment/i)).toBeInTheDocument();
  });

  it('does not show phishing warning when content disposition is ATTACHMENT', () => {
    render(
      <Theme>
        <RawFacet formData={{ ...baseFormData, format: 'raw', raw: { contentDisposition: 'ATTACHMENT' } }} onNestedChange={jest.fn()} />
      </Theme>
    );
    expect(screen.queryByText(/phishing/i)).not.toBeInTheDocument();
  });

  it('shows phishing warning when content disposition is INLINE', () => {
    render(
      <Theme>
        <RawFacet formData={{ ...baseFormData, format: 'raw', raw: { contentDisposition: 'INLINE' } }} onNestedChange={jest.fn()} />
      </Theme>
    );
    expect(screen.getByText(/phishing/i)).toBeInTheDocument();
  });
});
