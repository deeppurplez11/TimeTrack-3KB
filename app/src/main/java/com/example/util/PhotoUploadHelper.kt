package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class UploadedDevotionalPhoto(
    val storagePath: String,
    val downloadUrl: String
)

private data class PreparedDevotionalImage(
    val bytes: ByteArray,
    val contentType: String,
    val extension: String,
    val isDecodedImage: Boolean
)

object PhotoUploadHelper {
    private const val MAX_DIMENSION_PX = 1280
    private const val JPEG_QUALITY = 82
    private const val MAX_STORAGE_BYTES = 5 * 1024 * 1024
    private const val INLINE_MAX_DIMENSION_PX = 720
    private const val INLINE_MAX_BYTES = 180 * 1024

    fun createCameraPhotoUri(context: Context): Uri {
        val dir = File(context.cacheDir, "devotional_photos").apply { mkdirs() }
        val file = File(dir, "devotional_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun resolvePreviewModel(photoUrl: String): Any? {
        if (photoUrl.isBlank()) return null
        if (photoUrl.startsWith("data:image/", ignoreCase = true)) {
            val base64Part = photoUrl.substringAfter("base64,", "")
            if (base64Part.isBlank()) return null
            return runCatching { Base64.decode(base64Part, Base64.DEFAULT) }.getOrNull()
        }
        if (photoUrl.startsWith("inline://", ignoreCase = true)) {
            return null
        }
        return photoUrl
    }

    internal fun resolveImageExtension(mimeType: String): String {
        return when (mimeType.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            "image/heic", "image/heif" -> "heic"
            "image/svg+xml" -> "svg"
            else -> "jpg"
        }
    }

    suspend fun uploadDevotionalPhoto(
        context: Context,
        sourceUri: Uri,
        employeeUid: String,
        dateKey: String,
        onProgress: (Float) -> Unit
    ): UploadedDevotionalPhoto {
        val uid = Firebase.auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Google authentication is required before uploading a devotional photo.")

        val prepared = withContext(Dispatchers.IO) {
            prepareImagePayload(context, sourceUri)
        }

        val fileUuid = UUID.randomUUID().toString()
        val objectPath = "workspaces/3kb_main/devotionals/$employeeUid/$dateKey/$fileUuid.${prepared.extension}"
        val storageRef = Firebase.storage.reference.child(objectPath)
        val metadata = StorageMetadata.Builder()
            .setContentType(prepared.contentType)
            .setCustomMetadata("ownerUid", uid)
            .setCustomMetadata("employeeUid", employeeUid)
            .setCustomMetadata("dateKey", dateKey)
            .setCustomMetadata("readableImage", prepared.isDecodedImage.toString())
            .build()

        onProgress(0.05f)

        return try {
            suspendCancellableCoroutine { cont ->
                val task = storageRef.putBytes(prepared.bytes, metadata)
                task.addOnProgressListener { snap ->
                    val total = snap.totalByteCount.coerceAtLeast(1L)
                    val fraction = (snap.bytesTransferred.toFloat() / total.toFloat()).coerceIn(0.05f, 0.95f)
                    onProgress(fraction)
                }
                task.addOnSuccessListener {
                    if (cont.isActive) cont.resume(Unit)
                }
                task.addOnFailureListener { exc ->
                    if (cont.isActive) cont.resumeWithException(exc)
                }
                task.addOnCanceledListener {
                    cont.cancel()
                }
                cont.invokeOnCancellation {
                    runCatching { task.cancel() }
                }
            }

            val downloadUrl = suspendCancellableCoroutine<String> { cont ->
                storageRef.downloadUrl
                    .addOnSuccessListener { uri ->
                        if (cont.isActive) cont.resume(uri.toString())
                    }
                    .addOnFailureListener { exc ->
                        if (cont.isActive) cont.resumeWithException(exc)
                    }
            }

            onProgress(1f)
            UploadedDevotionalPhoto(
                storagePath = objectPath,
                downloadUrl = downloadUrl
            )
        } catch (exc: StorageException) {
            // HTTP 404 ("Object does not exist at location" / bucket missing) occurs when Firebase Storage
            // bucket is not yet initialized in Firebase Console; fall back to compact Firestore-safe image data URI.
            if (isStorageNotProvisioned(exc)) {
                val fallbackUrl = withContext(Dispatchers.IO) {
                    buildInlineFallbackUrl(prepared)
                }
                onProgress(1f)
                UploadedDevotionalPhoto(
                    storagePath = "firestore-inline/$objectPath",
                    downloadUrl = fallbackUrl
                )
            } else {
                throw exc
            }
        }
    }

    private fun isStorageNotProvisioned(exc: StorageException): Boolean {
        val code = exc.errorCode
        if (code == StorageException.ERROR_OBJECT_NOT_FOUND || code == StorageException.ERROR_BUCKET_NOT_FOUND) {
            return true
        }
        val msg = exc.message.orEmpty()
        return msg.contains("Object does not exist", ignoreCase = true) ||
            msg.contains("Bucket does not exist", ignoreCase = true) ||
            msg.contains("404", ignoreCase = true)
    }

    private fun buildInlineFallbackUrl(prepared: PreparedDevotionalImage): String {
        if (prepared.bytes.isEmpty()) {
            return "inline://unreadable-image/${UUID.randomUUID()}"
        }
        val compact = runCatching {
            var quality = 72
            var result: ByteArray? = null
            while (quality >= 30) {
                val candidate = tryCompressBitmapBytes(
                    rawBytes = prepared.bytes,
                    maxDim = INLINE_MAX_DIMENSION_PX,
                    jpegQuality = quality
                ) ?: break
                result = candidate
                if (candidate.size <= INLINE_MAX_BYTES) break
                quality -= 14
            }
            result?.takeIf { it.size <= INLINE_MAX_BYTES }
        }.getOrNull()

        return when {
            compact != null -> {
                val b64 = Base64.encodeToString(compact, Base64.NO_WRAP)
                "data:image/jpeg;base64,$b64"
            }
            prepared.bytes.size <= INLINE_MAX_BYTES -> {
                val b64 = Base64.encodeToString(prepared.bytes, Base64.NO_WRAP)
                "data:${prepared.contentType};base64,$b64"
            }
            else -> "inline://unreadable-image/${UUID.randomUUID()}"
        }
    }

    private fun prepareImagePayload(context: Context, uri: Uri): PreparedDevotionalImage {
        val resolver = context.contentResolver
        val rawMime = runCatching { resolver.getType(uri) }
            .getOrNull()
            ?.lowercase()
            ?.takeIf { it.startsWith("image/") }
            ?: "image/jpeg"

        val rawBytes = runCatching {
            resolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()

        if (rawBytes != null && rawBytes.isNotEmpty()) {
            val compressed = runCatching {
                tryCompressBitmapBytes(rawBytes, MAX_DIMENSION_PX, JPEG_QUALITY)
            }.getOrNull()
            if (compressed != null && compressed.isNotEmpty() && compressed.size <= MAX_STORAGE_BYTES) {
                return PreparedDevotionalImage(
                    bytes = compressed,
                    contentType = "image/jpeg",
                    extension = "jpg",
                    isDecodedImage = true
                )
            }

            if (rawBytes.size > MAX_STORAGE_BYTES) {
                throw IllegalArgumentException("Selected image exceeds the 5 MB upload limit and could not be resized.")
            }

            // Accept unreadable/un-decodable image bytes directly
            return PreparedDevotionalImage(
                bytes = rawBytes,
                contentType = rawMime,
                extension = resolveImageExtension(rawMime),
                isDecodedImage = false
            )
        }

        // Stream itself was empty or unreadable — still accept as an image attachment
        return PreparedDevotionalImage(
            bytes = byteArrayOf(),
            contentType = rawMime,
            extension = resolveImageExtension(rawMime),
            isDecodedImage = false
        )
    }

    private fun tryCompressBitmapBytes(
        rawBytes: ByteArray,
        maxDim: Int,
        jpegQuality: Int
    ): ByteArray? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDim)
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val rawBitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts)
            ?: return null

        val orientation = runCatching {
            ExifInterface(ByteArrayInputStream(rawBytes)).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val rotated = rotateBitmapIfNeeded(rawBitmap, orientation)
        val scaled = scaleDownIfNeeded(rotated, maxDim)

        return ByteArrayOutputStream().use { baos ->
            val ok = scaled.compress(Bitmap.CompressFormat.JPEG, jpegQuality, baos)
            if (scaled !== rotated) scaled.recycle()
            if (rotated !== rawBitmap) rotated.recycle()
            rawBitmap.recycle()
            if (ok) baos.toByteArray() else null
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var inSampleSize = 1
        if (width > maxDim || height > maxDim) {
            var halfW = width / 2
            var halfH = height / 2
            while (halfW / inSampleSize >= maxDim && halfH / inSampleSize >= maxDim) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun rotateBitmapIfNeeded(bitmap: Bitmap, orientation: Int): Bitmap {
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val ratio = minOf(maxDim.toFloat() / w.toFloat(), maxDim.toFloat() / h.toFloat())
        val targetW = (w * ratio).toInt().coerceAtLeast(1)
        val targetH = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }
}
