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

import React, { useMemo } from 'react';
import { Box, Text, Flex } from '@radix-ui/themes';
import { Eye } from 'lucide-react';
import { interpretExpression } from './cselValidator';

import './ExpressionPreview.scss';

interface ExpressionPreviewProps {
  expression: string;
}

/**
 * ExpressionPreview - Shows a human-readable interpretation of the CSEL expression
 *
 * This is a read-only, best-effort preview that helps users understand what
 * their expression will match. It does NOT block save if parsing fails.
 */
export function ExpressionPreview({ expression }: ExpressionPreviewProps) {
  const interpretation = useMemo(() => interpretExpression(expression), [expression]);

  if (!(expression.trim() && interpretation.success)) {
    return null;
  }

  return (
    <Box className="expression-preview">
      <Flex align="center" gap="2" className="expression-preview__header">
        <Eye size={14} />
        <Text size="1" weight="medium">Expression Preview</Text>
      </Flex>
      <Text as="p" size="2" className="expression-preview__text">
        {interpretation.text}
      </Text>
    </Box>
  );
}

export default ExpressionPreview;


