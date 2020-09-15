package com.example.sensorsstation

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.ContextCompat

const val REQUEST_BT_PERMISSIONS_CODE = 1000

val permissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.BLUETOOTH
)

fun checkBluetoothPermissions(context: Context): Boolean = permissions
    .all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

fun requestBluetoothPermissions(activity: Activity) =
    activity.requestPermissions(permissions, REQUEST_BT_PERMISSIONS_CODE)

val Any.tag: String
    get() = this.javaClass.simpleName

fun showShortToast(context: Context, text: String) =
    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()