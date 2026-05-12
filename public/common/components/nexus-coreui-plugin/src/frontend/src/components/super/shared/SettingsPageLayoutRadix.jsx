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

import React, { Suspense } from 'react';
import { UIView } from '@uirouter/react';
import { SecurityEntityModalProvider } from '../settings/security/SecurityEntityModalContext';
import { Box, Flex } from '@radix-ui/themes';

import './SettingsPageLayoutRadix.scss';

export default function SettingsPageLayoutRadix() {
  return (
    <SecurityEntityModalProvider>
      <Flex direction="column" className="settings-layout-radix">
        <Box px="5" pt="3" pb="2">
          <a
            href="#preview/settings"
            style={{
              textDecoration: 'none',
              color: 'var(--gray-11)',
              fontSize: 'var(--font-size-2)',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '4px',
            }}
          >
            ← Back to Settings
          </a>
        </Box>
        <Box className="settings-layout-radix__content">
          <Suspense fallback={<div className="settings-layout-radix__loading">Loading...</div>}>
            <UIView />
          </Suspense>
        </Box>
      </Flex>
    </SecurityEntityModalProvider>
  );
}
