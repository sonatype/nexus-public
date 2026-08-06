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
import '@testing-library/jest-dom';
import { Theme } from '@radix-ui/themes';

import { SystemAlerts } from '../SystemAlerts';

// Mutable flag the useIsPreviewUI mock reads so each test can toggle the UI mode.
// Prefixed with `mock` so jest allows referencing it inside the factory.
let mockPreviewUI = true;
jest.mock('../../Navigation', () => ({
  useIsPreviewUI: () => mockPreviewUI,
}));

// Stub the child alert so this test isolates the host's gating/composition.
jest.mock('../RecoveryModeAlert', () => ({
  RecoveryModeAlert: () => <div data-testid="recovery-mode-alert" />,
}));

const renderInTheme = (ui: React.ReactElement) => render(<Theme>{ui}</Theme>);

describe('SystemAlerts', () => {
  it('renders the host with its alert children in the Preview UI', () => {
    mockPreviewUI = true;
    renderInTheme(<SystemAlerts />);
    expect(screen.getByTestId('nxrm-system-alerts')).toBeInTheDocument();
    expect(screen.getByTestId('recovery-mode-alert')).toBeInTheDocument();
  });

  it('renders nothing outside the Preview UI', () => {
    mockPreviewUI = false;
    const { container } = renderInTheme(<SystemAlerts />);
    expect(screen.queryByTestId('nxrm-system-alerts')).not.toBeInTheDocument();
    // Only the Radix Theme wrapper remains, no host box.
    expect(container.querySelector('.nxrm-system-alerts')).toBeNull();
  });

  it('appends a custom className to the host', () => {
    mockPreviewUI = true;
    renderInTheme(<SystemAlerts className="extra-class" />);
    const host = screen.getByTestId('nxrm-system-alerts');
    expect(host).toHaveClass('nxrm-system-alerts');
    expect(host).toHaveClass('extra-class');
  });
});
