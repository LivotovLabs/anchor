package io.anchorkmp.core

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal actual fun platformPermissionCheck(scope: PermissionScope): PermissionStatus {
    val context = try {
        AnchorContext.getContext()
    } catch (e: Exception) {
        return PermissionStatus.DENIED
    }
    
    // 1. Check Foreground (Fine/Coarse)
    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    
    if (scope == PermissionScope.FOREGROUND) {
         return if (hasFine) PermissionStatus.GRANTED else PermissionStatus.DENIED
    }

    // 2. Check Background
    if (scope == PermissionScope.BACKGROUND) {
        if (!hasFine) return PermissionStatus.DENIED
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasBackground = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (!hasBackground) return PermissionStatus.DENIED
        }
        return PermissionStatus.GRANTED
    }
    
    if (scope == PermissionScope.MOTION) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasActivity = ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
            return if (hasActivity) PermissionStatus.GRANTED else PermissionStatus.DENIED
        }
        return PermissionStatus.GRANTED
    }

    if (scope == PermissionScope.NOTIFICATIONS) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotifications = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            return if (hasNotifications) PermissionStatus.GRANTED else PermissionStatus.DENIED
        }
        return PermissionStatus.GRANTED
    }

    return PermissionStatus.DENIED
}

internal actual suspend fun platformPermissionRequest(scope: PermissionScope): PermissionStatus = suspendCancellableCoroutine { cont ->
    val context = try {
        AnchorContext.getContext()
    } catch (e: Exception) {
        cont.resume(PermissionStatus.DENIED)
        return@suspendCancellableCoroutine
    }

    if (platformPermissionCheck(scope) == PermissionStatus.GRANTED) {
        cont.resume(PermissionStatus.GRANTED)
        return@suspendCancellableCoroutine
    }

    val requestId = RequestManager.register(cont)
    
    val intent = Intent(context, AnchorPermissionActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
        putExtra("scope", scope.name)
        putExtra("id", requestId)
    }
    
    context.startActivity(intent)
}
