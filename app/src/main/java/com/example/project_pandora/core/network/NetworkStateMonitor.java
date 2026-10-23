package com.example.project_pandora.core.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import com.example.project_pandora.core.sync.NetworkMode;
import com.example.project_pandora.core.sync.ServerConnectivity;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class NetworkStateMonitor {

    private static volatile NetworkStateMonitor instance;

    private final BehaviorSubject<NetworkMode> modeSubject;
    private volatile NetworkMode currentMode;
    private final AtomicBoolean registered;
    private volatile boolean networkAvailable;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    private NetworkStateMonitor() {
        modeSubject = BehaviorSubject.createDefault(NetworkMode.OFFLINE);
        currentMode = NetworkMode.OFFLINE;
        registered = new AtomicBoolean(false);
        networkAvailable = false;
    }

    public static NetworkStateMonitor getInstance() {
        if (instance == null) {
            synchronized (NetworkStateMonitor.class) {
                if (instance == null) {
                    instance = new NetworkStateMonitor();
                }
            }
        }
        return instance;
    }

    public void register(Context context) {
        if (registered.getAndSet(true)) return;
        try {
            connectivityManager = (ConnectivityManager) context.getApplicationContext()
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager == null) {
                networkAvailable = false;
                updateMode();
                return;
            }

            NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();

            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    networkAvailable = true;
                    updateMode();
                }

                @Override
                public void onLost(Network network) {
                    networkAvailable = false;
                    updateMode();
                }

                @Override
                public void onUnavailable() {
                    networkAvailable = false;
                    updateMode();
                }
            };

            connectivityManager.registerNetworkCallback(request, networkCallback);

            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork != null) {
                NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(activeNetwork);
                networkAvailable = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            }
            updateMode();
        } catch (Exception e) {
            networkAvailable = false;
            currentMode = NetworkMode.OFFLINE;
            modeSubject.onNext(NetworkMode.OFFLINE);
        }
    }

    public void unregister() {
        if (!registered.getAndSet(false)) return;
        try {
            if (connectivityManager != null && networkCallback != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            }
        } catch (Exception ignored) {}
    }

    private void updateMode() {
        NetworkMode newMode;
        if (networkAvailable && ServerConnectivityMonitor.getInstance().getCurrentState() == ServerConnectivity.CONNECTED) {
            newMode = NetworkMode.ONLINE;
        } else {
            newMode = NetworkMode.OFFLINE;
        }
        if (newMode != currentMode) {
            currentMode = newMode;
            modeSubject.onNext(newMode);
        }
    }

    public void onServerConnectivityChanged(ServerConnectivity connectivity) {
        if (connectivity == ServerConnectivity.CONNECTED) {
            if (!networkAvailable) {
                reevaluateNetworkAvailable();
            }
            if (networkAvailable) {
                if (currentMode != NetworkMode.ONLINE) {
                    currentMode = NetworkMode.ONLINE;
                    modeSubject.onNext(NetworkMode.ONLINE);
                }
            }
        } else {
            if (currentMode != NetworkMode.OFFLINE) {
                currentMode = NetworkMode.OFFLINE;
                modeSubject.onNext(NetworkMode.OFFLINE);
            }
        }
    }

    private void reevaluateNetworkAvailable() {
        try {
            if (connectivityManager == null) return;
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork != null) {
                NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(activeNetwork);
                networkAvailable = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            } else {
                networkAvailable = false;
            }
        } catch (Exception ignored) {}
    }

    public boolean isNetworkAvailable() {
        return networkAvailable;
    }

    public NetworkMode getCurrentMode() {
        return currentMode;
    }

    public io.reactivex.rxjava3.core.Observable<NetworkMode> modeStream() {
        return modeSubject.hide();
    }
}