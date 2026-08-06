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
import { Box, Button, Flex, Heading, Text as RadixText } from '@radix-ui/themes';
import { ChevronRight } from 'lucide-react';

import './PageHeader.scss';

export interface BreadcrumbItem {
  /** Display label */
  label: string;
  /** Optional link URL or click handler */
  href?: string;
  onClick?: () => void;
}

export interface PageHeaderProps {
  /** Page title */
  title: string;
  /** Optional description text */
  description?: string;
  /** Optional action buttons (rendered on the right) */
  actions?: React.ReactNode;
  /** Optional breadcrumb navigation */
  breadcrumbs?: BreadcrumbItem[];
  /** Custom class name */
  className?: string;
  /** Children rendered below the header (e.g., filters, search) */
  children?: React.ReactNode;
}

/**
 * PageHeader provides a consistent header for all pages using Radix Themes patterns.
 *
 * Features:
 * - Title and optional description
 * - Action buttons area
 * - Breadcrumb navigation
 * - No padding (parent adds spacing)
 * - Radix-only layout (no custom CSS)
 *
 * Standards:
 * - No icons in page headers (per radix-component-standards.md)
 * - Breadcrumbs use Button variant="ghost" size="2" color="blue"
 * - ChevronRight separator at 14px
 * - Header has no padding; parent adds mb="4"
 *
 * @example
 * ```tsx
 * <PageHeader
 *   title="Repositories"
 *   description="Manage your Maven, npm, Docker, and other repositories"
 *   actions={
 *     <Button variant="solid" size="2">
 *       Create Repository
 *     </Button>
 *   }
 *   breadcrumbs={[
 *     { label: 'Settings', onClick: () => navigateTo('/settings') },
 *     { label: 'Repository' },
 *   ]}
 * />
 * ```
 */
export function PageHeader({
  title,
  description,
  actions,
  breadcrumbs,
  className = '',
  children,
}: PageHeaderProps): JSX.Element {
  return (
    <Box className={className} data-testid="page-header">
      {/* Breadcrumbs */}
      {breadcrumbs && breadcrumbs.length > 0 && (
        <Box mb="3">
          <Flex as="nav" gap="1" align="center" aria-label="Breadcrumb">
            {breadcrumbs.map((item, index) => {
              const isLast = index === breadcrumbs.length - 1;
              const isClickable = item.href || item.onClick;

              return (
                <React.Fragment key={index}>
                  {isClickable && !isLast ? (
                    <Button
                      variant="ghost"
                      size="2"
                      color="blue"
                      onClick={item.onClick}
                    >
                      {item.label}
                    </Button>
                  ) : (
                    <RadixText
                      size="2"
                      color={isLast ? 'gray' : undefined}
                      weight={isLast ? 'medium' : undefined}
                      aria-current={isLast ? 'page' : undefined}
                    >
                      {item.label}
                    </RadixText>
                  )}
                  {!isLast && (
                    <ChevronRight
                      size={14}
                      color="var(--gray-a9)"
                      aria-hidden="true"
                    />
                  )}
                </React.Fragment>
              );
            })}
          </Flex>
        </Box>
      )}

      {/* Main Header */}
      <Flex align="center" justify="between" gap="4">
        <Flex align="center" gap="2">
          <Heading as="h1" size="6" weight="bold">
            {title}
          </Heading>
          {description && (
            <RadixText size="2" color="gray">
              {description}
            </RadixText>
          )}
        </Flex>

        {/* Actions */}
        {actions && (
          <Flex gap="2" className="nxrm-page-header__actions">
            {actions}
          </Flex>
        )}
      </Flex>

      {/* Children (e.g., filters, search inputs) */}
      {children && <Box mt="3">{children}</Box>}
    </Box>
  );
}

export default PageHeader;

