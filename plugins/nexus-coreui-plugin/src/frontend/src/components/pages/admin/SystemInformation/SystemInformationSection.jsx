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

import {Information, Section} from '@sonatype/nexus-ui-plugin';

/**
 * @since 3.24
 * @param sectionName - the name of the section
 * @param information - a key value map of information to display
 */
export default function SystemInformationSection({sectionName, information}) {
  if (information === undefined || information === null) {
    return <></>;
  }

  return <Section>
    <h2>{sectionName}</h2>
    <Information information={information}/>
  </Section>;
}
