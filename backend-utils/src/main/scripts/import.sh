#/bin/bash

if [ $# -eq 0 ]
  then
    echo "Der Port der Datenbank, auf die importiert werden soll, muss angegeben werden (Login-Daten müssen weiterhin in der .my.cnf angegeben werden)."
    exit 1
fi

echo "Beginne Datenimport, Port: $1"

mysql irpsim -P $1 -e "drop table series_data";
mysql irpsim -P $1 -e "drop table series_metadata";
mysql irpsim -P $1 -e "CREATE TABLE series_data ( ap varchar(255) comment 'lookup' DEFAULT NULL, value double DEFAULT NULL, unixtimestamp bigint(20) DEFAULT NULL ) ENGINE=BRIGHTHOUSE"
mysql irpsim -P $1 -e "CREATE TABLE series_metadata (seriesname varchar(100) COLLATE latin1_bin NOT NULL, creation date DEFAULT NULL, description text COLLATE latin1_bin, unit varchar(100) COLLATE latin1_bin DEFAULT NULL, year int(11) DEFAULT NULL, isSynthetic tinyint(1) DEFAULT NULL, type varchar(100) COLLATE latin1_bin DEFAULT NULL, source varchar(100) COLLATE latin1_bin DEFAULT NULL,intervall varchar(100) COLLATE latin1_bin DEFAULT NULL, isTestdata tinyint(1) DEFAULT '0',  PRIMARY KEY (seriesname)) ENGINE=MyISAM";

echo "Tabellenerstellung abgeschlossen, Beginne Smart-Meter-Datenimport"

#for i in {0..9};
#  do
#   DATEI="/var/lib/import/SM_"$i".csv"
#   echo "I: $i Datei: $DATEI"
#   mysql irpsim -e "load data infile '$DATEI' into table series_data fields terminated by ';' lines terminated by '\n'"
#done
echo "Beginne RLM-Daten-Import"
mysql irpsim -P $1 -e "load data infile '/var/lib/import/STOM_LASTGÄNGE_2013.csv.out' into table series_data fields terminated by ';' lines terminated by '\n'"

echo "Beginne Metadatendefinition für RLM-Daten und SmartMeter"
mysql irpsim -P $1 -e "INSERT INTO series_metadata(seriesname, creation, description,year) SELECT DISTINCT ap, NOW(), ap, 2013 FROM series_data;"

echo "Lade Excel-Daten"
mysql irpsim -P $1 -e "load data infile '/var/lib/import/importall.csv' into table series_data fields terminated by ';' lines terminated by '\n'"

echo "Importiere Metameta- und Metadaten für Daten aus Excel-Dateien"
mysql irpsim -P $1 < MetadataDump.sql

