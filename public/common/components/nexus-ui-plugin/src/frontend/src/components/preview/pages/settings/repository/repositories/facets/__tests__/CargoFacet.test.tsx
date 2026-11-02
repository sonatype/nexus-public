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
import { CargoFacet } from '../CargoFacet';
import { RepositoryFormData, RepositoryType } from '../../types';

const CHECKBOX = /Restrict repository content to authenticated users/i;
const SECTION = /Authentication Requirements/i;

const formDataFor = (type: RepositoryType): RepositoryFormData => ({
  name: 'test',
  format: 'cargo',
  type,
  recipe: `cargo-${type}`,
  online: true,
  storage: { blobStoreName: 'default', strictContentTypeValidation: true },
});

const renderFacet = (
  formData: RepositoryFormData,
  onNestedChange: jest.Mock = jest.fn(),
  isCloud = false
) =>
  render(
    <Theme>
      <CargoFacet formData={formData} onNestedChange={onNestedChange} isCloud={isCloud} />
    </Theme>
  );

describe('CargoFacet', () => {
  it('renders the Authentication Requirements section and checkbox', () => {
    renderFacet(formDataFor('proxy'));

    expect(screen.getByText(SECTION)).toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: CHECKBOX })).toBeInTheDocument();
  });

  // NEXUS-55104: the control must be present for all three Cargo repository
  // types. The backend has supported cargo.requireAuthentication for proxy and
  // group since NEXUS-44559 and for hosted since NEXUS-55006.
  it.each<RepositoryType>(['proxy', 'hosted', 'group'])(
    'renders for cargo %s repositories',
    (type) => {
      renderFacet(formDataFor(type));

      expect(screen.getByRole('checkbox', { name: CHECKBOX })).toBeInTheDocument();
    }
  );

  // Existing repositories created before the setting was exposed have no
  // persisted `cargo` block, so formData.cargo is undefined on edit. They must
  // render unchecked rather than crashing on the optional chain.
  it('renders unchecked when the cargo block is absent', () => {
    renderFacet(formDataFor('proxy'));

    expect(screen.getByRole('checkbox', { name: CHECKBOX })).not.toBeChecked();
  });

  it('renders unchecked when requireAuthentication is false', () => {
    renderFacet({
      ...formDataFor('proxy'),
      cargo: { requireAuthentication: false },
    });

    expect(screen.getByRole('checkbox', { name: CHECKBOX })).not.toBeChecked();
  });

  it('renders checked when requireAuthentication is true', () => {
    renderFacet({
      ...formDataFor('hosted'),
      cargo: { requireAuthentication: true },
    });

    expect(screen.getByRole('checkbox', { name: CHECKBOX })).toBeChecked();
  });

  it('calls onNestedChange with true when toggled on', () => {
    const onNestedChange = jest.fn();
    renderFacet(
      { ...formDataFor('proxy'), cargo: { requireAuthentication: false } },
      onNestedChange
    );

    fireEvent.click(screen.getByRole('checkbox', { name: CHECKBOX }));

    expect(onNestedChange).toHaveBeenCalledWith('cargo', {
      requireAuthentication: true,
    });
  });

  it('calls onNestedChange with false when toggled off', () => {
    const onNestedChange = jest.fn();
    renderFacet(
      { ...formDataFor('proxy'), cargo: { requireAuthentication: true } },
      onNestedChange
    );

    fireEvent.click(screen.getByRole('checkbox', { name: CHECKBOX }));

    expect(onNestedChange).toHaveBeenCalledWith('cargo', {
      requireAuthentication: false,
    });
  });

  // NEXUS-47331 parity: Repo Cloud has no Anonymous Access, so authentication is
  // always required there and the backend forces the value via the
  // nexus.cargo.requireAuthentication.override property. Offering a control the
  // server ignores would mislead the administrator, so hide it entirely.
  it('renders nothing on Repo Cloud deployments', () => {
    const { container } = renderFacet(formDataFor('hosted'), jest.fn(), true);

    // container holds the <Theme> wrapper; the facet renders inside it.
    expect(container.firstChild).toBeEmptyDOMElement();
    expect(screen.queryByText(SECTION)).not.toBeInTheDocument();
    expect(screen.queryByRole('checkbox', { name: CHECKBOX })).not.toBeInTheDocument();
  });

  // NEXUS-55104 (review follow-up): hiding the checkbox on Repo Cloud is not enough on its
  // own — buildRepositoryConfig still unconditionally emits config.cargo from whatever
  // formData.cargo holds, so a seeded/persisted `false` would silently be sent to the server
  // even though no control is shown to the administrator. The server-side override happens to
  // force it back to true today, but the local form state and the value actually saved must not
  // diverge just because the override exists — the effect below keeps them in sync explicitly.
  it('forces requireAuthentication to true on Repo Cloud when the seeded value is false', () => {
    const onNestedChange = jest.fn();
    renderFacet(
      { ...formDataFor('hosted'), cargo: { requireAuthentication: false } },
      onNestedChange,
      true
    );

    expect(onNestedChange).toHaveBeenCalledWith('cargo', { requireAuthentication: true });
  });

  it('does not call onNestedChange on Repo Cloud when requireAuthentication is already true', () => {
    const onNestedChange = jest.fn();
    renderFacet(
      { ...formDataFor('hosted'), cargo: { requireAuthentication: true } },
      onNestedChange,
      true
    );

    expect(onNestedChange).not.toHaveBeenCalled();
  });

  it('does not force requireAuthentication when isCloud is false', () => {
    const onNestedChange = jest.fn();
    renderFacet(
      { ...formDataFor('hosted'), cargo: { requireAuthentication: false } },
      onNestedChange,
      false
    );

    expect(onNestedChange).not.toHaveBeenCalled();
  });
});
