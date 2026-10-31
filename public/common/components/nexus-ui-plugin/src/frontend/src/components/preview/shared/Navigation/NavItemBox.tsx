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
import { Box } from '@radix-ui/themes';
import { NavItem, NavItemProps } from './NavItem';
import { useRouteVisibility } from './useRouteVisibility';

export interface NavItemBoxProps extends NavItemProps {
  /** Route name to check visibility against (if different from name) */
  visibilityRoute?: string;
}

/**
 * Wraps NavItem in Box only when visible.
 * Prevents empty Box elements from participating in Flex layout
 * and creating extra gaps when NavItem returns null.
 */
export function NavItemBox(props: NavItemBoxProps) {
  const { visibilityRoute, name, href, ...restProps } = props;
  const routeForVisibility = visibilityRoute || name;
  const routeIsVisible = useRouteVisibility(routeForVisibility);

  // For external links (href), always render. For routes, check visibility.
  if (!(routeIsVisible || href)) return null;

  return (
    <Box>
      <NavItem name={name} href={href} {...restProps} />
    </Box>
  );
}
