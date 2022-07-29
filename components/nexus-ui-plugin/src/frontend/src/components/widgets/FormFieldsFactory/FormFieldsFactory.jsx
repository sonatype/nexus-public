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
import ValidationUtils from '../../../interface/ValidationUtils';

import TextFieldFactory from './factory/TextFieldFactory';
import ComboboxFieldFactory from './factory/ComboboxFieldFactory';

import {SUPPORTED_FIELD_TYPES} from './FormFieldsFactoryConstants';

export default class {
  static ALL_FIELDS = [
      TextFieldFactory,
      ComboboxFieldFactory,
  ];

  static getFields(fields) {
    return fields.map(props => {
      const field = this.ALL_FIELDS.find(field => field.types.includes(props.type));
      return {
        Field: field.component,
        props,
      };
    });
  }

  static defaultValues(type, types) {
    const fields = types[type]?.formFields;
    const result = {};
    let value = null;
    fields?.forEach(({id, type, initialValue}) => {
      if (initialValue) {
        value = initialValue;
      } else if (
          SUPPORTED_FIELD_TYPES.TEXT.includes(type) ||
          SUPPORTED_FIELD_TYPES.COMBOBOX.includes(type)
      ) {
        value = '';
      }
      result[id] = value;
    });

    return result;
  }

  static getValidations(data, types) {
    const fields = types[data.type]?.formFields;
    const errors = {};

    fields?.forEach(({id, required}) => {
      if (required) {
        errors[id] = ValidationUtils.validateNotBlank(data[id]);
      }
    });
    return errors;
  }
}
