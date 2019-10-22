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
import PropTypes from 'prop-types';
import React, {useEffect, useRef, useState} from 'react';

import './SettingsSection.scss';

import UIStrings from '../../../../constants/UIStrings';

export default function SettingsSection({isLoading, children}) {
  const wrapperRef = useRef();
  const [loadingMaskHeight, setLoadingMaskHeight] = useState(0);

  useEffect(() => {
    const wrapperHeight = wrapperRef.current.clientHeight;
    if (wrapperHeight !== loadingMaskHeight) {
      setLoadingMaskHeight(wrapperHeight);
    }
  });

  let loadingMask = null;
  if (isLoading) {
    const loadingMaskStyle = {
      height: loadingMaskHeight
    };
    loadingMask = <span style={loadingMaskStyle} className='nxrm-settings-section-loading-mask'>
      {UIStrings.SETTINGS.LOADING_MASK}
    </span>;
  }

  return <>
    {loadingMask}
    <div className='nxrm-settings-section' ref={wrapperRef}>
      {children}
    </div>
  </>;
}

SettingsSection.propTypes = {
  isLoading: PropTypes.bool
};

SettingsSection.FieldWrapper = function({labelText, children}) {
  const WrapperElement = labelText ? 'label' : 'div';

  return <WrapperElement className='nxrm-settings-section-field-wrapper'>
    {labelText ? <span className='nxrm-settings-section-field-wrapper-label'>{labelText}</span> : null}
    {children}
  </WrapperElement>;
};

SettingsSection.FieldWrapper.propTypes = {
  labelText: PropTypes.string
};

SettingsSection.Footer = function({children}) {
  return <div className='nxrm-settings-section-footer'>
    {children}
  </div>;
};
