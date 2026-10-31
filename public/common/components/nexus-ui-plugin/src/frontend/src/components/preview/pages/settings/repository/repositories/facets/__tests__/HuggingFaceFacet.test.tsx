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
import { HuggingFaceFacet } from '../HuggingFaceFacet';
import { RepositoryFormData } from '../../types';

const mockGetValue = jest.fn();

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    useState: (fn: () => any) => fn(),
    state: () => ({
      getValue: mockGetValue,
    }),
  },
}));

const baseFormData: RepositoryFormData = {
  name: 'test',
  format: 'huggingface',
  type: 'proxy',
  recipe: 'huggingface-proxy',
  online: true,
  storage: { blobStoreName: 'default', strictContentTypeValidation: true },
};

describe('HuggingFaceFacet', () => {
  beforeEach(() => {
    mockGetValue.mockReset();
  });

  it('renders nothing when the XET feature flag is off', () => {
    mockGetValue.mockImplementation((key: string) =>
      key === 'nexus.format.huggingface.xet.enabled' ? false : undefined
    );

    render(
      <Theme>
        <HuggingFaceFacet formData={baseFormData} onNestedChange={jest.fn()} />
      </Theme>
    );

    // Facet returns null when flag is off — neither section nor checkbox should render.
    expect(screen.queryByText(/Transfer Protocol/i)).not.toBeInTheDocument();
    expect(screen.queryByRole('checkbox', { name: /Enable XET/i })).not.toBeInTheDocument();
  });

  it('renders the Transfer Protocol section when the flag is on', () => {
    mockGetValue.mockImplementation((key: string) =>
      key === 'nexus.format.huggingface.xet.enabled' ? true : undefined
    );

    render(
      <Theme>
        <HuggingFaceFacet formData={baseFormData} onNestedChange={jest.fn()} />
      </Theme>
    );

    expect(screen.getByText(/Transfer Protocol/i)).toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: /Enable XET/i })).toBeInTheDocument();
  });

  // NEXUS-48685: pre-feature HuggingFace proxies have no persisted `xet` block.
  // Their GET responses omit the field (Jackson NON_NULL), so formData.xet is
  // undefined on edit. Existing repositories must render the XET toggle as
  // UNCHECKED (LFS behavior preserved on upgrade). Create-mode also seeds
  // `xet.enabled=false` explicitly in the form machine so new repos likewise
  // default to unchecked with an explicit xet block in the save payload.
  it('defaults toggle to unchecked when xet block is absent (existing/pre-feature repo)', () => {
    mockGetValue.mockImplementation((key: string) =>
      key === 'nexus.format.huggingface.xet.enabled' ? true : undefined
    );

    render(
      <Theme>
        <HuggingFaceFacet formData={baseFormData} onNestedChange={jest.fn()} />
      </Theme>
    );

    expect(
      screen.getByRole('checkbox', { name: /Enable XET/i }),
    ).not.toBeChecked();
  });

  it('reflects saved xet.enabled=false when editing', () => {
    mockGetValue.mockImplementation((key: string) =>
      key === 'nexus.format.huggingface.xet.enabled' ? true : undefined
    );

    render(
      <Theme>
        <HuggingFaceFacet
          formData={{ ...baseFormData, xet: { enabled: false } }}
          onNestedChange={jest.fn()}
        />
      </Theme>
    );

    expect(screen.getByRole('checkbox', { name: /Enable XET/i })).not.toBeChecked();
  });

  it('calls onNestedChange with updated enabled when toggled', () => {
    mockGetValue.mockImplementation((key: string) =>
      key === 'nexus.format.huggingface.xet.enabled' ? true : undefined
    );
    const onNestedChange = jest.fn();

    // baseFormData has no xet block -> facet renders unchecked (NEXUS-48685
    // upgrade-safe default). Clicking flips it to checked, so the emitted
    // payload is {enabled: true}.
    render(
      <Theme>
        <HuggingFaceFacet formData={baseFormData} onNestedChange={onNestedChange} />
      </Theme>
    );

    fireEvent.click(screen.getByRole('checkbox', { name: /Enable XET/i }));

    expect(onNestedChange).toHaveBeenCalledWith('xet', { enabled: true });
  });
});
