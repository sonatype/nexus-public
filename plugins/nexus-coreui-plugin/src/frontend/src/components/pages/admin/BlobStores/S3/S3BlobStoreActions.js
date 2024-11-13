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
      bucket: {
        region: 'DEFAULT',
        expiration: '3',
        prefix: ''
      },
      bucketSecurity: null,
      encryption: null,
      advancedBucketConnection: {
        endpoint: '',
        forcePathStyle: false
      },
      failoverBuckets: [],
      activeRegion: null,
    }
  }),

  validation: (data) => {
    const {bucket, bucketSecurity, advancedBucketConnection, failoverBuckets} = data.bucketConfiguration;
    const validationErrors = {
      bucketConfiguration: {
        bucket: {
          name: ValidationUtils.validateNotBlank(bucket?.name),
          expiration:
              ValidationUtils.validateNotBlank(bucket?.expiration) ||
              ValidationUtils.isInRange({
                value: bucket?.expiration,
                min: -1,
                allowDecimals: false
              })
        },
        bucketSecurity: {},
        advancedBucketConnection: {
          endpoint: ValidationUtils.notBlank(advancedBucketConnection?.endpoint) ?
              ValidationUtils.validateIsUri(advancedBucketConnection?.endpoint) :
              null,
          maxConnectionPoolSize: ValidationUtils.notBlank(advancedBucketConnection?.maxConnectionPoolSize) ?
              ValidationUtils.isInRange({
                value: advancedBucketConnection.maxConnectionPoolSize,
                min: 1,
                max: 1000000000
              }) :
              null
        },
        failoverBuckets: []
      }
    };

    if (ValidationUtils.notBlank(bucketSecurity?.secretAccessKey) ||
        ValidationUtils.notBlank(bucketSecurity?.role) ||
        ValidationUtils.notBlank(bucketSecurity?.sessionToken)) {
      validationErrors.bucketConfiguration.bucketSecurity.accessKeyId =
          ValidationUtils.validateNotBlank(bucketSecurity?.accessKeyId);
    }

    if (ValidationUtils.notBlank(bucketSecurity?.accessKeyId) ||
        ValidationUtils.notBlank(bucketSecurity?.role) ||
        ValidationUtils.notBlank(bucketSecurity?.sessionToken)) {
      validationErrors.bucketConfiguration.bucketSecurity.secretAccessKey =
          ValidationUtils.validateNotBlank(bucketSecurity?.secretAccessKey);
    }

    validationErrors.bucketConfiguration.failoverBuckets = failoverBuckets?.map(failoverBucket => {
      return {
        region: ValidationUtils.validateNotBlank(failoverBucket.region) ||
            (failoverBucket.region === bucket.region ? 'Replication region is the same as primary region' : null) ||
            (failoverBucket.region === 'DEFAULT' ? 'Replication region cannot be DEFAULT' : null),
        bucketName: ValidationUtils.validateNotBlank(failoverBucket.bucketName)
      };
    });

    return validationErrors;
  }
};
