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
import React, {useState, useEffect} from 'react';

import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faDownload} from '@fortawesome/free-solid-svg-icons';

import Axios from 'axios';
import {BreadcrumbActions, Button, ContentBody, Information, Section, Utils} from 'nexus-ui-plugin';
import UIStrings from '../../../../constants/UIStrings';

import './SystemInformation.scss';

const INITIAL_VALUE = {};

export default function SystemInformation() {
  const [systemInformation, setSystemInformation] = useState(INITIAL_VALUE);
  const isLoaded = systemInformation !== INITIAL_VALUE;

  useEffect(() => {
    if (isLoaded) {
      return;
    }

    Axios.get('/service/rest/atlas/system-information').then((result) => {
      setSystemInformation(result.data);
    });
  });

  function downloadSystemInformation() {
    window.open(Utils.urlFromPath('/service/rest/atlas/system-information'), '_blank');
  }

  return <>
    <BreadcrumbActions>
      <Button variant="primary" onClick={() => downloadSystemInformation()} disabled={!isLoaded}>
        <FontAwesomeIcon icon={faDownload} pull="left" />
        {UIStrings.SYSTEM_INFORMATION.ACTIONS.download}
      </Button>
    </BreadcrumbActions>
    {isLoaded ?
        <ContentBody className="nxrm-system-information">
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
          <NestedSystemInformationSection
              sectionName="nexus-bundles"
              sectionInformation={systemInformation['nexus-bundles']}
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
        </ContentBody> :
        null
    }
  </>;
}

function SystemInformationSection({sectionName, information}) {
  return <Section>
    <h2>{sectionName}</h2>
    <Information information={information} />
  </Section>;
}

function NestedSystemInformationSection({sectionName, sectionInformation}) {
  return <Section>
    <h2>{sectionName}</h2>
    {Object.entries(sectionInformation).map(([nestedName, nestedInformation]) =>
        <div key={nestedName}>
          <h3>{nestedName}</h3>
          <Information information={nestedInformation} />
        </div>
    )}
  </Section>;
}
