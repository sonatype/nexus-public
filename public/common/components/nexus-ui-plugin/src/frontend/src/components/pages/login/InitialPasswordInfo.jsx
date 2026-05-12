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
import { Callout, Text } from '@radix-ui/themes';
import { Info } from 'lucide-react';
import LoginPageStrings from '../../../constants/LoginPageStrings';

const { INITIAL_PASSWORD_MESSAGE } = LoginPageStrings;

/**
 * Displays initial admin password file path information using a Radix Callout.
 * Shown during first-time setup when admin password needs to be retrieved from file.
 */
export default function InitialPasswordInfo({ passwordFilePath }) {
  return (
    <Callout.Root color="blue" size="2">
      <Callout.Icon>
        <Info size={16} />
      </Callout.Icon>
      <Callout.Text asChild>
        <span>
          <Text size="2">{INITIAL_PASSWORD_MESSAGE}</Text>
          <br />
          <Text as="span" weight="medium" size="2" style={{ fontFamily: 'monospace', overflowWrap: 'break-word' }}>
            {passwordFilePath}
          </Text>
        </span>
      </Callout.Text>
    </Callout.Root>
  );
}
