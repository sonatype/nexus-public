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

import {ReadOnlyField, FormUtils} from '@sonatype/nexus-ui-plugin';

import {NxInfoAlert} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';

const {
  USER_TOKEN_CONFIGURATION: {USER_TOKENS_CHECKBOX, USER_TOKEN_EXPIRY, EXPIRATION_CHECKBOX, REPOSITORY_AUTHENTICATION_CHECKBOX},
  SETTINGS: {
    READ_ONLY: {WARNING}
  }
} = UIStrings;

export default function UserTokensReadOnly({data}) {
  const {enabled, expirationEnabled, expirationDays, protectContent} = data;

  return (
    <>
      <NxInfoAlert>{WARNING}</NxInfoAlert>
      <ReadOnlyField
        label={USER_TOKENS_CHECKBOX.LABEL}
        value={FormUtils.readOnlyCheckboxValueLabel(enabled)}
      />
      <ReadOnlyField
        label={REPOSITORY_AUTHENTICATION_CHECKBOX.LABEL}
        value={FormUtils.readOnlyCheckboxValueLabel(protectContent)}
      />
      <ReadOnlyField
          label={EXPIRATION_CHECKBOX.LABEL}
          value={FormUtils.readOnlyCheckboxValueLabel(expirationEnabled)}
      />
      <ReadOnlyField
        label={USER_TOKEN_EXPIRY.LABEL}
        value={`${expirationDays} Day${expirationDays > 1 ? 's' : ''}`}
      />
    </>
  );
}
