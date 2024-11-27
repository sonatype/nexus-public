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
import {ValidationUtils} from '@sonatype/nexus-ui-plugin';

export default {
  init: () => ({
    bucketConfiguration: {
      bucketSecurity: {
        authenticationMethod: 'applicationDefault'
      },
      encryption: {
        encryptionType: 'default'
      }
    }
  }),

  validation: (data, pristineData) => {
    const validationErrors = {
      bucketConfiguration: {
        bucket: {
          name: ValidationUtils.validateNotBlank(data.bucketConfiguration?.bucket?.name)
        }
      }
    };

    const isEdit = Boolean(pristineData?.name);

    if (data.bucketConfiguration?.bucketSecurity?.authenticationMethod === 'accountKey' && !isEdit) {
      validationErrors.bucketConfiguration.bucketSecurity = {
        file: ValidationUtils.validateNotBlank(data.bucketConfiguration?.bucketSecurity.file)
      }
    }

    if (data.bucketConfiguration?.encryption?.encryptionType === 'kmsManagedEncryption') {
      validationErrors.bucketConfiguration.encryption = {
        encryptionKey: ValidationUtils.validateNotBlank(data.bucketConfiguration?.encryption.encryptionKey)
      }
    }

    return validationErrors;
  }
};
