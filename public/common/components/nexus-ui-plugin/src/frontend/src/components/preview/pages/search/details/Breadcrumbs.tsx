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
import { Box, Flex, Button, Text } from '@radix-ui/themes';
import { ChevronRight } from 'lucide-react';

export interface BreadcrumbItem {
  label: string;
  href?: string;
  onClick?: () => void;
}

interface BreadcrumbsProps {
  items: BreadcrumbItem[];
}

export function Breadcrumbs({ items }: BreadcrumbsProps) {
  return (
    <Box p="4" pt="4">
      <Flex align="center" gap="2">
        {items.map((item, index) => (
          <Flex key={index} align="center" gap="2">
            {index > 0 && <ChevronRight size={14} color="var(--gray-9)" />}
            {item.onClick || item.href ? (
              <Button
                variant="ghost"
                size="2"
                color="blue"
                onClick={item.onClick}
              >
                <Text size="2" color="blue">
                  {item.label}
                </Text>
              </Button>
            ) : (
              <Text size="2" weight="medium" color="gray">
                {item.label}
              </Text>
            )}
          </Flex>
        ))}
      </Flex>
    </Box>
  );
}

