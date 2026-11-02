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
import UIStrings from '../../../../../../../../constants/pages/admin/repository/RepositoriesStrings';

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
  SettingsTextArea: ({ name, label, value, onChange, error, required, rows, monospace, helpText }: any) => (
    <div>
      <label htmlFor={name}>{label}</label>
      {helpText ? <p>{helpText}</p> : null}
      <textarea
        id={name}
        aria-label={label}
        aria-required={required ? 'true' : undefined}
        data-rows={rows}
        data-monospace={monospace ? 'true' : undefined}
        value={value || ''}
        onChange={(e: any) => onChange?.(e.target.value)}
      />
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
  name: 'alpine-repo',
  format: 'alpine',
  type: 'hosted',
  online: true,
  storage: { blobStoreName: 'default', strictContentTypeValidation: true },
  alpineSigning: { keypair: '', passphrase: '' },
};

function renderFacet(
  repoType: 'hosted' | 'proxy' | 'group' = 'hosted',
  overrides: Partial<RepositoryFormData> = {},
  errors: Record<string, string | undefined> = {}
) {
  const formData = { ...BASE_FORM_DATA, type: repoType, ...overrides };
  const onNestedChange = jest.fn();
  render(
    <Theme>
      <AlpineFacet
        formData={formData}
        onNestedChange={onNestedChange}
        errors={errors}
      />
    </Theme>
  );
  return { onNestedChange };
}

// Titles and labels follow the other One UI signing facets (APT/Yum/Terraform use
// '<Format> Signing' plus a description). Only the keypair help text is taken from Classic,
// because the previous string gave no guidance on generating a key.
describe('AlpineFacet signing strings', () => {
  const KEYPAIR_HELP =
    'RSA private key in PKCS#8 format (PEM encoded). Used to sign APKINDEX files. ' +
    'Generate with: openssl genrsa 2048 | openssl pkcs8 -topk8 -nocrypt';

  it('uses a format-prefixed section title, like the sibling facets', () => {
    expect(UIStrings.ALPINE.SIGNING.title).toBe('Alpine Signing');
  });

  it('keeps the section description, like the sibling facets', () => {
    expect(UIStrings.ALPINE.SIGNING.description).toBe(
      'RSA signing configuration for Alpine repositories'
    );
  });

  it('labels the key and passphrase fields', () => {
    expect(UIStrings.ALPINE.SIGNING.KEYPAIR.label).toBe('RSA Signing Key');
    expect(UIStrings.ALPINE.SIGNING.PASSPHRASE.label).toBe('RSA Signing Key Passphrase');
  });

  it("uses Classic's keypair help text so the generation command is shown", () => {
    expect(UIStrings.ALPINE.SIGNING.KEYPAIR.helpText).toBe(KEYPAIR_HELP);
  });

  it('renders the keypair help text on the form', () => {
    render(
      <Theme>
        <AlpineFacet
          formData={BASE_FORM_DATA}
          onNestedChange={jest.fn()}
          errors={{}}
        />
      </Theme>
    );
    expect(screen.getByText(KEYPAIR_HELP)).toBeInTheDocument();
  });

  // Classic shows help text next to the allowBlank error.
  it('shows the help text at the same time as the required error', () => {
    render(
      <Theme>
        <AlpineFacet
          formData={BASE_FORM_DATA}
          onNestedChange={jest.fn()}
          errors={{ 'alpineSigning.keypair': 'This field is required' }}
        />
      </Theme>
    );
    expect(screen.getByText(KEYPAIR_HELP)).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent('This field is required');
  });
});

describe('AlpineFacet', () => {
  it('renders the Alpine Signing section for hosted repositories', () => {
    renderFacet('hosted');
    expect(screen.getByText('Alpine Signing')).toBeInTheDocument();
  });

  it('renders the Alpine Signing section for group repositories', () => {
    renderFacet('group');
    expect(screen.getByText('Alpine Signing')).toBeInTheDocument();
  });

  // Proxy was the NEXUS-55134 regression: the section rendered for hosted and group only.
  it('renders the Alpine Signing section for proxy repositories', () => {
    renderFacet('proxy');
    expect(screen.getByText('Alpine Signing')).toBeInTheDocument();
  });

  it('renders the RSA Signing Key input for proxy repos', () => {
    renderFacet('proxy');
    expect(screen.getByRole('textbox', { name: /signing key/i })).toBeInTheDocument();
  });

  it('renders the RSA Signing Key Passphrase input for proxy repos', () => {
    renderFacet('proxy');
    expect(screen.getByText('RSA Signing Key Passphrase')).toBeInTheDocument();
  });

  it('calls onNestedChange with alpineSigning when the proxy keypair changes', () => {
    const { onNestedChange } = renderFacet('proxy');
    const keypairInput = screen.getByRole('textbox', { name: /signing key/i });
    fireEvent.change(keypairInput, { target: { value: 'proxy-rsa-key' } });
    expect(onNestedChange).toHaveBeenCalledWith('alpineSigning', {
      keypair: 'proxy-rsa-key',
    });
  });

  // Classic sets allowBlank:false on all three recipes.
  it.each(['hosted', 'proxy', 'group'] as const)(
    'marks the keypair required for %s repositories',
    (repoType) => {
      renderFacet(repoType);
      expect(screen.getByRole('textbox', { name: /signing key/i })).toHaveAttribute(
        'aria-required',
        'true'
      );
    }
  );

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
    const keypairInput = screen.getByRole('textbox', { name: /signing key/i });
    fireEvent.change(keypairInput, { target: { value: 'mock-rsa-key-content' } });
    expect(onNestedChange).toHaveBeenCalledWith('alpineSigning', {
      keypair: 'mock-rsa-key-content',
    });
  });

  it('shows existing keypair value from formData', () => {
    renderFacet('hosted', {
      alpineSigning: { keypair: 'existing-key-content', passphrase: '' },
    });
    const keypairInput = screen.getByRole('textbox', { name: /signing key/i });
    expect((keypairInput as HTMLInputElement).value).toBe('existing-key-content');
  });

  // A PEM key spans many lines; Alpine used a single-line input. Siblings use a textarea.
  it.each(['hosted', 'proxy', 'group'] as const)(
    'renders the keypair as a multi-line monospace textarea for %s',
    (repoType) => {
      renderFacet(repoType);
      const keypairInput = screen.getByRole('textbox', { name: /signing key/i });
      expect(keypairInput.tagName).toBe('TEXTAREA');
      expect(keypairInput).toHaveAttribute('data-monospace', 'true');
    }
  );

  // The facet declared the nested shape while the form machine passes flat dotted keys, so
  // no message reached the user. resolveError bridges both.
  it('displays the flat dotted-key validation error for the keypair', () => {
    renderFacet('proxy', {}, {
      'alpineSigning.keypair': 'This field is required',
    });
    expect(screen.getByRole('alert')).toHaveTextContent(
      'This field is required'
    );
  });

  it('displays a nested-shape validation error for the keypair', () => {
    const onNestedChange = jest.fn();
    render(
      <Theme>
        <AlpineFacet
          formData={{ ...BASE_FORM_DATA, type: 'proxy' }}
          onNestedChange={onNestedChange}
          errors={{ alpineSigning: { keypair: 'nested-shape message' } }}
        />
      </Theme>
    );
    expect(screen.getByRole('alert')).toHaveTextContent('nested-shape message');
  });
});
