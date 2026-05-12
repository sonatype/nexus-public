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
import { getHeritageEquivalent } from '../../../../../GlobalHeader/previewHeritageNavigation';

const CLASSIC_UI_TOOLTIP = 'Opens in default UI. Nexus One UI settings coming soon.';

interface ClassicSettingsLinkProps {
  previewPath: string;
  label?: string;
}

/**
 * Opens the Classic UI equivalent of a Preview admin settings page in a new tab.
 * Uses the existing heritage route mapping to compute the Classic URL.
 * NEXUS-51915: Prevents Repository Profile from bypassing Coming Soon gates.
 */
export function ClassicSettingsLink({ previewPath, label = 'Configure' }: ClassicSettingsLinkProps) {
  const classicHash = getHeritageEquivalent(previewPath);
  if (!classicHash) return null;

  const handleClick = () => {
    const baseUrl = `${window.location.origin}${window.location.pathname}`;
    window.open(`${baseUrl}#${classicHash}`, '_blank', 'noopener,noreferrer');
  };

  return (
    <Tooltip content={CLASSIC_UI_TOOLTIP}>
      <Button
        variant="ghost"
        size="1"
        onClick={handleClick}
      >
        {label} <ExternalLink size={12} />
      </Button>
    </Tooltip>
  );
}

export default ClassicSettingsLink;
