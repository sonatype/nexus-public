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

interface ReadOnlySelectedListProps {
  label?: string;
  helpText?: string;
  values: string[];
  emptyText?: string;
  testId?: string;
}

/**
 * Read-only "Selected"-only display for a task field that mirrors Classic renderExecutePlanFields:
 * the blob-store / repository values are shown without an Available column or transfer controls.
 * Presentational only — no data fetching, no editing.
 */
export function ReadOnlySelectedList({ label, helpText, values, emptyText, testId }: ReadOnlySelectedListProps): React.ReactElement {
  return (
    <Box className="dynamic-field read-only-selected-list">
      {label && (
        <Text as="div" size="2" weight="medium" className="dynamic-field__label">{label}</Text>
      )}
      {helpText && (
        <Text as="p" size="1" color="gray" className="dynamic-field__help">{helpText}</Text>
      )}
      <Text as="div" size="2" weight="medium" className="read-only-selected-list__heading" mt="1">Selected</Text>
      <Box
        role="list"
        className="read-only-selected-list__box"
        data-testid={testId}
        style={{
          background: 'var(--gray-3)',
          border: '1px solid var(--gray-5)',
          borderRadius: 'var(--radius-2)',
          padding: 'var(--space-2)',
          minHeight: '120px',
          color: 'var(--gray-11)',
        }}
      >
        {values.length === 0 && emptyText && (
          <Text as="div" size="2" color="gray" className="read-only-selected-list__empty">{emptyText}</Text>
        )}
        {values.map((v) => (
          <div key={v} role="listitem" className="read-only-selected-list__item">{v}</div>
        ))}
      </Box>
    </Box>
  );
}

export default ReadOnlySelectedList;
