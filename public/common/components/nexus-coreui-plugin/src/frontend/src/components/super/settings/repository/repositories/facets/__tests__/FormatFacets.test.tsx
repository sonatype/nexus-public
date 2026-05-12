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

  it('renders flat repository checkbox', () => {
    render(
      <Theme>
        <AptFacet
          formData={{ ...baseFormData, format: 'apt' }}
          onNestedChange={jest.fn()}
          errors={{}}
          repoType="proxy"
        />
      </Theme>
    );
    expect(screen.getByText('Flat Repository')).toBeInTheDocument();
  });

  it('shows APT Signing section for hosted repos only', () => {
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
  it('renders Yum Settings section', () => {
    render(
      <Theme>
        <YumFacet formData={{ ...baseFormData, format: 'yum' }} onNestedChange={jest.fn()} errors={{}} repoType="hosted" />
      </Theme>
    );
    expect(screen.getByText('Yum Settings')).toBeInTheDocument();
    expect(screen.getByText('Repodata Depth')).toBeInTheDocument();
  });

  it('shows deploy policy for hosted repos only', () => {
    const { rerender } = render(
      <Theme>
        <YumFacet formData={{ ...baseFormData, format: 'yum' }} onNestedChange={jest.fn()} errors={{}} repoType="hosted" />
      </Theme>
    );
    expect(screen.getByText('Deploy Policy')).toBeInTheDocument();

    rerender(
      <Theme>
        <YumFacet formData={{ ...baseFormData, format: 'yum' }} onNestedChange={jest.fn()} errors={{}} repoType="proxy" />
      </Theme>
    );
    expect(screen.queryByText('Deploy Policy')).not.toBeInTheDocument();
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
});

describe('NpmFacet', () => {
  it('renders npm Settings section', () => {
    render(
      <Theme>
        <NpmFacet formData={{ ...baseFormData, format: 'npm' }} onNestedChange={jest.fn()} />
      </Theme>
    );
    expect(screen.getByText('npm Settings')).toBeInTheDocument();
    expect(screen.getByText('Remove Quarantined Versions')).toBeInTheDocument();
  });

  it('calls onNestedChange when checkbox toggled', () => {
    const onNestedChange = jest.fn();
    render(
      <Theme>
        <NpmFacet formData={{ ...baseFormData, format: 'npm' }} onNestedChange={onNestedChange} />
      </Theme>
    );
    fireEvent.click(screen.getByRole('checkbox'));
    expect(onNestedChange).toHaveBeenCalledWith('npm', { removeQuarantined: true });
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
