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
import {faTrash} from '@fortawesome/free-solid-svg-icons';

import {
  NxButton,
  NxButtonBar,
  NxFontAwesomeIcon,
  NxFormGroup,
  NxFormRow,
  NxTextInput
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';

const {ROUTING_RULES} = UIStrings;

export default function RoutingRuleMatcherRow({label, showRemoveButton, onRemove, ...props}) {
  return <NxFormRow className="routing-rule-matcher">
    {/*Use an empty label to avoid error with no label being provided*/}
    <NxFormGroup label={label}>
      <NxTextInput
          className="nx-text-input--long"
          label={label}
          {...props} />
    </NxFormGroup>
    {showRemoveButton &&
        <NxButtonBar>
          <NxButton
              type="button"
              title={ROUTING_RULES.FORM.DELETE_MATCHER_BUTTON}
              onClick={onRemove}>
            <NxFontAwesomeIcon icon={faTrash}/>
          </NxButton>
        </NxButtonBar>}
  </NxFormRow>;
}
