package com.example.vipayee;

import android.app.Application;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Security.removeProvider("BC");
        Security.insertProviderAt(
                new BouncyCastleProvider(), 1
        );
    }
}
