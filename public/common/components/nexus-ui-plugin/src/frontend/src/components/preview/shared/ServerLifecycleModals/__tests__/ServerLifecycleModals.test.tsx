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
import { render, screen, act } from '@testing-library/react';
import { ServerLifecycleModals } from '../ServerLifecycleModals';
import { NEXUS_SERVER_RESTARTED_EVENT } from '../../../../../interface/api/serverRestartBroadcast';
import { NEXUS_SERVER_DISCONNECTED_EVENT } from '../../../../../interface/api/serverDisconnectedBroadcast';

const RESTART_MODAL = 'server-restart-modal';
const DISCONNECTED_MODAL = 'server-disconnected-modal';

const fireRestart = () =>
  act(() => {
    window.dispatchEvent(new CustomEvent(NEXUS_SERVER_RESTARTED_EVENT));
  });

const fireDisconnect = () =>
  act(() => {
    window.dispatchEvent(new CustomEvent(NEXUS_SERVER_DISCONNECTED_EVENT));
  });

describe('ServerLifecycleModals', () => {
  it('renders neither modal while the server is healthy', () => {
    render(<ServerLifecycleModals />);
    expect(screen.queryByTestId(RESTART_MODAL)).not.toBeInTheDocument();
    expect(screen.queryByTestId(DISCONNECTED_MODAL)).not.toBeInTheDocument();
  });

  it('shows only the disconnect modal when the server disconnects', () => {
    render(<ServerLifecycleModals />);
    fireDisconnect();
    expect(screen.getByTestId(DISCONNECTED_MODAL)).toBeInTheDocument();
    expect(screen.queryByTestId(RESTART_MODAL)).not.toBeInTheDocument();
  });

  it('shows only the restart modal when the server restarts', () => {
    render(<ServerLifecycleModals />);
    fireRestart();
    expect(screen.getByTestId(RESTART_MODAL)).toBeInTheDocument();
    expect(screen.queryByTestId(DISCONNECTED_MODAL)).not.toBeInTheDocument();
  });

  it('shows only the restart modal when both a disconnect and a restart fire (restart wins)', () => {
    render(<ServerLifecycleModals />);
    fireDisconnect();
    fireRestart();
    expect(screen.getByTestId(RESTART_MODAL)).toBeInTheDocument();
    expect(screen.queryByTestId(DISCONNECTED_MODAL)).not.toBeInTheDocument();
  });
});
