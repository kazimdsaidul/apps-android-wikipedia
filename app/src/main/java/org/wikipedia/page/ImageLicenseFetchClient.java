package org.wikipedia.page;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryImageLicensePage;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.retrofit.MwCachedService;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class ImageLicenseFetchClient {
    @NonNull private MwCachedService<Service> cachedService = new MwCachedService<>(Service.class);

    public interface Callback {
        void success(@NonNull Call<MwQueryResponse<QueryResult>> call, @NonNull List<MwQueryImageLicensePage> results);
        void failure(@NonNull Call<MwQueryResponse<QueryResult>> call, @NonNull Throwable caught);
    }

    public Call<MwQueryResponse<QueryResult>> request(@NonNull WikiSite wiki,
                                                      @NonNull PageTitle title,
                                                      @NonNull Callback cb) {
        return request(wiki, cachedService.service(wiki), title, cb);
    }

    @VisibleForTesting Call<MwQueryResponse<QueryResult>> request(final WikiSite wiki, @NonNull Service service,
                                                                  @NonNull final PageTitle title,
                                                                  @NonNull final Callback cb) {
        Call<MwQueryResponse<QueryResult>> call = service.request(title.toString());

        call.enqueue(new retrofit2.Callback<MwQueryResponse<QueryResult>>() {
            @Override public void onResponse(Call<MwQueryResponse<QueryResult>> call,
                                             Response<MwQueryResponse<QueryResult>> response) {
                if (response.body().success()) {
                    cb.success(call, response.body().query().pages());
                } else if (response.body().hasError()) {
                    cb.failure(call, new MwException(response.body().getError()));
                } else {
                    cb.failure(call, new IOException("An unknown error occurred."));
                }
            }

            @Override
            public void onFailure(Call<MwQueryResponse<QueryResult>> call, Throwable t) {
                cb.failure(call, t);
            }
        });

        return call;
    }


    public class QueryResult {
        @SuppressWarnings("unused") @Nullable private List<MwQueryImageLicensePage> pages;
        @Nullable List<MwQueryImageLicensePage> pages() {
            return pages;
        }
    }

    @VisibleForTesting interface Service {
        @GET("w/api.php?action=query&format=json&formatversion=2&prop=imageinfo&iiprop=extmetadata")
        Call<MwQueryResponse<QueryResult>> request(@NonNull @Query("titles") String titles);
    }
}
