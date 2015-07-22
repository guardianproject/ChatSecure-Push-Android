/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.chatsecure.pushdemo;

import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.widget.FrameLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import org.chatsecure.pushdemo.gcm.GcmService;
import org.chatsecure.pushdemo.gcm.RegistrationIntentService;
import org.chatsecure.pushdemo.ui.fragment.MessagingFragment;
import org.chatsecure.pushdemo.ui.fragment.RegistrationFragment;
import org.chatsecure.pushsecure.pushsecure.PushSecureClient;
import org.chatsecure.pushsecure.pushsecure.response.Account;
import org.chatsecure.pushsecure.pushsecure.response.PushToken;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.subjects.PublishSubject;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements RegistrationFragment.AccountRegistrationListener {

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    @Bind(R.id.container)
    FrameLayout container;

    private PushSecureClient client;
    private DataProvider dataProvider;

    private PublishSubject<Account> newAccountObservable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        if (checkPlayServices()) {

            dataProvider = new DataProvider(this);

            client = new PushSecureClient("https://chatsecure-push.herokuapp.com/api/v1/");

            Registration.register(RegistrationIntentService.refreshGcmToken(this),
                    client,
                    dataProvider, () -> {
                        // Registration needed
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.container, RegistrationFragment.newInstance(client), "signup")
                                .commit();

                        newAccountObservable = PublishSubject.create();
                        return newAccountObservable;
                    })
                    .subscribe(pushSecureClient -> {
                        Timber.d("Registered");
                        // Show a "Create / Share Whitelist token" Fragment
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.container, MessagingFragment.newInstance(pushSecureClient, dataProvider), "signup")
                                .commit();
                    });
        }
        processIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processIntent(intent);
    }

    private void processIntent(Intent intent) {
        if (intent.getAction().equals(GcmService.REVOKE_TOKEN_ACTION)) {
            handleRevokeTokenIntent(intent);
        }
    }

    private void handleRevokeTokenIntent(Intent intent) {
        client.deleteToken(new PushToken(intent.getStringExtra(GcmService.TOKEN_EXTRA)))
                .subscribe(resp -> {
                            Timber.d("Delete token http response %d", resp.getStatus());
                            handleRevokeTokenIntentProcessed(intent);
                        },
                        throwable -> {
                            Timber.e(throwable, "Failed to delete token");
                            handleRevokeTokenIntentProcessed(intent);
                        });
    }

    private void handleRevokeTokenIntentProcessed(Intent intent) {
        dismissNotification(intent.getIntExtra(GcmService.NOTIFICATION_ID_EXTRA, 1));
        Snackbar.make(container, getString(R.string.revoked_token), Snackbar.LENGTH_SHORT).show();
    }

    private void dismissNotification(int id) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(id);
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Timber.i("This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    @Override
    public void onAccountCreated(Account account) {
        // PushSecureRegistration doesn't need the ChatSecure Push username
        // so our app is responsible for persisting it in order to personalize our UI
        dataProvider.setPushSecureUsername(account.username);

        // Notify PushSecureRegistration
        if (newAccountObservable != null) {
            newAccountObservable.onNext(account);

            // Change this when we support the user changing their account in-app
            newAccountObservable.onCompleted();
        }

    }
}