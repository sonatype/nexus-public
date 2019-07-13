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

import Colors from '../../../../constants/Colors';
import UIStrings from '../../../../constants/UIStrings';

export default function SettingsSection({isLoading, children}) {
  const wrapperRef = useRef();
  const [loadingMaskHeight, setLoadingMaskHeight] = useState(0);

  useEffect (() => {
    const wrapperHeight = wrapperRef.current.clientHeight;
    if (wrapperHeight !== loadingMaskHeight) {
      setLoadingMaskHeight(wrapperHeight);
    }
  });

  const wrapperStyle = {
    backgroundColor: Colors.SETTINGS_SECTION.BACKGROUND,
    border: `solid 1px ${Colors.SETTINGS_SECTION.BORDER}`,
    padding: '12px'
  };
  const loadingMaskStyle = {
    alignItems: 'center',
    backgroundColor: Colors.LOADING_MASK.BACKGROUND,
    color: Colors.LOADING_MASK.FONT,
    display: 'flex',
    fontSize: '1.5em',
    height: loadingMaskHeight,
    justifyContent: 'center',
    position: 'absolute',
    width: 'calc(100% - 24px)'
  };

  return <>
    {isLoading ? <span style={loadingMaskStyle}>{UIStrings.SETTINGS.LOADING_MASK}</span> : null}
    <div style={wrapperStyle} ref={wrapperRef}>
      {children}
    </div>
  </>;
}

SettingsSection.propTypes = {
  isLoading: PropTypes.bool
};

SettingsSection.FieldWrapper = function({labelText, children}) {
  const fieldWrapperStyle = {
    display: 'block',
    marginBottom: '15px'
  };
  const labelTextStyle = {
    display: 'block',
    fontWeight: 'bold',
    marginBottom: '4px'
  };
  const WrapperElement = labelText ? 'label' : 'div';

  return <WrapperElement style={fieldWrapperStyle}>
    {labelText ? <span style={labelTextStyle}>{labelText}</span> : null}
    {children}
  </WrapperElement>;
};

SettingsSection.FieldWrapper.propTypes = {
  labelText: PropTypes.string
};

SettingsSection.Footer = function({children}) {
  const wrapperStyle = {
    display: 'flex',
    marginTop: '10px'
  };

  return <div style={wrapperStyle}>
    {children}
  </div>;
};
