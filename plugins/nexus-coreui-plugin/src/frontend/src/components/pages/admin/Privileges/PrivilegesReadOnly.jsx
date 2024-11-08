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
import React, {Fragment} from 'react';
import {useActor} from '@xstate/react';

import {
  NxButton,
  NxH2,
  NxReadOnly,
  NxLoadWrapper,
  NxInfoAlert,
  NxFooter,
  NxButtonBar,
  NxTile,
} from '@sonatype/react-shared-components';
import {FormFieldsFactory} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

const {PRIVILEGES: {FORM: LABELS}} = UIStrings;

export default function PrivilegesReadOnly({service, onDone}) {
  const [current, send] = useActor(service);

  const {
    data,
    types = {},
    loadError
  } = current.context;

  const {type, name, description, readOnly:isDefaultPrivilege = true} = data;
  const isLoading = current.matches('loading');
  const fields = types[type]?.formFields || [];

  const cancel = () => onDone();

  const retry = () => send({type: 'RETRY'});

  return <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={retry}>
    <NxTile.Content>
      <NxInfoAlert>
        {isDefaultPrivilege ? LABELS.DEFAULT_PRIVILEGE_WARNING : UIStrings.SETTINGS.READ_ONLY.WARNING}
      </NxInfoAlert>
      <NxH2>{LABELS.SECTIONS.SETUP}</NxH2>
      <NxReadOnly>
        <NxReadOnly.Label>{LABELS.TYPE.LABEL}</NxReadOnly.Label>
        <NxReadOnly.Data>{types[type]?.name}</NxReadOnly.Data>
        <NxReadOnly.Label>{LABELS.NAME.LABEL}</NxReadOnly.Label>
        <NxReadOnly.Data>{name}</NxReadOnly.Data>
        {description && <>
          <NxReadOnly.Label>{LABELS.DESCRIPTION.LABEL}</NxReadOnly.Label>
          <NxReadOnly.Data>{description}</NxReadOnly.Data>
        </>}
        {FormFieldsFactory.getFields(fields)?.map(({Field, props}) => (
            <Fragment key={props.id}>
              <NxReadOnly.Label>{props.label}</NxReadOnly.Label>
              <NxReadOnly.Data>
                <Field
                    id={props.id}
                    dynamicProps={{...props, readOnly: true}}
                    current={current}
                />
              </NxReadOnly.Data>
            </Fragment>
        ))}
      </NxReadOnly>
    </NxTile.Content>
    <NxFooter>
      <NxButtonBar>
        <NxButton type="button" onClick={cancel}>{UIStrings.SETTINGS.CANCEL_BUTTON_LABEL}</NxButton>
      </NxButtonBar>
    </NxFooter>
  </NxLoadWrapper>;
}
