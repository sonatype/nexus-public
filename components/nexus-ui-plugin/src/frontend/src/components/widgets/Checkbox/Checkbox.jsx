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
/**
 * @since 3.21
 */
import PropTypes from 'prop-types';
import React from 'react';

import './Checkbox.scss';

export default function Checkbox({ checkboxId, isChecked, onChange, children }) {
  return <label>
    <input
      name={checkboxId}
      id={checkboxId}
      type='checkbox'
      checked={isChecked}
      onChange={onChange}
      className='nxrm-checkbox'
    />
    {children}
  </label>;
}

Checkbox.propTypes = {
  name: PropTypes.string,
  isChecked: PropTypes.bool,
  onChange: PropTypes.func
};
