/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import React from 'react';

import {
  NxH2,
  NxCheckbox,
  NxFieldset,
  NxFormGroup,
  NxFormSelect,
  NxWarningAlert
} from '@sonatype/react-shared-components';
import {FormUtils, ExtJS} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../../constants/UIStrings';

import ToggleableTextInput from './ToggleableTextInput/ToggleableTextInput';

const {CONNECTORS, COSIGN} = UIStrings.REPOSITORIES.EDITOR.OCI;

/**
 * OciConnectorSettings - connector configuration for OCI v2 registries.
 *
 * Unlike Docker this facet exposes ONLY the v2 connector surface: HTTP port,
 * HTTPS port, optional subdomain, path-based routing toggle, and force-basic-auth.
 * No Docker v1 toggle is emitted; OCI repositories always speak the v2 distribution spec.
 *
 * Cosign keyless policy lives below the connector fields. The KEYLESS enforcement
 * option is intentionally hidden until a real Cosign verifier ships - the stub
 * verifier fails OPEN at both upload and pull time, so exposing the option would
 * give admins a false sense of supply-chain enforcement (UX P0-1 / NEXUS-53064).
 */
export default function OciConnectorSettings({parentMachine}) {
  const [parentState, sendParent] = parentMachine;

  const repositoryName = parentState.context.data.name;
  const isProEdition = ExtJS.isProEdition();

  return (
    <>
      <NxH2 className="nxrm-oci-connectors-caption">{CONNECTORS.CAPTION}</NxH2>

      <p className="nxrm-oci-connectors-help">{CONNECTORS.HELP}</p>

      <NxFieldset
        label={CONNECTORS.PATH_ENABLED.LABEL}
        className="nxrm-form-group-oci-connector-path-enabled"
      >
        <NxCheckbox
          {...FormUtils.checkboxProps('oci.pathEnabled', parentState)}
          onChange={FormUtils.handleUpdate('oci.pathEnabled', sendParent)}
        >
          {CONNECTORS.PATH_ENABLED.DESCR}
        </NxCheckbox>
      </NxFieldset>

      {isProEdition && (
        <ToggleableTextInput
          parentMachine={parentMachine}
          contextPropName="oci.subdomain"
          label={CONNECTORS.SUBDOMAIN.LABEL}
          sublabel={CONNECTORS.SUBDOMAIN.SUBLABEL}
          defaultValue={repositoryName}
          placeholder={CONNECTORS.SUBDOMAIN.PLACEHOLDER}
          clearIfDisabled
          className="nxrm-form-group-oci-connector-subdomain"
        />
      )}

      <ToggleableTextInput
        parentMachine={parentMachine}
        contextPropName="oci.httpPort"
        label={CONNECTORS.HTTP.LABEL}
        sublabel={CONNECTORS.HTTP.SUBLABEL}
        placeholder={CONNECTORS.HTTP.PLACEHOLDER}
        clearIfDisabled
        className="nxrm-form-group-oci-connector-http-port"
      />
      <ToggleableTextInput
        parentMachine={parentMachine}
        contextPropName="oci.httpsPort"
        label={CONNECTORS.HTTPS.LABEL}
        sublabel={CONNECTORS.HTTPS.SUBLABEL}
        placeholder={CONNECTORS.HTTPS.PLACEHOLDER}
        clearIfDisabled
        className="nxrm-form-group-oci-connector-https-port"
      />

      <NxFieldset
        label={CONNECTORS.FORCE_BASIC_AUTH.LABEL}
        className="nxrm-form-group-oci-force-basic-auth"
      >
        <NxCheckbox
          {...FormUtils.checkboxProps('oci.forceBasicAuth', parentState)}
          onChange={FormUtils.handleUpdate('oci.forceBasicAuth', sendParent)}
        >
          {CONNECTORS.FORCE_BASIC_AUTH.DESCR}
        </NxCheckbox>
        {/* NEXUS-53064 B2: surface the security implication of disabling
            Force Basic Authentication as a warning banner so the admin cannot
            silently leave the registry open to anonymous pulls. */}
        <NxWarningAlert className="nxrm-oci-force-basic-auth-warning">
          {CONNECTORS.FORCE_BASIC_AUTH.WARNING}
        </NxWarningAlert>
      </NxFieldset>

      <NxH2 className="nxrm-oci-cosign-caption">{COSIGN.CAPTION}</NxH2>
      <p className="nxrm-oci-cosign-help">{COSIGN.HELP}</p>

      <NxFormGroup
        label={COSIGN.ENFORCEMENT.LABEL}
        sublabel={COSIGN.ENFORCEMENT.SUBLABEL}
        className="nxrm-form-group-oci-cosign-enforcement"
      >
        <NxFormSelect
          {...FormUtils.selectProps('oci.cosign.enforcement', parentState)}
          onChange={FormUtils.handleUpdate('oci.cosign.enforcement', sendParent)}
        >
          {/*
            UX P0-1 / NEXUS-53064: KEYLESS is intentionally NOT exposed in the
            dropdown until a real Cosign verifier ships. The current
            OciCosignKeylessVerifierNoop fails open at both upload AND pull
            time, so showing this option would let admins believe they had
            configured signature enforcement when in reality every signature
            is accepted. Re-enable when a non-stub verifier is wired in.
          */}
          <option value="NONE">{COSIGN.ENFORCEMENT.OPTIONS.NONE}</option>
        </NxFormSelect>
      </NxFormGroup>
    </>
  );
}
