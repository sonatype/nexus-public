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
import {move} from 'ramda';

import {NxButton, NxFilterInput, NxFontAwesomeIcon, NxTooltip} from '@sonatype/react-shared-components';
import {faCaretDown, faCaretLeft, faCaretRight, faCaretUp} from '@fortawesome/free-solid-svg-icons';
import Select from '../Select/Select';
import Utils from '../../../interface/Utils';

import './MultiSelect.scss';
import UIStrings from '../../../constants/UIStrings';

/**
 * @since 3.31
 */
export default function MultiSelect({
                                      fromOptions,
                                      value,
                                      fromLabel = UIStrings.MULTI_SELECT.FROM_LABEL,
                                      toLabel = UIStrings.MULTI_SELECT.TO_LABEL,
                                      onChange = () => {
                                      }
                                    }) {
  const [filter, setFilter] = useState('');
  const [fromSelected, setFromSelected] = useState([]);
  const [toSelected, setToSelected] = useState([]);
  const fromValues = fromOptions.filter(from => !value.includes(from)).filter(from => from.includes(filter));

  const fromId = Utils.useRandomId('nxrm-multiselect-from');
  const toId = Utils.useRandomId('nxrm-multiselect-to');

  const onFilterChange = (value) => {
    setFilter(value);
  }
  const changeFromSelection = (event) => {
    setFromSelected(Array.from(event.currentTarget.selectedOptions, (option) => option.value));
  }
  const changeToSelection = (event) => {
    setToSelected(Array.from(event.currentTarget.selectedOptions, (option) => option.value));
  }
  const moveRight = () => {
    setFromSelected([]);
    onChange(value.concat(fromSelected));
  };
  const moveLeft = () => {
    setToSelected([]);
    onChange(value.filter(option => !toSelected.includes(option)))
  };
  const moveUp = () => {
    let newValue = value;
    let last = 0;
    for (const selection of toSelected) {
      const i = newValue.indexOf(selection);
      if (i == last) {
        last += 1;
      }
      else {
        newValue = move(i, i - 1, newValue);
      }
    }
    onChange(newValue);
  }
  const moveDown = () => {
    let newValue = value;
    let last = value.length - 1;
    for (const selection of toSelected.reverse()) {
      const i = newValue.indexOf(selection);
      if (i == last) {
        last -= 1;
      }
      else {
        newValue = move(i, i + 1, newValue);
      }
    }
    onChange(newValue);
  }

  return <div className="nxrm-multiselect">
    <div className="nxrm-multiselect__from">
      <label className="nx-sub-label" htmlFor={fromId}>{fromLabel}</label>
      <NxFilterInput value={filter} onChange={onFilterChange}/>
      <Select multiple id={fromId} onChange={changeFromSelection} onDoubleClick={moveRight} value={fromSelected}>
        {fromValues?.map(from => <option key={from} value={from}>{from}</option>)}
      </Select>
    </div>
    <div className="nxrm-multiselect__controls">
      <NxTooltip title={UIStrings.MULTI_SELECT.MOVE_RIGHT}>
        <NxButton variant="icon-only" onClick={moveRight}>
          <NxFontAwesomeIcon icon={faCaretRight}/>
        </NxButton>
      </NxTooltip>
      <NxTooltip title={UIStrings.MULTI_SELECT.MOVE_LEFT}>
        <NxButton variant="icon-only" onClick={moveLeft}>
          <NxFontAwesomeIcon icon={faCaretLeft}/>
        </NxButton>
      </NxTooltip>
    </div>
    <div className="nxrm-multiselect__to">
      <label className="nx-sub-label" htmlFor={toId}>{toLabel}</label>
      <Select multiple id={toId} onChange={changeToSelection} onDoubleClick={moveLeft} value={toSelected}>
        {value?.map(to => <option key={to} value={to}>{to}</option>)}
      </Select>
    </div>
    <div className="nxrm-multiselect__controls">
      <NxTooltip title={UIStrings.MULTI_SELECT.MOVE_UP}>
        <NxButton variant="icon-only" onClick={moveUp}>
          <NxFontAwesomeIcon icon={faCaretUp}/>
        </NxButton>
      </NxTooltip>
      <NxTooltip title={UIStrings.MULTI_SELECT.MOVE_DOWN}>
        <NxButton variant="icon-only" onClick={moveDown}>
          <NxFontAwesomeIcon icon={faCaretDown}/>
        </NxButton>
      </NxTooltip>
    </div>
  </div>;
}
