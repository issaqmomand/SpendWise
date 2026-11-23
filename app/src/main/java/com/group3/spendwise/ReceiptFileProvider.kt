
package com.group3.spendwise

import androidx.core.content.FileProvider

// Provides secure access to receipt photos for the camera.
/**
 * Registered in AndroidManifest.xml with the authority "<package>.receiptprovider".
 * It turns private receipt files into content:// URIs so the camera app can save the photo
 * and image viewer apps can open it, without exposing the app's whole storage.
 */
class ReceiptFileProvider : FileProvider()
