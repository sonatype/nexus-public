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
import {NxButton, NxTextInput} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';

const {ROUTING_RULES} = UIStrings;

export default function RoutingRulesPreview({value, onChange, onTest}) {
  function handleEnter(event) {
    if (event === 'Enter') {
      onTest(event);
    }
  }

  return <div className="nx-form-group">
    <label htmlFor="path" className="nx-label"><span className="nx-label__text">{ROUTING_RULES.PATH_LABEL}</span></label>
    <div className="nx-sub-label">{ROUTING_RULES.PATH_DESCRIPTION}</div>
    <div className="nx-form-row">
      <div className="nx-form-group">
        {/* Ensure the button is at the correct height and prepend with a / */}
        <label className="nx-label nxrm-path">
          <NxTextInput
            className="nx-text-input--long nxrm-path-input"
            name="path"
            value={value}
            onChange={onChange}
            onKeyPress={handleEnter}
            validatable={false}
            isPristine={true}
          />
        </label>
      </div>
      <div className="nx-btn-bar">
        <NxButton variant="primary" onClick={onTest}>{ROUTING_RULES.TEST_BUTTON}</NxButton>
      </div>
    </div>
  </div>;
}
