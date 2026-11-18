package io.jafka.jeos.impl;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

@Slf4j
public class LoggingInterceptor implements Interceptor {

    private static final AtomicLong SEQ = new AtomicLong(0);

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        final long index = SEQ.incrementAndGet();
        long t1 = System.currentTimeMillis();
        log.info("OkHttp-{}-1-->head {} {}", index, request.url(), request.headers());

        if (request.body() != null) {
            Buffer requestBuffer = new Buffer();
            request.body().writeTo(requestBuffer);
            log.info("OkHttp-{}-2-->body {}", index, requestBuffer.readUtf8());
        }

        Response response = chain.proceed(request);

        long t2 = System.currentTimeMillis();
        log.info("OkHttp-{}-3--<head {} {}ms {}", index, request.url(), (t2 - t1), response.headers());

        ResponseBody body = response.body();
        if (body == null) {
            // 204/304 or a bodyless response: nothing to log and nothing to re-wrap.
            return response;
        }
        MediaType contentType = body.contentType();
        String content = body.string();
        log.info("OkHttp-{}-4--<body {}", index, content);

        // The previous implementation read the body and always re-created it, which turned a
        // server-side timeout into a client side "timeout" report after the transaction had
        // already been accepted (upstream issue #9); the body is now re-wrapped only when it was
        // actually consumed here.
        return response.newBuilder().body(ResponseBody.create(contentType, content)).build();
    }
}