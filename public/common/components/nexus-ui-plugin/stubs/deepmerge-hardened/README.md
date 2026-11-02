# deepmerge-hardened

A hardened drop-in for `deepmerge@4.3.1` that `swagger-client` and `jest-config` are redirected
to, replacing the vulnerable upstream code rather than hiding it (NEXUS-55250).

## Why it exists

`deepmerge@4.3.1` carries CVE-2026-93753 (CVSS 8.7): `mergeObject()` copies an own `__proto__`
key into the result, which replaces the result's prototype. It does this both from a target
that has one (e.g. produced by `JSON.parse`) and from a source when the target has a null
prototype. There is no upgrade path:

- 4.3.1 is the latest deepmerge release, and the upstream fix PR was closed unmerged.
- `swagger-client` (a dependency of `swagger-ui-react`) still declares `deepmerge: ~4.3.0`.
- Unlike `prismjs` (see `../empty-stub/README.md`), deepmerge **is reachable**: swagger-client
  calls it for `mergeDeep` patches while resolving Swagger 2.0 / OpenAPI 3.0 `allOf` schemas
  (`swagger-client/{es,lib}/resolver/specmap/lib/index.js`). It cannot be replaced by an empty
  stub.

## How it works

The root `package.json` redirects deepmerge with **scoped** Yarn `resolutions` using the
`portal:` protocol (see `../empty-stub/README.md` for why scoped keys are preferred):

| Resolution key | Why |
|----------------|-----|
| `swagger-client/deepmerge` | The only production dependent; code is bundled into the coreui and cloudui plugins. |
| `jest-config/deepmerge` | Dev-only (test runner), but `yarn.lock` has no dev/prod split and is scanned directly, so leaving the npm 4.3.1 entry there would keep the component in the scan. |

With both keys, `yarn.lock` contains no `deepmerge@npm:` resolution. Any *new* dependent of
deepmerge resolves the real package from npm and shows up in the next IQ scan, which is the
outcome we want.

`index.cjs` is a line-for-line port of upstream `dist/cjs.js` with one change: a `__proto__`
key is never copied from the target or the source into the result. `constructor` and
`prototype` are left alone on purpose: writing them to a fresh `{}` only creates an own
property, and OpenAPI schemas may legitimately use them as property names.

The file is deliberately `.cjs`, not `.js`. The portal resolves to a real path outside
`node_modules`, so a `.js` file matches the coreui and cloudui `swc-loader` rule
(`/\.[jt]sx?$/`, excluding `node_modules`). swc then injects ESM `@swc/helpers` imports,
rspack treats the file as an ES module, and `module.exports` throws at runtime ("ES Modules may
not assign module.exports"), leaving the API page blank. `.cjs` keeps it out of that rule, so it
is bundled untouched, like the real package.

## Tests

`index.test.js` is a plain Node assertion script, run by `yarn test-stubs` in `nexus-ui-plugin`
(and so by the Maven build via `test-no-lint`). It covers the security cases and upstream
parity. To confirm the security cases fail against upstream, point it at the real package:

```bash
DEEPMERGE_IMPL=/path/to/deepmerge/dist/cjs.js \
  node public/common/components/nexus-ui-plugin/stubs/deepmerge-hardened/index.test.js
```

## Rules

**Keep it dependency-free.** `portal:` preserves the target's dependencies, so anything added
to `package.json` is injected into every dependent listed above.

**Keep it in sync with upstream.** Behaviour must match deepmerge 4.3.1 apart from the
`__proto__` guard. Do not add features.

**Remove it** when upstream publishes a fixed release that the dependents accept, or when
`swagger-client` stops depending on deepmerge: delete the resolution keys, this directory, and
the `test-stubs` entry, then re-check the advisories of whatever version resolves.

## Note on the `name` field

The `name` is `@sonatype/deepmerge-hardened`, in a scope we control and unpublished on npm,
so license scanners and SBOM tooling that introspect the directory do not mistake it for the
real package. The upstream MIT license is kept in `LICENSE`.

## Licensing

`index.cjs` is derived from upstream deepmerge and stays under its MIT license, so it carries
the upstream copyright and permission notice instead of the Nexus OSS (EPL) header. `LICENSE`
and `package.json` (`"license": "MIT"`) say the same. `index.test.js` and this README are
Sonatype code and follow the usual repository rules.

The license sets of `license-maven-plugin` in the root `pom.xml` only cover `*.js`, `*.java`,
`*.xml` and `*.groovy`, so `index.cjs` is outside them and needs no exclusion. If it is ever
renamed to `.js`, add an explicit exclusion for it rather than letting `license:format` add the
Nexus header.
