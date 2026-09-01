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
import SearchComposerExt from './SearchComposerExt';

const mockSearchFeatureExt = jest.fn(() => <div data-testid="search-feature-ext" />);

jest.mock('./SearchFeatureExt', () => ({
  __esModule: true,
  default: (props) => mockSearchFeatureExt(props),
}));

describe('SearchComposerExt', () => {
  beforeEach(() => {
    mockSearchFeatureExt.mockClear();
  });

  it('renders without crashing', () => {
    const { getByTestId } = render(<SearchComposerExt />);
    expect(getByTestId('search-feature-ext')).toBeInTheDocument();
  });

  it('passes the composer format filter to SearchFeatureExt', () => {
    render(<SearchComposerExt />);
    const [props] = mockSearchFeatureExt.mock.calls[0];
    expect(props.filter.id).toBe('composer');
  });

  it('includes hidden format criteria with value composer', () => {
    render(<SearchComposerExt />);
    const [props] = mockSearchFeatureExt.mock.calls[0];
    const formatCriteria = props.filter.criterias.find((c) => c.id === 'format');
    expect(formatCriteria).toBeDefined();
    expect(formatCriteria.value).toBe('composer');
    expect(formatCriteria.hidden).toBe(true);
  });

  it('references the generic version criterion in the filter (not composer-specific)', () => {
    render(<SearchComposerExt />);
    const [props] = mockSearchFeatureExt.mock.calls[0];
    const ids = props.filter.criterias.map((c) => c.id);
    expect(ids).toContain('version');
    expect(ids).not.toContain('assets.attributes.composer.version');
  });

  it('does not re-register the generic version as a composer-specific criterion', () => {
    render(<SearchComposerExt />);
    const [props] = mockSearchFeatureExt.mock.calls[0];
    const ids = (props.criterias || []).map((c) => c.id);
    expect(ids).not.toContain('assets.attributes.composer.version');
  });

  it('sets the filter as readOnly', () => {
    render(<SearchComposerExt />);
    const [props] = mockSearchFeatureExt.mock.calls[0];
    expect(props.filter.readOnly).toBe(true);
  });

  it('includes composer description and keywords in the default filter', () => {
    render(<SearchComposerExt />);
    const [props] = mockSearchFeatureExt.mock.calls[0];
    const ids = props.filter.criterias.map((c) => c.id);
    expect(ids).toContain('composer.description');
    expect(ids).toContain('composer.keywords');
  });

  it('registers composer description and keywords as composer-specific criteria', () => {
    render(<SearchComposerExt />);
    const [props] = mockSearchFeatureExt.mock.calls[0];
    const ids = (props.criterias || []).map((c) => c.id);
    expect(ids).toContain('composer.description');
    expect(ids).toContain('composer.keywords');
  });
});
