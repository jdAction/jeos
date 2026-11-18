# EOS Blockchain Java SDK — jeos

EOSIO RPC client for Java: accounts, keys, transactions, tables and history, plus a local
signing path that works without `wallet_api_plugin`.

This repository is the maintained fork of [adyliu/jeos](https://github.com/adyliu/jeos); version
`0.9.17` upgrades the dependency set and fixes the long standing issues listed below.

| | |
|---|---|
| Coordinates | `io.github.jdaction:jeos:0.9.17` |
| Java | 17+ |
| HTTP | retrofit 3.0.0 + okhttp 4.12.0 |
| JSON | jackson 2.21.5 |

## Fixes in 0.9.17

| Upstream issue | Symptom | Fix |
|---|---|---|
| #19 / PR #23 | `getActions()` failed with `Cannot deserialize instance of java.util.ArrayList<java.lang.Object> out of START_OBJECT token` | `Receipt.authSequence` is now `List<AuthSequence>` and accepts both nodeos shapes: `[["eosio",42]]` and `[{"account":"eosio","sequence":42}]` |
| #22 | action return values (EOSIO 2.1+) were dropped | `ActionTrace` exposes `returnValueHexData` / `returnValueData` |
| — | no way to read the block that included a pushed transaction | `PushedTransaction.getBlockNum()` / `getBlockTime()` |
| #7 | private keys were derived from `UUID` and a truncated `BigInteger` | keys come from `SecureRandom`, are validated against the curve order and encoded to exactly 32 bytes |
| #13 | no way to verify a signature | `KeyUtil.verify(publicKey, data, signature)` and `verifyDigest(publicKey, hexDigest, signature)` |
| #5 | `findAndRegisterModules()` blew up during `ServiceLoader` on Android / restricted loaders | Jackson modules are registered explicitly with `JavaTimeModule` + `Jdk8Module` |
| #9 | a slow node surfaced as `SocketTimeoutException` after the transaction was already accepted | configurable timeouts (`EosApiServiceGenerator.configureTimeouts`) and a null-safe logging interceptor |
| #10 | `get_table_rows` could not page a large table | `getTableRows(scope, code, table, lowerBound, upperBound, limit)` |
| #12 | transaction id only available after broadcast | `LocalApi.getTransactionId(transaction)` |
| #15 | wallet API is gone from nodeos | `LocalApi.createTransaction(signArg, privateKey, actions)` signs offline; push with `EosApi.pushTransaction` |
| #6 | errors lost the node's assertion details | `EosApiException.getDetails()` |
| — | the published jar was a shaded uber jar | the shade plugin was removed: a library must not ship relocated dependencies |

Locally produced signatures were also invalid because the packed transaction omitted
`transaction_extensions` and the digest appended 33 instead of 32 zero bytes. `Packer` now
serializes the canonical transaction body, `Packer.transactionId(...)` and
`Packer.signDigest(...)` derive both hashes from it.

## Dependency

```xml
<dependency>
  <groupId>io.github.jdaction</groupId>
  <artifactId>jeos</artifactId>
  <version>0.9.17</version>
</dependency>
```

## Quick start

```java
EosApi client = EosApiFactory.create("https://eos.greymass.com");
ChainInfo info = client.getChainInfo();
Account account = client.getAccount("eosio.token");
```

## Offline signing (no wallet API)

```java
EosApi chain = EosApiFactory.create("https://eos.greymass.com");
LocalApi local = EosApiFactory.createLocalApi();

SignArg arg = chain.getSignArg(90);                  // expiration = head_block_time + 90s
String privateKey = "5K...";                          // WIF, stays on this machine

PushTransactionRequest request = local.transfer(
        arg, privateKey, "eosio.token", "fromaccount", "toaccount", "1.0000 EOS", "memo");

System.out.println("trx id before broadcast: " + local.getTransactionId(request.getTransaction()));

PushedTransaction pushed = chain.pushTransaction(request);
System.out.println("included in block " + pushed.getBlockNum() + " at " + pushed.getBlockTime());
```

Arbitrary actions go through `LocalApi.createTransaction(signArg, privateKey, actions)`.

## Signature verification

```java
String signature = KeyUtil.signHash(privateKey, payload);
boolean ok = KeyUtil.verify(KeyUtil.toPublicKey(privateKey), payload, signature);
```

## Reading a large table

```java
TableRow page = client.getTableRows("mytoken", "mytoken", "accounts", "alice", "carol", "100");
boolean more = Boolean.TRUE.equals(page.getMore());
```

## Build and test

```bash
mvn -B clean test        # unit tests: key handling, deserialization, packing, signing
mvn -B clean install     # install into the local repository
mvn -B -P release package -Dgpg.skip=true   # additional sources and javadoc jars
```

Publishing to GitHub and to Maven Central is described in [RELEASE.md](RELEASE.md).

## License

Apache License 2.0, see [LICENSE](LICENSE).