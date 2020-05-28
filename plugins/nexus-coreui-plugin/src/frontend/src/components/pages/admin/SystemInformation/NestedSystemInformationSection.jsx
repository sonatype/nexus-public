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

import {Information, Section} from 'nexus-ui-plugin';

/**
 * @since 3.24
 * @param sectionName - the name of the nested section to display
 * @param sectionInformation - system information that has objects as the value instead of simple strings
 */
export default function NestedSystemInformationSection({sectionName, sectionInformation}) {
  return <Section>
    <h2>{sectionName}</h2>
    {Object.entries(sectionInformation).map(([nestedName, nestedInformation]) =>
        <div key={nestedName}>
          <h3>{nestedName}</h3>
          <Information information={nestedInformation}/>
        </div>
    )}
  </Section>;
}
