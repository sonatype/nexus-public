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
import { Flex, Text, Heading, Button, Box } from '@radix-ui/themes';
import { Ghost, BookOpen, Home } from 'lucide-react';
import { useSref } from '@uirouter/react';
import { ROUTE_NAMES } from '../../constants/RouteNames';

import './MissingRoutePage.scss';

/**
 * Preview UI 404 Page - Missing Route
 *
 * A modern, Radix UI-based 404 page for the Preview UI.
 * Features:
 * - Clean, centered layout with ghost icon
 * - Clear messaging about the missing resource
 * - Actions to return home or visit documentation
 * - Dark mode support
 */
export function MissingRoutePage(): JSX.Element {
  const { href: dashboardHref } = useSref(ROUTE_NAMES.BROWSE.WELCOME.ROOT);

  return (
    <Flex
      direction="column"
      align="center"
      justify="center"
      className="nxrm-missing-route-page"
    >
      <Box className="nxrm-missing-route-page__content">
        {/* Ghost Icon */}
        <Box className="nxrm-missing-route-page__ghost-icon">
          <Ghost size={80} strokeWidth={1.5} />
        </Box>

        {/* 404 Code */}
        <Text className="nxrm-missing-route-page__code" as="div">
          404
        </Text>

        {/* Title */}
        <Heading size="6" className="nxrm-missing-route-page__title">
          Resource Not Found
        </Heading>

        {/* Divider */}
        <Box className="nxrm-missing-route-page__divider" />

        {/* Message */}
        <Text size="3" color="gray" className="nxrm-missing-route-page__message">
          This resource is not available. It may have been moved, deleted, or you don&apos;t have access.
        </Text>

        {/* Actions */}
        <Flex direction="column" gap="4" align="center" className="nxrm-missing-route-page__actions">
          <Text size="2" color="gray" className="nxrm-missing-route-page__actions-message">
            You might want to:
          </Text>

          <Flex gap="3" wrap="wrap" justify="center" className="nxrm-missing-route-page__buttons">
            <Button asChild variant="solid" size="3">
              <a href={dashboardHref} data-analytics-id="nxrm-404-page-dashboard-lnk">
                <Home size={16} />
                Return to Dashboard
              </a>
            </Button>

            <Button asChild variant="soft" size="3">
              <a
                href="https://links.sonatype.com/products/nexus/docs"
                target="_blank"
                rel="noopener noreferrer"
                data-analytics-id="nxrm-404-page-support-lnk"
              >
                <BookOpen size={16} />
                Visit Documentation
              </a>
            </Button>
          </Flex>
        </Flex>
      </Box>
    </Flex>
  );
}

export default MissingRoutePage;
