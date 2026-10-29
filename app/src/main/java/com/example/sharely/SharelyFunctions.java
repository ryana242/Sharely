package com.example.sharely;

import android.app.Activity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Thin Android client for the server-authorised transaction workflow. */
final class SharelyFunctions {
    interface Callback { void done(Map<String, Object> data); }
    private SharelyFunctions() {}
    static boolean signedIn(Activity a) {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) return true;
        SharelyUi.notice(a, "Please sign in to Sharely first.");
        return false;
    }
    static String uid() {
        return FirebaseAuth.getInstance().getCurrentUser() == null ? null
                : FirebaseAuth.getInstance().getCurrentUser().getUid();
    }
    static Map<String, Object> args(Object... pairs) {
        Map<String, Object> result = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put(String.valueOf(pairs[i]), pairs[i + 1]);
        return result;
    }
    @SuppressWarnings("unchecked")
    static void call(Activity activity, String function, Map<String, Object> data, Callback callback) {
        if (!signedIn(activity)) return;
        FirebaseFunctions.getInstance("australia-southeast1").getHttpsCallable(function).call(data)
            .addOnSuccessListener(result -> {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                Object payload = result.getData();
                callback.done(payload instanceof Map ? (Map<String, Object>) payload : Collections.emptyMap());
            })
            .addOnFailureListener(error -> {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                SharelyUi.notice(activity, "Action failed: " + error.getMessage());
            });
    }
}
