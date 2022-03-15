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

import SystemInformationSection from "./SystemInformationSection";
import NestedSystemInformationSection from "./NestedSystemInformationSection";

/**
 * @since 3.24
 * @param systemInformation - a map with objects representing each section of system information
 */
export default function SystemInformationBody({systemInformation}) {
  return <>
    <SystemInformationSection
        sectionName="nexus-status"
        information={systemInformation['nexus-status']}
    />
    <SystemInformationSection
        sectionName="nexus-node"
        information={systemInformation['nexus-node']}
    />
    <SystemInformationSection
        sectionName="nexus-configuration"
        information={systemInformation['nexus-configuration']}
    />
    <SystemInformationSection
        sectionName="nexus-properties"
        information={systemInformation['nexus-properties']}
    />
    <SystemInformationSection
        sectionName="nexus-license"
        information={systemInformation['nexus-license']}
    />
    <SystemInformationSection
        sectionName="system-time"
        information={systemInformation['system-time']}
    />
    <SystemInformationSection
        sectionName="system-properties"
        information={systemInformation['system-properties']}
    />
    <SystemInformationSection
        sectionName="system-environment"
        information={systemInformation['system-environment']}
    />
    <SystemInformationSection
        sectionName="system-runtime"
        information={systemInformation['system-runtime']}
    />
    <NestedSystemInformationSection
        sectionName="system-network"
        sectionInformation={systemInformation['system-network']}
    />
    <NestedSystemInformationSection
        sectionName="system-filestores"
        sectionInformation={systemInformation['system-filestores']}
    />
  </>;
}
