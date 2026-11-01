# Glitchy – Setup-, Test- und Release-Anleitung

> Wichtig vorab: Das Projekt wurde hier **nicht kompiliert oder getestet** (kein Android SDK in meiner Umgebung).
> Erwarte beim ersten Build in Android Studio ein paar kleine Fehler (Importe, Versionen). Ich helfe dir gern, sie zu fixen – schick mir einfach die Fehlermeldung.

## 0. Projekt öffnen

1. Android Studio (aktuelle Version) installieren.
2. Ordner `Glitchy/` öffnen („Open“). Gradle-Sync abwarten.
   - Falls Studio Versions-Updates vorschlägt (AGP, Kotlin, Compose BOM, Supabase), akzeptiere die Vorschläge.
3. `local.properties.example` kopieren nach **`local.properties`** und Werte eintragen (siehe unten). `keystore.properties.example` brauchst du erst für das Release.
4. Es liegt kein Gradle-Wrapper (`gradlew`) im Projekt. Android Studio kümmert sich beim Sync darum; für die Terminal-Befehle weiter unten einmalig `gradle wrapper` ausführen (oder stattdessen in Studio: *Build → Generate Signed App Bundle / APK*).
5. Ein App-Icon ist als einfaches Pixel-Maskottchen enthalten; ein eigenes erzeugst du über *Rechtsklick auf res → New → Image Asset*.

## 1. Supabase einrichten

1. Konto auf https://supabase.com → **New project**. Region: **EU (Frankfurt)** (DSGVO). Starkes Datenbank-Passwort speichern.
2. **SQL Editor → New query** → Inhalt von `supabase/schema.sql` einfügen → **Run**. Das legt Tabellen, Sicherheitsregeln (RLS), Leaderboard-Funktionen, Zertifikats-Funktion, Konto-Löschung und den Storage-Bucket `avatars` an.
3. **Project Settings → API**: `Project URL` und den **anon/public** Key in `local.properties` eintragen (`supabase.url`, `supabase.anon`).
   **Niemals** den `service_role`-Key in die App oder auf GitHub!
4. **Authentication → Providers → Email**: aktiviert lassen.
   - „Confirm email“: empfohlen **an** (die App erklärt dem Nutzer, dass er die Mail bestätigen muss).
   - Passwort-Mindestlänge unter *Authentication → Policies/Sign In* auf 8+ stellen.
5. **Authentication → URL Configuration**: Site URL setzen (z. B. deine Webseite oder `https://localhost`).
6. **E-Mail-Versand**: Der eingebaute Supabase-Versand ist stark limitiert (nur wenige Mails pro Stunde). Für echte Nutzer unter *Authentication → SMTP Settings* einen eigenen SMTP-Dienst eintragen (z. B. Resend, Brevo, Mailgun).
7. **Google-Login** (siehe Abschnitt 2).
8. **Storage** prüfen: Bucket `avatars` existiert, ist public, 1 MB Limit (steht im SQL).
9. **Sicherheit**: *Authentication → Attack Protection*: Captcha/Rate-Limits aktivieren, sobald echte Nutzer kommen.
10. Hinweis: Kostenlose Supabase-Projekte werden bei längerer Inaktivität pausiert – für die Veröffentlichung prüfen, ob du einen bezahlten Plan brauchst (aktuelle Bedingungen bei Supabase nachlesen).

## 2. Google-Anmeldung

1. https://console.cloud.google.com → neues Projekt → **APIs & Services → OAuth consent screen** (External, App-Name, Support-Mail, Datenschutz-URL).
2. **Credentials → Create credentials → OAuth client ID**:
   - Typ **Web application** → Client-ID **und** Secret kopieren.
   - Typ **Android**: Package name `de.fabiobaensch.glitchy` + **SHA-1** deines Zertifikats. Du brauchst je einen Eintrag für:
     - Debug-Key (`./gradlew signingReport` oder Studio → Gradle → app → Tasks → android → signingReport)
     - Release-Key (aus deinem Keystore)
     - **Play App Signing**-Key (SHA-1 siehe Play Console → App-Integrität), falls du über Play veröffentlichst
3. In Supabase: **Authentication → Providers → Google** → aktivieren → **Web**-Client-ID + Secret eintragen. Die Option **„Skip nonce check“** aktivieren (nötig für den nativen Android-Login).
4. Die **Web**-Client-ID kommt auch in `local.properties` (`google.web.client.id`) – nicht die Android-ID.

## 3. Mit Samsung-Handy + PC testen

**Variante A: USB (am einfachsten)**
1. Handy: *Einstellungen → Über das Telefon → Softwareinformationen* → 7× auf **Build-Nummer** tippen → Entwickleroptionen sind aktiv.
2. *Entwickleroptionen → USB-Debugging* einschalten.
3. Per USB an den PC; am Handy „Debugging zulassen“ bestätigen. (Windows: ggf. den Samsung USB Driver installieren.)
4. In Android Studio oben dein Handy auswählen → grüner **Run**-Button.

**Variante B: WLAN** – *Entwickleroptionen → Wireless-Debugging*; in Studio *Device Manager → Pair devices using Wi-Fi*.

**Was du testen solltest**: Erststart-Animation + Vibration, Registrierung (Username belegt?), E-Mail-Bestätigung, Google-Login, Fortschritt/Prozent, Test, Zertifikat-PDF, Daily + Erinnerung (Benachrichtigungs-Erlaubnis!), Leaderboards mit 2 Testkonten, Konto löschen, Deutsch/Englisch (Systemsprache umstellen), Flugmodus (Fehlermeldungen).

## 4. Release als APK / AAB

1. **Keystore erstellen** (einmalig, **mehrfach sichern**, ohne ihn kannst du nie Updates veröffentlichen):
   ```
   keytool -genkeypair -v -keystore glitchy-release.jks -alias glitchy -keyalg RSA -keysize 2048 -validity 10000
   ```
   (oder Studio: *Build → Generate Signed Bundle/APK*).
2. `keystore.properties` aus der Vorlage anlegen und ausfüllen (Datei liegt im Projektordner neben `settings.gradle.kts`, `.jks` ebenfalls).
3. Bauen:
   - APK: `./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`
   - AAB (für Google Play): `./gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`
4. Release-APK **auf dem Handy testen** (Minify/R8 kann Fehler zeigen, die Debug nicht hat – v. a. Supabase/Serialization; Regeln stehen in `proguard-rules.pro`).
5. Bei jedem Update `versionCode` in `app/build.gradle.kts` erhöhen.
6. **Direktes APK-Verteilen** (z. B. GitHub Releases/Webseite): Nutzer müssen „Installation aus unbekannten Quellen“ erlauben. Google Play ist für Nutzer deutlich einfacher und vertrauenswürdiger.
7. **Google Play**: Entwicklerkonto (einmalig ca. 25 USD), neuer App-Eintrag, AAB hochladen, Store-Listing (Screenshots, Beschreibung DE/EN), **Datenschutz-URL**, **Data-Safety-Formular**, Inhaltsbewertung, Zielgruppe (16+), Link/Info zur **Kontolöschung**. Für neue private Entwicklerkonten gelten aktuell Pflicht-Testphasen (geschlossener Test mit mehreren Testern über mehrere Tage) – prüfe die aktuellen Regeln in der Play Console.

## 5. Weitere wichtige Schritte (mein Rat)

**Rechtliches**
- `assets/legal/*.txt` ausfüllen (Name, **ladungsfähige Anschrift**, E-Mail, Datum, Supabase-Region). Das sind Vorlagen und keine Rechtsberatung – einmal von einer Fachperson/Generator (z. B. eRecht24) prüfen lassen.
- Datenschutzerklärung zusätzlich **online** hosten (z. B. GitHub Pages) – Google Play verlangt eine URL.
- Mit Supabase den **AV-Vertrag (DPA)** abschließen (im Dashboard unter Legal/Compliance).
- Alter: Zielgruppe Klasse 11–13 ist teils unter 18, teils unter 16. In der App steht „ab 16“. Wenn du jüngere Nutzer willst, brauchst du Einwilligung der Eltern.
- Impressumspflicht gilt bei geschäftsmäßigen Angeboten (auch „kostenlos mit Ziel Reichweite/Geschäft“) – im Zweifel Impressum drin lassen.
- Cybersecurity-Inhalte: Warnhinweis „nur eigene Systeme“ ist in den Lektionen drin. Halte Aufgaben bei Simulation, keine Anleitungen gegen fremde, reale Systeme. Prüfe die Google-Play-Richtlinien (Malware/„Schädliche Inhalte“).
- Name „Glitchy“ vor dem Start auf Marken (DPMA/EUIPO) und im Play Store prüfen.
- Zertifikate sind **keine** staatlich anerkannten Abschlüsse (steht auf dem PDF).
- Nutzergenerierte Inhalte (Username/Profilbild): „Melden“ (Long-Press im Leaderboard) ist eingebaut → Meldungen im Supabase-Dashboard (`reports`) regelmäßig prüfen und Konten ggf. sperren/löschen.

**Technik / Qualität**
- Die Leaderboards zählen Tage und werden serverseitig über Funktionen geschrieben, aber die **Lösungs-Prüfung läuft in der App**. Ein technisch versierter Nutzer könnte Aktivität fälschen. Für ein Ranking mit Preisen später Prüfung serverseitig machen (Edge Function).
- Das Zertifikat prüft serverseitig nur den Test ≥ 95 %. „Modul 100 %“ prüft die App. Für mehr Fälschungssicherheit später `verify_certificate(id)` über eine kleine Webseite anbieten (Funktion existiert bereits).
- Offline: Aktuell braucht die App Internet für Fortschritt. Später lokal puffern (Room) einbauen.
- Crash-Reports (z. B. Firebase Crashlytics) erst nach Anpassung der Datenschutzerklärung.

**Inhalte (der größte Brocken)**
- 20–30 Stunden pro Modul sind realistisch etwa **150–250 Lektionen** pro Modul plus 45 Testfragen. Die mitgelieferten JSON-Dateien (`assets/content/prog.json`, `math.json`) zeigen das Format mit 8 (Programmierung) + 5 (Mathe) Beispiel-Lektionen. Neue Lektion = neuer Eintrag, kein Code nötig.
- Tipp: Lektionen in Etappen schreiben (zuerst 1 Section komplett), jede Aufgabe selbst testen, Mathe-Lösungen immer gegenprüfen (mehrere Schreibweisen in `answers` eintragen).
- Test-Fragen: Das Feld `finalTest` auf 45 Einträge pro Modul bringen.
- Später: Inhalte aus Supabase laden, damit du ohne App-Update neue Lektionen ausliefern kannst.
