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


import React, { ReactNode } from 'react';
import { Badge, Box, Callout, Link, Text } from '@radix-ui/themes';
import { Info, TriangleAlert } from 'lucide-react';

import { UPGRADE_URL } from '../../banners/bannerConstants';
import { useProUpsell } from '../ProUpsellContext';

/** The amber "Example" pill used beside example rows and above the example frame. */
export function ExampleBadge(): JSX.Element {
  return (
    <Badge color="amber" variant="solid" size="1" data-testid="example-badge">
      Example
    </Badge>
  );
}

/** Amber warning that the tab's content is illustrative. */
export function ExampleDataCallout(): JSX.Element {
  return (
    <Callout.Root color="amber" variant="surface" size="2" data-testid="example-data-callout">
      <Callout.Icon>
        <TriangleAlert size={16} aria-hidden="true" />
      </Callout.Icon>
      <Callout.Text>Example data — nothing on this tab reflects your repositories.</Callout.Text>
    </Callout.Root>
  );
}

const DEFAULT_UPGRADE_MESSAGE = 'to quarantine malicious and policy-violating components in your own repositories.';

/**
 * Blue prompt to upgrade to see the real thing. `children` is the sentence that follows the link. Renders nothing
 * unless the user can upgrade (the 'preview-pro' tier).
 */
export function UpgradeCallout({ children = DEFAULT_UPGRADE_MESSAGE }: { children?: ReactNode }): JSX.Element | null {
  if (!useProUpsell()) return null;
  return (
    <Callout.Root color="blue" variant="soft" size="2" data-testid="upgrade-callout">
      <Callout.Icon>
        <Info size={16} aria-hidden="true" />
      </Callout.Icon>
      <Callout.Text>
        <Link href={UPGRADE_URL}>
          Upgrade to Nexus Repository Pro Plus
        </Link>{' '}
        {children}
      </Callout.Text>
    </Callout.Root>
  );
}

export interface ExampleFrameProps {
  label: string;
  children: ReactNode;
  /** Wrap the children in a bordered box. Turn off when the frame holds more than one table. */
  bordered?: boolean;
}

/** Dashed amber frame, labelled, that wraps an example table. */
export function ExampleFrame({ label, children, bordered = true }: ExampleFrameProps): JSX.Element {
  return (
    <Box
      role="region"
      aria-label={label}
      data-testid="example-data-frame"
      position="relative"
      pt="4"
      style={{
        border: '1px dashed var(--amber-8)',
        borderRadius: 'var(--radius-3)',
        backgroundColor: 'var(--amber-a2)',
      }}
    >
      <Text
        size="1"
        weight="medium"
        style={{
          position: 'absolute',
          top: -9,
          left: 12,
          padding: '0 6px',
          backgroundColor: 'var(--color-background)',
          color: 'var(--amber-11)',
        }}
      >
        {label}
      </Text>
      {bordered ? (
        <Box
          style={{ border: '1px solid var(--gray-a6)', borderRadius: 'var(--radius-3)', overflow: 'hidden' }}
          m="0"
        >
          {children}
        </Box>
      ) : (
        <Box p="3" pt="2">
          {children}
        </Box>
      )}
    </Box>
  );
}
