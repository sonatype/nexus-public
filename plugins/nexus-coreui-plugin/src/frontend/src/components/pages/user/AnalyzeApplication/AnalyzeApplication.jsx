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
import PropTypes from "prop-types";

import {useMachine} from "@xstate/react";
import AnalyzeApplicationMachine from "./AnalyzeApplicationMachine";
import HealthCheckEula from "../HealthCheck/HealthCheckEula";
import AnalyzeApplicationModal from "./AnalyzeApplicationModal";
import {NxLoadWrapper} from '@sonatype/react-shared-components';

export default function AnalyzeApplication(props) {
  const [state, send] = useMachine(AnalyzeApplicationMachine, {
    context: {
      componentModel: props.componentModel
    },
    actions: {
      onClose: () => props.onClose()
    },
    devTools: true
  });

  const isLoading = state.matches('loading');
  const showEula = state.matches('showEula');
  const showAnalyze = !state.matches('loading') && !showEula;

  function handleAnalyzed() {
    send({type: 'ANALYZED'});
  }

  function handleCancel() {
    send({type: 'CANCEL'});
  }

  function handleAccept() {
    send({type: 'EULA_ACCEPTED'});
  }

  function handleDecline() {
    send({type: 'EULA_DECLINED'});
  }

  function retry() {
    send({type: 'RETRY'});
  }

  // This is needed to allow ExtJs to rerender the window 'after' React has rendered its components onto the DOM
  setTimeout(() => { props.rerender()}, 100);

  return <NxLoadWrapper loading={isLoading} retryHandler={retry}>
      {showEula && <HealthCheckEula onDecline={handleDecline} onAccept={handleAccept}/>}
      {showAnalyze && <AnalyzeApplicationModal componentModel={state.context.componentModel} onAnalyzed={handleAnalyzed} onCancel={handleCancel}/>}
    </NxLoadWrapper>

}

AnalyzeApplication.propTypes = {
  componentModel: PropTypes.object,
  rerender: PropTypes.func,
  onClose: PropTypes.func,
}
