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
import React from 'react';
import {keys, mergeDeepRight} from 'ramda';

import {NxCheckbox} from '@sonatype/react-shared-components';
import FormUtils from '../../../../interface/FormUtils';

import {SUPPORTED_FIELD_TYPES} from '../FormFieldsFactoryConstants';

const Field = ({id, dynamicProps, current, onChange}) => {
  const {attributes: {options}, readOnly} = dynamicProps;
  const {data} = current.context;

  if (readOnly) {
    return keys(data[id]).map(e => e.charAt(0).toUpperCase() + e.slice(1)).join(', ');
  }

  return options.map(option =>
      <NxCheckbox key={option}
                  {...FormUtils.checkboxProps([id, option], current)}
                  onChange={ e => onChange(id, mergeDeepRight(data[id], {[option]: e}))}>
        {option.charAt(0).toUpperCase() + option.slice(1)}
      </NxCheckbox>
  );
};

export default {
  types: SUPPORTED_FIELD_TYPES.SET_OF_CHECKBOXES,
  component: Field,
};
