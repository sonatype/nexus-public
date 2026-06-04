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
import { buildDefaultSearchRoute } from './search.routes';

/**
 * SwitchToDefaultUI - Link to switch from Preview UI to Default UI
 * 
 * This component provides a way for users to return to the ExtJS-based
 * Default UI search when they're in the Preview UI GA search.
 * 
 * Owner: Agent 0 (Tech Lead)
 */

interface SwitchToDefaultUIProps {
  /** Optional custom label */
  label?: string;
  /** Optional CSS class */
  className?: string;
}

export function SwitchToDefaultUI({ 
  label = 'Switch to Classic Search',
  className = ''
}: SwitchToDefaultUIProps) {
  const defaultUrl = buildDefaultSearchRoute();
  
  return (
    <a 
      href={defaultUrl}
      className={`nx-switch-to-default-ui ${className}`.trim()}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '0.5rem',
        fontSize: '0.875rem',
        color: 'var(--nx-text-color-secondary, #6b7280)',
        textDecoration: 'none',
      }}
    >
      <svg 
        width="16" 
        height="16" 
        viewBox="0 0 24 24" 
        fill="none" 
        stroke="currentColor" 
        strokeWidth="2"
        strokeLinecap="round" 
        strokeLinejoin="round"
      >
        <path d="M19 12H5M12 19l-7-7 7-7" />
      </svg>
      {label}
    </a>
  );
}

export default SwitchToDefaultUI;


