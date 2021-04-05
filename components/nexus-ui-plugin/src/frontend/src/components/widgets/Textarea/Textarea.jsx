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
import React, {forwardRef} from 'react';
import PropTypes from 'prop-types';
import {NxTextInput} from "@sonatype/react-shared-components";

/**
 * @since 3.22
 * @deprecated prefer NxTextInput instead
 */
const Textarea = forwardRef(({name, id, onChange, ...attrs}, ref) => {
  const handleChange = (value) => {
    if (onChange) {
      onChange({
        target: {
          id: id || name,
          name: name,
          type: "textarea",
          value
        }
      });
    }
  };
  return <NxTextInput id={id || name}
                      name={name}
                      ref={ref}
                      type="textarea"
                      isPristine={false}
                      onChange={handleChange}
                      {...attrs} />;
});

Textarea.propTypes = {
  validationErrors: PropTypes.oneOfType([PropTypes.string, PropTypes.array])
};

export default Textarea;
