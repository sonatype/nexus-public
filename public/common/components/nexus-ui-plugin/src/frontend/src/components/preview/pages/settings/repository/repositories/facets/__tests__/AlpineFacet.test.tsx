/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/oss/attributions.
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
import { AlpineFacet } from '../AlpineFacet';
import { RepositoryFormData } from '../../types';

jest.mock('../../../../../../shared/form', () => ({
  SettingsFormSection: ({ title, children }: any) => (
    <section><h3>{title}</h3>{children}</section>
  ),
  SettingsTextInput: ({ name, label, value, onChange }: any) => (
    <div>
      <label htmlFor={name}>{label}</label>
      <input id={name} aria-label={label} value={value || ''} onChange={(e: any) => onChange?.(e.target.value)} />
    </div>
  ),
  SettingsPasswordInput: ({ name, label, value, onChange }: any) => (
    <div>
      <label htmlFor={name}>{label}</label>
      <input id={name} type="password" aria-label={label} value={value || ''} onChange={(e: any) => onChange?.(e.target.value)} />
    </div>
  ),
}));

const BASE_FORM_DATA: RepositoryFormData = {
  name: 'alpine-repo',
  format: 'alpine',
  type: 'hosted',
  online: true,
  storage: { blobStoreName: 'default', strictContentTypeValidation: true },
  alpineSigning: { keypair: '', passphrase: '' },
};

function renderFacet(
  repoType: 'hosted' | 'proxy' | 'group' = 'hosted',
  overrides: Partial<RepositoryFormData> = {}
) {
  const formData = { ...BASE_FORM_DATA, type: repoType, ...overrides };
  const onNestedChange = jest.fn();
  render(
    <Theme>
      <AlpineFacet
        formData={formData}
        onNestedChange={onNestedChange}
        errors={{}}
        repoType={repoType}
      />
    </Theme>
  );
  return { onNestedChange };
}

describe('AlpineFacet', () => {
  it('renders the Alpine Signing section for hosted repositories', () => {
    renderFacet('hosted');
    expect(screen.getByText('Alpine Signing')).toBeInTheDocument();
  });

  it('renders the Alpine Signing section for group repositories', () => {
    renderFacet('group');
    expect(screen.getByText('Alpine Signing')).toBeInTheDocument();
  });

  it('does not render the Alpine Signing section for proxy repositories', () => {
    renderFacet('proxy');
    expect(screen.queryByText('Alpine Signing')).not.toBeInTheDocument();
  });

  it('renders the RSA Signing Key input for hosted repos', () => {
    renderFacet('hosted');
    expect(screen.getByText('RSA Signing Key')).toBeInTheDocument();
  });

  it('renders the RSA Signing Key Passphrase input for hosted repos', () => {
    renderFacet('hosted');
    expect(screen.getByText('RSA Signing Key Passphrase')).toBeInTheDocument();
  });

  it('calls onNestedChange with alpineSigning when keypair changes', () => {
    const { onNestedChange } = renderFacet('hosted');
    const keypairInput = screen.getByRole('textbox', { name: /rsa signing key/i });
    fireEvent.change(keypairInput, { target: { value: 'mock-rsa-key-content' } });
    expect(onNestedChange).toHaveBeenCalledWith('alpineSigning', {
      keypair: 'mock-rsa-key-content',
    });
  });

  it('shows existing keypair value from formData', () => {
    renderFacet('hosted', {
      alpineSigning: { keypair: 'existing-key-content', passphrase: '' },
    });
    const keypairInput = screen.getByRole('textbox', { name: /rsa signing key/i });
    expect((keypairInput as HTMLInputElement).value).toBe('existing-key-content');
  });

  it('renders nothing for proxy repos (empty fragment)', () => {
    const { container } = render(
      <Theme>
        <AlpineFacet
          formData={{ ...BASE_FORM_DATA, type: 'proxy' }}
          onNestedChange={jest.fn()}
          errors={{}}
          repoType="proxy"
        />
      </Theme>
    );
    expect(container.firstChild).toBeEmptyDOMElement();
  });
});
