package com.zapp.pdfviewer

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.zapp.pdfviewer.util.hide
import com.zapp.pdfviewer.util.show
import kotlinx.coroutines.CoroutineScope
import java.io.File


class PdfRendererView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var recyclerView: RecyclerView

    private lateinit var pdfRendererCore: PdfRendererCore
    private lateinit var pdfViewAdapter: PdfViewAdapter
    private var quality = PdfQuality.NORMAL
    private var engine = PdfEngine.INTERNAL
    private var showDivider = true
    private var divider: Drawable? = null
    var enableLoadingForPages: Boolean = false

    private var pdfRendererCoreInitialised = false
    var pageMargin: Rect = Rect(0, 0, 0, 0)
    var statusListener: StatusCallBack? = null

    val totalPageCount: Int
        get() {
            return pdfRendererCore.getPageCount()
        }

    interface StatusCallBack {
        fun onDownloadStart() {}
        fun onDownloadProgress(progress: Int, downloadedBytes: Long, totalBytes: Long?) {}
        fun onDownloadSuccess() {}
        fun onError(error: Throwable) {}
        fun onPageChanged(currentPage: Int, totalPage: Int) {}
    }

    fun initWithUrl(
        url: String,
        pdfQuality: PdfQuality = this.quality,
        engine: PdfEngine = this.engine,
        lifecycleScope: LifecycleCoroutineScope = (context as AppCompatActivity).lifecycleScope
    ) {
        val v = LayoutInflater.from(context).inflate(R.layout.pdf_rendererview, this, false)
        addView(v)
        findViewById<ProgressBar>(R.id.progressBar).show()
        PdfDownloader(url, object : PdfDownloader.StatusListener {
            override fun getContext(): Context = context
            override fun onDownloadStart() {
                statusListener?.onDownloadStart()
            }

            override fun onDownloadProgress(currentBytes: Long, totalBytes: Long) {
                var progress = (currentBytes.toFloat() / totalBytes.toFloat() * 100F).toInt()
                if (progress >= 100)
                    progress = 100
                statusListener?.onDownloadProgress(progress, currentBytes, totalBytes)
            }

            override fun onDownloadSuccess(absolutePath: String) {
                initWithPath(absolutePath, pdfQuality)
                statusListener?.onDownloadSuccess()
            }

            override fun onError(error: Throwable) {
                error.printStackTrace()
                findViewById<ProgressBar>(R.id.progressBar).hide()
                findViewById<ProgressBar>(R.id.errorView).show()

                statusListener?.onError(error)
            }

            override fun getCoroutineScope(): CoroutineScope = lifecycleScope
        })
    }

    fun initWithPath(path: String, pdfQuality: PdfQuality = this.quality) {
        initWithFile(File(path), pdfQuality)
    }

    fun initWithFile(file: File, pdfQuality: PdfQuality = this.quality) {
        init(file, pdfQuality)
    }

    private fun init(file: File, pdfQuality: PdfQuality) {
        pdfRendererCore = PdfRendererCore(context, file, pdfQuality)
        pdfRendererCoreInitialised = true
        pdfViewAdapter = PdfViewAdapter(pdfRendererCore, pageMargin, enableLoadingForPages)

        recyclerView = findViewById(R.id.recyclerView)
        findViewById<ProgressBar>(R.id.progressBar).hide()
        recyclerView.apply {
            adapter = pdfViewAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            itemAnimator = DefaultItemAnimator()
            if (showDivider) {
                DividerItemDecoration(context, DividerItemDecoration.VERTICAL).apply {
                    divider?.let { setDrawable(it) }
                }.let { addItemDecoration(it) }
            }
            addOnScrollListener(scrollListener)
        }


    }

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            (recyclerView.layoutManager as LinearLayoutManager).run {
                var foundPosition: Int = findFirstCompletelyVisibleItemPosition()

                if (foundPosition != NO_POSITION) {
                    statusListener?.onPageChanged(foundPosition, totalPageCount)
                    return@run
                }
                foundPosition = findFirstVisibleItemPosition()
                if (foundPosition != NO_POSITION) {
                    statusListener?.onPageChanged(foundPosition, totalPageCount)
                    return@run
                }
            }
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)

        }

    }


    init {
        getAttrs(attrs, defStyleAttr)
    }

    private fun getAttrs(attrs: AttributeSet?, defStyle: Int) {
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.PdfRendererView, defStyle, 0)
        setTypeArray(typedArray)
    }

    private fun setTypeArray(typedArray: TypedArray) {
        val ratio =
            typedArray.getInt(R.styleable.PdfRendererView_pdfView_quality, PdfQuality.NORMAL.ratio)
        quality = PdfQuality.values().first { it.ratio == ratio }
        val engineValue =
            typedArray.getInt(R.styleable.PdfRendererView_pdfView_engine, PdfEngine.INTERNAL.value)
        engine = PdfEngine.values().first { it.value == engineValue }
        showDivider = typedArray.getBoolean(R.styleable.PdfRendererView_pdfView_showDivider, true)
        divider = typedArray.getDrawable(R.styleable.PdfRendererView_pdfView_divider)
        enableLoadingForPages = typedArray.getBoolean(
            R.styleable.PdfRendererView_pdfView_enableLoadingForPages,
            enableLoadingForPages
        )

        val marginDim =
            typedArray.getDimensionPixelSize(R.styleable.PdfRendererView_pdfView_page_margin, 0)
        pageMargin = Rect(marginDim, marginDim, marginDim, marginDim).apply {
            top = typedArray.getDimensionPixelSize(
                R.styleable.PdfRendererView_pdfView_page_marginTop,
                top
            )
            left = typedArray.getDimensionPixelSize(
                R.styleable.PdfRendererView_pdfView_page_marginLeft,
                left
            )
            right = typedArray.getDimensionPixelSize(
                R.styleable.PdfRendererView_pdfView_page_marginRight,
                right
            )
            bottom = typedArray.getDimensionPixelSize(
                R.styleable.PdfRendererView_pdfView_page_marginBottom,
                bottom
            )

        }

        typedArray.recycle()
    }

    fun setNoPdf() {
        val v = LayoutInflater.from(context).inflate(R.layout.pdf_rendererview, this, false)
        addView(v)
        findViewById<ProgressBar>(R.id.errorView).show()
    }

}
