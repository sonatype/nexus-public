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
import {ExtJS} from '@sonatype/nexus-ui-plugin';

import NugetSymbolServerConfiguration from './NugetSymbolServerConfiguration';
import UIStrings from '../../../../../constants/UIStrings';

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    useState: jest.fn(),
    state: jest.fn()
  }
}));

const {NUGET} = UIStrings.REPOSITORIES.EDITOR;

describe('NugetSymbolServerConfiguration', () => {
  const parentMachine = [
    {
      context: {
        data: {nugetProxy: {symbolServerUrl: '', allowAnonymousSymbolAccess: true}},
        pristineData: {nugetProxy: {symbolServerUrl: '', allowAnonymousSymbolAccess: true}}
      }
    },
    jest.fn()
  ];

  beforeEach(() => {
    jest.clearAllMocks();
  });

  const mockFlag = (enabled) => {
    ExtJS.useState.mockImplementation((selector) => selector());
    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue(enabled)
    });
  };

  it('renders nothing when the symbol server feature flag is off', () => {
    mockFlag(false);

    const {container} = render(<NugetSymbolServerConfiguration parentMachine={parentMachine} />);

    expect(container).toBeEmptyDOMElement();
    expect(screen.queryByText(NUGET.SYMBOL_SERVER_URL.LABEL)).not.toBeInTheDocument();
    expect(screen.queryByText(NUGET.ALLOW_ANONYMOUS_SYMBOL_ACCESS.LABEL)).not.toBeInTheDocument();
  });

  it('renders the Symbol Server URL and Anonymous Access fields when the flag is on', () => {
    mockFlag(true);

    render(<NugetSymbolServerConfiguration parentMachine={parentMachine} />);

    expect(screen.getByText(NUGET.SYMBOL_SERVER_URL.LABEL)).toBeInTheDocument();
    // The checkbox label text appears twice: once as the NxFormGroup label, once as the checkbox
    // child text. Use getAllByText to accept both occurrences.
    expect(screen.getAllByText(NUGET.ALLOW_ANONYMOUS_SYMBOL_ACCESS.LABEL).length).toBeGreaterThan(0);
  });
});
