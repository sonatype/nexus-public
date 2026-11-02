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
import { render, screen } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

import { MalwareLivePlaceholder } from '../malware/MalwareLivePlaceholder';
import { WaiversLivePlaceholder } from '../waivers/WaiversLivePlaceholder';
import { QuarantineLivePlaceholder } from '../quarantine/QuarantineLivePlaceholder';
import { PoliciesLivePlaceholder } from '../policies/PoliciesLivePlaceholder';
import { EvaluationsLivePlaceholder } from '../evaluations/EvaluationsLivePlaceholder';

jest.mock('../../../../../../interface/api', () => ({
  restClient: { get: jest.fn(), post: jest.fn() },
  ENDPOINTS: {},
}));

const CASES: Array<[string, () => JSX.Element, string]> = [
  ['Malware', MalwareLivePlaceholder, 'Malware'],
  ['Waivers', WaiversLivePlaceholder, 'Waivers'],
  ['Quarantine', QuarantineLivePlaceholder, 'Quarantine'],
  ['Policies', PoliciesLivePlaceholder, 'Policies'],
  ['Evaluations', EvaluationsLivePlaceholder, 'Evaluations'],
];

describe('Live placeholders', () => {
  it.each(CASES)('%s renders a Coming soon empty state', (_name, Component, title) => {
    render(
      <Theme>
        <Component />
      </Theme>
    );

    expect(screen.getByTestId('empty-state')).toBeInTheDocument();
    expect(screen.getByText(`${title}: coming soon`)).toBeInTheDocument();
    expect(screen.getByText(/Coming soon/)).toBeInTheDocument();
  });

  it.each(CASES)('%s makes no restClient call', (_name, Component) => {
    const { restClient } = jest.requireMock('../../../../../../interface/api');
    render(
      <Theme>
        <Component />
      </Theme>
    );

    expect(restClient.get).not.toHaveBeenCalled();
    expect(restClient.post).not.toHaveBeenCalled();
  });
});
