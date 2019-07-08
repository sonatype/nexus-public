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
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faExclamationCircle } from '@fortawesome/free-solid-svg-icons';
import PropTypes from 'prop-types';
import React from 'react';

import Colors from '../../constants/Colors';
import UIStrings from '../../constants/UIStrings';

const requiredErrorMessageStyle = {
  alignItems: 'center',
  color: Colors.TEXTFIELD.ERROR.FONT,
  display: 'flex'
};
const errorTextStyle = {
  padding: '2px 0 0 2px'
};
const RequiredErrorMessage = <span style={requiredErrorMessageStyle}>
  <FontAwesomeIcon icon={faExclamationCircle} />
  <span style={errorTextStyle}> { UIStrings.ERROR.FIELD_REQUIRED } </span>
</span>;

export default function Textfield({name, value, onChange, isRequired, style}) {
  const isMissingRequiredValue = isRequired && !value;
  const inputStyle = {
    border: 'solid 1px',
    borderColor: isMissingRequiredValue ? Colors.TEXTFIELD.ERROR.BORDER : Colors.TEXTFIELD.BORDER,
    borderTopColor: isMissingRequiredValue ? Colors.TEXTFIELD.ERROR.BORDER_TOP : Colors.TEXTFIELD.BORDER_TOP,
    fontSize: '1em',
    height: '2em',
    paddingLeft: '0.4em',
    width: '100%',
    ...style
  };
  return <>
    <input
        name={name}
        type='text'
        value={value}
        onChange={onChange}
        style={inputStyle}
    />
    { isMissingRequiredValue ? RequiredErrorMessage : null }
  </>;
}

Textfield.propTypes = {
  name: PropTypes.string,
  value: PropTypes.string,
  onChange: PropTypes.func,
  isRequired: PropTypes.bool
};
