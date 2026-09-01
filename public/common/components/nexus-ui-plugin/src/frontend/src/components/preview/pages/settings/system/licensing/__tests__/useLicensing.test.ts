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

import { renderHook, waitFor, act } from '@testing-library/react';
import { useLicensing } from '../useLicensing';

const mockGet = jest.fn();
jest.mock('../../../../../../../interface/api', () => ({
  restClient: { get: (...args: unknown[]) => mockGet(...args) },
}));
jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: { checkPermission: jest.fn().mockReturnValue(true) },
}));

describe('useLicensing', () => {
  beforeEach(() => jest.clearAllMocks());

  it('loads the license and exposes the permission flag', async () => {
    mockGet.mockResolvedValue({ contactCompany: 'Acme' });
    const { result } = renderHook(() => useLicensing());

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.license.contactCompany).toBe('Acme');
    expect(result.current.canViewHistoricalUsage).toBe(true);
    expect(result.current.activeTab).toBe('license');
  });

  it('changes the active tab', async () => {
    mockGet.mockResolvedValue({});
    const { result } = renderHook(() => useLicensing());
    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => result.current.setActiveTab('usage'));
    expect(result.current.activeTab).toBe('usage');
  });

  it('replaces the license on install', async () => {
    mockGet.mockResolvedValue({});
    const { result } = renderHook(() => useLicensing());
    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => result.current.handleLicenseInstalled({ contactCompany: 'NewCo' }));
    expect(result.current.license.contactCompany).toBe('NewCo');
    expect(result.current.error).toBeNull();
  });
});
