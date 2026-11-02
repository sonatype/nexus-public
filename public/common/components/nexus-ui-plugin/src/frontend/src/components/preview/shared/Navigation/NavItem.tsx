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
import { Badge, Flex } from '@radix-ui/themes';
import { ExternalLink, LucideIcon } from 'lucide-react';
import { useSref, useIsActive } from '@uirouter/react';
import { Tooltip } from '../Tooltip/TooltipContainerContext';
import { useContextAwareRouteName } from './useContextAwareRouteName';
import { formatBadgeCount } from './formatBadgeCount';

export interface NavItemProps {
  /** Route name for navigation */
  name: string;
  /** Display text */
  text: string;
  /** Icon component */
  icon?: LucideIcon;
  /** Route to check for active state (if different from name) */
  selectedState?: string;
  /** Route parameters */
  params?: Record<string, unknown>;
  /** Whether the sidebar is collapsed */
  isCollapsed?: boolean;
  /** Direct href for external links */
  href?: string;
  /** Whether this is an external link */
  isExternal?: boolean;
  /** Optional count badge. Hidden for null, undefined and 0; shown as "99+" above 99. */
  badge?: number | null;
  /** Accessible label for the badge, from the count. Defaults to the bare count. */
  badgeLabel?: (count: number) => string;
  /** Usage analytics id, rendered as data-analytics-id on the link. */
  analyticsId?: string;
}

/**
 * Navigation Item Component using Radix + uirouter.
 * Context-aware: uses preview.* routes when in Preview UI.
 */
export function NavItem({
  name,
  text,
  icon: Icon,
  selectedState,
  params = {},
  isCollapsed,
  href: directHref,
  isExternal,
  badge,
  badgeLabel: getBadgeLabel,
  analyticsId,
}: NavItemProps) {
  // Defensive check: ensure name is always a valid string for useSref
  if (!name) {
    console.warn('[NavItem] name prop is required but received:', name);
    return null;
  }

  const contextAwareName = useContextAwareRouteName(name) || name;
  const contextAwareSelectedState = useContextAwareRouteName(selectedState);

  // Ensure we always pass a string to useSref
  const safeRouteName = contextAwareName || name;

  const sref = useSref(safeRouteName, params);
  const isActive = useIsActive(contextAwareSelectedState || safeRouteName);

  // Use direct href for external links, otherwise use router-generated href
  const finalHref = directHref || sref.href;

  const showBadge = typeof badge === 'number' && badge > 0;
  const badgeLabel = showBadge ? (getBadgeLabel ? getBadgeLabel(badge) : `${badge}`) : undefined;

  const content = (
    <a
      href={finalHref}
      onClick={directHref ? undefined : sref.onClick}
      target={isExternal ? '_blank' : undefined}
      rel={isExternal ? 'noopener noreferrer' : undefined}
      className={`guide-nav-item ${isActive ? 'guide-nav-item--active' : ''}`}
      data-analytics-id={analyticsId}
      aria-label={showBadge ? `${text}, ${badgeLabel}` : isCollapsed ? text : undefined}
    >
      <Flex align="center" justify={isCollapsed ? 'center' : 'start'} gap="3" px="3" py="2">
        <span className="guide-nav-item__icon">
          {Icon && <Icon size={18} />}
          {isCollapsed && showBadge && <span aria-hidden="true" className="guide-nav-item__badge-dot" />}
        </span>
        {!isCollapsed && (
          <span className="guide-nav-item__text">{text}</span>
        )}
        {!isCollapsed && showBadge && (
          <Badge size="1" color="red" variant="solid" radius="full" className="guide-nav-item__badge">
            {formatBadgeCount(badge as number)}
          </Badge>
        )}
        {isExternal && !isCollapsed && (
          <ExternalLink size={18} className="guide-nav-item__external" />
        )}
      </Flex>
    </a>
  );

  if (isCollapsed) {
    return (
      <Tooltip content={text} side="right">
        {content}
      </Tooltip>
    );
  }

  return content;
}
