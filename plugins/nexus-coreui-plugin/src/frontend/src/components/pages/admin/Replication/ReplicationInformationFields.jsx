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
import {useService} from '@xstate/react';

import {FormUtils} from '@sonatype/nexus-ui-plugin';

import {
  NxCode,
  NxFormGroup,
  NxFormSelect,
  NxCheckbox,
  NxFieldset,
  NxH4,
  NxP,
  NxRadio,
  NxTextInput,
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';

import './ReplicationForm.scss';

const FORM = UIStrings.REPLICATION.FORM;

function ContentRegexField(props) {
  return <>
    <NxH4>Example Regular Expression Patterns</NxH4>
    <NxP>Select all "groupA" format content: <NxCode>/my/company/groupA/.*</NxCode></NxP>
    <NxP>Select an npm project scope: <NxCode>.*@myscope/.*</NxCode></NxP>
    <NxTextInput {...props}/>
  </>;
}

export default function ReplicationInformationFields({isEdit, service}) {
  const [current, send] = useService(service);

  const {
    data,
    pristineData,
    sourceRepositories
  } = current.context;

  return (
      <div className="nxrm-replication-form--replication-information">
        <NxFormGroup label={FORM.NAME_LABEL} sublabel={FORM.NAME_DESCRIPTION} isRequired>
          <NxTextInput
              {...FormUtils.fieldProps('name', current)}
              disabled={pristineData.name}
              onChange={FormUtils.handleUpdate('name', send)}/>
        </NxFormGroup>

        <NxFormGroup label={FORM.SOURCE_REPO_LABEL} sublabel={FORM.SOURCE_REPO_DESCRIPTION} isRequired>
          <NxFormSelect {...FormUtils.fieldProps('sourceRepositoryName', current)}
                        onChange={FormUtils.handleUpdate('sourceRepositoryName', send, 'UPDATE_TEST_CONNECTION')}
                        validatable>
            <option value=""/>
            {sourceRepositories.map(({id, name}) =>
                <option key={'sourceRepositoryName' + id} value={id}>{name}</option>
            )}
          </NxFormSelect>
        </NxFormGroup>

        <NxFieldset label={FORM.SOURCE_CONTENT_OPTIONS} isRequired>
          <NxRadio name="replicatedContent"
                   value="all"
                   isChecked={data.replicatedContent === 'all'}
                   onChange={FormUtils.handleUpdate('replicatedContent', send)}>
            {FORM.SOURCE_CONTENT_OPTION_ALL}
          </NxRadio>
          <NxRadio name="replicatedContent"
                   value="regex"
                   isChecked={data.replicatedContent === 'regex'}
                   onChange={FormUtils.handleUpdate('replicatedContent', send)}>
            {FORM.SOURCE_CONTENT_OPTION_REGEX}
          </NxRadio>
          <NxCheckbox
              disabled={isEdit}
              {...FormUtils.checkboxProps('includeExistingContent', current)}
              onChange={FormUtils.handleUpdate('includeExistingContent', send)}
          >
            {FORM.INCLUDE_EXISTING_CONTENT}
          </NxCheckbox>
        </NxFieldset>

        {data.replicatedContent === 'regex' &&
            <NxFormGroup label={FORM.CONTENT_FILTER} sublabel={FORM.CONTENT_FILTER_DESCRIPTION} isRequired>
              <ContentRegexField {...FormUtils.fieldProps('contentRegex', current)}
                                 onChange={FormUtils.handleUpdate('contentRegex', send)}/>
            </NxFormGroup>
        }
      </div>
  );
}
