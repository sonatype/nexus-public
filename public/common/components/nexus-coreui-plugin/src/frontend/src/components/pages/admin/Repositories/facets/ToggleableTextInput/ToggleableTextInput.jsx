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
import React, {useEffect, useState} from 'react';

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
  clearIfDisabled = false,
  onToggle
}) {
  const [currentParent, sendParent] = parentMachine;

  const actualValue = path(contextPropName.split('.'), currentParent.context.data);

  const [isEnabled, setIsEnabled] = useState(!!actualValue);

  const [tempValue, setTempValue] = useState(actualValue ? actualValue.toString() : '');

  // Sync tempValue when the machine context is updated externally (e.g. fetchSuggestedPort).
  // Only applies to primitive values (string/number) — guards against unexpected types.
  useEffect(() => {
    if (actualValue != null && actualValue !== '' &&
        (typeof actualValue === 'string' || typeof actualValue === 'number')) {
      setTempValue(prev => {
        const str = String(actualValue);
        return prev !== str ? str : prev;
      });
    }
  }, [actualValue]);

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

    if (onToggle) {
      onToggle(newIsEnabled);
    }
  };

  const handleTextInputChange = (value) => {
    updateActualValue(value);
    setTempValue(value);
  };

  const displayedValue = !isEnabled && clearIfDisabled ? '' : tempValue;

  // NEXUS-53064 B3: render the toggle checkbox alongside the sublabel rather
  //   than next to the input field. The checkbox controls whether this field
  //   is active, so it belongs with the descriptive copy, not crammed into the
  //   input row where it pushed the input flush against the form gutter.
  // NEXUS-53064 B4: lifting the checkbox out of the input row gives the input
  //   the full available width, which fixes the right-edge truncation that
  //   was visible on Subdomain / HTTP / HTTPS fields.
  const sublabelWithCheckbox = (
    <span className="nxrm-toggleable-sublabel-row">
      {sublabel ? (
        <span className="nxrm-toggleable-sublabel-text">{sublabel}</span>
      ) : null}
      <NxCheckbox
        onChange={toggleCheckbox}
        isChecked={isEnabled}
        aria-label="Toggle Text Input"
      />
    </span>
  );

  return (
    <NxFieldset label={label} sublabel={sublabelWithCheckbox} className={className}>
      <div className="nxrm-toggleable-text-input-container">
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
