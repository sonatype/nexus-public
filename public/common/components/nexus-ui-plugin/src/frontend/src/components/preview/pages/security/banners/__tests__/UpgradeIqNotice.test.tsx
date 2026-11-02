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
import { UpgradeIqNotice } from '../UpgradeIqNotice';

const renderNotice = (minIqVersion?: string | null) =>
  render(
    <Theme>
      <UpgradeIqNotice minIqVersion={minIqVersion} />
    </Theme>
  );

describe('UpgradeIqNotice', () => {
  it('names the minimum IQ Server version', () => {
    renderNotice('1.204');
    expect(screen.getByRole('status')).toHaveTextContent('Upgrade IQ Server to 1.204 or later');
  });

  it('falls back to generic wording when the minimum version is unknown', () => {
    renderNotice(null);
    expect(screen.getByRole('status')).toHaveTextContent('Upgrade IQ Server to a newer version');
  });
});
