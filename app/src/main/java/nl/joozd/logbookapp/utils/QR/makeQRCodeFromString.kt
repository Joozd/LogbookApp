package nl.joozd.logbookapp.sharing

import android.widget.ImageView
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

fun makeQRCodeFromStringAndPutInImageView(target: ImageView, string: String){
    val bitmap = BarcodeEncoder().encodeBitmap(string, BarcodeFormat.QR_CODE, 400, 400);
    target.setImageBitmap(bitmap);
}