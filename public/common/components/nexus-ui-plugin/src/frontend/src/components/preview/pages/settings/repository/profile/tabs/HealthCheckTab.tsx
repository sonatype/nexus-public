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

import React, { useState, useCallback, useEffect } from 'react';
import { Box, Flex, IconButton, Tooltip } from '@radix-ui/themes';
import { Maximize2, Minimize2 } from 'lucide-react';
import { SecurityReportPage } from '../../../../../shared/security/SecurityReportPage';

import './HealthCheckTab.scss';

export interface HealthCheckTabProps {
  repositoryName: string;
}

/**
 * HealthCheckTab - Full Health Check report in a tab, with fullscreen toggle.
 * Reuses SecurityReportPage content; same pattern as Structure tab.
 */
export function HealthCheckTab({ repositoryName }: HealthCheckTabProps): JSX.Element {
  const [isFullscreen, setIsFullscreen] = useState(false);

  const toggleFullscreen = useCallback(() => {
    setIsFullscreen((prev) => !prev);
  }, []);

  useEffect(() => {
    if (isFullscreen) {
      window.dispatchEvent(new CustomEvent('nx-sidebar-toggle', { detail: { open: true } }));
    } else {
      window.dispatchEvent(new CustomEvent('nx-sidebar-toggle', { detail: { open: false } }));
    }
  }, [isFullscreen]);

  return (
    <Box
      className={`health-check-tab-container ${isFullscreen ? 'health-check-tab-container--fullscreen' : ''}`}
    >
      <Flex gap="3" align="center" className="health-check-tab-container__toolbar">
        <Tooltip content={isFullscreen ? 'Exit Fullscreen' : 'Fullscreen Mode'}>
          <IconButton variant="soft" size="2" onClick={toggleFullscreen} aria-label={isFullscreen ? 'Exit fullscreen' : 'Enter fullscreen'}>
            {isFullscreen ? <Minimize2 size={16} /> : <Maximize2 size={16} />}
          </IconButton>
        </Tooltip>
      </Flex>
      <Box className="health-check-tab-container__content">
        <SecurityReportPage repositoryName={repositoryName} reportType="health-check" embedded />
      </Box>
    </Box>
  );
}

export default HealthCheckTab;
