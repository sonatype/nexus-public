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
import SearchDockerExt from './SearchDockerExt';
import SearchFeatureExt from './SearchFeatureExt';

jest.mock('./SearchFeatureExt', () => jest.fn(() => null));

describe('SearchDockerExt', () => {
  beforeEach(() => {
    SearchFeatureExt.mockClear();
  });

  it('passes os criteria to SearchFeatureExt', () => {
    render(<SearchDockerExt />);
    const { criterias } = SearchFeatureExt.mock.calls[0][0];
    expect(criterias.some(c => c.id === 'attributes.docker.os')).toBe(true);
  });

  it('passes arch criteria to SearchFeatureExt', () => {
    render(<SearchDockerExt />);
    const { criterias } = SearchFeatureExt.mock.calls[0][0];
    expect(criterias.some(c => c.id === 'attributes.docker.architecture')).toBe(true);
  });

  it('passes labels criteria to SearchFeatureExt', () => {
    render(<SearchDockerExt />);
    const { criterias } = SearchFeatureExt.mock.calls[0][0];
    expect(criterias.some(c => c.id === 'attributes.docker.labels')).toBe(true);
  });

  it('passes author criteria to SearchFeatureExt', () => {
    render(<SearchDockerExt />);
    const { criterias } = SearchFeatureExt.mock.calls[0][0];
    expect(criterias.some(c => c.id === 'attributes.docker.author')).toBe(true);
  });

  it('includes new criteria ids in filter.criterias', () => {
    render(<SearchDockerExt />);
    const { filter } = SearchFeatureExt.mock.calls[0][0];
    const ids = filter.criterias.map(c => c.id);
    expect(ids).toContain('attributes.docker.os');
    expect(ids).toContain('attributes.docker.architecture');
    expect(ids).toContain('attributes.docker.labels');
    expect(ids).toContain('attributes.docker.author');
  });
});
