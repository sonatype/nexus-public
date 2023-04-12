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
import { NxTextLink } from '@sonatype/react-shared-components';

export default {
  WELCOME: {
    MENU: {
      text: 'Welcome'
    },
    LOG4J_ALERT_CONTENT: (
      <>
        In response to the log4j vulnerability identified in{' '}
        <NxTextLink external
                    href="https://ossindex.sonatype.org/vulnerability/f0ac54b6-9b81-45bb-99a4-e6cb54749f9d">
          CVE-2021-44228
        </NxTextLink>{' '}
        (also known as "log4shell") impacting organizations world-wide, we are providing an experimental Log4j
        Visualizer capability to help our users identify log4j downloads impacted by CVE-2021-44228 so that they can
        mitigate the impact. Note that enabling this capability may impact Nexus Repository performance. Also note
        that the visualizer does not currently identify or track other log4j vulnerabilities.
      </>
    ),
    LOG4J_ENABLE_BUTTON_CONTENT: 'Enable Capability',
    LOG4J_SUBMIT_MASK_MESSAGE: 'Enabling…'
  }
};
