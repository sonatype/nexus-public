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

import {NxSegmentedButton, useToggle} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';

const {HEALTH_CHECK} = UIStrings.REPOSITORIES.LIST;

const MODE = {
  THIS: 'this',
  ALL: 'all'
};

export default function AnalyzeButton({enableHealthCheck, name}) {
  const [mode, setMode] = useState(MODE.THIS);
  const [isOpen, onToggleOpen] = useToggle(false);

  const analyze = () => {
    const repoName = mode === MODE.THIS ? name : null;
    enableHealthCheck(repoName);
  };

  const selectAnalyzeThisRepo = () => {
    setMode(MODE.THIS);
    onToggleOpen();
  };

  const selectAnalyzeAllRepos = () => {
    setMode(MODE.ALL);
    onToggleOpen();
  };

  return (
    <NxSegmentedButton
      variant="primary"
      isOpen={isOpen}
      onToggleOpen={onToggleOpen}
      onClick={analyze}
      buttonContent={HEALTH_CHECK.ANALYZE_BUTTON}
    >
      <button className="nx-dropdown-button" onClick={selectAnalyzeThisRepo}>
        {HEALTH_CHECK.ANALYZE_THIS(name)}
      </button>
      <button className="nx-dropdown-button" onClick={selectAnalyzeAllRepos}>
        {HEALTH_CHECK.ANALYZE_ALL}
      </button>
    </NxSegmentedButton>
  );
}
