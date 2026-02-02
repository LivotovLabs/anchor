package io.anchorkmp.core

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CancellableContinuation
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

internal object RequestManager {
    private val nextId = AtomicInteger(0)
    private val pendingRequests = ConcurrentHashMap<Int, CancellableContinuation<PermissionStatus>>()

    fun register(continuation: CancellableContinuation<PermissionStatus>): Int {
        val id = nextId.getAndIncrement()
        pendingRequests[id] = continuation
        return id
    }

    fun consume(id: Int): CancellableContinuation<PermissionStatus>? {
        return pendingRequests.remove(id)
    }
}

class AnchorPermissionActivity : Activity() {

    private var requestId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No setContentView, just transparent

        requestId = intent.getIntExtra("id", -1)
        val scopeName = intent.getStringExtra("scope")
        
        if (requestId == -1 || scopeName == null) {
            finish()
            return
        }

        val scope = PermissionScope.valueOf(scopeName)
        requestPermissionsForScope(scope)
    }

    private fun requestPermissionsForScope(scope: PermissionScope) {
        val permissions = when (scope) {
            PermissionScope.FOREGROUND -> arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
            PermissionScope.BACKGROUND -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else {
                    arrayOf() // Background implicit on older versions
                }
            }
        }

        if (permissions.isEmpty()) {
            reportResult(PermissionStatus.GRANTED)
            return
        }

        ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == REQUEST_CODE) {
            val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                reportResult(PermissionStatus.GRANTED)
            } else {
                // Check for permanently denied (Rationale logic omitted for brevity, mapping simplified)
                // If shouldShowRequestPermissionRationale returns false and it's denied, it's permanently denied.
                val anyPermanentlyDenied = permissions.zip(grantResults.toList()).any { (perm, result) ->
                    result != PackageManager.PERMISSION_GRANTED && !ActivityCompat.shouldShowRequestPermissionRationale(this, perm)
                }
                
                if (anyPermanentlyDenied) {
                    reportResult(PermissionStatus.PERMANENTLY_DENIED)
                } else {
                    reportResult(PermissionStatus.DENIED)
                }
            }
        }
    }

    private fun reportResult(status: PermissionStatus) {
        RequestManager.consume(requestId)?.resume(status)
        finish()
        overridePendingTransition(0, 0)
    }

    companion object {
        private const val REQUEST_CODE = 101
    }
}
