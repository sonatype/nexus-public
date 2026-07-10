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
import { IconButton } from '@radix-ui/themes';
import { Tooltip } from '@sonatype/nexus-ui-plugin';
import { Sun, MoonStar } from 'lucide-react';
import { useTheme, THEMES } from '../../contexts/ThemeContext';

/**
 * ThemeSwitcher - Icon-only toggle for dark/light mode.
 * Uses Radix IconButton with Lucide icons (16px).
 * Toggles between Light and Dark only.
 */
export function ThemeSwitcher() {
  const { effectiveTheme, setTheme } = useTheme();

  const toggleTheme = () => {
    setTheme(effectiveTheme === 'dark' ? THEMES.LIGHT : THEMES.DARK);
  };

  const getIcon = () => {
    return effectiveTheme === 'dark' ? <Sun size={16} /> : <MoonStar size={16} />;
  };

  const getTooltip = () => {
    return effectiveTheme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode';
  };

  return (
    <Tooltip content={getTooltip()}>
      <IconButton
        variant="outline"
        size="2"
        color="gray"
        aria-label={getTooltip()}
        data-analytics-id="nxrm-header-toggle-theme"
        onClick={toggleTheme}
      >
        {getIcon()}
      </IconButton>
    </Tooltip>
  );
}

export default ThemeSwitcher;
