# Changelog

## 0.9.17

Dependency and packaging:
- jackson 2.21.5 (BOM managed), retrofit 3.0.0, okhttp 4.12.0, kotlin 2.2.21, lombok 1.18.46, slf4j-api 2.0.17
- removed the `maven-shade-plugin` execution: the library no longer ships a shaded uber jar
- removed the duplicated `jackson-databind` declaration
- Maven Central publication moved to the Central Portal plugin, sources and javadoc jars added,
  GPG signing wired for the `release` profile

Fixes (upstream issue numbers):
- #19 / PR #23 `getActions()` deserialization: `auth_sequence` accepted as object or array
- #22 action return values: `returnValueHexData`, `returnValueData`
- #6 node error details kept in `EosApiException#getDetails()`
- #7 private keys generated from `SecureRandom`, validated and encoded to exactly 32 bytes
- #13 `KeyUtil.verify` / `KeyUtil.verifyDigest`
- #5 Jackson modules registered explicitly instead of `findAndRegisterModules()`
- #9 configurable HTTP timeouts, null-safe logging interceptor
- #10 `getTableRows` with `lower_bound` / `upper_bound` / `limit`
- #12 `LocalApi.getTransactionId(PackedTransaction)`
- #15 `LocalApi.createTransaction(...)` for offline signing without the wallet API
- transaction packing now includes `transaction_extensions`; the signature digest uses the
  canonical `sha256(chain_id || transaction || 32 zero bytes)` construction
- `PushedTransaction.getBlockNum()` / `getBlockTime()` report the block that included the transaction

## 0.9.16

- upstream release of adyliu/jeos