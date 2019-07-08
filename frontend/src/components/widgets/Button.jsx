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
import React from 'react';

import Colors from '../../constants/Colors'

export default function Button({isPrimary, children, style, ...rest}) {
  const colors = isPrimary ? Colors.BUTTON.PRIMARY : Colors.BUTTON.SECONDARY;
  const buttonStyle = {
    backgroundColor: colors.BACKGROUND,
    border: `solid 1px ${colors.BORDER}`,
    borderRadius: '3px',
    color: colors.FONT,
    fontFamily: 'inherit',
    fontSize: '1em',
    height: '2em',
    minWidth: '76px',
    opacity: rest && rest.disabled ? '0.5' : '1',
    ...style
  };

  return <button style={buttonStyle} {...rest}>
      { children }
    </button>;
}

Button.propTypes = {
  isPrimary: PropTypes.bool,
  style: PropTypes.object
};
