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
import {render, screen} from '@testing-library/react';
import ReadOnlyField from './ReadOnlyField';

describe('readOnlyRenderField', () => {
  const label = 'Best Label';
  const data = 'email@test.com';

  const selectors = {
    label: () => screen.getByText(label),
    data: () => screen.getByText(data),
    queryLabel: () => screen.queryByText(label),
    queryData: () => screen.queryByText(data),
  };

  const renderComponent = () =>
    render(<ReadOnlyField label={label} value={data} />);

  it('returns an element', () => {
    renderComponent();

    expect(selectors.label()).toBeInTheDocument();
    expect(selectors.data()).toBeInTheDocument();
  });

  it('does not return an element if the data is empty, null or undefined', async () => {
    const {rerender} = renderComponent();

    rerender(<ReadOnlyField label={label} />);

    expect(selectors.queryLabel()).not.toBeInTheDocument();
    expect(selectors.queryData()).not.toBeInTheDocument();

    rerender(<ReadOnlyField label={label} value="" />);

    expect(selectors.queryLabel()).not.toBeInTheDocument();
    expect(selectors.queryData()).not.toBeInTheDocument();

    rerender(<ReadOnlyField label={label} value={null} />);

    expect(selectors.queryLabel()).not.toBeInTheDocument();
    expect(selectors.queryData()).not.toBeInTheDocument();

    rerender(<ReadOnlyField label={label} value={undefined} />);

    expect(selectors.queryLabel()).not.toBeInTheDocument();
    expect(selectors.queryData()).not.toBeInTheDocument();
  });
});
