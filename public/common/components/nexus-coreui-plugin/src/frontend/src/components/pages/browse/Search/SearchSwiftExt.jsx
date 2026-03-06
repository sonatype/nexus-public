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

import SearchFeatureExt from './SearchFeatureExt';
import UIStrings from '../../../../constants/UIStrings';

/**
 * Search component for Swift package repositories.
 * Provides format-specific search criteria including name, scope, version,
 * type, package identifier, and manifest filename.
 */
export default function SearchSwiftExt() {
  const CRITERIA = UIStrings.SEARCH.SWIFT.CRITERIA;

  return (
    <SearchFeatureExt
      title={UIStrings.SEARCH.SWIFT.MENU.text}
      icon={UIStrings.SEARCH.SWIFT.MENU.icon}
      criterias={[
        {
          id: 'attributes.swift.scope',
          group: CRITERIA.GROUP,
          config: {
            format: 'swift',
            fieldLabel: CRITERIA.FIELD_LABEL.SCOPE,
            width: 250
          }
        },
      ]}
      filter={{
        id: 'swift',
        name: 'swift',
        text: UIStrings.SEARCH.SWIFT.MENU.text,
        description: UIStrings.SEARCH.SWIFT.MENU.description,
        readOnly: true,
        criterias: [
          { id: 'format', value: 'swift', hidden: true },
          { id: 'name.raw' },
          { id: 'attributes.swift.scope' },
          { id: 'version' }
        ]
      }}
    />
  );
}
