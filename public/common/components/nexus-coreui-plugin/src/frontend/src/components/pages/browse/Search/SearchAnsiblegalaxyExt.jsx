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

export default function SearchAnsiblegalaxyExt() {
    const CRITERIA = UIStrings.SEARCH.ANSIBLEGALAXY.CRITERIA;

  return (
    <SearchFeatureExt
      title={UIStrings.SEARCH.ANSIBLEGALAXY.MENU.text}
      icon={UIStrings.SEARCH.ANSIBLEGALAXY.MENU.icon}
      criterias={[
          {
              id: 'assets.attributes.ansiblegalaxy.namespace',
              group: CRITERIA.GROUP,
              config: {
                  format: 'ansiblegalaxy',
                  fieldLabel: CRITERIA.FIELD_LABEL.NAMESPACE,
                  width: 250
              }
          },
          {
              id: 'assets.attributes.ansiblegalaxy.name',
              group: CRITERIA.GROUP,
              config: {
                  format: 'ansiblegalaxy',
                  fieldLabel: CRITERIA.FIELD_LABEL.NAME,
                  width: 250
              }
          },
          {
              id: 'assets.attributes.ansiblegalaxy.version',
              group: CRITERIA.GROUP,
              config: {
                  format: 'ansiblegalaxy',
                  fieldLabel: CRITERIA.FIELD_LABEL.VERSION,
                  width: 250
              }
          }
      ]}
      filter={{
        id: 'ansiblegalaxy',
        name: 'Ansible Galaxy',
        text: UIStrings.SEARCH.ANSIBLEGALAXY.MENU.text,
        description: UIStrings.SEARCH.ANSIBLEGALAXY.MENU.description,
        readOnly: true,
        criterias: [
          { id: 'format', value: 'ansiblegalaxy', hidden: true },
            { id: 'assets.attributes.ansiblegalaxy.namespace' },
            { id: 'assets.attributes.ansiblegalaxy.name' },
            { id: 'assets.attributes.ansiblegalaxy.version' }
        ]
      }}
    />
  );
}
