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

export default function SearchTerraformStateBackendExt() {
  const STRINGS = UIStrings.SEARCH.TERRAFORM_STATE_BACKEND;

  return (
    <SearchFeatureExt
      title={STRINGS.MENU.text}
      icon={STRINGS.MENU.icon}
      criterias={[
        {
          id: 'name',
          group: STRINGS.CRITERIA.GROUP,
          config: {
            format: 'terraformbackend',
            fieldLabel: STRINGS.CRITERIA.FIELD_LABEL.PATH,
            width: 300
          }
        }
      ]}
      filter={{
        id: 'terraformbackend',
        name: 'Terraform State Backend',
        text: STRINGS.MENU.text,
        description: STRINGS.MENU.description,
        readOnly: true,
        criterias: [
          { id: 'format', value: 'terraformbackend', hidden: true },
          { id: 'name' }
        ]
      }}
    />
  );
}
