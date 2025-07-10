package ci.technchange.prestationscmu.utils;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

public class InputStreamRequest extends Request<InputStream> {
    private final Response.Listener<InputStream> listener;

    // Constructeur
    public InputStreamRequest(int method, String url, Response.Listener<InputStream> listener, Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.listener = listener;
    }

    @Override
    protected Response<InputStream> parseNetworkResponse(NetworkResponse response) {
        // Convertir les données binaires en InputStream
        InputStream inputStream = new ByteArrayInputStream(response.data);
        return Response.success(inputStream, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected void deliverResponse(InputStream response) {
        // Transmettre l'InputStream à l'écouteur
        listener.onResponse(response);
    }
}
