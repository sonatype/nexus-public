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
import { Callout, Link } from '@radix-ui/themes';
import { Info } from 'lucide-react';
import { UPGRADE_URL } from './bannerConstants';

/** Shown on Pro, where the Security pages are an example-data preview of what Pro+ and Enterprise include. */
export function ProPreviewBanner(): JSX.Element {
  // TODO(Hermes): final copy
  return (
    <Callout.Root role="status" color="blue" mb="4" data-testid="pro-preview-banner">
      <Callout.Icon>
        <Info size={16} />
      </Callout.Icon>
      <Callout.Text>
        Preview: the pages below show example data.{' '}
        <Link href={UPGRADE_URL} target="_blank" rel="noopener noreferrer" data-analytics-id="nxrm-security-learn-more-link">
          Learn more
        </Link>
      </Callout.Text>
    </Callout.Root>
  );
}

export default ProPreviewBanner;
