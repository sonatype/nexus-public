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
import classNames from 'classnames';

import {faChevronRight} from '@fortawesome/free-solid-svg-icons';
import {NxFontAwesomeIcon} from '@sonatype/react-shared-components';

export default function QuickAction({title, subTitle, icon, action, className}) {
  const handleEnter = (event) => {
    if (event.key === 'Enter') {
      action();
    }
  };

  return (
      <div
          className={classNames('nxrm-quick-action', className)}
          onClick={action}
          onKeyDown={handleEnter}
      >
        <div
            className="nxrm-quick-action-outline"
            tabIndex="0"
            role="button"
        >
          <div className="nxrm-quick-action-body">
            <div className="nxrm-quick-action-logo">
              <div className="nxrm-quick-action-icon-container">
                <NxFontAwesomeIcon icon={icon} />
              </div>
            </div>
            <div className="nxrm-quick-action-name">
              <h4 className="nxrm-quick-action-title">{title}</h4>
              <div className="nxrm-quick-action-subtitle">{subTitle}</div>
            </div>
            <div className="nxrm-quick-action-chevron">
              <NxFontAwesomeIcon
                  icon={faChevronRight}
                  transform="down-1"
                  className="nxrm-quick-action-chevron-icon"
              />
            </div>
          </div>
        </div>
      </div>
  );
}
