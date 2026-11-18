package io.jafka.jeos.exception;

/**
 * Exception raised for a failed chain, history or wallet API call.
 *
 * <p>The node reports the failing assertion as a list of {@link EosErrorDetails}; they are kept
 * here so callers can inspect the exact wasm/file/line that failed instead of only the summary
 * message (upstream issue #6).</p>
 */
public class EosApiException extends RuntimeException {

    private ErrorCode eosErrorCode;

    private EosErrorDetails[] details;

    public EosApiException(ErrorCode eosErrorCode) {
        this.eosErrorCode = eosErrorCode;
    }

    public EosApiException(String message, ErrorCode eosErrorCode) {
        super(message);
        this.eosErrorCode = eosErrorCode;
    }

    public EosApiException(String message, ErrorCode eosErrorCode, EosErrorDetails[] details) {
        super(message);
        this.eosErrorCode = eosErrorCode;
        this.details = details;
    }

    public EosApiException(Throwable cause, ErrorCode eosErrorCode) {
        super(cause);
        this.eosErrorCode = eosErrorCode;
    }

    public EosApiException(Throwable cause) {
        super(cause);
    }

    public EosApiException(String message, Throwable cause, ErrorCode eosErrorCode) {
        super(message, cause);
        this.eosErrorCode = eosErrorCode;
    }

    public ErrorCode getEosErrorCode() {
        return eosErrorCode;
    }

    public void setEosErrorCode(ErrorCode eosErrorCode) {
        this.eosErrorCode = eosErrorCode;
    }

    public EosErrorDetails[] getDetails() {
        return details;
    }

    public void setDetails(EosErrorDetails[] details) {
        this.details = details;
    }
}