package io.metersphere.node.util;

import java.io.IOException;
import java.lang.reflect.Type;

import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.util.Watch;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.ResponseBody;

@Slf4j
public class K8sLogWatch extends Watch<String> {

    protected K8sLogWatch(JSON json, ResponseBody body,
        Type watchType,
        Call call) {
        super(json, body, watchType, call);
    }

    public static K8sLogWatch createLogWatch(ApiClient client, Call call)
        throws ApiException {
        if (client.isDebugging()) {
            log.warn(
                "Watch is (for now) incompatible with debugging mode active. Watches will not return data until the watch connection terminates");
            throw new ApiException("Watch is incompatible with debugging mode active.");
        }
        try {
            okhttp3.Response response = call.execute();
            if (!response.isSuccessful()) {
                String respBody = null;
                try (ResponseBody body = response.body()) {
                    if (body != null) {
                        respBody = body.string();
                    }
                } catch (IOException e) {
                    throw new ApiException(
                        response.message(), e, response.code(), response.headers().toMultimap());
                }
                throw new ApiException(
                    response.message(), response.code(), response.headers().toMultimap(), respBody);
            }
            return new K8sLogWatch(client.getJSON(), response.body(), (new TypeToken<String>() {
            }).getType(), call);
        } catch (IOException e) {
            throw new ApiException(e);
        }
    }

    @Override
    protected Response<String> parseLine(String line) throws IOException {
        return new Response<>("log", line);
    }
}
