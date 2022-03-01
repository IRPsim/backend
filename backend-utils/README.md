# Import von Exceldaten 

Um Excel-Dateien mit Datenreihen und Metadaten zu importieren, sind folgende Schritte auszuführen:

## 1. Konvertierung

Die Daten müssen in CSV-Dateien konvertiert werden. Dies geschieht mittels `de.unileipzig.irpsim.utils.migration.ExcelMetadataImporter` im Modul backend-utils. Der main-Methode muss als Parameter lediglich der Name der Excel-Datei übergeben werden. Im Standard-Output werden bei der Ausführung Querys ausgegeben, die zum Import der Metadaten auszuführen sind, weiterhin wird eine CSV-Datei erstellt.

## 2. Kopieren

Die CSV-Datei ist bspw. mittels scp auf den Server zu kopieren. Sie sollte dann nach `/var/dataimport` verschoben werden. Dieser Pfad ist im Docker-Container von MySQL unter /var/lib/import verfügbar. 

## 3. Import der Metadaten

Die Metadaten sind zu importieren, indem das, was die Standardausgabe des ExcelMetadataImporter enthielt, in mysql ausgeführt wird.

## 4. Import der Daten

Die Daten, die aus Sicht von MySQl unter `/var/lib/import liegen`, sind zu importieren. Dies geschieht mittels

`LOAD DATA INFILE '/var/lib/import/importall.csv' INTO TABLE series_data FIELDS TERMINATED BY ';' LINES TERMINATED BY '\n';`

##  Anpassung an neue Modellversion 

Um das Modell an eine andere Version anzupassen, sind folgende Schritte auszuführen:

## 1. Auschecken der korrekten Version

In `backend-server/gams/model` liegt ein git-Submodul, dass die Modelle enthält. Mit den üblichen Git-Befehlen sollte es in der Version ausgecheckt liegen, die genutzt werden soll.

## 2. Transformation der Modelldaten

Um die neue Version nutzen zu können, ist die Modelltransformation durchzuführen, die aus dem Modell die für backend notwendigen Informationen ausliest. Dies geschieht durch den Aufruf der main-Methode von `de.unileipzig.irpsim.utils.ModelEnvironmentTransformer`. Die Transformation erfolgt für alle derzeitig vorhandenen Modelle (Eisbatterie, Basismodell). Anschließend sind die Tests in backend-server (mit `mvn test`) auszuführen. Laufen diese korrekt durch, kann die erneuerte Version gepusht werden.

Bei der Transformation der Modelldaten werden folgende Teilaufgaben durchgeführt:

    a) Veränderung des Modell-Quelltextes, so dass er für GAMS lesbar ist (zur Nutzung im backend)
    b) Generierung der Ausgabe-Abhängigkeiten der GAMS-Parameter (zur Nutzung im backend)
    c) Generierung der Modelldefinitionen für die Oberflächengestaltung (zur Nutzung im frontend)
    d) Generierung der Standard-Parametersätze für die Modelle (zur Nutzung in front- und backend)

## 3. Import der neuen Parametersätze

Der Import der neuen Parametersätze passiert in der Regel automatisch beim Start des Servers. Hierbei werden die aus den vorhandenen Excel-Daten extrahierten JSON-Parametersätze importiert. Achtung: Dabei werden die alten Standard-Parametersätze gelöscht.
