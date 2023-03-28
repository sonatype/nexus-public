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
import {useActor} from '@xstate/react';
import {
  NxInfoAlert,
  NxTile,
  NxH2,
  NxReadOnly,
} from '@sonatype/react-shared-components';
import {ReadOnlyField, FormUtils} from '@sonatype/nexus-ui-plugin';
import {isDynamicGroup, isStaticGroup} from './LdapServersHelper';
import UIStrings from '../../../../constants/UIStrings';
import {findGroupType} from './LdapServersHelper';

const {
  LDAP_SERVERS: {FORM: LABELS},
  SETTINGS,
} = UIStrings;

export default function LdapServersReadOnly({actor}) {
  const [state] = useActor(actor);
  const {data} = state.context;

  const userSubtree = FormUtils.readOnlyCheckboxValueLabel(data.userSubtree);
  const mapLdap = FormUtils.readOnlyCheckboxValueLabel(data.ldapGroupsAsRoles);
  const groupSubtree = FormUtils.readOnlyCheckboxValueLabel(data.groupSubtree);
  const groupType = findGroupType(data.groupType);

  return (
    <>
      <NxInfoAlert>{SETTINGS.READ_ONLY.WARNING}</NxInfoAlert>
      <NxTile>
        <NxTile.Header>
          <NxTile.HeaderTitle>
            <NxH2>{LABELS.CONFIGURATION}</NxH2>
          </NxTile.HeaderTitle>
        </NxTile.Header>
        <NxTile.Content>
          <NxReadOnly>
            <ReadOnlyField
              label={LABELS.USER_RELATIVE_DN.LABEL}
              value={data.userBaseDn}
            />
            <ReadOnlyField
              label={LABELS.USER_SUBTREE.LABEL}
              value={userSubtree}
            />
            <ReadOnlyField
              label={LABELS.OBJECT_CLASS.LABEL}
              value={data.userObjectClass}
            />
            <ReadOnlyField
              label={LABELS.USER_FILTER.LABEL}
              value={data.userLdapFilter}
            />
            <ReadOnlyField
              label={LABELS.USER_ID_ATTRIBUTE}
              value={data.userIdAttribute}
            />
            <ReadOnlyField
              label={LABELS.REAL_NAME_ATTRIBUTE}
              value={data.userRealNameAttribute}
            />
            <ReadOnlyField
              label={LABELS.EMAIL_ATTRIBUTE}
              value={data.userEmailAddressAttribute}
            />
            <ReadOnlyField
              label={LABELS.PASSWORD_ATTRIBUTE.LABEL}
              value={data.userPasswordAttribute}
            />
            <ReadOnlyField label={LABELS.MAP_LDAP.LABEL} value={mapLdap} />
            <ReadOnlyField
              label={LABELS.GROUP_TYPE.LABEL}
              value={groupType?.label}
            />
            {isDynamicGroup(data.groupType) && (
              <ReadOnlyField
                label={LABELS.GROUP_MEMBER_OF_ATTRIBUTE.LABEL}
                value={data.userMemberOfAttribute}
              />
            )}
            {isStaticGroup(data.groupType) && (
              <>
                <ReadOnlyField
                  label={LABELS.GROUP_DN.LABEL}
                  value={data.groupBaseDn}
                />
                <ReadOnlyField
                  label={LABELS.GROUP_SUBTREE.LABEL}
                  value={groupSubtree}
                />
                <ReadOnlyField
                  label={LABELS.GROUP_OBJECT_CLASS.LABEL}
                  value={data.groupObjectClass}
                />
                <ReadOnlyField
                  label={LABELS.GROUP_ID_ATTRIBUTE.LABEL}
                  value={data.groupIdAttribute}
                />
                <ReadOnlyField
                  label={LABELS.GROUP_MEMBER_ATTRIBUTE.LABEL}
                  value={data.groupMemberAttribute}
                />
                <ReadOnlyField
                  label={LABELS.GROUP_MEMBER_FORMAT.LABEL}
                  value={data.groupMemberFormat}
                />
              </>
            )}
          </NxReadOnly>
        </NxTile.Content>
      </NxTile>
    </>
  );
}
