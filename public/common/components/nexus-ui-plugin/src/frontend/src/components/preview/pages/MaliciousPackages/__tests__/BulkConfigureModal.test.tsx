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

import { ExtJS } from '../../../../../interface/ExtJS';
import Permissions from '../../../../../constants/Permissions';
// BulkConfigureModal reads permissions through the provider-independent ExtJS.usePermission
// (NEXUS-54212); spy on checkPermission so tests keep driving behavior via permission strings.
const mockCheckPermission = jest.spyOn(ExtJS, 'checkPermission');

import { BulkConfigureModal } from '../BulkConfigureModal';

const IDLE_PROGRESS = { total: 0, completed: 0, active: false };

const renderModal = () =>
  render(
    <Theme>
      <BulkConfigureModal
        open
        repoCount={3}
        bulkProgress={IDLE_PROGRESS}
        onConfirm={jest.fn()}
        onClose={jest.fn()}
      />
    </Theme>
  );

describe('BulkConfigureModal gating (NEXUS-54212)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockCheckPermission.mockReturnValue(true);
  });

  it('shows the create-tasks button with tasks:create', () => {
    mockCheckPermission.mockImplementation((p: string) => p === Permissions.TASKS.CREATE);
    renderModal();
    expect(screen.getByRole('button', { name: /create \d+ tasks/i })).toBeInTheDocument();
  });

  it('hides the create-tasks button without tasks:create', () => {
    mockCheckPermission.mockReturnValue(false);
    renderModal();
    expect(screen.queryByRole('button', { name: /create \d+ tasks/i })).not.toBeInTheDocument();
  });

  it('keeps Cancel visible without tasks:create', () => {
    mockCheckPermission.mockReturnValue(false);
    renderModal();
    expect(screen.getByRole('button', { name: /cancel/i })).toBeInTheDocument();
  });
});
