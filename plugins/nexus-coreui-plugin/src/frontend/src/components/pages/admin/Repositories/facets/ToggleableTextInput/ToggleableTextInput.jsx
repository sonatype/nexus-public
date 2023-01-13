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
import React, {useState} from 'react';

import {path} from 'ramda';

import {FormUtils} from '@sonatype/nexus-ui-plugin';

import {NxCheckbox, NxFieldset, NxTextInput} from '@sonatype/react-shared-components';

import './ToggleableTextInput.scss';

export default function ToggleableTextInput({
  parentMachine,
  contextPropName,
  defaultValue = '',
  label,
  sublabel = '',
  placeholder = '',
  className = '',
  clearIfDisabled = false
}) {
  const [currentParent, sendParent] = parentMachine;

  const actualValue = path(contextPropName.split('.'), currentParent.context.data);

  const [isEnabled, setIsEnabled] = useState(!!actualValue);

  const [tempValue, setTempValue] = useState(actualValue ? actualValue.toString() : '');

  const updateActualValue = (value) => sendParent({type: 'UPDATE', name: contextPropName, value});

  const toggleCheckbox = () => {
    const newIsEnabled = !isEnabled;

    if (newIsEnabled) {
      if (tempValue) {
          updateActualValue(tempValue);
      } else {
        updateActualValue(defaultValue);
        setTempValue(defaultValue);
      }
    } else {
      updateActualValue(null)
    }

    setIsEnabled(newIsEnabled);
  };

  const handleTextInputChange = (value) => {
    updateActualValue(value);
    setTempValue(value);
  };

  const displayedValue = !isEnabled && clearIfDisabled ? '' : tempValue;

  return (
    <NxFieldset label={label} sublabel={sublabel} className={className}>
      <div className="nxrm-toggleable-text-input-container">
        <NxCheckbox
          onChange={toggleCheckbox}
          isChecked={isEnabled}
          aria-label="Toggle Text Input"
        />
        <NxTextInput
          {...FormUtils.fieldProps(contextPropName, currentParent)}
          value={displayedValue}
          onChange={handleTextInputChange}
          disabled={!isEnabled}
          placeholder={placeholder}
        />
      </div>
    </NxFieldset>
  );
}
