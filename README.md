# PDF_Viewer


#build.gradle(:app)
```
dependencies{
 implementation project(":pdfViewer")
}
```


#activity_main.xml
```
              <com.zapp.pdfviewer.PdfRendererView
                android:id="@+id/iv_pdf"
                android:layout_width="match_parent"
                android:layout_height="200dp"
                android:layout_marginHorizontal="16dp"
                android:layout_marginTop="8dp"
                android:background="@drawable/pdf_bg"
                android:padding="4dp"
                app:layout_constraintEnd_toEndOf="parent"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintTop_toTopOf="parent" />
```

#pdf_bg.xml

```
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">

    <corners android:radius="4dp"/>
    <stroke android:color="#dddddd" android:width="1dp"/>
    <solid android:color="#ffffff"/>
</shape>
```

#MainActivty.kt

```
 private fun loadPDF(s: String) {
        binding.download.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(s))
            startActivity(intent)
        }

        binding.ivPdf.initWithUrl(s, PdfQuality.FAST, PdfEngine.INTERNAL, lifecycleScope)


    }

fun View.hide() {
    visibility = View.GONE
}
```

#onCreate()

```
 if (!pdfLink.isNullOrBlank())
            loadPDF(pdfLink)
        else {
            binding.ivPdf.hide()
        }

```
