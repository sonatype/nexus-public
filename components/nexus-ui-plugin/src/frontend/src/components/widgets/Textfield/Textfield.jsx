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
import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';

import './Textfield.scss';

import RequiredErrorMessage from '../RequiredErrorMessage/RequiredErrorMessage';

/**
 * @since 3.next
 */
export default function Textfield({name, value, onChange, isRequired, className}) {
  const isMissingRequiredValue = isRequired && !value;
  const classes = classNames('nxrm-textfield', className, {
    'missing-required-value': isMissingRequiredValue
  });
  return <>
    <input
        name={name}
        type='text'
        value={value}
        onChange={onChange}
        className={classes}
    />
    {isMissingRequiredValue ? <RequiredErrorMessage/> : null}
  </>;
}

Textfield.propTypes = {
  name: PropTypes.string,
  value: PropTypes.string,
  onChange: PropTypes.func,
  isRequired: PropTypes.bool
};
