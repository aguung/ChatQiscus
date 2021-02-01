package com.devfutech.chatqiscus.utils

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Environment
import androidx.exifinterface.media.ExifInterface
import com.qiscus.sdk.chat.core.QiscusCore
import com.qiscus.sdk.chat.core.data.local.QiscusCacheManager
import com.qiscus.sdk.chat.core.util.QiscusFileUtil
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToLong

object QiscusImageUtil {
    private fun getScaledBitmap(imageUri: Uri?): Bitmap? {
        val filePath = QiscusFileUtil.getRealPathFromURI(imageUri)
        var scaledBitmap: Bitmap? = null
        val options = BitmapFactory.Options()

        //by setting this field as true, the actual bitmap pixels are not loaded in the memory. Just the bounds are loaded. If
        //you try the use the bitmap here, you will get null.
        options.inJustDecodeBounds = true
        var bmp = BitmapFactory.decodeFile(filePath, options)
        var actualHeight = options.outHeight
        var actualWidth = options.outWidth
        if (actualWidth < 0 || actualHeight < 0) {
            val bitmap2 = BitmapFactory.decodeFile(filePath)
            actualWidth = bitmap2.width
            actualHeight = bitmap2.height
        }

        //max Height and width values of the compressed image is taken as 1440x900
        val maxHeight = QiscusCore.getChatConfig().qiscusImageCompressionConfig.maxHeight
        val maxWidth = QiscusCore.getChatConfig().qiscusImageCompressionConfig.maxWidth
        var imgRatio = (actualWidth / actualHeight).toFloat()
        val maxRatio = maxWidth / maxHeight

        //width and height values are set maintaining the aspect ratio of the image
        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            when {
                imgRatio < maxRatio -> {
                    imgRatio = maxHeight / actualHeight
                    actualWidth = (imgRatio * actualWidth).toInt()
                    actualHeight = maxHeight.toInt()
                }
                imgRatio > maxRatio -> {
                    imgRatio = maxWidth / actualWidth
                    actualHeight = (imgRatio * actualHeight).toInt()
                    actualWidth = maxWidth.toInt()
                }
                else -> {
                    actualHeight = maxHeight.toInt()
                    actualWidth = maxWidth.toInt()
                }
            }
        }

        //setting inSampleSize value allows to load a scaled down version of the original image
        options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight)

        //inJustDecodeBounds set to false to load the actual bitmap
        options.inJustDecodeBounds = false

        //this options allow android to claim the bitmap memory if it runs low on memory
        options.inPurgeable = true
        options.inInputShareable = true
        options.inTempStorage = ByteArray(16 * 1024)
        try {
            //load the bitmap from its path
            bmp = BitmapFactory.decodeFile(filePath, options)
        } catch (exception: OutOfMemoryError) {
            exception.printStackTrace()
        }
        try {
            scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.ARGB_8888)
        } catch (exception: OutOfMemoryError) {
            exception.printStackTrace()
        }
        val ratioX = actualWidth / options.outWidth.toFloat()
        val ratioY = actualHeight / options.outHeight.toFloat()
        val middleX = actualWidth / 2.0f
        val middleY = actualHeight / 2.0f
        val scaleMatrix = Matrix()
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY)
        val canvas = Canvas(scaledBitmap!!)
        canvas.setMatrix(scaleMatrix)
        canvas.drawBitmap(
            bmp,
            middleX - bmp.width / 2,
            middleY - bmp.height / 2,
            Paint(Paint.FILTER_BITMAP_FLAG)
        )

        //check the rotation of the image and display it properly
        val exif: ExifInterface
        try {
            exif = ExifInterface(filePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)
            val matrix = Matrix()
            when (orientation) {
                6 -> {
                    matrix.postRotate(90f)
                }
                3 -> {
                    matrix.postRotate(180f)
                }
                8 -> {
                    matrix.postRotate(270f)
                }
            }
            scaledBitmap = Bitmap.createBitmap(
                scaledBitmap, 0, 0,
                scaledBitmap.width, scaledBitmap.height, matrix,
                true
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return scaledBitmap
    }

    private fun generateFilePath(context: Context, fileName: String?, extension: String): String {
        val file = File(
            context.getExternalFilesDir(null),
            if (QiscusFileUtil.isImage(fileName)) QiscusFileUtil.IMAGE_PATH else QiscusFileUtil.FILES_PATH
        )
        if (!file.exists()) {
            file.mkdirs()
        }
        var index = 0
        val directory = file.absolutePath + File.separator
        val fileNameSplit = QiscusFileUtil.splitFileName(fileName)
        while (true) {
            val newFile: File = if (index == 0) {
                File(directory + fileNameSplit[0] + extension)
            } else {
                File(directory + fileNameSplit[0] + "-" + index + extension)
            }
            if (!newFile.exists()) {
                return newFile.absolutePath
            }
            index++
        }
    }

    fun compressImage(context: Context, imageFile: File): File {
        var out: FileOutputStream? = null
        val filename = generateFilePath(context, imageFile.name, ".${imageFile.extension}")
        try {
            out = FileOutputStream(filename)
            getScaledBitmap(Uri.fromFile(imageFile))!!.compress(
                Bitmap.CompressFormat.JPEG,
                QiscusCore.getChatConfig().qiscusImageCompressionConfig.quality, out
            )
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } finally {
            try {
                out?.close()
            } catch (ignored: IOException) {
                //Do nothing
            }
        }
        val compressedImage = File(filename)
        QiscusFileUtil.notifySystem(compressedImage)
        return compressedImage
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val heightRatio = (height.toFloat() / reqHeight.toFloat()).roundToLong()
            val widthRatio = (width.toFloat() / reqWidth.toFloat()).roundToLong()
            inSampleSize = if (heightRatio < widthRatio) heightRatio.toInt() else widthRatio.toInt()
        }
        val totalPixels = (width * height).toFloat()
        val totalReqPixelsCap = (reqWidth * reqHeight * 2).toFloat()
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++
        }
        return inSampleSize
    }

    fun isImage(file: File): Boolean {
        return QiscusFileUtil.isImage(file.path)
    }

    fun addImageToGallery(picture: File?) {
        QiscusFileUtil.notifySystem(picture)
    }

    @Throws(IOException::class)
    fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val imageFileName = "JPEG-$timeStamp-"
        val storageDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(imageFileName, ".jpg", storageDir)
        QiscusCacheManager.getInstance().cacheLastImagePath("file:" + image.absolutePath)
        return image
    }

}
