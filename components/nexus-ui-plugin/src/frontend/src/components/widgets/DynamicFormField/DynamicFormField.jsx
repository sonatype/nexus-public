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
import {mapObjIndexed, values} from 'ramda';
import Textfield from "../Textfield/Textfield";
import MultiSelect from '../MultiSelect/MultiSelect';
import Select from '../Select/Select';
import Utils from '../../../interface/Utils';

export default function DynamicFormField({current, dynamicProps, id, initialValue, onChange}) {
  if (dynamicProps.type === 'string') {
    const fieldProps = Utils.fieldProps(id, current, initialValue || '');
    const className = dynamicProps.attributes.long ? 'nx-text-input--long' : '';

    return <Textfield {...fieldProps}
                      className={className}
                      disabled={dynamicProps.disabled}
                      readOnly={dynamicProps.readOnly}
                      onChange={(event) => onChange(fieldProps.name, event.currentTarget.value)}
    />
  }
  else if (dynamicProps.type === 'itemselect') {
    const fieldProps = Utils.fieldProps(id, current, initialValue || [], (value) => value)
    return <MultiSelect
        {...fieldProps}
        filterValue={dynamicProps.filterValue}
        toLabel={dynamicProps.attributes.toTitle}
        fromLabel={dynamicProps.attributes.fromTitle}
        fromOptions={dynamicProps.attributes.options}
        onChange={(value) => onChange(fieldProps.name, value)}
    />;
  }
  else if (dynamicProps.type === 'combobox') {
    const fieldProps = Utils.fieldProps(id, current, initialValue || '');
    return <Select {...fieldProps} onChange={(event) => onChange(fieldProps.name, event.currentTarget.value)}>
      <option/>
      {values(mapObjIndexed((v, k) => <option key={k} value={k}>{v}</option>, dynamicProps.attributes.options))}
    </Select>;
  }
  else {
    console.warn(`form field type=${dynamicProps.type} is unknown`);
    return <div/>;
  }
}
