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
import PropTypes from 'prop-types';
import {faHandsHelping} from "@fortawesome/free-solid-svg-icons";
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import {NxButton, NxFontAwesomeIcon} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';
import {useMachine} from "@xstate/react";
import HealthCheckEulaMachine from "./HealthCheckEulaMachine";

export default function HealthCheckEula(props) {
  const [state, send] = useMachine(HealthCheckEulaMachine, {
    actions: {
      eulaAccepted: () => props.onAccept(),
      eulaDeclined: () => props.onDecline()
    },
    devTools: true
  });

  const iframe = `<iframe style="width: 100%" src=${ExtJS.urlOf('/static/healthcheck-tos.html')}></iframe>`;

  function handleEulaAccept() {
    send({type: 'ACCEPT'});
  }

  function handleEulaDecline() {
    send({type: 'DECLINE'});
  }

  return <div>
    <header className="nx-modal-header">
      <h2 className="nx-h2">
        <NxFontAwesomeIcon icon={faHandsHelping} fixedWidth/>
        <span>{UIStrings.HEALTHCHECK_EULA.HEADER}</span>
      </h2>
    </header>

    <div className="nx-modal-content">
      <p dangerouslySetInnerHTML={{__html: iframe}} />
    </div>

    <footer className="nx-footer">
      <NxButton variant="primary" onClick={handleEulaAccept}>
        {UIStrings.HEALTHCHECK_EULA.BUTTONS.ACCEPT}
      </NxButton>
      <NxButton onClick={handleEulaDecline}>
        {UIStrings.HEALTHCHECK_EULA.BUTTONS.DECLINE}
      </NxButton>
    </footer>
  </div>
}

HealthCheckEula.propTypes = {
  onAccept: PropTypes.func,
  onDecline: PropTypes.func
}
