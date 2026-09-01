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

import React, { useEffect, useRef } from 'react';
import { Callout } from '@radix-ui/themes';
import { Info } from 'lucide-react';

import { useAnonymousAccessStep } from './useAnonymousAccessStep';
import UIStrings from './UIStrings';

import './AnonymousAccessStep.scss';

export default function AnonymousAccessStep(): JSX.Element {
  const { enabled, onSelect, onFormSubmit } = useAnonymousAccessStep();
  const firstRadioRef = useRef<HTMLInputElement | null>(null);

  // AC #8: focus the first radio on mount so keyboard users can select without
  // an extra Tab. Handled imperatively via ref instead of the `autoFocus`
  // attribute because Biome's a11y rule (correctly) flags autofocus in JSX.
  useEffect(() => {
    firstRadioRef.current?.focus();
  }, []);

  const {
    TITLE,
    SUBTITLE,
    ENABLE_LABEL,
    DISABLE_LABEL,
    ENABLE_OPTION_DESCRIPTION,
    DISABLE_OPTION_DESCRIPTION,
    DESCRIPTION,
    MORE_INFO_LABEL,
    MORE_INFO_HREF,
  } = UIStrings.ONBOARDING_WIZARD.CONFIGURE_ANONYMOUS_ACCESS;

  return (
    <div className="onboarding-wizard__anonymous-access" data-testid="onboarding-wizard__anonymous-access">
      <form onSubmit={onFormSubmit} className="onboarding-wizard__anonymous-access-form">
        <fieldset
          className="onboarding-wizard__anonymous-access-fieldset"
          aria-describedby="onboarding-wizard__anonymous-access-description"
        >
          <legend className="onboarding-wizard__anonymous-access-legend">{TITLE}</legend>
          <p className="onboarding-wizard__anonymous-access-subtitle">{SUBTITLE}</p>
          <Callout.Root
            id="onboarding-wizard__anonymous-access-description"
            className="onboarding-wizard__anonymous-access-description"
            data-testid="onboarding-wizard__anonymous-access-description-callout"
            color="blue"
            variant="surface"
          >
            <Callout.Icon>
              <Info size={16} />
            </Callout.Icon>
            <Callout.Text>
              {DESCRIPTION}{' '}
              <a
                href={MORE_INFO_HREF}
                target="_blank"
                rel="noopener noreferrer"
                data-testid="onboarding-wizard__anonymous-access-more-info-link"
              >
                {MORE_INFO_LABEL}
              </a>
            </Callout.Text>
          </Callout.Root>
          <div className="onboarding-wizard__anonymous-access-options">
            <label
              htmlFor="onboarding-wizard__anonymous-access-enable"
              className="onboarding-wizard__anonymous-access-option"
              data-selected={enabled === true ? 'true' : 'false'}
            >
              <input
                id="onboarding-wizard__anonymous-access-enable"
                type="radio"
                name="onboarding-wizard__anonymous-access"
                checked={enabled === true}
                onChange={() => onSelect(true)}
                ref={firstRadioRef}
                data-testid="onboarding-wizard__anonymous-access-enable"
                className="onboarding-wizard__anonymous-access-option-input"
              />
              <span className="onboarding-wizard__anonymous-access-option-content">
                <span className="onboarding-wizard__anonymous-access-option-title">
                  {ENABLE_LABEL}
                </span>
                <span className="onboarding-wizard__anonymous-access-option-description">
                  {ENABLE_OPTION_DESCRIPTION}
                </span>
              </span>
            </label>
            <label
              htmlFor="onboarding-wizard__anonymous-access-disable"
              className="onboarding-wizard__anonymous-access-option"
              data-selected={enabled === false ? 'true' : 'false'}
            >
              <input
                id="onboarding-wizard__anonymous-access-disable"
                type="radio"
                name="onboarding-wizard__anonymous-access"
                checked={enabled === false}
                onChange={() => onSelect(false)}
                data-testid="onboarding-wizard__anonymous-access-disable"
                className="onboarding-wizard__anonymous-access-option-input"
              />
              <span className="onboarding-wizard__anonymous-access-option-content">
                <span className="onboarding-wizard__anonymous-access-option-title">
                  {DISABLE_LABEL}
                </span>
                <span className="onboarding-wizard__anonymous-access-option-description">
                  {DISABLE_OPTION_DESCRIPTION}
                </span>
              </span>
            </label>
          </div>
        </fieldset>
        <button
          type="submit"
          className="onboarding-wizard__anonymous-access-hidden-submit"
          aria-hidden="true"
          tabIndex={-1}
        />
      </form>
    </div>
  );
}
