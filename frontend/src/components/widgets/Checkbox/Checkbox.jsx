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

import './Checkbox.scss';

export default function Checkbox({name, isChecked, onChange, labelText}) {
  return <label>
    <input
        name={name}
        type='checkbox'
        checked={isChecked}
        onChange={onChange}
        className='nxrm-checkbox'
    />
    {labelText}
  </label>;
}

Checkbox.propTypes = {
  name: PropTypes.string,
  isChecked: PropTypes.bool.isRequired,
  onChange: PropTypes.func,
  labelText: PropTypes.string
};
