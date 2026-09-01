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

import React from "react";

import SearchFeatureExt from './SearchFeatureExt';
import UIStrings from "../../../../constants/UIStrings";

export default function SearchComposerExt() {
  const CRITERIA = UIStrings.SEARCH.COMPOSER.CRITERIA;

  return (
    <SearchFeatureExt
      title={UIStrings.SEARCH.COMPOSER.MENU.text}
      icon={UIStrings.SEARCH.COMPOSER.MENU.icon}
      criterias={[
        {
          id: 'assets.attributes.composer.vendor',
          group: CRITERIA.GROUP,
          config: {
            format: 'composer',
            fieldLabel: CRITERIA.FIELD_LABEL.VENDOR,
            width: 250
          }
        },
        {
          id: 'assets.attributes.composer.package',
          group: CRITERIA.GROUP,
          config: {
            format: 'composer',
            fieldLabel: CRITERIA.FIELD_LABEL.PACKAGE,
            width: 250
          }
        },
        {
          id: 'composer.description',
          group: CRITERIA.GROUP,
          config: {
            format: 'composer',
            fieldLabel: CRITERIA.FIELD_LABEL.DESCRIPTION,
            width: 250
          }
        },
        {
          id: 'composer.keywords',
          group: CRITERIA.GROUP,
          config: {
            format: 'composer',
            fieldLabel: CRITERIA.FIELD_LABEL.KEYWORDS,
            width: 250
          }
        }
      ]}
      filter={{
        id: 'composer',
        name: 'Composer',
        text: UIStrings.SEARCH.COMPOSER.MENU.text,
        description: UIStrings.SEARCH.COMPOSER.MENU.description,
        readOnly: true,
        criterias: [
          { id: 'format', value: 'composer', hidden: true },
          { id: 'assets.attributes.composer.vendor' },
          { id: 'assets.attributes.composer.package' },
          { id: 'version' },
          { id: 'composer.description' },
          { id: 'composer.keywords' }
        ]
      }}
    />
  );
}
