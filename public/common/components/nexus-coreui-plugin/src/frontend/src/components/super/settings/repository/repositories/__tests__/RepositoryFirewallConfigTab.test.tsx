/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are
 * trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark
 * of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import '@testing-library/jest-dom';

import { RepositoryFirewallConfigTab } from '../RepositoryFirewallConfigTab';

const mockRestGet = jest.fn();
jest.mock('@/utils/api', () => {
  const actual = jest.requireActual<typeof import('@/utils/api')>('@/utils/api');
  return {
    ...actual,
    restClient: {
      ...actual.restClient,
      get: (...args: unknown[]) => mockRestGet(...args),
    },
  };
});

jest.mock('../useRepositoriesApi', () => ({
  useRepositoriesApi: () => ({
    enableHealthCheck: jest.fn(),
  }),
}));

function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

describe('RepositoryFirewallConfigTab', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockRestGet.mockImplementation((url: string) => {
      if (typeof url === 'string' && url.includes('health-check')) {
        return Promise.resolve({ enabled: false });
      }
      if (typeof url === 'string' && url.includes('iq/audit')) {
        return Promise.resolve({ repositoryName: 'maven-proxy', enabled: false, enabledQuarantine: false });
      }
      return Promise.resolve(null);
    });
  });

  it('renders audit trail placeholder in Firewall and Health Check cards', async () => {
    renderWithTheme(
      <RepositoryFirewallConfigTab repositoryName="maven-proxy" hasFirewallLicense={true} />
    );

    const auditTexts = await waitFor(() =>
      screen.getAllByText(/Audit trail: Configuration change history will be available in a future release/)
    );
    expect(auditTexts).toHaveLength(2);
  });
});
