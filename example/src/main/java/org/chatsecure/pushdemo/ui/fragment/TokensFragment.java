package org.chatsecure.pushdemo.ui.fragment;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.chatsecure.pushdemo.DataProvider;
import org.chatsecure.pushdemo.R;
import org.chatsecure.pushdemo.gcm.GcmService;
import org.chatsecure.pushdemo.ui.adapter.DeviceAdapter;
import org.chatsecure.pushdemo.ui.adapter.TokenAdapter;
import org.chatsecure.pushsecure.PushSecureClient;
import org.chatsecure.pushsecure.response.Device;
import org.chatsecure.pushsecure.response.PushToken;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.android.schedulers.AndroidSchedulers;
import timber.log.Timber;

/**
 * UI for managing a user's tokens
 */
public class TokensFragment extends Fragment implements TokenAdapter.Listener {

    private PushSecureClient client;
    private DataProvider provider;

    private TokenAdapter adapter;

    public static TokensFragment newInstance(PushSecureClient client, DataProvider provider) {
        TokensFragment fragment = new TokensFragment();
        fragment.setPushSecureClient(client);
        fragment.setDataProvider(provider);
        return fragment;
    }

    public TokensFragment() {
        // Required empty public constructor
    }

    public void setPushSecureClient(PushSecureClient client) {
        this.client = client;
    }

    public void setDataProvider(DataProvider provider) {
        this.provider = provider;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        RecyclerView root = (RecyclerView) inflater.inflate(R.layout.recyclerview, container, false);
        adapter = new TokenAdapter(this);
        root.setLayoutManager(new LinearLayoutManager(getActivity()));
        root.setAdapter(adapter);
        displayTokens();
        return root;
    }

    private void displayTokens() {
        client.getTokens()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tokens -> {
                    adapter.setTokens(tokens.results);
                });
    }

    @Override
    public void onRevokeTokenRequested(PushToken token) {
        client.deleteToken(token)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(resp -> {
                            Timber.d("Delete token http response %d", resp.getStatus());
                            adapter.removeToken(token);
                        },
                        throwable -> {
                            String message = "Failed to delete token";
                            Timber.e(throwable, message);
                            Snackbar.make(null, message, Snackbar.LENGTH_SHORT)
                                    .show();
                        });
    }
}
