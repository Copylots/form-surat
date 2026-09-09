package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.LetterDraft
import com.example.ui.components.SignaturePad
import com.example.viewmodel.ExportState
import com.example.viewmodel.LetterViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFormScreen(
    viewModel: LetterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val drafts by viewModel.allDrafts.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()

    var showSaveDialog by remember { mutableStateOf(false) }
    var draftNameInput by remember { mutableStateOf("") }
    var confirmDeleteDraft by remember { mutableStateOf<LetterDraft?>(null) }

    // Section states for Accordion collapse/expand
    var kopExpanded by remember { mutableStateOf(true) }
    var metaExpanded by remember { mutableStateOf(true) }
    var isiExpanded by remember { mutableStateOf(true) }
    var ttdExpanded by remember { mutableStateOf(true) }
    var pjExpanded by remember { mutableStateOf(false) }
    var footerExpanded by remember { mutableStateOf(false) }

    // Media Picker for Kop Logo
    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val localPath = copyUriToInternalFile(context, uri, "logo_kop.png")
            if (localPath != null) {
                viewModel.logoPath = localPath
                Toast.makeText(context, "Logo berhasil dimuat.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Gagal memproses gambar logo.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Scroll State for entire form
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Pembuat Surat & Proposal",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.createNewForm()
                            Toast.makeText(context, "Formulir baru dikosongkan.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("new_form_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddCircleOutline,
                            contentDescription = "Buat Baru",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        bottomBar = {
            // Elegant fixed actions bar
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            draftNameInput = viewModel.perihal.ifBlank { viewModel.title }.ifBlank { "" }
                            showSaveDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("save_draft_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simpan Draf", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.exportToPdf(context)
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(50.dp)
                            .testTag("export_pdf_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cetak PDF", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Horizontal scroll list of saved drafts (if any)
            if (drafts.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                        .padding(vertical = 10.dp)
                ) {
                    Text(
                        text = "Draf Tersimpan (${drafts.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(drafts, key = { it.id }) { draft ->
                            DraftCard(
                                draft = draft,
                                isActive = viewModel.currentDraftId == draft.id,
                                onLoad = { viewModel.loadDraft(draft) },
                                onDelete = { confirmDeleteDraft = draft }
                            )
                        }
                    }
                }
            }

            // Scrollable forms list
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section 1: Kop & Judul
                FormSectionCard(
                    title = "Kop & Judul Surat",
                    icon = Icons.Outlined.Business,
                    isExpanded = kopExpanded,
                    onToggle = { kopExpanded = !kopExpanded }
                ) {
                    // Logo Picker Component
                    Text(
                        "Logo Kop Surat (Opsional)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    LogoPickerBox(
                        logoPath = viewModel.logoPath,
                        onPick = {
                            logoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onClear = {
                            viewModel.logoPath = null
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = viewModel.header,
                        onValueChange = { viewModel.header = it },
                        label = { Text("KOP SURAT (HEADER)") },
                        placeholder = { Text("Masukkan instansi, alamat, kontak...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("header_input"),
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = viewModel.title,
                        onValueChange = { viewModel.title = it },
                        label = { Text("JUDUL DOKUMEN") },
                        placeholder = { Text("Contoh: SURAT TUGAS, PROPOSAL") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("title_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Section 2: Identitas Surat (Meta)
                FormSectionCard(
                    title = "Identitas Dokumen (Meta)",
                    icon = Icons.Outlined.Info,
                    isExpanded = metaExpanded,
                    onToggle = { metaExpanded = !metaExpanded }
                ) {
                    OutlinedTextField(
                        value = viewModel.nomor,
                        onValueChange = { viewModel.nomor = it },
                        label = { Text("NOMOR SURAT") },
                        placeholder = { Text("Contoh: 012/ST-ORG/IX/2026") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("nomor_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = viewModel.lampiran,
                        onValueChange = { viewModel.lampiran = it },
                        label = { Text("LAMPIRAN") },
                        placeholder = { Text("Contoh: 1 Lembar, -") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lampiran_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = viewModel.perihal,
                        onValueChange = { viewModel.perihal = it },
                        label = { Text("PERIHAL / HAL") },
                        placeholder = { Text("Contoh: Permohonan Dana Kegiatan") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("perihal_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Section 3: Isi Surat & Alignment
                FormSectionCard(
                    title = "Isi Surat & Format",
                    icon = Icons.Outlined.FormatAlignLeft,
                    isExpanded = isiExpanded,
                    onToggle = { isiExpanded = !isiExpanded }
                ) {
                    Text(
                        "Perataan Teks Isi Surat",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    AlignmentSelector(
                        selectedAlignment = viewModel.alignment,
                        onSelect = { viewModel.alignment = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = viewModel.bodyText,
                        onValueChange = { viewModel.bodyText = it },
                        label = { Text("ISI SURAT / KONTEN") },
                        placeholder = { Text("Tulis isi surat di sini...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("body_input"),
                        minLines = 8,
                        shape = RoundedCornerShape(12.dp),
                        helperText = "Gunakan titik dua (:) untuk meratakan teks list, e.g. Hari : Senin"
                    )
                }

                // Section 4: Tanda Tangan Pengurus
                FormSectionCard(
                    title = "Tanda Tangan Pengurus",
                    icon = Icons.Outlined.Draw,
                    isExpanded = ttdExpanded,
                    onToggle = { ttdExpanded = !ttdExpanded }
                ) {
                    OutlinedTextField(
                        value = viewModel.ketuaNama,
                        onValueChange = { viewModel.ketuaNama = it },
                        label = { Text("NAMA KETUA") },
                        placeholder = { Text("Contoh: Ahmad Subardjo") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ketua_nama_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SignaturePad(
                        strokes = viewModel.ketuaStrokes,
                        savedImagePath = viewModel.savedSigKetuaPath,
                        label = "Tanda Tangan Ketua",
                        onClear = {
                            viewModel.ketuaStrokes.clear()
                            viewModel.savedSigKetuaPath = null
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = viewModel.sekretarisNama,
                        onValueChange = { viewModel.sekretarisNama = it },
                        label = { Text("NAMA SEKRETARIS") },
                        placeholder = { Text("Contoh: Siti Rahma") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sekretaris_nama_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SignaturePad(
                        strokes = viewModel.sekretarisStrokes,
                        savedImagePath = viewModel.savedSigSekretarisPath,
                        label = "Tanda Tangan Sekretaris",
                        onClear = {
                            viewModel.sekretarisStrokes.clear()
                            viewModel.savedSigSekretarisPath = null
                        }
                    )
                }

                // Section 5: Penanggung Jawab
                FormSectionCard(
                    title = "Penanggung Jawab (Opsional)",
                    icon = Icons.Outlined.AssignmentInd,
                    isExpanded = pjExpanded,
                    onToggle = { pjExpanded = !pjExpanded }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Gunakan Penanggung Jawab",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Tampilkan kolom tanda tangan Mengetahui / PJ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = viewModel.usePj,
                            onCheckedChange = { viewModel.usePj = it },
                            modifier = Modifier.testTag("use_pj_switch")
                        )
                    }

                    AnimatedVisibility(
                        visible = viewModel.usePj,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            OutlinedTextField(
                                value = viewModel.pjNama,
                                onValueChange = { viewModel.pjNama = it },
                                label = { Text("NAMA PENANGGUNG JAWAB") },
                                placeholder = { Text("Contoh: Prof. Dr. Hermawan") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pj_nama_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            SignaturePad(
                                strokes = viewModel.pjStrokes,
                                savedImagePath = viewModel.savedSigPjPath,
                                label = "Tanda Tangan Penanggung Jawab",
                                onClear = {
                                    viewModel.pjStrokes.clear()
                                    viewModel.savedSigPjPath = null
                                }
                            )
                        }
                    }
                }

                // Section 6: Footer
                FormSectionCard(
                    title = "Isi Catatan Kaki (Footer)",
                    icon = Icons.Outlined.Notes,
                    isExpanded = footerExpanded,
                    onToggle = { footerExpanded = !footerExpanded }
                ) {
                    OutlinedTextField(
                        value = viewModel.footerText,
                        onValueChange = { viewModel.footerText = it },
                        label = { Text("ISI FOOTER") },
                        placeholder = { Text("Contoh: Sekretariat Panitia Pelaksana, Tembusan...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("footer_input"),
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Modal Progress & Success/Error Dialog for PDF Export
    ExportStatusDialog(
        state = exportState,
        onDismiss = { viewModel.resetExportState() }
    )

    // Save Draft Dialog
    if (showSaveDialog) {
        Dialog(onDismissRequest = { showSaveDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Simpan sebagai Draf",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        "Masukkan nama draf/template ini agar mudah dikenali nanti.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = draftNameInput,
                        onValueChange = { draftNameInput = it },
                        placeholder = { Text("Contoh: Surat Undangan Rapat") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("draft_name_input_field"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showSaveDialog = false }) {
                            Text("Batal")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.saveDraft(context, draftNameInput) {
                                    Toast.makeText(context, "Draf berhasil disimpan.", Toast.LENGTH_SHORT).show()
                                    showSaveDialog = false
                                }
                            },
                            modifier = Modifier.testTag("draft_confirm_save_button")
                        ) {
                            Text("Simpan")
                        }
                    }
                }
            }
        }
    }

    // Delete Draft Confirmation Dialog
    if (confirmDeleteDraft != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteDraft = null },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Hapus Draf?") },
            text = { Text("Apakah Anda yakin ingin menghapus draf '${confirmDeleteDraft?.templateName}'? Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDeleteDraft?.id?.let { viewModel.deleteDraft(it) }
                        confirmDeleteDraft = null
                        Toast.makeText(context, "Draf berhasil dihapus.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteDraft = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

// Collapsible Form Section Card (Accordion)
@Composable
fun FormSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row (Clickable to Toggle Collapse)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = content
                )
            }
        }
    }
}

// Styled OutlinedTextField that has custom Helper / Info Label at the bottom
@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable (() -> Unit),
    placeholder: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1,
    shape: androidx.compose.ui.graphics.Shape = OutlinedTextFieldDefaults.shape,
    helperText: String? = null
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            placeholder = placeholder,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = if (singleLine) 1 else Int.MAX_VALUE,
            shape = shape,
            modifier = Modifier.fillMaxWidth()
        )
        if (!helperText.isNullOrEmpty()) {
            Text(
                text = helperText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                fontStyle = FontStyle.Italic
            )
        }
    }
}

// Styled Alignment Selector using Segmented Row
@Composable
fun AlignmentSelector(
    selectedAlignment: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        Triple("Left", "Kiri", Icons.Outlined.FormatAlignLeft),
        Triple("Center", "Tengah", Icons.Outlined.FormatAlignCenter),
        Triple("Right", "Kanan", Icons.Outlined.FormatAlignRight),
        Triple("Justify", "Rata Kiri-Kanan", Icons.Outlined.FormatAlignJustify)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (key, name, icon) ->
            val isSelected = selectedAlignment == key
            Surface(
                onClick = { onSelect(key) },
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("align_btn_$key")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = name, modifier = Modifier.size(16.dp))
                        Text(name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// Styled Card to pick and preview Kop Logo image
@Composable
fun LogoPickerBox(
    logoPath: String?,
    onPick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val logoBitmap = remember(logoPath) {
        if (!logoPath.isNullOrEmpty()) {
            val file = File(logoPath)
            if (file.exists()) {
                BitmapFactory.decodeFile(logoPath).asImageBitmap()
            } else null
        } else null
    }

    if (logoBitmap != null) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            bitmap = logoBitmap,
                            contentDescription = "Logo Kop",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            contentScale = ContentScale.Inside
                        )
                    }

                    Column {
                        Text(
                            "Logo Terpilih",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Akan dicetak di kiri kop surat",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(onClick = onPick) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Ganti Logo", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onClear) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Hapus Logo", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    RoundedCornerShape(12.dp)
                )
                .clickable { onPick() }
                .testTag("logo_picker_box"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    "PILIH LOGO KOP SURAT",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Saved Draft Horizontal Card Component
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraftCard(
    draft: LetterDraft,
    isActive: Boolean,
    onLoad: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(160.dp)
            .combinedClickable(
                onClick = onLoad,
                onLongClick = onDelete
            )
            .testTag("draft_card_${draft.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (isActive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = draft.templateName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Text(
                text = draft.perihal.ifBlank { draft.title }.ifBlank { "Draf Tanpa Judul" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(32.dp)
            )

            val dateStr = remember(draft.lastModified) {
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                sdf.format(Date(draft.lastModified))
            }
            Text(
                text = dateStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                maxLines = 1
            )
        }
    }
}

// Modal Dialog to display Export Progress, Success, or Errors
@Composable
fun ExportStatusDialog(
    state: ExportState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    when (state) {
        ExportState.Idle -> { /* Do nothing */ }
        ExportState.Loading -> {
            Dialog(onDismissRequest = { /* Prevent dismiss while loading */ }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 6.dp,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            "Sedang mencetak PDF...",
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
        is ExportState.Success -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                },
                title = { Text("PDF Berhasil Dicetak!") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Berkas tersimpan di folder Download/Hasil_Surat:")
                        Text(
                            state.fileName,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            sharePdf(context, state.uri, state.fileName)
                        },
                        modifier = Modifier.testTag("share_pdf_dialog_button")
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bagikan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Tutup")
                    }
                }
            )
        }
        is ExportState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                },
                title = { Text("Cetak PDF Gagal") },
                text = { Text(state.message) },
                confirmButton = {
                    Button(onClick = onDismiss) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

// Helpers to copy chosen URI contents into internal storage
fun copyUriToInternalFile(context: Context, uri: Uri, filename: String): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.filesDir, filename)
        FileOutputStream(file).use { out ->
            inputStream?.copyTo(out)
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Helper to share generated PDF via implicit ACTION_SEND Intent
fun sharePdf(context: Context, uri: Uri, fileName: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Dokumen Surat PDF"))
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal membagikan file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
