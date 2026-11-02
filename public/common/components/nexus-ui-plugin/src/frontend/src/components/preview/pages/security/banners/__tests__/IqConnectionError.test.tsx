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
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import { restClient } from '../../../../../../interface/api';
import { ExtJS } from '../../../../../../interface/ExtJS';
import { IqConnectionError } from '../IqConnectionError';

jest.mock('../../../../../../interface/api', () => ({
  restClient: { post: jest.fn() },
  ENDPOINTS: { FIREWALL_SECURITY_IQ_STATUS_REFRESH: '/refresh-endpoint' },
}));

jest.mock('../../../../shared/hooks/useHasUser', () => ({ useHasUser: () => true }));

const mockPost = restClient.post as jest.Mock;
const mockRefreshNow = jest.fn();

let granted: string[] = [];

beforeEach(() => {
  jest.clearAllMocks();
  granted = ['nexus:iq-violation-summary:read'];
  jest.spyOn(ExtJS, 'usePermission').mockImplementation((getValue: () => boolean) => getValue());
  jest.spyOn(ExtJS, 'checkPermission').mockImplementation((p: string) => granted.includes(p));
  (window as any).NX = { State: { refreshNow: mockRefreshNow } };
});

afterEach(() => {
  delete (window as any).NX;
});

const renderError = () =>
  render(
    <Theme>
      <IqConnectionError />
    </Theme>
  );

const clickRetry = () =>
  act(async () => {
    await userEvent.click(screen.getByRole('button', { name: 'Retry' }));
  });

describe('IqConnectionError', () => {
  it('offers Retry with nexus:settings:read only', () => {
    granted = ['nexus:settings:read'];
    renderError();
    expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument();
  });

  it('offers Retry with nexus:iq-violation-summary:read only', () => {
    granted = ['nexus:iq-violation-summary:read'];
    renderError();
    expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument();
  });

  it('offers no Retry button without nexus:settings:read or nexus:iq-violation-summary:read', () => {
    granted = [];
    renderError();
    expect(screen.getByRole('alert')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Retry' })).not.toBeInTheDocument();
  });

  it('tags the Retry button for usage analytics', () => {
    renderError();
    expect(screen.getByRole('button', { name: 'Retry' })).toHaveAttribute(
      'data-analytics-id',
      'nxrm-security-iq-retry-btn'
    );
  });

  it('shows the still-cannot-reach text and still refreshes state when IQ answers unreachable', async () => {
    mockPost.mockResolvedValue({ iqReachable: false });
    renderError();

    await clickRetry();

    expect(await screen.findByText(/still cannot reach/i)).toBeInTheDocument();
    await waitFor(() => expect(screen.getByRole('button', { name: 'Retry' })).toBeEnabled());
    expect(mockRefreshNow).toHaveBeenCalledTimes(1);
  });

  it('does not show the failure text when IQ answers reachable', async () => {
    mockPost.mockResolvedValue({ iqReachable: true });
    renderError();

    await clickRetry();

    await waitFor(() => expect(mockRefreshNow).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Retry' })).toBeEnabled());
    expect(screen.queryByText(/still cannot reach/i)).not.toBeInTheDocument();
  });

  it('is an alert that says IQ Server cannot be reached and offers Retry', () => {
    renderError();
    expect(screen.getByRole('alert')).toHaveTextContent(/cannot|could not|unable/i);
    expect(screen.getByRole('button', { name: 'Retry' })).toBeEnabled();
  });

  it('posts to the refresh endpoint, then calls refreshNow', async () => {
    let resolvePost: (v: unknown) => void = () => undefined;
    mockPost.mockReturnValue(new Promise((resolve) => {
      resolvePost = resolve;
    }));
    renderError();

    await userEvent.click(screen.getByRole('button', { name: 'Retry' }));

    expect(mockPost).toHaveBeenCalledWith('/refresh-endpoint');
    expect(mockRefreshNow).not.toHaveBeenCalled();

    await act(async () => {
      resolvePost({});
    });

    await waitFor(() => expect(mockRefreshNow).toHaveBeenCalledTimes(1));
  });

  it('is disabled and shows a retrying label while the request is pending', async () => {
    let resolvePost: (v: unknown) => void = () => undefined;
    mockPost.mockReturnValue(new Promise((resolve) => {
      resolvePost = resolve;
    }));
    renderError();

    await userEvent.click(screen.getByRole('button', { name: 'Retry' }));

    const pending = await screen.findByRole('button', { name: /retrying/i });
    expect(pending).toBeDisabled();

    await act(async () => {
      resolvePost({});
    });
    await waitFor(() => expect(screen.getByRole('button', { name: 'Retry' })).toBeEnabled());
  });

  it('keeps the error, shows a failure message and does not refresh state when the request fails', async () => {
    mockPost.mockRejectedValue(new Error('boom'));
    renderError();

    await clickRetry();

    expect(await screen.findByText(/still cannot reach|retry failed|try again/i)).toBeInTheDocument();
    await waitFor(() => expect(screen.getByRole('button', { name: 'Retry' })).toBeEnabled());
    expect(screen.getByRole('alert')).toBeInTheDocument();
    expect(mockRefreshNow).not.toHaveBeenCalled();
    expect(screen.getByRole('button', { name: 'Retry' })).toBeEnabled();
  });

  it('does not warn when the banner unmounts while Retry is settling', async () => {
    const err = jest.spyOn(console, 'error').mockImplementation(() => undefined);
    let resolvePost: (v: unknown) => void = () => undefined;
    mockPost.mockReturnValue(new Promise((r) => {
      resolvePost = r;
    }));
    const { unmount } = renderError();
    await userEvent.click(screen.getByRole('button', { name: 'Retry' }));
    unmount();
    await act(async () => {
      resolvePost({});
    });
    expect(err).not.toHaveBeenCalled();
    err.mockRestore();
  });
});
