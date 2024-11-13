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

import {FormUtils, useSimpleMachine} from '@sonatype/nexus-ui-plugin';

import {NxFormGroup, NxFormSelect, NxLoadWrapper} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';

const {EDITOR} = UIStrings.REPOSITORIES;

export const RECIPES_URL = 'service/rest/internal/ui/repositories/recipes';

export default function GenericFormatConfiguration({parentMachine}) {
  const {current, retry, isLoading} = useSimpleMachine({
    id: 'GenericFormatConfigurationMachine',
    url: RECIPES_URL
  });

  const [currentParent, sendParent] = parentMachine;

  const {
    pristineData: {name}
  } = currentParent.context;

  const isEdit = !!name;

  const {data: recipes, error} = current.context;

  const formats = getFormats(recipes);

  const types = getTypes(recipes);

  const {format, type} = currentParent.context.data;

  const handleFormatUpdate = (format) => {
    const repoType = types.get(format)?.includes(type) ? type : '';
    sendParent({type: 'RESET_DATA', format, repoType});
  };

  const handleTypeUpdate = (repoType) => {
    sendParent({type: 'RESET_DATA', format, repoType});
  };

  return (
    <>
      <h2 className="nx-h2">{EDITOR.FORMAT_AND_TYPE_CAPTION}</h2>
      <NxLoadWrapper loading={isLoading} error={error} retryHandler={retry}>
        <div className="nx-form-row">
          <NxFormGroup label={EDITOR.FORMAT_LABEL} isRequired className="nxrm-form-group-format">
            <NxFormSelect
              {...FormUtils.selectProps('format', currentParent)}
              onChange={handleFormatUpdate}
              disabled={isEdit}
            >
              <option value="">{EDITOR.SELECT_FORMAT_OPTION}</option>
              {formats?.map((format) => (
                <option key={format} value={format}>
                  {format}
                </option>
              ))}
            </NxFormSelect>
          </NxFormGroup>

          <NxFormGroup label={EDITOR.TYPE_LABEL} isRequired className="nxrm-form-group-type">
            <NxFormSelect
              {...FormUtils.selectProps('type', currentParent)}
              onChange={handleTypeUpdate}
              disabled={!format || isEdit}
            >
              <option value="">{EDITOR.SELECT_TYPE_OPTION}</option>
              {types?.get(format)?.map((type) => (
                <option key={type} value={type}>
                  {type}
                </option>
              ))}
            </NxFormSelect>
          </NxFormGroup>
        </div>
      </NxLoadWrapper>
    </>
  );
}

const getFormats = (recipes) => [...new Set(recipes?.map((r) => r.format))].sort();

const getTypes = (recipes) =>
  recipes?.reduce((acc, curr) => {
    const {format, type} = curr;
    acc.has(format) ? acc.get(format).push(type) : acc.set(format, [type]);
    return acc;
  }, new Map());
