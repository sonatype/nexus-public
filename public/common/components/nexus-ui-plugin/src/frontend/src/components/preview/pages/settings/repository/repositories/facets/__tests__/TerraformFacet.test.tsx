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
import { TerraformFacet } from '../TerraformFacet';
import { RepositoryFormData } from '../../types';

jest.mock('../../../../../../shared/form', () => ({
  SettingsFormSection: ({ title, children }: any) => (
    <section><h3>{title}</h3>{children}</section>
  ),
  SettingsTextArea: ({ name, label, value, onChange, error }: any) => (
    <div>
      <label htmlFor={name}>{label}</label>
      <textarea id={name} aria-label={label} value={value || ''} onChange={(e: any) => onChange?.(e.target.value)} />
      {error ? <span role="alert">{error}</span> : null}
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
  name: 'terraform-repo',
  format: 'terraform',
  type: 'hosted',
  online: true,
  storage: { blobStoreName: 'default', strictContentTypeValidation: true },
  terraformSigning: { keypair: '', passphrase: '' },
};

function renderFacet(
  repoType: 'hosted' | 'proxy' | 'group' = 'hosted',
  overrides: Partial<RepositoryFormData> = {},
  errors: Record<string, string | null> = {}
) {
  const formData = { ...BASE_FORM_DATA, type: repoType, ...overrides };
  const onNestedChange = jest.fn();
  const { container } = render(
    <Theme>
      <TerraformFacet
        formData={formData}
        onNestedChange={onNestedChange}
        errors={errors}
        repoType={repoType}
      />
    </Theme>
  );
  return { onNestedChange, container };
}

describe('TerraformFacet', () => {
  it('renders the Terraform Signing section for hosted repositories', () => {
    renderFacet('hosted');
    expect(screen.getByText('Terraform Signing')).toBeInTheDocument();
  });

  it('renders the GPG Signing Key field for hosted repos', () => {
    renderFacet('hosted');
    expect(screen.getByText('GPG Signing Key')).toBeInTheDocument();
  });

  it('renders the GPG Signing Key Passphrase field for hosted repos', () => {
    renderFacet('hosted');
    expect(screen.getByText('GPG Signing Key Passphrase')).toBeInTheDocument();
  });

  it('does not render signing for proxy repositories', () => {
    const { container } = renderFacet('proxy');
    expect(screen.queryByText('Terraform Signing')).not.toBeInTheDocument();
    expect(container.firstChild).toBeEmptyDOMElement();
  });

  it('does not render signing for group repositories', () => {
    const { container } = renderFacet('group');
    expect(screen.queryByText('Terraform Signing')).not.toBeInTheDocument();
    expect(container.firstChild).toBeEmptyDOMElement();
  });

  it('calls onNestedChange with terraformSigning when the keypair changes', () => {
    const { onNestedChange } = renderFacet('hosted');
    const keypair = screen.getByRole('textbox', { name: /gpg signing key/i });
    fireEvent.change(keypair, { target: { value: 'mock-pgp-key' } });
    expect(onNestedChange).toHaveBeenCalledWith('terraformSigning', {
      keypair: 'mock-pgp-key',
    });
  });

  it('calls onNestedChange with terraformSigning when the passphrase changes', () => {
    const { onNestedChange } = renderFacet('hosted');
    const passphrase = screen.getByLabelText('GPG Signing Key Passphrase');
    fireEvent.change(passphrase, { target: { value: 'secret' } });
    expect(onNestedChange).toHaveBeenCalledWith('terraformSigning', {
      passphrase: 'secret',
    });
  });

  it('shows the existing keypair value from formData', () => {
    renderFacet('hosted', {
      terraformSigning: { keypair: 'existing-pgp-key', passphrase: '' },
    });
    const keypair = screen.getByRole('textbox', { name: /gpg signing key/i });
    expect((keypair as HTMLTextAreaElement).value).toBe('existing-pgp-key');
  });

  it('renders without crashing when terraformSigning is absent', () => {
    renderFacet('hosted', { terraformSigning: null });
    const keypair = screen.getByRole('textbox', { name: /gpg signing key/i });
    expect((keypair as HTMLTextAreaElement).value).toBe('');
  });

  it('surfaces the keypair validation error on the field', () => {
    renderFacet('hosted', {}, {
      'terraformSigning.keypair': 'GPG signing key is required for Terraform hosted repositories',
    });
    expect(screen.getByRole('alert')).toHaveTextContent(
      'GPG signing key is required for Terraform hosted repositories'
    );
  });
});
