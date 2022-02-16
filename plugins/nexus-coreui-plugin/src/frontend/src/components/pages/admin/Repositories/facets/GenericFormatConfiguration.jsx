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

import {Select, FormUtils, useSimpleMachine} from '@sonatype/nexus-ui-plugin';

import {NxFormGroup, NxLoadWrapper} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';

const {EDITOR} = UIStrings.REPOSITORIES;

export const RECIPES_URL = '/service/rest/internal/ui/repositories/recipes';

export default function GenericFormatConfiguration({parentMachine}) {
  const {current, retry, isLoading} = useSimpleMachine(
    'GenericFormatConfigurationMachine',
    RECIPES_URL,
    true
  );

  const [currentParent, sendParent] = parentMachine;

  const {data: recipes, error} = current.context;

  const formats = getFormats(recipes);

  const types = getTypes(recipes);

  const {format, type} = currentParent.context.data;

  const handleFormatUpdate = (event) => {
    const format = event.target.value;
    const data = {format, memberNames: []};
    if (type) {
      data.type = types.get(format)?.includes(type) ? type : '';
    }
    sendParent({type: 'UPDATE', data});
  };

  return (
    <>
      <h2 className="nx-h2">{EDITOR.FORMAT_AND_TYPE_CAPTION}</h2>
      <NxLoadWrapper loading={isLoading} error={error} retryHandler={retry}>
        <div className="nx-form-row">
          <NxFormGroup
            label={EDITOR.FORMAT_LABEL}
            isRequired
            className="nxrm-form-group-format"
          >
            <Select
              {...FormUtils.fieldProps('format', currentParent)}
              name="format"
              onChange={handleFormatUpdate}
            >
              <option value="">{EDITOR.SELECT_FORMAT_OPTION}</option>
              {formats?.map((format) => (
                <option key={format} value={format}>
                  {format}
                </option>
              ))}
            </Select>
          </NxFormGroup>

          <NxFormGroup
            label={EDITOR.TYPE_LABEL}
            isRequired
            className="nxrm-form-group-type"
          >
            <Select
              {...FormUtils.fieldProps('type', currentParent)}
              name="type"
              onChange={FormUtils.handleUpdate('type', sendParent)}
              disabled={!format}
            >
              <option value="">{EDITOR.SELECT_TYPE_OPTION}</option>
              {types?.get(format)?.map((type) => (
                <option key={type} value={type}>
                  {type}
                </option>
              ))}
            </Select>
          </NxFormGroup>
        </div>
      </NxLoadWrapper>
    </>
  );
}

const getFormats = (recipes) =>
  [...new Set(recipes?.map((r) => r.format))].sort();

const getTypes = (recipes) =>
  recipes?.reduce((acc, curr) => {
    const {format, type} = curr;
    acc.has(format) ? acc.get(format).push(type) : acc.set(format, [type]);
    return acc;
  }, new Map());
