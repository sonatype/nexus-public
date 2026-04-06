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
import React, {useState} from 'react';
import {useRouter} from '@uirouter/react';
import {
  ContentBody,
  Page,
  PageHeader,
  Section
} from '@sonatype/nexus-ui-plugin';
import {
  NxStatefulBreadcrumb,
  NxH1
} from '@sonatype/react-shared-components';

import MonitoringSettingsForm from './MonitoringSettingsForm';
import ProgressSteps from './ProgressSteps';
import UIStrings from '../../../../constants/UIStrings';
import {ROUTE_NAMES} from '../../../../routerConfig/routeNames/routeNames';

/**
 * Hosted Repositories Evaluation
 *
 * Main page with SETTINGS and REPOSITORIES tabs
 */
export default function HostedRepositoriesEvaluation() {
  const router = useRouter();
  const [activeTab, setActiveTab] = useState(0);
  const [formData, setFormData] = useState({
    activityTimeFrame: '',
    artifactLatestVersions: '',
    policyEvaluationStage: '',
    applyToNewRepos: false
  });

  const handleNext = (stepData) => {
    setFormData({...formData, ...stepData});
    // TODO: Call backend API when ready (CLM-38702)
    console.log('Form data submitted:', {...formData, ...stepData});
  };

  const handleCancel = () => {
    // TODO (CLM-38702): Navigate back or reset form
    console.log('Form cancelled');
  };

  const STRINGS = UIStrings.SONATYPE_LIFECYCLE.HOSTED_REPOSITORIES_EVALUATION;
  const steps = [STRINGS.tabs.settings, STRINGS.tabs.repositories];

  const breadcrumbs = [
    {
      name: UIStrings.IQ_SERVER.MENU.text,
      href: router.stateService.href(ROUTE_NAMES.ADMIN.IQ.CONNECTED)
    },
    {
      name: UIStrings.SONATYPE_LIFECYCLE.MENU.text,
      href: router.stateService.href(ROUTE_NAMES.ADMIN.IQ.SONATYPE_LIFECYCLE.ROOT)
    },
    {
      name: STRINGS.title,
      href: router.stateService.href(ROUTE_NAMES.ADMIN.IQ.HOSTED_REPOS_EVAL.ROOT)
    }
  ];

  return (
    <Page>
      <PageHeader>
        <div className="hosted-repos-eval-header">
          <NxStatefulBreadcrumb crumbs={breadcrumbs} />
          <NxH1>{STRINGS.title}</NxH1>
        </div>
      </PageHeader>
      <ContentBody className='nxrm-hosted-repos-evaluation'>
        <Section>
          <ProgressSteps
            steps={steps}
            currentStep={activeTab}
            onStepClick={setActiveTab}
          />
          {activeTab === 0 && (
            <MonitoringSettingsForm
              initialData={formData}
              onNext={handleNext}
              onCancel={handleCancel}
            />
          )}
          {activeTab === 1 && (
            // TODO (CLM-38702): Implement repository selection UI
            <p>Repository selection will be implemented in the next milestone (CLM-38702)</p>
          )}
        </Section>
      </ContentBody>
    </Page>
  );
}
