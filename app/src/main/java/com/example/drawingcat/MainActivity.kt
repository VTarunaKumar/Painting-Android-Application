package com.example.drawingcat

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {
    private var drawingView: DrawingView? = null
    private var mImageButtonCurrentpaint: ImageButton? = null
    var customProgressDialog : Dialog? = null
    val openGalleryLuncher :ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        result->
        if (result.resultCode == RESULT_OK && result.data!=null){
            val imageBackground : ImageView = findViewById(R.id.iv_background)
            imageBackground.setImageURI(result.data?.data)
        }
    }



        val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissions ->
            permissions.entries.forEach{
                val permissionName = it.key
                val isGranted = it.value
                if (isGranted){
                    Toast.makeText(this@MainActivity,"PermissionGranted",Toast.LENGTH_LONG).show()

            val pickIntent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLuncher.launch(pickIntent)

                }



                else{
                    if (permissionName==Manifest.permission.READ_EXTERNAL_STORAGE){
                        Toast.makeText(this@MainActivity,"Permission Just Denied",Toast.LENGTH_LONG).show()

                    }
                }

    }
}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(20.toFloat())

        val linearLayoutPaintColor = findViewById<LinearLayout>(R.id.ll_paint_color)
        mImageButtonCurrentpaint = linearLayoutPaintColor[3] as ImageButton
        mImageButtonCurrentpaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_pressed)

        )


        val ib_brush: ImageButton = findViewById(R.id.ib_brush)
        ib_brush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        val ibGallery: ImageButton = findViewById(R.id.ib_gallary)
        ibGallery.setOnClickListener {

            requestPermission()

        }

        val ibUndo: ImageButton = findViewById(R.id.ib_undo)
        ibUndo.setOnClickListener {

            drawingView?.onClickUndo()


        }
        val saveBtn: ImageButton = findViewById(R.id.saveImage)
        saveBtn.setOnClickListener {
            if (isReadStorageAllowed()) {
                showProgressDialog()
                lifecycleScope.launch {
                    val flDrawingView: FrameLayout = findViewById(R.id.fl_drawing_view_container)
                    val myBitmap: Bitmap = getBitmapFromView(flDrawingView)
                    saveBitmapFile(myBitmap)
                }

            }
        }
    }
    private fun showBrushSizeChooserDialog(){
        var brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("BrushSize: ")
        val smallBtn : ImageButton = brushDialog.findViewById(R.id.ib_small_brush)
        smallBtn.setOnClickListener{
            drawingView?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }
        val mediumBtn : ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
       mediumBtn.setOnClickListener{
            drawingView?.setSizeForBrush(17.toFloat())
            brushDialog.dismiss()
        }
        val largeBtn : ImageButton = brushDialog.findViewById(R.id.ib_large_brush)
        largeBtn.setOnClickListener{
            drawingView?.setSizeForBrush(35.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()

    }
    fun paintClicked(view: View){
        if (view !== mImageButtonCurrentpaint){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setColor(colorTag)


           imageButton.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_pressed))
            mImageButtonCurrentpaint?.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_normal))
            mImageButtonCurrentpaint = view

        }
    }


    private fun requestPermission(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)){
            showRationaleDialog("Kids Drawing App","Kids drawning app"+"Need to Access your External Storage")

        }else{
            requestPermission.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE

            ))
        }
    }


    private fun showRationaleDialog(
        title: String,
        message: String
    ) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()

    }

    private fun  isReadStorageAllowed():Boolean {
        val result =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

        return result == PackageManager.PERMISSION_GRANTED
    }


    private fun getBitmapFromView(view: View) : Bitmap{
        val returnBitMap = Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
    val canvas = Canvas(returnBitMap)
        val bgDrawable = view.background
        if (bgDrawable != null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return returnBitMap
    }
    private suspend fun saveBitmapFile(mBitmap: Bitmap?):String {
        var result = ""
        withContext(Dispatchers.IO) {
            if (mBitmap != null)
                    try {
                        val bytes = ByteArrayOutputStream()
                        mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                        val f = File(
                            externalCacheDir?.absoluteFile.toString() + File.separator + "KidsDrawingApp_" +
                                    System.currentTimeMillis() / 1000 + ".png"
                        )

                        val fo = FileOutputStream(f)
                        fo.write(bytes.toByteArray())
                        fo.close()

                        result = f.absolutePath

                        runOnUiThread {
                            cancelProgressDialog()
                            if (result.isNotEmpty()) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "File SAve Successfully : $result",
                                    Toast.LENGTH_SHORT
                                ).show()
                                shareImage(result)
                            } else {
                                Toast.makeText(
                                    this@MainActivity,
                                    "File not Saved !!!!",
                                    Toast.LENGTH_SHORT
                                ).show()

                            }
                        }

                    }
                    catch (e: Exception) {
                        result = ""
                        e.printStackTrace()
                    }
                }
            return result
        }
    private fun showProgressDialog(){
        customProgressDialog = Dialog(this@MainActivity)
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)
        customProgressDialog?.show()
    }
    private fun cancelProgressDialog(){
        if (customProgressDialog != null){
            customProgressDialog?.dismiss()
            customProgressDialog = null


        }
    }
    private fun shareImage(result : String){
        MediaScannerConnection.scanFile(this, arrayOf(result),null){
            path,uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM,uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent,"Share"))
        }
    }

    }
