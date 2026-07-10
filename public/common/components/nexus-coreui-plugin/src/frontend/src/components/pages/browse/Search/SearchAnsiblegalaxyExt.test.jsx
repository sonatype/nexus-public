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
import SearchAnsiblegalaxyExt from './SearchAnsiblegalaxyExt';

const mockSearchFeatureExt = jest.fn(() => <div data-testid="search-feature-ext" />);

jest.mock('./SearchFeatureExt', () => ({
  __esModule: true,
  default: (props) => mockSearchFeatureExt(props),
}));

describe('SearchAnsiblegalaxyExt', () => {
  beforeEach(() => {
    mockSearchFeatureExt.mockClear();
  });

  it('renders without crashing', () => {
    const { getByTestId } = render(<SearchAnsiblegalaxyExt />);
    expect(getByTestId('search-feature-ext')).toBeInTheDocument();
  });

  it('passes the ansiblegalaxy format filter to SearchFeatureExt', () => {
    render(<SearchAnsiblegalaxyExt />);
    const [props] = mockSearchFeatureExt.mock.calls[0];
    expect(props.filter.id).toBe('ansiblegalaxy');
  });

  it('sets the filter name to Ansible Galaxy', () => {
    render(<SearchAnsiblegalaxyExt />);
    const [props] = mockSearchFeatureExt.mock.calls[0];
    expect(props.filter.name).toBe('Ansible Galaxy');
  });

  it('includes hidden format criteria with value ansiblegalaxy', () => {
    render(<SearchAnsiblegalaxyExt />);
    const [props] = mockSearchFeatureExt.mock.calls[0];
    const formatCriteria = props.filter.criterias.find((c) => c.id === 'format');
    expect(formatCriteria).toBeDefined();
    expect(formatCriteria.value).toBe('ansiblegalaxy');
    expect(formatCriteria.hidden).toBe(true);
  });

  it('includes namespace, name, and version in filter criterias', () => {
    render(<SearchAnsiblegalaxyExt />);
    const [props] = mockSearchFeatureExt.mock.calls[0];
    const ids = props.filter.criterias.map((c) => c.id);
    expect(ids).toContain('assets.attributes.ansiblegalaxy.namespace');
    expect(ids).toContain('assets.attributes.ansiblegalaxy.name');
    expect(ids).toContain('assets.attributes.ansiblegalaxy.version');
  });

  it('passes criterias with namespace, name, and version configurations', () => {
    render(<SearchAnsiblegalaxyExt />);
    const [props] = mockSearchFeatureExt.mock.calls[0];
    expect(props.criterias).toHaveLength(3);
    const namespaceConfig = props.criterias.find(
      (c) => c.id === 'assets.attributes.ansiblegalaxy.namespace'
    );
    expect(namespaceConfig.config.format).toBe('ansiblegalaxy');
    expect(namespaceConfig.config.fieldLabel).toBe('Namespace');
  });

  it('sets the filter as readOnly', () => {
    render(<SearchAnsiblegalaxyExt />);
    const [props] = mockSearchFeatureExt.mock.calls[0];
    expect(props.filter.readOnly).toBe(true);
  });

  it('passes a title and icon to SearchFeatureExt', () => {
    render(<SearchAnsiblegalaxyExt />);
    const [props] = mockSearchFeatureExt.mock.calls[0];
    expect(props.title).toBeTruthy();
    expect(props.icon).toBeTruthy();
  });
});
