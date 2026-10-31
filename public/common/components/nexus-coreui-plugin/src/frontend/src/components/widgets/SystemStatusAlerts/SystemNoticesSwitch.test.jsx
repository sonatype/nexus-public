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
import '@testing-library/jest-dom';

import SystemNoticesSwitch from './SystemNoticesSwitch';

// Mutable flag the useIsPreviewUI mock reads so each test can toggle the UI mode.
let previewUI = false;

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  useIsPreviewUI: () => previewUI,
  SystemAlerts: () => <div data-testid="system-alerts" />,
  CELimitsAlert: () => <div data-testid="ce-limits-alert" />,
}));

// SystemNotices records the isPreviewUI prop it was rendered with so we can
// assert the switch forwards it correctly.
jest.mock('./SystemNotices', () => ({
  __esModule: true,
  default: ({isPreviewUI = false} = {}) => (
    <div data-testid="system-notices" data-preview={String(isPreviewUI)} />
  ),
}));

describe('SystemNoticesSwitch', () => {
  it('classic UI: renders CELimitsAlert + SystemNotices(isPreviewUI=false), not SystemAlerts', () => {
    previewUI = false;
    render(<SystemNoticesSwitch />);
    expect(screen.getByTestId('ce-limits-alert')).toBeInTheDocument();
    expect(screen.getByTestId('system-notices')).toHaveAttribute('data-preview', 'false');
    expect(screen.queryByTestId('system-alerts')).not.toBeInTheDocument();
  });

  it('preview UI: renders CELimitsAlert + SystemAlerts + SystemNotices(isPreviewUI=true)', () => {
    previewUI = true;
    render(<SystemNoticesSwitch />);
    expect(screen.getByTestId('ce-limits-alert')).toBeInTheDocument();
    expect(screen.getByTestId('system-alerts')).toBeInTheDocument();
    expect(screen.getByTestId('system-notices')).toHaveAttribute('data-preview', 'true');
  });
});
