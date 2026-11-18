package io.jafka.jeos.impl;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.jafka.jeos.exception.EosApiError;
import io.jafka.jeos.exception.EosApiErrorCode;
import io.jafka.jeos.exception.EosApiException;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class EosApiServiceGenerator {

    private static final long DEFAULT_CONNECT_TIMEOUT_SECONDS = 10L;
    private static final long DEFAULT_READ_TIMEOUT_SECONDS = 30L;

    private static final ObjectMapper MAPPER = createMapper();

    private static volatile OkHttpClient httpClient = createHttpClient(
            DEFAULT_CONNECT_TIMEOUT_SECONDS, DEFAULT_READ_TIMEOUT_SECONDS);

    private static volatile boolean loggingEnabled = true;

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        // Register the Java 8 / java.time modules explicitly instead of relying on
        // ServiceLoader discovery: the service-file based lookup is what broke on Android and on
        // class-loader restricted platforms (upstream issue #5), and it also breaks when the
        // dependency jars are re-packed.
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new Jdk8Module());
        return mapper;
    }

    private static OkHttpClient createHttpClient(long connectTimeoutSeconds, long readTimeoutSeconds) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                .callTimeout(0L, TimeUnit.MILLISECONDS)
                .writeTimeout(readTimeoutSeconds, TimeUnit.SECONDS);
        if (loggingEnabled) {
            builder.addInterceptor(new LoggingInterceptor());
        }
        return builder.build();
    }

    /**
     * Configures the HTTP timeouts used by every client created afterwards. A node that is slow to
     * answer used to surface as {@code SocketTimeoutException: timeout} after the transaction had
     * already been accepted (upstream issue #9); raising the read timeout is the intended fix.
     */
    public static synchronized void configureTimeouts(long connectTimeoutSeconds, long readTimeoutSeconds) {
        httpClient = createHttpClient(connectTimeoutSeconds, readTimeoutSeconds);
    }

    /**
     * Enables or disables the request/response body logging interceptor.
     */
    public static synchronized void setLoggingEnabled(boolean enabled) {
        loggingEnabled = enabled;
        httpClient = createHttpClient(DEFAULT_CONNECT_TIMEOUT_SECONDS, DEFAULT_READ_TIMEOUT_SECONDS);
    }

    public static ObjectMapper getMapper() {
        return MAPPER;
    }

    public static <S> S createService(Class<S> serviceClass, String baseUrl) {
        return createRetrofit(baseUrl, httpClient).create(serviceClass);
    }

    public static <S> S createService(Class<S> serviceClass, String baseUrl, String bearerToken) {
        OkHttpClient authenticated = httpClient.newBuilder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public okhttp3.Response intercept(Chain chain) throws IOException {
                        Request newRequest = chain.request().newBuilder()
                                .addHeader("Authorization", "Bearer " + bearerToken)
                                .build();
                        return chain.proceed(newRequest);
                    }
                })
                .build();
        return createRetrofit(baseUrl, authenticated).create(serviceClass);
    }

    private static Retrofit createRetrofit(String baseUrl, OkHttpClient client) {
        Retrofit.Builder builder = new Retrofit.Builder();
        builder.baseUrl(baseUrl);
        builder.client(client);
        builder.addConverterFactory(JacksonConverterFactory.create(MAPPER));
        return builder.build();
    }

    /**
     * Execute a REST call and block until the response is received.
     */
    public static <T> T executeSync(Call<T> call) {
        try {
            Response<T> response = call.execute();
            if (response.isSuccessful()) {
                return response.body();
            }
            EosApiError apiError = getEosApiError(response);
            throw new EosApiException(apiError.getDetailedMessage(), EosApiErrorCode.get(apiError.getEosErrorCode()),
                    apiError.getError() == null ? null : apiError.getError().getDetails());
        } catch (IOException e) {
            throw new EosApiException(e);
        }
    }

    /**
     * Extracts and converts the response error body into an object, using this generator's mapper
     * instead of a shared {@link Retrofit} instance so concurrent clients cannot mix error bodies.
     */
    private static EosApiError getEosApiError(Response<?> response) throws IOException, EosApiException {
        if (response.errorBody() == null) {
            throw new EosApiException("HTTP " + response.code() + " without an error body",
                    EosApiErrorCode.get(null));
        }
        EosApiError error = MAPPER.readValue(response.errorBody().bytes(), EosApiError.class);
        if (error == null) {
            throw new EosApiException("HTTP " + response.code() + " without a parsable error body",
                    EosApiErrorCode.get(null));
        }
        return error;
    }
}