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
import { Button, Tooltip } from '@radix-ui/themes';
import { ExternalLink } from 'lucide-react';
import { buildGuideComponentUrl } from '@/utils/guideIntegration';

export interface DeepResearchLinkProps {
  ecosystem: string;
  packageName: string;
  version: string;
  variant?: 'solid' | 'soft' | 'outline' | 'ghost';
  size?: '1' | '2' | '3';
  className?: string;
  /** Show as icon-only button (no text label) */
  iconOnly?: boolean;
  /** Analytics referrer parameter to track source page (e.g., 'repo-componentdetail', 'search-results') */
  referrer?: string;
}

/**
 * DeepResearchLink - Link to Guide component page for deep research.
 *
 * Opens https://guide.sonatype.com/component/{ecosystem}/{package}/{version} in a new tab.
 * Automatically handles URL encoding and ecosystem mapping.
 *
 * @example
 * <DeepResearchLink ecosystem="npm" packageName="lodash" version="4.17.21" />
 * <DeepResearchLink ecosystem="maven2" packageName="org.apache.commons:commons-lang3" version="3.12.0" iconOnly />
 */
export function DeepResearchLink({
  ecosystem,
  packageName,
  version,
  variant = 'ghost',
  size = '1',
  className = '',
  iconOnly = false,
  referrer,
}: DeepResearchLinkProps) {
  const url = buildGuideComponentUrl(ecosystem, packageName, version, referrer);

  if (!url) {
    return null;
  }

  const button = (
    <Button
      variant={variant}
      size={size}
      className={className}
      asChild
      data-testid="deep-research-link"
    >
      <a href={url} target="_blank" rel="noopener noreferrer">
        {iconOnly ? (
          <ExternalLink size={14} aria-label="Research in Guide" />
        ) : (
          <>
            <ExternalLink size={14} aria-hidden="true" />
            Research in Guide
          </>
        )}
      </a>
    </Button>
  );

  if (iconOnly) {
    return (
      <Tooltip content="Research this component in Sonatype Guide">
        {button}
      </Tooltip>
    );
  }

  return button;
}

export default DeepResearchLink;
