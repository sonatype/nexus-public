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
import UIStrings from '../../../../constants/UIStrings';
import {Button, ContentBody, NxFontAwesomeIcon, Section} from 'nexus-ui-plugin';
import './SupportRequest.scss';
import {faExternalLinkAlt} from "@fortawesome/free-solid-svg-icons";

export default function SupportRequest() {
  return <ContentBody className='nxrm-support-request'>
    <SupportRequestSection/>
  </ContentBody>
}

function SupportRequestSection() {
  function openSupportRequestPage() {
    window.open('https://links.sonatype.com/products/nexus/pro/support-request', '_blank');
  }

  return <Section>
    <p>
      {UIStrings.SUPPORT_REQUEST.DESCRIPTION}
    </p>
    <p>
      <a href="#admin/support/supportzip">{UIStrings.SUPPORT_REQUEST.ATTACH_SUPPORT_ZIP}</a>
    </p>
    <Button variant="primary" onClick={() => openSupportRequestPage()}>
      <NxFontAwesomeIcon icon={faExternalLinkAlt}/>
      <span>{UIStrings.SUPPORT_REQUEST.ACTIONS.submitRequest}</span>
    </Button>
  </Section>
}
