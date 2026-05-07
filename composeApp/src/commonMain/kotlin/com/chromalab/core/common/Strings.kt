package com.chromalab.core.common

import androidx.compose.runtime.*

/**
 * In-memory string resources for all supported languages.
 * Allows instant language switching without app restart.
 */
object Strings {

    private val currentLanguage = mutableStateOf(AppLanguage.DEFAULT)

    val language: AppLanguage
        @Composable get() = currentLanguage.value

    fun setLanguage(lang: AppLanguage) {
        currentLanguage.value = lang
    }

    // --- Navigation ---
    val appName @Composable get() = s("ChromaLab")
    val tabProjects @Composable get() = s("Проекты", "Projects", "Projets", "Projekte", "Proyectos", "Progetti", "Projekty", "Projekty")
    val tabCapture @Composable get() = s("Съёмка", "Capture", "Capture", "Aufnahme", "Captura", "Cattura", "Zdjęcie", "Snímek")
    val tabCalculations @Composable get() = s("Расчёты", "Calculations", "Calculs", "Berechnungen", "Cálculos", "Calcoli", "Obliczenia", "Výpočty")
    val tabMore @Composable get() = s("Ещё", "More", "Plus", "Mehr", "Más", "Altro", "Więcej", "Více")

    // --- Buttons ---
    val btnNewProject @Composable get() = s("Новый проект", "New Project", "Nouveau projet", "Neues Projekt", "Nuevo proyecto", "Nuovo progetto", "Nowy projekt", "Nový projekt")
    val btnTakePhoto @Composable get() = s("Сфотографировать", "Take Photo", "Prendre une photo", "Foto aufnehmen", "Tomar foto", "Scatta foto", "Zrób zdjęcie", "Vyfotit")
    val btnFromGallery @Composable get() = s("Из галереи", "From Gallery", "Depuis la galerie", "Aus Galerie", "Desde galería", "Dalla galleria", "Z galerii", "Z galerie")
    val btnImportFile @Composable get() = s("Импорт файла", "Import File", "Importer fichier", "Datei importieren", "Importar archivo", "Importa file", "Importuj plik", "Importovat soubor")
    val btnCalculate @Composable get() = s("Рассчитать", "Calculate", "Calculer", "Berechnen", "Calcular", "Calcola", "Oblicz", "Vypočítat")
    val btnExport @Composable get() = s("Экспорт", "Export", "Exporter", "Exportieren", "Exportar", "Esporta", "Eksportuj", "Exportovat")
    val btnSave @Composable get() = s("Сохранить", "Save", "Sauvegarder", "Speichern", "Guardar", "Salva", "Zapisz", "Uložit")
    val btnCancel @Composable get() = s("Отмена", "Cancel", "Annuler", "Abbrechen", "Cancelar", "Annulla", "Anuluj", "Zrušit")
    val btnDelete @Composable get() = s("Удалить", "Delete", "Supprimer", "Löschen", "Eliminar", "Elimina", "Usuń", "Smazat")

    // --- Labels ---
    val labelRetentionTime @Composable get() = s("Время удерживания", "Retention Time", "Temps de rétention", "Retentionszeit", "Tiempo de retención", "Tempo di ritenzione", "Czas retencji", "Retenční čas")
    val labelPeakArea @Composable get() = s("Площадь пика", "Peak Area", "Aire du pic", "Peakfläche", "Área del pico", "Area del picco", "Pole piku", "Plocha píku")
    val labelPeakHeight @Composable get() = s("Высота пика", "Peak Height", "Hauteur du pic", "Peakhöhe", "Altura del pico", "Altezza del picco", "Wysokość piku", "Výška píku")
    val labelSnRatio @Composable get() = s("S/N")
    val labelIonRatio @Composable get() = s("Ионное отношение", "Ion Ratio", "Rapport ionique", "Ionenverhältnis", "Relación iónica", "Rapporto ionico", "Stosunek jonów", "Iontový poměr")

    // --- Status ---
    val statusConfirmed @Composable get() = s("Подтверждено", "Confirmed", "Confirmé", "Bestätigt", "Confirmado", "Confermato", "Potwierdzone", "Potvrzeno")
    val statusDoubtful @Composable get() = s("Сомнительно", "Doubtful", "Douteux", "Zweifelhaft", "Dudoso", "Dubbio", "Wątpliwe", "Pochybné")
    val statusNotConfirmed @Composable get() = s("Не подтверждено", "Not Confirmed", "Non confirmé", "Nicht bestätigt", "No confirmado", "Non confermato", "Niepotwierdzone", "Nepotvrzeno")
    val statusPending @Composable get() = s("Ожидание", "Pending", "En attente", "Ausstehend", "Pendiente", "In attesa", "Oczekuje", "Čeká")

    // --- Quality ---
    val qualityGood @Composable get() = s("Хорошее качество", "Good Quality", "Bonne qualité", "Gute Qualität", "Buena calidad", "Buona qualità", "Dobra jakość", "Dobrá kvalita")
    val qualityMedium @Composable get() = s("Среднее качество", "Medium Quality", "Qualité moyenne", "Mittlere Qualität", "Calidad media", "Qualità media", "Średnia jakość", "Střední kvalita")
    val qualityPoor @Composable get() = s("Низкое качество", "Poor Quality", "Mauvaise qualité", "Schlechte Qualität", "Mala calidad", "Scarsa qualità", "Niska jakość", "Špatná kvalita")

    // --- Empty States ---
    val emptyProjects @Composable get() = s("Нет проектов", "No Projects", "Aucun projet", "Keine Projekte", "Sin proyectos", "Nessun progetto", "Brak projektów", "Žádné projekty")
    val emptySamples @Composable get() = s("Нет образцов", "No Samples", "Aucun échantillon", "Keine Proben", "Sin muestras", "Nessun campione", "Brak próbek", "Žádné vzorky")

    // --- Camera ---
    val hintPlaceChromatogram @Composable get() = s("Поместите хроматограмму в рамку", "Place chromatogram in frame", "Placez le chromatogramme dans le cadre", "Chromatogramm im Rahmen platzieren", "Coloque el cromatograma en el marco", "Posiziona il cromatogramma nel riquadro", "Umieść chromatogram w ramce", "Umístěte chromatogram do rámečku")

    // --- Settings ---
    val settingsLanguage @Composable get() = s("Язык", "Language", "Langue", "Sprache", "Idioma", "Lingua", "Język", "Jazyk")
    val settingsTheme @Composable get() = s("Тема", "Theme", "Thème", "Thema", "Tema", "Tema", "Motyw", "Motiv")
    val settingsAbout @Composable get() = s("О приложении", "About", "À propos", "Über", "Acerca de", "Informazioni", "O aplikacji", "O aplikaci")
    val settingsReports @Composable get() = s("Отчёты", "Reports", "Rapports", "Berichte", "Informes", "Rapporti", "Raporty", "Zprávy")

    // --- Helper ---
    // Order: RU, EN, FR, DE, ES, IT, PL, CS
    @Composable
    private fun s(
        ru: String,
        en: String = ru,
        fr: String = en,
        de: String = en,
        es: String = en,
        it: String = en,
        pl: String = en,
        cs: String = en,
    ): String = when (currentLanguage.value) {
        AppLanguage.RU -> ru
        AppLanguage.EN -> en
        AppLanguage.FR -> fr
        AppLanguage.DE -> de
        AppLanguage.ES -> es
        AppLanguage.IT -> it
        AppLanguage.PL -> pl
        AppLanguage.CS -> cs
    }
}
