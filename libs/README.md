# afid libraries

Implementations of the [afid](../README.md) id format for different languages.

| Language   | Path                        | README                                       |
| ---------- | --------------------------- | -------------------------------------------- |
| Go         | [`go/`](go)                 | [go/README.md](go/README.md)                 |
| Java       | [`java/`](java)             | [java/README.md](java/README.md)             |
| Python     | [`python/`](python)         | [python/README.md](python/README.md)         |
| Rust       | [`rust/`](rust)             | [rust/README.md](rust/README.md)             |
| TypeScript | [`typescript/`](typescript) | [typescript/README.md](typescript/README.md) |

## Running checks across all libs

A [`Justfile`](Justfile) in this directory orchestrates format, lint, and test
tasks across every implementation. Install [`just`](https://just.systems/) and
then, from this directory:

```sh
just            # list available recipes
just check      # format-check + lint + tests, for every lib
just test       # tests only, every lib
just format     # apply formatting, every lib
```

Per-lib recipes are also available (e.g. `just check-python`, `just test-java`).

## Cutting a release

Each lib publishes from a GitHub release whose tag matches `libs/<lang>/v<version>`
(e.g. `libs/python/v0.3.0`). The corresponding `publish-<lang>.yml` workflow runs
on `release: published` and pushes to the language's registry.

Steps:

1. For every lib (except Go), bump the version in the manifest on `main` and merge:

   | Lang       | File                          |
   | ---------- | ----------------------------- |
   | Java       | `libs/java/gradle.properties` |
   | Python     | `libs/python/pyproject.toml`  |
   | Rust       | `libs/rust/Cargo.toml`        |
   | TypeScript | `libs/typescript/package.json` |

   The publish workflow fails if the manifest version doesn't match the tag.
   Go takes its version from the tag itself, so no file change is needed.

2. From `main`, tag and push:

   ```sh
   git tag libs/<lang>/v<version>
   git push origin libs/<lang>/v<version>
   ```

3. Create a GitHub release for that tag (e.g. `gh release create libs/<lang>/v<version> --generate-notes`).
   Publishing the release triggers the workflow.

4. Verify the workflow run under Actions and that the package appears on the
   registry (crates.io, Maven Central, npm, PyPI, or — for Go — `go list -m`).

Registry destinations:

| Lang       | Registry                          |
| ---------- | --------------------------------- |
| Go         | `proxy.golang.org` (module proxy) |
| Java       | Maven Central                     |
| Python     | PyPI                              |
| Rust       | crates.io                         |
| TypeScript | npm                               |
