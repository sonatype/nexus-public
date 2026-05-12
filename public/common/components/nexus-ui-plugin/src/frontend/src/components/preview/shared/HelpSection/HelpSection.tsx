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
import { Box, Text } from '@radix-ui/themes';
import { ExternalLink, HelpCircle } from 'lucide-react';

import './HelpSection.scss';

export interface DocLink {
  /** Link label */
  label: string;
  /** Link URL */
  href: string;
}

export interface HelpSectionProps {
  /** Help section title */
  title: string;
  /** Help content (can include markdown-like formatting) */
  content: string;
  /** Optional documentation link */
  docLink?: DocLink;
  /** Show help icon in title */
  showIcon?: boolean;
  /** Custom class name */
  className?: string;
}

/**
 * HelpSection provides contextual help for settings pages.
 *
 * Features:
 * - Title with optional help icon
 * - Multi-line content support
 * - Optional documentation link
 * - Consistent styling for sidebar help
 *
 * @example
 * ```tsx
 * <HelpSection
 *   title="What is a blob store?"
 *   content="The binary assets you download via proxy repositories, or publish to hosted repositories, are stored in the blob store attached to those repositories."
 *   docLink={{
 *     label: "View Documentation",
 *     href: "https://help.sonatype.com/...",
 *   }}
 * />
 * ```
 */
export function HelpSection({
  title,
  content,
  docLink,
  showIcon = true,
  className = '',
}: HelpSectionProps): JSX.Element {
  // Split content by newlines to support multi-paragraph help text
  const paragraphs = content.split('\n').filter((p) => p.trim());

  return (
    <Box className={`help-section ${className}`} data-testid="help-section">
      {/* Title */}
      <Text as="h4" size="2" weight="medium" className="help-section__title">
        {showIcon && <HelpCircle size={14} aria-hidden="true" />}
        {title}
      </Text>

      {/* Content */}
      <Box className="help-section__content">
        {paragraphs.map((paragraph, index) => (
          <Text key={index} as="p" size="2" color="gray" className="help-section__paragraph">
            {paragraph}
          </Text>
        ))}
      </Box>

      {/* Documentation Link */}
      {docLink && (
        <a
          href={docLink.href}
          target="_blank"
          rel="noopener noreferrer"
          className="help-section__link"
        >
          {docLink.label}
          <ExternalLink size={12} aria-hidden="true" />
        </a>
      )}
    </Box>
  );
}

export default HelpSection;


