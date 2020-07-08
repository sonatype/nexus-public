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
import {Button, ContentBody, NxFontAwesomeIcon, Page, PageHeader, PageTitle, Section} from 'nexus-ui-plugin';
import {faExternalLinkAlt, faUserCircle} from '@fortawesome/free-solid-svg-icons';

import UIStrings from '../../../../constants/UIStrings';
import './SupportRequest.scss';

export default function SupportRequest() {
  function openSupportRequestPage() {
    window.open('https://links.sonatype.com/products/nexus/pro/support-request', '_blank');
  }

  return <Page>
    <PageHeader><PageTitle icon={faUserCircle} {...UIStrings.SUPPORT_REQUEST.MENU}/></PageHeader>
    <ContentBody className='nxrm-support-request'>
      <Section>
        <p>{UIStrings.SUPPORT_REQUEST.DESCRIPTION}</p>
        <p><a href="#admin/support/supportzip">{UIStrings.SUPPORT_REQUEST.ATTACH_SUPPORT_ZIP}</a></p>
        <Button variant="primary" onClick={() => openSupportRequestPage()}>
          <NxFontAwesomeIcon icon={faExternalLinkAlt}/>
          <span>{UIStrings.SUPPORT_REQUEST.ACTIONS.submitRequest}</span>
        </Button>
      </Section>
    </ContentBody>
  </Page>;
}
