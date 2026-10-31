# Publishing SENTINEL as a Maven library

SENTINEL ships a library other teams can add as a dependency. Nothing has been published yet; this page is the
checklist for doing it.

## What gets published

| Artifact | Purpose | Who uses it directly |
|---|---|---|
| `com.sentinel:sentinel-parent` (pom) | Parent POM the others inherit from | nobody (required by Maven) |
| `com.sentinel:sentinel-common` | Enums: `Tier`, `AlgorithmType` | pulled in transitively |
| `com.sentinel:sentinel-limiter-core` | The four Redis/Lua rate-limit algorithms | teams building their own integration |
| `com.sentinel:sentinel-spring-boot-starter` | Auto-configured rate limiting for Spring MVC apps | **application teams** |

The gateway, admin service, sample services, infrastructure and load-test modules are applications, not libraries.
They are marked `maven.deploy.skip=true` and are never published. Ship those as Docker images instead.

How customers use the starter: see [`sentinel-spring-boot-starter/README.md`](../sentinel-spring-boot-starter/README.md).

## Before you publish: things only you can decide

1. **Replace the `TODO` values in the root `pom.xml`:** project URL, developer id/name/URL and the three SCM URLs.
2. **Confirm the license.** The POM declares Apache-2.0 as a placeholder. Choose the license you actually want, then
   add the matching `LICENSE` file at the repository root. Central requires both.
3. **Check you are allowed to publish this code publicly.** If it was written as part of your job, confirm with your
   employer's open-source or legal process first. A public release cannot be taken back.
4. **Choose a `groupId` you own.** Maven Central only accepts a namespace you have proven you control, for example
   `io.github.<your-github-name>` or a reverse domain you own. `com.sentinel` will be rejected unless you own
   `sentinel.com`. Changing it means updating `groupId` in the parent POM and every `com.sentinel` dependency, and the
   `package` names can stay as they are.
5. **Decide the version.** `1.0.0` is the current value. Releases are permanent; use `1.0.0-SNAPSHOT` while testing
   against an internal repository.

## Keep your employer identity out of the release

- **POMs:** they contain no email or employer id. The developer entry in the root `pom.xml` is a `TODO`; fill it with a
  personal id and name, and do not add an email if you prefer not to publish one.
- **Git:** commits carry the name and email from your git config. If your global config uses a work address, set a
  personal one for this repository before the first commit:

  ```bash
  git config user.name  "Your Name"
  git config user.email "you@personal-domain.example"
  ```

  (GitHub also offers a private `ID+username@users.noreply.github.com` address.)
- **GPG key:** create it with a personal email, since the key's user id is public.
- **Central account:** register with a personal email, not a work one.
- **Docs:** `architecture/KT-guide.md` lists an owner name on its first lines. Edit it if you do not want that public.

## One-time setup (Maven Central)

1. Create an account at <https://central.sonatype.com> and verify your namespace.
2. Generate a user token there and add it to `~/.m2/settings.xml`:

   ```xml
   <servers>
     <server>
       <id>central</id>
       <username>TOKEN_USERNAME</username>
       <password>TOKEN_PASSWORD</password>
     </server>
   </servers>
   ```

3. Create a GPG key (`gpg --gen-key`) and publish its public key to a key server
   (`gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>`).

## Check the release locally (publishes nothing)

```bash
mvn clean verify                                   # all tests
mvn -Prelease -DskipTests -Dgpg.skip=true install \
    -pl .,sentinel-common,sentinel-limiter-core,sentinel-spring-boot-starter
```

Look in each module's `target/` for `*.jar`, `*-sources.jar` and `*-javadoc.jar`. Then create a throw-away Spring Boot
project, add the starter from your local repository, and confirm a request gets `X-RateLimit-*` headers.

## Publish

```bash
mvn -Prelease -DskipTests deploy \
    -pl .,sentinel-common,sentinel-limiter-core,sentinel-spring-boot-starter
```

This signs the artifacts and uploads them to Central. `autoPublish` is off, so the upload waits in the Central web UI
for you to review it and press **Publish**.

## Publishing to an internal repository instead

If the library is only for your company, a repository such as Artifactory is simpler: no namespace check and no
signing. Add to the root `pom.xml`:

```xml
<distributionManagement>
  <repository>
    <id>internal-releases</id>
    <url>https://artifactory.example.com/artifactory/libs-release-local</url>
  </repository>
  <snapshotRepository>
    <id>internal-snapshots</id>
    <url>https://artifactory.example.com/artifactory/libs-snapshot-local</url>
  </snapshotRepository>
</distributionManagement>
```

and put matching `<server>` entries (with your credentials) in `settings.xml`. Then run
`mvn deploy -pl .,sentinel-common,sentinel-limiter-core,sentinel-spring-boot-starter`.

## What was verified, and what was not

Verified with a real Maven build:

- All library modules compile and their tests pass: `sentinel-common` (8), `sentinel-limiter-core` (20, of which 6 are
  skipped, see below) and `sentinel-spring-boot-starter` (28). The gateway (39) and admin service (129) tests also pass
  after the refactor; a few are skipped because they need Docker.
- The release profile builds the main, `-sources` and `-javadoc` JARs for every library module
  (`mvn -Prelease -DskipTests -Dgpg.skip=true install -pl .,sentinel-common,sentinel-limiter-core,sentinel-spring-boot-starter`).
- A sample app ([`examples/starter-demo`](../examples/starter-demo)) that adds only the starter dependency and a few
  properties starts, registers the filter, applies the default limit and a path rule, keeps clients separate, and returns
  `429` with `X-RateLimit-*` and `Retry-After` headers.

Not verified:

- **The Redis Lua limiters against a real Redis.** The Redis integration tests were skipped because Testcontainers could
  not start a container here (Docker Hub rate limiting, and the Testcontainers 1.19 in use may not support the newest
  Docker). The demo used an in-memory limiter instead. Run `mvn clean verify` on a machine with working Docker, or
  start any Redis and try the demo without the in-memory bean.
- **GPG signing and the upload to Maven Central.** Those need your key and account.
