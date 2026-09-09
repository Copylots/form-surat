package com.example.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.LetterDraft
import com.example.data.LetterDraftRepository
import com.example.util.PdfDocumentGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

sealed interface ExportState {
    object Idle : ExportState
    object Loading : ExportState
    data class Success(val uri: Uri, val fileName: String) : ExportState
    data class Error(val message: String) : ExportState
}

class LetterViewModel(private val repository: LetterDraftRepository) : ViewModel() {

    // List of saved drafts from Room
    val allDrafts: StateFlow<List<LetterDraft>> = repository.allDrafts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Form inputs state
    var currentDraftId by mutableStateOf<Int?>(null)
    var templateName by mutableStateOf("")
    var header by mutableStateOf("KOP SURAT (HEADER)\nNAMA ORGANISASI / LEMBAGA\nAlamat Lengkap, No. Telp, Email")
    var title by mutableStateOf("JUDUL DOKUMEN")
    var nomor by mutableStateOf("")
    var lampiran by mutableStateOf("-")
    var perihal by mutableStateOf("")
    var alignment by mutableStateOf("Justify")
    var bodyText by mutableStateOf("Isi Surat:\nDengan ini kami menyampaikan...")
    var ketuaNama by mutableStateOf("")
    var sekretarisNama by mutableStateOf("")
    var usePj by mutableStateOf(true)
    var pjNama by mutableStateOf("")
    var footerText by mutableStateOf("Isi Footer / Catatan Kaki")
    var logoPath by mutableStateOf<String?>(null)

    // Signature drawn paths (strokes)
    val ketuaStrokes = mutableStateListOf<List<Offset>>()
    val sekretarisStrokes = mutableStateListOf<List<Offset>>()
    val pjStrokes = mutableStateListOf<List<Offset>>()

    // Paths of saved signature images
    var savedSigKetuaPath by mutableStateOf<String?>(null)
    var savedSigSekretarisPath by mutableStateOf<String?>(null)
    var savedSigPjPath by mutableStateOf<String?>(null)

    // PDF generation status
    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }

    // Set form state from a draft
    fun loadDraft(draft: LetterDraft) {
        currentDraftId = draft.id
        templateName = draft.templateName
        header = draft.header
        title = draft.title
        nomor = draft.nomor
        lampiran = draft.lampiran
        perihal = draft.perihal
        alignment = draft.alignment
        bodyText = draft.body
        ketuaNama = draft.ketuaNama
        sekretarisNama = draft.sekretarisNama
        usePj = draft.usePj
        pjNama = draft.pjNama
        footerText = draft.footerText
        logoPath = draft.logoPath
        
        savedSigKetuaPath = draft.sigKetuaPath
        savedSigSekretarisPath = draft.sigSekretarisPath
        savedSigPjPath = draft.sigPjPath

        // Clear active strokes (since old sigs are saved in files)
        ketuaStrokes.clear()
        sekretarisStrokes.clear()
        pjStrokes.clear()
    }

    // Reset form to defaults
    fun createNewForm() {
        currentDraftId = null
        templateName = ""
        header = "KOP SURAT (HEADER)\nNAMA ORGANISASI / LEMBAGA\nAlamat Lengkap, No. Telp, Email"
        title = "JUDUL DOKUMEN"
        nomor = ""
        lampiran = "-"
        perihal = ""
        alignment = "Justify"
        bodyText = "Isi Surat:\nDengan ini kami menyampaikan..."
        ketuaNama = ""
        sekretarisNama = ""
        usePj = true
        pjNama = ""
        footerText = "Isi Footer / Catatan Kaki"
        logoPath = null
        
        savedSigKetuaPath = null
        savedSigSekretarisPath = null
        savedSigPjPath = null

        ketuaStrokes.clear()
        sekretarisStrokes.clear()
        pjStrokes.clear()
    }

    // Save current form state as draft to database
    fun saveDraft(context: Context, name: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val templateToSave = name.ifBlank { "Draf Surat ${System.currentTimeMillis()}" }
            
            // Save active signature drawing canvases if any strokes are present
            val kPath = saveStrokeToInternalFile(context, ketuaStrokes, "sig_ketua_${System.currentTimeMillis()}.png") ?: savedSigKetuaPath
            val sPath = saveStrokeToInternalFile(context, sekretarisStrokes, "sig_sekretaris_${System.currentTimeMillis()}.png") ?: savedSigSekretarisPath
            val pjPath = if (usePj) {
                saveStrokeToInternalFile(context, pjStrokes, "sig_pj_${System.currentTimeMillis()}.png") ?: savedSigPjPath
            } else null

            val draft = LetterDraft(
                id = currentDraftId ?: 0,
                templateName = templateToSave,
                header = header,
                title = title,
                nomor = nomor,
                lampiran = lampiran,
                perihal = perihal,
                alignment = alignment,
                body = bodyText,
                ketuaNama = ketuaNama,
                sekretarisNama = sekretarisNama,
                usePj = usePj,
                pjNama = pjNama,
                footerText = footerText,
                logoPath = logoPath,
                sigKetuaPath = kPath,
                sigSekretarisPath = sPath,
                sigPjPath = pjPath,
                lastModified = System.currentTimeMillis()
            )

            val insertedId = repository.insertDraft(draft)
            if (currentDraftId == null) {
                currentDraftId = insertedId.toInt()
            }
            
            // Refresh signature paths
            savedSigKetuaPath = kPath
            savedSigSekretarisPath = sPath
            savedSigPjPath = pjPath
            
            // Clear current canvas strokes since they are now part of the saved file path
            ketuaStrokes.clear()
            sekretarisStrokes.clear()
            pjStrokes.clear()

            onComplete()
        }
    }

    // Delete draft
    fun deleteDraft(draftId: Int) {
        viewModelScope.launch {
            repository.deleteDraftById(draftId)
            if (currentDraftId == draftId) {
                createNewForm()
            }
        }
    }

    // Generate PDF and save
    fun exportToPdf(context: Context) {
        _exportState.value = ExportState.Loading
        viewModelScope.launch {
            try {
                // Ensure current inputs are saved to files first
                val kPath = saveStrokeToInternalFile(context, ketuaStrokes, "sig_ketua_export.png") ?: savedSigKetuaPath
                val sPath = saveStrokeToInternalFile(context, sekretarisStrokes, "sig_sekretaris_export.png") ?: savedSigSekretarisPath
                val pjPath = if (usePj) {
                    saveStrokeToInternalFile(context, pjStrokes, "sig_pj_export.png") ?: savedSigPjPath
                } else null

                // Verify signatures exist (warning or blank placeholder is okay, but Kivy had them)
                // We generate a blank placeholder bitmap if not signed, to avoid crash.
                val finalKPath = kPath ?: createBlankSignatureFile(context, "blank_k.png")
                val finalSPath = sPath ?: createBlankSignatureFile(context, "blank_s.png")
                val finalPjPath = if (usePj) {
                    pjPath ?: createBlankSignatureFile(context, "blank_pj.png")
                } else null

                val pdfGenerator = PdfDocumentGenerator(context)
                val sanitizedPerihal = perihal.replace("\\s+".toRegex(), "_").ifBlank { "Dokumen" }
                val outFileName = "Surat_$sanitizedPerihal.pdf"

                val uri = pdfGenerator.generateLetterPdf(
                    header = header,
                    title = title,
                    nomor = nomor,
                    lampiran = lampiran,
                    perihal = perihal,
                    alignment = alignment,
                    bodyText = bodyText,
                    ketuaNama = ketuaNama,
                    sekretarisNama = sekretarisNama,
                    usePj = usePj,
                    pjNama = pjNama,
                    footerText = footerText,
                    logoPathOrUri = logoPath,
                    sigKetuaPath = finalKPath,
                    sigSekretarisPath = finalSPath,
                    sigPjPath = finalPjPath,
                    fileName = outFileName
                )

                if (uri != null) {
                    _exportState.value = ExportState.Success(uri, outFileName)
                } else {
                    _exportState.value = ExportState.Error("Gagal merender PDF Dokumen.")
                }

            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Terjadi kesalahan saat mengekspor PDF.")
            }
        }
    }

    private fun saveStrokeToInternalFile(context: Context, strokesList: List<List<Offset>>, filename: String): String? {
        if (strokesList.isEmpty()) return null
        return try {
            // High fidelity signature bitmap (400x150)
            val width = 400
            val height = 150
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)

            // White Background for PDF
            canvas.drawColor(android.graphics.Color.WHITE)

            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                strokeWidth = 5f
                style = android.graphics.Paint.Style.STROKE
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeJoin = android.graphics.Paint.Join.ROUND
                isAntiAlias = true
            }

            // We need to scale the canvas offsets (drawn on a smaller/larger Compose view) to fit our 400x150 signature bitmap.
            // First find bounds of drawn strokes, or use relative view bounds if available.
            // Since Compose Signature Pad will have its own width/height in UI, let's assume we map coordinate space.
            // To be robust, let's map based on actual strokes bounds.
            var minX = Float.MAX_VALUE
            var maxX = Float.MIN_VALUE
            var minY = Float.MAX_VALUE
            var maxY = Float.MIN_VALUE

            for (stroke in strokesList) {
                for (pt in stroke) {
                    if (pt.x < minX) minX = pt.x
                    if (pt.x > maxX) maxX = pt.x
                    if (pt.y < minY) minY = pt.y
                    if (pt.y > maxY) maxY = pt.y
                }
            }

            val strokeWidthVal = maxX - minX
            val strokeHeightVal = maxY - minY

            val scaleX = if (strokeWidthVal > 0) (width - 40f) / strokeWidthVal else 1f
            val scaleY = if (strokeHeightVal > 0) (height - 40f) / strokeHeightVal else 1f
            val scale = minOf(scaleX, scaleY).coerceIn(0.1f, 5.0f)

            // Draw paths centered
            val offsetX = 20f - minX * scale
            val offsetY = 20f - minY * scale

            for (stroke in strokesList) {
                if (stroke.size > 1) {
                    val path = android.graphics.Path()
                    path.moveTo(stroke[0].x * scale + offsetX, stroke[0].y * scale + offsetY)
                    for (i in 1 until stroke.size) {
                        path.lineTo(stroke[i].x * scale + offsetX, stroke[i].y * scale + offsetY)
                    }
                    canvas.drawPath(path, paint)
                } else if (stroke.size == 1) {
                    canvas.drawCircle(stroke[0].x * scale + offsetX, stroke[0].y * scale + offsetY, 2.5f, paint)
                }
            }

            val file = File(context.filesDir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun createBlankSignatureFile(context: Context, filename: String): String {
        val file = File(context.filesDir, filename)
        if (!file.exists()) {
            val bitmap = Bitmap.createBitmap(400, 150, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()
        }
        return file.absolutePath
    }
}

class LetterViewModelFactory(private val repository: LetterDraftRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LetterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LetterViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
