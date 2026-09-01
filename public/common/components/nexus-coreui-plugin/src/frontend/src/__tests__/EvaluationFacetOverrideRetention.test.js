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
/**
 * NEXUS-54168 regression test for EvaluationFacet.js.
 *
 * The ExtJS view at public/common/components/nexus-coreui-plugin/src/main/resources/static/
 * rapture/NX/coreui/view/repository/facet/EvaluationFacet.js is loaded by an Ext runtime that
 * Jest does not host, so we lift the two relevant methods (_setFieldValues and
 * _applyGlobalSettingsIfInherit) out of the source text and evaluate them against a minimal
 * form/radiogroup stub. The behavioural guarantee we lock in: when _fetchAndApplyGlobalSettings
 * resolves, it applies global settings to the fields only if the current mode is INHERIT. It
 * must NOT clobber OVERRIDE values that loadRecord placed on the fields moments earlier.
 */
const fs = require('fs');
const path = require('path');

const EVALUATION_FACET_PATH = path.resolve(
  __dirname,
  '../../../main/resources/static/rapture/NX/coreui/view/repository/facet/EvaluationFacet.js'
);

// Assumptions this helper relies on (kept explicit so a future maintainer reformatting
// EvaluationFacet.js notices before the tests fail on them):
//   - Methods are declared as object-literal properties in `methodName: function(args) { ... }`
//     form. A named function expression (`_setFieldValues: function _setFieldValues(...)`) or
//     any other shape would break the regex.
//   - The extracted body is `new Function(...)`-evaluated, which discards the enclosing
//     `Ext.define({...})` scope. Any closure reference introduced later (e.g. `Ext.something`
//     or `me.something` from an outer function) would throw ReferenceError at test time.
function extractMethodBody(source, methodName) {
  // Match "methodName: function(args) { ... },"  where the closing } is at the same indentation
  // depth as the opening; we brace-count instead of regexing greedily to survive nested braces.
  const signature = new RegExp(`${methodName}:\\s*function\\s*\\(([^)]*)\\)\\s*\\{`);
  const match = source.match(signature);
  if (!match) {
    throw new Error(`Could not locate method ${methodName} in EvaluationFacet.js`);
  }
  const params = match[1];
  const bodyStart = match.index + match[0].length;
  let depth = 1;
  for (let i = bodyStart; i < source.length; i++) {
    const c = source[i];
    if (c === '{') depth++;
    else if (c === '}') {
      depth--;
      if (depth === 0) {
        return {params, body: source.slice(bodyStart, i)};
      }
    }
  }
  throw new Error(`Unbalanced braces while extracting ${methodName}`);
}

function buildFacetStub(source) {
  const setField = extractMethodBody(source, '_setFieldValues');
  const applyGuard = extractMethodBody(source, '_applyGlobalSettingsIfInherit');
  // Bind both extracted bodies onto a plain object so `this._setFieldValues(...)` inside
  // _applyGlobalSettingsIfInherit resolves against the same host.
  const stub = {};
  stub._setFieldValues = new Function(...setField.params.split(',').map(s => s.trim()).filter(Boolean), setField.body).bind(stub);
  // Single-arg signature — the helper now resolves the form via radiogroup.up('form').
  stub._applyGlobalSettingsIfInherit = new Function('radiogroup', applyGuard.body).bind(stub);
  return stub;
}

function makeField(initialValue) {
  return {
    _value: initialValue,
    setValue: jest.fn(function(v) { this._value = v; }),
    clearInvalid: jest.fn(),
    getValue() { return this._value; }
  };
}

function makeForm(activityTimeFrame, artifactLatestVersions, policyEvaluationStage) {
  const fields = {
    '#evaluationActivityTimeFrame': makeField(activityTimeFrame),
    '#evaluationArtifactLatestVersions': makeField(artifactLatestVersions),
    '#evaluationPolicyEvaluationStage': makeField(policyEvaluationStage)
  };
  return {
    fields,
    down: (selector) => fields[selector] || null
  };
}

function makeRadiogroup(mode, globalSettings, form) {
  return {
    globalEvaluationSettings: globalSettings,
    getValue: () => (mode == null ? null : {'attributes.evaluation.mode': mode}),
    up: (selector) => (selector === 'form' ? (form || null) : null)
  };
}

describe('EvaluationFacet _applyGlobalSettingsIfInherit (NEXUS-54168)', () => {
  const source = fs.readFileSync(EVALUATION_FACET_PATH, 'utf8');
  const facet = buildFacetStub(source);

  const globalSettings = {
    activityTimeFrame: '30',
    artifactLatestVersions: '1',
    policyEvaluationStage: 'build'
  };

  it('does NOT overwrite OVERRIDE field values with global settings', () => {
    const form = makeForm('60', '2', 'release');
    const radiogroup = makeRadiogroup('OVERRIDE', globalSettings, form);

    facet._applyGlobalSettingsIfInherit(radiogroup);

    expect(form.fields['#evaluationActivityTimeFrame'].setValue).not.toHaveBeenCalled();
    expect(form.fields['#evaluationArtifactLatestVersions'].setValue).not.toHaveBeenCalled();
    expect(form.fields['#evaluationPolicyEvaluationStage'].setValue).not.toHaveBeenCalled();

    // Persisted override values remain untouched
    expect(form.fields['#evaluationActivityTimeFrame'].getValue()).toBe('60');
    expect(form.fields['#evaluationArtifactLatestVersions'].getValue()).toBe('2');
    expect(form.fields['#evaluationPolicyEvaluationStage'].getValue()).toBe('release');
  });

  it('applies global settings to fields when mode is INHERIT', () => {
    const form = makeForm(null, null, null);
    const radiogroup = makeRadiogroup('INHERIT', globalSettings, form);

    facet._applyGlobalSettingsIfInherit(radiogroup);

    expect(form.fields['#evaluationActivityTimeFrame'].setValue).toHaveBeenCalledWith('30');
    expect(form.fields['#evaluationArtifactLatestVersions'].setValue).toHaveBeenCalledWith('1');
    expect(form.fields['#evaluationPolicyEvaluationStage'].setValue).toHaveBeenCalledWith('build');
    // clearInvalid=true was requested by the caller
    expect(form.fields['#evaluationActivityTimeFrame'].clearInvalid).toHaveBeenCalled();
  });

  it('does NOT overwrite fields when mode is DISABLE', () => {
    const form = makeForm(null, null, null);
    const radiogroup = makeRadiogroup('DISABLE', globalSettings, form);

    facet._applyGlobalSettingsIfInherit(radiogroup);

    expect(form.fields['#evaluationActivityTimeFrame'].setValue).not.toHaveBeenCalled();
    expect(form.fields['#evaluationArtifactLatestVersions'].setValue).not.toHaveBeenCalled();
    expect(form.fields['#evaluationPolicyEvaluationStage'].setValue).not.toHaveBeenCalled();
  });

  it('is a no-op when radiogroup is missing or its form lookup returns null (defensive)', () => {
    // Null radiogroup — the helper must bail before touching .up().
    expect(() => facet._applyGlobalSettingsIfInherit(null)).not.toThrow();
    // Radiogroup with no parent form (panel destroyed / not-yet-attached) — also no-op.
    expect(() => facet._applyGlobalSettingsIfInherit(makeRadiogroup('INHERIT', globalSettings, null))).not.toThrow();
  });

  it('reads mode at call-time so a mid-flight radio toggle is honoured (NEXUS-54168 race)', () => {
    // The whole reason _applyGlobalSettingsIfInherit reads mode via radiogroup.getValue() at
    // apply-time (rather than capturing it in a closure when the fetch was fired) is to
    // correctly handle the user toggling the radio while the async global-settings fetch is
    // in flight. Simulate that: build with INHERIT, then swap getValue() to return OVERRIDE
    // before invoking the helper. A future refactor that hoists the mode-read to fetch-start
    // would silently reintroduce the exact race NEXUS-54168 closed — this test fails loudly
    // when that happens.
    const form = makeForm('60', '2', 'release');
    const radiogroup = makeRadiogroup('INHERIT', globalSettings, form);
    radiogroup.getValue = () => ({'attributes.evaluation.mode': 'OVERRIDE'});

    facet._applyGlobalSettingsIfInherit(radiogroup);

    expect(form.fields['#evaluationActivityTimeFrame'].setValue).not.toHaveBeenCalled();
    expect(form.fields['#evaluationActivityTimeFrame'].getValue()).toBe('60');
    expect(form.fields['#evaluationArtifactLatestVersions'].getValue()).toBe('2');
    expect(form.fields['#evaluationPolicyEvaluationStage'].getValue()).toBe('release');
  });
});

describe('EvaluationFacet _fetchAndApplyGlobalSettings source wiring (NEXUS-54168)', () => {
  const source = fs.readFileSync(EVALUATION_FACET_PATH, 'utf8');
  const {body: fetchBody} = extractMethodBody(source, '_fetchAndApplyGlobalSettings');

  it('routes both success and failure callbacks through the INHERIT guard', () => {
    // Guard against a regression where a future edit re-introduces a direct _setFieldValues call
    // inside _fetchAndApplyGlobalSettings' callbacks, bypassing the mode check.
    expect(fetchBody).toContain('_applyGlobalSettingsIfInherit');
    const applyCount = (fetchBody.match(/_applyGlobalSettingsIfInherit/g) || []).length;
    expect(applyCount).toBeGreaterThanOrEqual(2); // once in success, once in failure
    expect(fetchBody).not.toMatch(/_setFieldValues\s*\(/);
  });
});
