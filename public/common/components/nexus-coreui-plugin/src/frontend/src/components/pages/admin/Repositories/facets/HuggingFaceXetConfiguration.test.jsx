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

import HuggingFaceXetConfiguration from './HuggingFaceXetConfiguration';

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    useState: jest.fn(),
    state: jest.fn()
  }
}));

describe('HuggingFaceXetConfiguration', () => {
  const buildParentMachine = (xetBlock) => [
    {
      context: {
        data: xetBlock === undefined ? {} : {xet: xetBlock},
        pristineData: xetBlock === undefined ? {} : {xet: xetBlock}
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

  it('renders nothing when the XET feature flag is off', () => {
    mockFlag(false);

    const {container} = render(
        <HuggingFaceXetConfiguration parentMachine={buildParentMachine({enabled: true})} />
    );

    expect(container).toBeEmptyDOMElement();
  });

  it('renders the Transfer Protocol toggle when the flag is on', () => {
    mockFlag(true);

    render(<HuggingFaceXetConfiguration parentMachine={buildParentMachine({enabled: true})} />);

    // Fieldset legend and checkbox label are both present.
    expect(screen.getByText(/Transfer Protocol/i)).toBeInTheDocument();
    expect(screen.getByRole('checkbox', {name: /Enable XET/i})).toBeInTheDocument();
  });

  // NEXUS-48685 parity with super UI: pre-feature HuggingFace proxies have no
  // persisted `xet` block; their GET responses omit the field entirely. The
  // facet must render UNCHECKED in that case, preserving legacy LFS behavior
  // for repositories upgraded across the feature boundary. New repositories
  // get xet.enabled=true seeded via RepositoryFormConfig defaultValues, so
  // create-mode still renders CHECKED — that path is covered by the form
  // integration, not this component-level test.
  it('defaults toggle to unchecked when the xet block is absent (existing/pre-feature repo)', () => {
    mockFlag(true);

    render(<HuggingFaceXetConfiguration parentMachine={buildParentMachine(undefined)} />);

    expect(screen.getByRole('checkbox', {name: /Enable XET/i})).not.toBeChecked();
  });

  it('reflects saved xet.enabled=true when editing an existing repository', () => {
    mockFlag(true);

    render(<HuggingFaceXetConfiguration parentMachine={buildParentMachine({enabled: true})} />);

    expect(screen.getByRole('checkbox', {name: /Enable XET/i})).toBeChecked();
  });

  it('reflects saved xet.enabled=false when editing an existing repository', () => {
    mockFlag(true);

    render(<HuggingFaceXetConfiguration parentMachine={buildParentMachine({enabled: false})} />);

    expect(screen.getByRole('checkbox', {name: /Enable XET/i})).not.toBeChecked();
  });
});
