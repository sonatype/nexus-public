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
import {faCheckCircle, faTimesCircle} from '@fortawesome/free-solid-svg-icons';

import {NxFontAwesomeIcon} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';

const {ROUTING_RULES} = UIStrings;

export default function RoutingRulesStatus({allowed}) {
  if (allowed) {
    return <span className="allowed">
      <NxFontAwesomeIcon icon={faCheckCircle}/>
      <span>{ROUTING_RULES.ALLOWED}</span>
    </span>;
  }
  else {
    return <span className="blocked">
      <NxFontAwesomeIcon icon={faTimesCircle}/>
      <span>{ROUTING_RULES.BLOCKED}</span>
    </span>;
  }
}
