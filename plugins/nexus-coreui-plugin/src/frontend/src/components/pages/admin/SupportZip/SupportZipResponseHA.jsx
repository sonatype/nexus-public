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
import React, {useState} from 'react';
import {Tab, TabPanel, TabLabel, TabList} from "nexus-ui-plugin";

import NodeSupportZipResponse from "./NodeSupportZipResponse";

export default function SupportZipResponseHA({response, download}) {
  const [activeTabId, setActiveTabId] = useState(response[0].nodeId);

  function onTabClicked(tabId) {
    setActiveTabId(tabId);
  }

  return <TabPanel>
    <TabList>
     {response.map((nodeZip) =>
       <TabLabel
         id={nodeZip.nodeId} 
         active={activeTabId == nodeZip.nodeId}
         key={nodeZip.nodeId}
         onClick={() => onTabClicked(nodeZip.nodeId)}
       >
         {nodeZip.nodeAlias}
       </TabLabel>)}
    </TabList>
    {response.map((nodeZip) =>
      activeTabId == nodeZip.nodeId &&
        <Tab id={nodeZip.nodeId} key={nodeZip.nodeId}>
          <NodeSupportZipResponse response={nodeZip} download={download} />
        </Tab>)}
  </TabPanel>;

}
