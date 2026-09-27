package dev.ajvanegasv.kontio.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.ajvanegasv.kontio.domain.model.StatementFile
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerMode
import platform.UIKit.UIDocumentPickerViewController
import platform.darwin.NSObject
import platform.posix.memcpy

@Composable
actual fun rememberStatementFilePicker(
    onFileSelected: (StatementFile) -> Unit
): () -> Unit {
    return remember(onFileSelected) {
        {
            val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
            if (rootViewController != null) {
                val types = listOf(
                    "com.adobe.pdf",
                    "public.comma-separated-values-text",
                    "public.delimited-values-text",
                    "public.text",
                    "public.data"
                )
                val picker = UIDocumentPickerViewController(
                    documentTypes = types,
                    inMode = UIDocumentPickerMode.UIDocumentPickerModeImport
                )
                val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
                    @OptIn(ExperimentalForeignApi::class)
                    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentAtURL: NSURL) {
                        val name = didPickDocumentAtURL.lastPathComponent ?: "extracto"
                        val data = NSData.dataWithContentsOfURL(didPickDocumentAtURL)
                        if (data != null) {
                            val length = data.length.toInt()
                            val bytes = ByteArray(length)
                            if (length > 0) {
                                bytes.usePinned { pinned ->
                                    memcpy(pinned.addressOf(0), data.bytes, data.length)
                                }
                            }
                            val mimeType = if (name.endsWith(".pdf", ignoreCase = true)) "application/pdf" else "text/csv"
                            onFileSelected(StatementFile(name = name, mimeType = mimeType, bytes = bytes))
                        }
                    }
                }
                picker.delegate = delegate
                rootViewController.presentViewController(picker, animated = true, completion = null)
            }
        }
    }
}
