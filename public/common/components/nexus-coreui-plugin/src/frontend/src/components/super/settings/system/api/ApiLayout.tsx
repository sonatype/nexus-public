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
import { Box, Flex, IconButton } from '@radix-ui/themes';
import { PanelLeftClose, PanelLeft } from 'lucide-react';

export interface ApiLayoutProps {
  leftPanel: React.ReactNode;
  rightPanel: React.ReactNode;
  leftCollapsed: boolean;
  onToggleLeft: () => void;
}

export function ApiLayout({ leftPanel, rightPanel, leftCollapsed, onToggleLeft }: ApiLayoutProps) {
  return (
    <Flex gap="3" align="start" className="api-layout">
      {!leftCollapsed && (
        <Box className="api-layout__left" data-testid="api-layout-left">
          {leftPanel}
        </Box>
      )}
      <Flex direction="column" gap="2" className="api-layout__toggle-col">
        <IconButton
          type="button"
          variant="soft"
          size="1"
          onClick={onToggleLeft}
          aria-label={leftCollapsed ? 'Expand endpoint list' : 'Collapse endpoint list'}
        >
          {leftCollapsed ? <PanelLeft size={16} /> : <PanelLeftClose size={16} />}
        </IconButton>
      </Flex>
      <Box className="api-layout__right" style={{ flex: 1, minWidth: 0 }} data-testid="api-layout-right">
        {rightPanel}
      </Box>
    </Flex>
  );
}
