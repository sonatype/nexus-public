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
import React, {useEffect, useState} from 'react';

import {NxButton, NxFontAwesomeIcon} from "@sonatype/react-shared-components";
import {faMoon, faSun} from "@fortawesome/free-solid-svg-icons";

export function ThemeSelector() {
  const [theme, setTheme] = useState(localStorage.getItem('theme') || 'light');
  const themeIcon = theme === 'dark' ? faMoon : faSun;
  const themeTitle = theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode';

  useEffect(() => {
    document.documentElement.classList.add('nx-html--enable-color-schemes');
    if (theme === 'dark') {
      document.documentElement.classList.add('nx-html--dark-mode');
      document.documentElement.classList.remove('nx-html--light-mode');
    } else {
      document.documentElement.classList.add('nx-html--light-mode');
      document.documentElement.classList.remove('nx-html--dark-mode');
    }
  }, [theme]);

  function toggleTheme() {
    const newTheme = theme === 'dark' ? 'light' : 'dark';
    setTheme(newTheme);
    localStorage.setItem('theme', newTheme);
  }

  return (
      <NxButton variant="icon-only" title={themeTitle} onClick={toggleTheme}>
        <NxFontAwesomeIcon icon={themeIcon}/>
      </NxButton>
  );
}
