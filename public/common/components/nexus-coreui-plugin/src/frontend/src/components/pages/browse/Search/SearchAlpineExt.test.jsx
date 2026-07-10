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
import { render } from '@testing-library/react';
import SearchAlpineExt from './SearchAlpineExt';

const mockSearchFeatureExt = jest.fn(() => <div data-testid="search-feature-ext" />);

jest.mock('./SearchFeatureExt', () => ({
  __esModule: true,
  default: (props) => mockSearchFeatureExt(props),
}));

describe('SearchAlpineExt', () => {
  beforeEach(() => {
    mockSearchFeatureExt.mockClear();
  });

  it('renders without crashing', () => {
    const { getByTestId } = render(<SearchAlpineExt />);
    expect(getByTestId('search-feature-ext')).toBeInTheDocument();
  });

  it('passes the alpine format filter to SearchFeatureExt', () => {
    render(<SearchAlpineExt />);
    const [props] = mockSearchFeatureExt.mock.calls[0];
    expect(props.filter.id).toBe('alpine');
  });

  it('sets the filter name to Alpine', () => {
    render(<SearchAlpineExt />);
    const [props] = mockSearchFeatureExt.mock.calls[0];
    expect(props.filter.name).toBe('Alpine');
  });

  it('includes hidden format criteria with value alpine', () => {
    render(<SearchAlpineExt />);
    const [props] = mockSearchFeatureExt.mock.calls[0];
    const formatCriteria = props.filter.criterias.find((c) => c.id === 'format');
    expect(formatCriteria).toBeDefined();
    expect(formatCriteria.value).toBe('alpine');
    expect(formatCriteria.hidden).toBe(true);
  });

  it('includes name.raw and version search criteria', () => {
    render(<SearchAlpineExt />);
    const [props] = mockSearchFeatureExt.mock.calls[0];
    const ids = props.filter.criterias.map((c) => c.id);
    expect(ids).toContain('name.raw');
    expect(ids).toContain('version');
  });

  it('sets the filter as readOnly', () => {
    render(<SearchAlpineExt />);
    const [props] = mockSearchFeatureExt.mock.calls[0];
    expect(props.filter.readOnly).toBe(true);
  });

  it('passes a title and icon to SearchFeatureExt', () => {
    render(<SearchAlpineExt />);
    const [props] = mockSearchFeatureExt.mock.calls[0];
    expect(props.title).toBeTruthy();
    expect(props.icon).toBeTruthy();
  });
});
