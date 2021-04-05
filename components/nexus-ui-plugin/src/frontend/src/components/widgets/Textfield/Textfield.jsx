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
import {NxTextInput} from '@sonatype/react-shared-components';

/**
 * @since 3.21
 * @deprecated prefer NxTextInput instead
 */
export default function Textfield({id, name, type = "text", onChange, isPristine = false, validatable = true, ...attrs}) {
  function handleChange(value) {
    if (onChange) {
      const target = {
        id: id || name,
        name,
        type,
        value
      };
      onChange({
        currentTarget: target,
        target: target
      });
    }
  }

  const inputAttrs = {
    ...attrs,
    id: id || name,
    name,
    type: type === 'number' ? 'text' : type,
    isPristine,
    validatable,
    onChange: handleChange
  };

  return <NxTextInput {...inputAttrs} />;
}

Textfield.propTypes = {
  id: PropTypes.string,
  name: PropTypes.string,
  value: PropTypes.string,
  validationErrors: PropTypes.oneOfType([PropTypes.string, PropTypes.array])
};
