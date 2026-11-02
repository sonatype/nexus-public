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

export default function SearchOpenVSXExt() {
  const CRITERIA = UIStrings.SEARCH.OPENVSX.CRITERIA;

  return (
    <SearchFeatureExt
      title={UIStrings.SEARCH.OPENVSX.MENU.text}
      icon={UIStrings.SEARCH.OPENVSX.MENU.icon}
      criterias={[
        {
          id: 'attributes.openvsx.publisher',
          group: CRITERIA.GROUP,
          config: {
            format: 'openvsx',
            fieldLabel: CRITERIA.FIELD_LABEL.PUBLISHER,
            width: 250
          }
        },
        {
          id: 'attributes.openvsx.targetPlatform',
          group: CRITERIA.GROUP,
          config: {
            format: 'openvsx',
            fieldLabel: CRITERIA.FIELD_LABEL.TARGET_PLATFORM,
            width: 250
          }
        }
      ]}
      filter={{
        id: 'openvsx',
        name: 'openvsx',
        text: UIStrings.SEARCH.OPENVSX.MENU.text,
        description: UIStrings.SEARCH.OPENVSX.MENU.description,
        readOnly: true,
        criterias: [
          {id: 'format', value: 'openvsx', hidden: true},
          {id: 'attributes.openvsx.publisher'},
          {id: 'attributes.openvsx.targetPlatform'},
          {id: 'name.raw'},
          {id: 'version'}
        ]
      }}
    />
  );
}
