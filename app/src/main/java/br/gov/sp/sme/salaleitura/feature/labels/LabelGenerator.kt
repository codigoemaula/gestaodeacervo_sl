package br.gov.sp.sme.salaleitura.feature.labels
import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
object LabelGenerator{
 fun copyPayload(code:String)="SL:$code"
 fun personPayload(code:String)="PERSON:$code"
 fun qrBitmap(payload:String,size:Int=640):Bitmap{val matrix=MultiFormatWriter().encode(payload,BarcodeFormat.QR_CODE,size,size);return Bitmap.createBitmap(size,size,Bitmap.Config.RGB_565).also{b->for(y in 0 until size)for(x in 0 until size)b.setPixel(x,y,if(matrix[x,y])android.graphics.Color.BLACK else android.graphics.Color.WHITE)}}
}
