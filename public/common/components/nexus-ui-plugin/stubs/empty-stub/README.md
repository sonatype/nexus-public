# empty-stub

A single empty package that unused transitive dependencies are redirected to, so they leave
the dependency graph entirely instead of being installed, bundled, or scanned.

## How it works

The root `package.json` uses Yarn `resolutions` with the `portal:` protocol to point a
package name at this directory. Yarn resolves by the **resolution key**, not by the `name`
field in this directory's `package.json`, so one stub can serve any number of packages.
`require('<any stubbed package>')` resolves here and gets an empty object.

Prefer a **scoped** resolution key — `"<dependent>/<package>"`, e.g.
`"react-syntax-highlighter/prismjs"` — over a bare `"<package>"` key for anything that is
browser-runtime code. A bare key overrides the package for every current *and future*
dependent in the graph, so a later dependency bump that legitimately needs the real package
gets the stub instead, silently. A scoped key constrains the override to the one dependent
whose reachability was actually verified; any new dependent resolves the real package from
npm and shows up in the next IQ scan, which is the outcome we want. Bare keys are reserved
for packages that are never browser-runtime code at all — the native `tree-sitter` bindings,
where the stub exists to avoid a `node-gyp` build rather than to prune reachable code.

The lockfile records these as `portal:` resolutions with `linkType: soft`. The dependents
still *declare* their original ranges (e.g. `prismjs: "npm:^1.30.0"`), but no `npm:`
resolution is created, nothing is downloaded, and dependency scanners no longer see the
component.

## Packages stubbed

| Package | Resolution key | Reachable? | Why it is stubbed |
|---------|----------------|-----------|-------------------|
| `prismjs` | `react-syntax-highlighter/prismjs` (scoped) | No | Reaches the graph via `swagger-ui-react` → `react-syntax-highlighter`, which imports only the highlight.js build (`dist/esm/light` + `languages/hljs/*` + `styles/hljs/*`). `react-syntax-highlighter` is the only dependent declaring a runtime `prismjs`; `refractor@5` dropped its own runtime dependency and keeps only `@types/prismjs`, which is a devDependency-grade types package that contributes no runtime code and is not stubbed. Carries `sonatype-2020-1579` (CVSS 7.5) with no upgrade path — 1.30.0 is the newest legitimate release. (NEXUS-54366) |
| `tree-sitter` | `tree-sitter` (bare) | No | Native Node.js bindings, require `node-gyp`. Pulled in by `swagger-ui-react` via `@swagger-api/apidom-parser-adapter-json`. Not needed for browser rendering. |
| `tree-sitter-json` | `tree-sitter-json` (bare) | No | Native JSON grammar, requires `node-gyp`. Same origin as above. |
| `web-tree-sitter` | `web-tree-sitter` (bare) | No | WebAssembly implementation. Not native, but unused. |
| `@tree-sitter-grammars/tree-sitter-yaml` | `@tree-sitter-grammars/tree-sitter-yaml` (bare) | No | Native YAML grammar, requires `node-gyp`. Pulled in via `@swagger-api/apidom-parser-adapter-yaml-1-2`. |

A package that *is* reachable cannot be stubbed here. `deepmerge` is redirected to a hardened
drop-in instead; see `../deepmerge-hardened/README.md`.

## Rules for this stub

**Keep it dependency-free.** Unlike `link:`, the `portal:` protocol *preserves* the target's
dependencies. Any dependency added to this `package.json` is injected into every consumer
listed above at once.

**Only stub what is genuinely unreachable.** Verify before adding a row: no source imports
it, and it does not appear as executable code in the built bundles under
`*/target/classes/static/`. A stub silences a real dependency as effectively as an unused
one.

**Know which of the two failure modes a real import would hit.** They are not the same, and
only one of them is loud:

- A **deep import** — `prismjs/components/prism-core`, `prismjs/themes/prism.css` — fails at
  build time. This directory exposes only `main`, so no such subpath exists and module
  resolution errors out. This is the good case: the build stops.
- A **bare import** — `require('prismjs')`, `import Prism from 'prismjs'` — resolves to
  `index.js` and yields `{}`. Nothing fails at build time; the feature breaks at runtime,
  in whatever way an empty object breaks it. This is the case the stub table cannot protect
  against on its own.

Because the second mode is silent, reachability is enforced structurally rather than by this
instruction alone: `../rspack.forbid-stubbed-modules.js` is applied by both
`nexus-coreui-plugin` and `nexus-cloudui-plugin` rspack builds and fails the build when any
module in the graph resolves into this directory, naming the importer. Grepping the emitted
bundles for a stubbed package name cannot do this job — the stub is empty, so a minified
bundle retains no trace of which package it stood in for.

When an import of a stubbed package becomes real, remove that package's row here and let it
resolve from npm again — checking its advisories first, since that is usually why it was
stubbed.

## Note on the `name` field

The `name` is `@sonatype/empty-stub`, not any of the packages above. Yarn ignores it for
resolution, but tooling that introspects the directory (license scanners, SBOM generators)
reports it — so it is deliberately a name in a scope we control and unpublished on npm.
A bare `empty-stub` would collide with a real npm package of that name.
