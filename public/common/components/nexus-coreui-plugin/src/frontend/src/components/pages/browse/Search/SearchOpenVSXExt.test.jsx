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
import {render} from '@testing-library/react';

import SearchOpenVSXExt from './SearchOpenVSXExt';
import UIStrings from '../../../../constants/UIStrings';

jest.mock('./SearchFeatureExt', () => (props) => {
  // Capture what the feature component is configured with, rather than rendering the whole
  // ExtJS-backed search page.
  global.__searchFeatureProps = props;
  return <div data-testid="search-feature" />;
});

describe('SearchOpenVSXExt', () => {
  beforeEach(() => {
    global.__searchFeatureProps = undefined;
  });

  it('renders without error', () => {
    const {getByTestId} = render(<SearchOpenVSXExt />);
    expect(getByTestId('search-feature')).toBeInTheDocument();
  });

  it('uses the Open VSX menu strings', () => {
    render(<SearchOpenVSXExt />);
    expect(global.__searchFeatureProps.title).toBe(UIStrings.SEARCH.OPENVSX.MENU.text);
    expect(UIStrings.SEARCH.OPENVSX.MENU.text).toBe('Open VSX');
  });

  it('scopes the saved filter to the openvsx format', () => {
    render(<SearchOpenVSXExt />);
    const {filter} = global.__searchFeatureProps;
    expect(filter.id).toBe('openvsx');
    const formatCriteria = filter.criterias.find((c) => c.id === 'format');
    expect(formatCriteria.value).toBe('openvsx');
    expect(formatCriteria.hidden).toBe(true);
  });

  it('offers publisher and target platform as search criteria', () => {
    render(<SearchOpenVSXExt />);
    const ids = global.__searchFeatureProps.criterias.map((c) => c.id);
    expect(ids).toContain('attributes.openvsx.publisher');
    expect(ids).toContain('attributes.openvsx.targetPlatform');
  });

  it('criteria attributes match what the backend search mapping stores', () => {
    // These strings must agree with OpenVSXSearchMappings on the server; a mismatch leaves the
    // fields permanently empty in the UI.
    render(<SearchOpenVSXExt />);
    global.__searchFeatureProps.criterias.forEach((criteria) => {
      expect(criteria.id).toMatch(/^attributes\.openvsx\./);
      expect(criteria.config.format).toBe('openvsx');
    });
  });

  it('scopes the filter to exactly the ids the ExtJS and React saved filters agree on', () => {
    // Asserting the full id set (not just toContain checks) catches a stray or typo'd criteria
    // id that the individual toContain assertions below would miss.
    render(<SearchOpenVSXExt />);
    const ids = global.__searchFeatureProps.filter.criterias.map((c) => c.id);
    expect(ids.sort()).toEqual(
      [
        'attributes.openvsx.publisher',
        'attributes.openvsx.targetPlatform',
        'format',
        'name.raw',
        'version'
      ].sort()
    );
  });

  it('includes the component name in the filter so plain-text search works', () => {
    render(<SearchOpenVSXExt />);
    const ids = global.__searchFeatureProps.filter.criterias.map((c) => c.id);
    expect(ids).toContain('name.raw');
  });

  it('includes version in the filter so the ExtJS and React saved filters agree', () => {
    render(<SearchOpenVSXExt />);
    const ids = global.__searchFeatureProps.filter.criterias.map((c) => c.id);
    expect(ids).toContain('version');
  });
});
