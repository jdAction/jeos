# Publishing jeos 0.9.17

Two upstream publishing targets are configured: GitHub (`jdAction/jeos`) and Maven Central
(`io.github.jdaction:jeos`). Both need an account action that cannot be performed by the build
itself; everything else is scripted.

## 1. GitHub

`github.com/jdAction/jeos` already exists and is a fork of `adyliu/jeos` last updated in
2020-09-02. Note that this network cannot reach `https://github.com` over HTTPS (TCP 443 times out)
while `git@github.com` over SSH works, so push over SSH.

```powershell
# add the public key of this machine to the account first (Settings -> SSH and GPG keys)
Get-Content "$env:USERPROFILE\.ssh\id_ed25519.pub"

git remote add github git@github.com:jdAction/jeos.git
git push github master --tags
```

If the local history has to stay untouched, push a branch instead and open a pull request:

```powershell
git push github HEAD:refs/heads/release/0.9.17
```

## 2. Maven Central

Requirements: a Central Portal account (https://central.sonatype.com) that owns the
`io.github.jdaction` namespace (verified through the GitHub account `jdAction`), a GPG key whose
public part is uploaded to `keys.openpgp.org`, and a portal token in `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>central</id>
      <username>TOKEN_USER</username>
      <password>TOKEN_PASSWORD</password>
    </server>
  </servers>
</settings>
```

Then, from the repository root:

```bash
mvn -B -P release deploy
```

The `central-publishing-maven-plugin` uploads the bundle and leaves it in the portal as a draft
(`autoPublish=false`); publish it in the portal UI when the validation report is clean.

## 3. Local verification (no account needed)

```bash
mvn -B clean install                     # usable from other local projects as io.github.jdaction:jeos:0.9.17
mvn -B -P release package -Dgpg.skip=true # + sources and javadoc jars
```