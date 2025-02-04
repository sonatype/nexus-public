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

import {NxInfoAlert} from '@sonatype/react-shared-components';
import './CommunityOnboarding.scss';

export default function CommunityEulaOnboarding() {
  return (
    <div className="ce-eula-onboarding-screen">
        <NxInfoAlert>By clicking 'Agree', you acknowledge that you have read and agree to the End User License Agreement.</NxInfoAlert>
    </div>
  );
}
