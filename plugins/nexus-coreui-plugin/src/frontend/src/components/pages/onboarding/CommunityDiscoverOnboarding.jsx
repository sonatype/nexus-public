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
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import {NxTextLink} from '@sonatype/react-shared-components';
import './CommunityOnboarding.scss';

export default function CommunityDiscoverOnboarding() {
  const params = {
    utm_medium: 'product',
    utm_source: 'nexus_repo_community',
    utm_campaign: 'repo_community_usage'
  };

  const discoverLink = `http://links.sonatype.com/products/nxrm3/discover-community-edition?${new URLSearchParams(params).toString()}`;

  return (
    <div className="discover-onboarding-screen">
      <div>
        <p>Sonatype Nexus Repository Community Edition provides powerful tools to support your deployments.
          <br/>
          <NxTextLink target="_blank" className="ce-onboarding-learn-more-link" href={discoverLink}>Learn more about the Community Edition.</NxTextLink>
        </p>
        
        <p><strong>Benefits:</strong> Support for all formats, OCI, PostgreSQL, Kubernetes, and smart usage tracking.</p>
      </div>

      <img className="onboarding-discover-img"
          src={ExtJS.urlOf("static/rapture/resources/images/ce-onboarding.png")}
          alt="CE Discover Onboarding Image"/>
    </div>
  )
}
