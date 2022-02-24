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
import {NxStatefulTabs, NxTabList, NxTab, NxTabPanel} from '@sonatype/react-shared-components';

import NodeSupportZipResponse from "./NodeSupportZipResponse";

export default function SupportZipResponseHA({response, download}) {
  return <NxStatefulTabs defaultActiveTab={0}>
    <NxTabList>
     {response.map((nodeZip) =>
       <NxTab id={nodeZip.nodeId} key={nodeZip.nodeId}>
         {nodeZip.nodeAlias}
       </NxTab>)}
    </NxTabList>
    {response.map((nodeZip) =>
        <NxTabPanel id={nodeZip.nodeId} key={nodeZip.nodeId}>
          <NodeSupportZipResponse response={nodeZip} download={download} />
        </NxTabPanel>
    )}
  </NxStatefulTabs>;
}
