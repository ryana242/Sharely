package com.example.sharely;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Appends photos to an existing listing owned by the authenticated user.
 * Storage objects use items/{itemId}/image_{digits}.jpg to match the
 * existing item-photo naming pattern. Each image is compressed as JPEG.
 */
public final class OwnerItemPhotoUploader {
    public static final int MAX_PHOTOS = 6;
    private static final int MAX_BYTES = 8 * 1024 * 1024;

    public interface Callback {
        void onSuccess(int uploaded);
        void onFailure(String error);
    }

    private final Context context;
    private final FirebaseFirestore database = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    public OwnerItemPhotoUploader(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    public void upload(@NonNull String itemId, @NonNull List<Uri> selected,
                       @NonNull Callback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onFailure("Please sign in first.");
            return;
        }
        if (selected.isEmpty()) {
            callback.onSuccess(0);
            return;
        }
        final String ownerUid = user.getUid();
        final DocumentReference itemRef = database.collection("items").document(itemId);
        itemRef.get().addOnSuccessListener(doc -> {
            if (!doc.exists() || !ownerUid.equals(doc.getString("ownerId"))) {
                callback.onFailure("Only the listing owner can add photos.");
                return;
            }
            if ("PAST".equals(doc.getString("status"))) {
                callback.onFailure("A removed listing cannot receive new photos.");
                return;
            }
            Object stored = doc.get("imageUrls");
            int existing = stored instanceof List ? ((List<?>) stored).size() : 0;
            if (existing >= MAX_PHOTOS) {
                callback.onFailure("This listing already has 6 photos.");
                return;
            }
            if (selected.size() > MAX_PHOTOS - existing) {
                callback.onFailure("Select at most " + (MAX_PHOTOS - existing)
                        + " additional photo(s).");
                return;
            }
            uploadNext(itemRef, ownerUid, new ArrayList<>(selected), 0, callback);
        }).addOnFailureListener(e -> callback.onFailure("Could not verify owner: "
                + e.getMessage()));
    }

    private void uploadNext(DocumentReference itemRef, String uid, List<Uri> photos,
                            int index, Callback callback) {
        if (index >= photos.size()) {
            callback.onSuccess(index);
            return;
        }
        final byte[] jpeg;
        try {
            jpeg = compressedJpeg(photos.get(index));
        } catch (Exception e) {
            callback.onFailure("Could not read photo " + (index + 1) + ": "
                    + e.getMessage());
            return;
        }
        // Digits-only filename suffix, unique even with multiple devices.
        String suffix = String.format(Locale.US, "%d%06d", System.currentTimeMillis(),
                Math.abs(UUID.randomUUID().hashCode() % 1000000));
        StorageReference ref = storage.getReference().child("items")
                .child(itemRef.getId()).child("image_" + suffix + ".jpg");
        StorageMetadata meta = new StorageMetadata.Builder()
                .setContentType("image/jpeg").build();
        ref.putBytes(jpeg, meta).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Exception error = task.getException();
                throw error != null ? error : new IllegalStateException("Upload failed");
            }
            return ref.getDownloadUrl();
        }).addOnSuccessListener(downloadUrl -> {
            // Transactionally enforce owner identity, item state and total photo count.
            database.runTransaction(transaction -> {
                DocumentSnapshot doc = transaction.get(itemRef);
                if (!doc.exists() || !uid.equals(doc.getString("ownerId"))) {
                    throw new IllegalStateException("Listing ownership changed");
                }
                if ("PAST".equals(doc.getString("status"))) {
                    throw new IllegalStateException("Listing was removed");
                }
                List<String> urls = new ArrayList<>();
                Object old = doc.get("imageUrls");
                if (old instanceof List<?>) {
                    for (Object value : (List<?>) old) {
                        if (value instanceof String) urls.add((String) value);
                    }
                }
                if (urls.size() >= MAX_PHOTOS) {
                    throw new IllegalStateException("The listing has reached the 6-photo limit");
                }
                urls.add(downloadUrl.toString());
                transaction.update(itemRef, "imageUrls", urls);
                return null;
            }).addOnSuccessListener(ignored ->
                    uploadNext(itemRef, uid, photos, index + 1, callback)
            ).addOnFailureListener(e -> {
                // Avoid leaving a photo object not referenced by Firestore.
                ref.delete();
                callback.onFailure("Photo saved in Storage but listing update failed: "
                        + e.getMessage());
            });
        }).addOnFailureListener(e -> callback.onFailure("Upload failed: " + e.getMessage()));
    }

    private byte[] compressedJpeg(Uri uri) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            if (stream == null) throw new Exception("Photo not accessible");
            BitmapFactory.decodeStream(stream, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new Exception("Unsupported image format");
        }
        int sample = 1;
        while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) {
            sample *= 2;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        Bitmap bitmap;
        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            if (stream == null) throw new Exception("Photo not accessible");
            bitmap = BitmapFactory.decodeStream(stream, null, options);
        }
        if (bitmap == null) throw new Exception("Unable to decode image");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 82, buffer)) {
                throw new Exception("Unable to encode JPEG");
            }
        } finally {
            bitmap.recycle();
        }
        byte[] bytes = buffer.toByteArray();
        if (bytes.length > MAX_BYTES) throw new Exception("Photo is too large");
        return bytes;
    }
}
