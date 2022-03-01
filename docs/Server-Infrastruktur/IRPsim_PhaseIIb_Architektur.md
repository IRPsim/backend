# IRPsim Architektur für Phase IIb

## Überblick der Architektur-Komponenten
<img src="./IRPsim_PhaseIIb_Architektur_3.svg" alt="Architektur-Komponenten" style="width: 90%;"/>

- Docker-Container
  - Backend (RESTful-Services)
  - Frontend (Webserver+Webanwendung)
    - ui-client
    - ui-kundengruppen-dashboard
    - ui-parameter-dashboard
    - ui-daten-dashboard
  - Infobright DB (Zeitreihen)
  - GAMS (Wirtschaftssimulation)
- Jenkins-Master (Automatisierter Build-Prozess)
- LDAP (Verzeichnisdienst)
- Docker-Compose
- Docker-Registry
- GIT (Sourcecode-Repository)
- Ansible
- Vagrant

## Systemvoraussetzung
- Linux-Betriebssystem (z.B. Ubuntu, Debian) mit SSH-Zugang

## Beschreibung der einzelnen Komponenten

#### Docker

- Anwendungen werden in Containern isoliert 
  - Container (Betriebssystemvirtualisierung)
  - Image (portable Abbildung eines Containers)
- Konfiguration einer Image über das Dockefile anpassbar
- Vereinfachte Bereitstellung (über Docker Hub/Docker Registry)
- Images sind Tag-bar (hilfreich für Releases)
- Ausführung der Images auf dem Docker-Daemon (Host, physikalische Maschine)
- Docker-Compose ist ein Tool zur Orchestrierung und Verlinkung von Docker-Containern
  - Konfiguration über die YAML-Datei
  - Start aller Container über den Befehl "docker-compose up -p irpsim"
- Docker Swarm ermöglichst es, mehrere Docker-Daemons zu einem Cluster zusammenzuschließen

<div style="page-break-after: always;"></div>

#### IRPsim-Docker-Images

- alle Docker-Images besitzen den selben Tag für ein Release
- Verlinkung über Docker-Compose
- Docker-Releases liegen in der selbstgehosteten Docker-Registry

##### Jenkins
- Bereitstellung eines Jenkins-Master-Builds zur Workload-Verteilung auf entfernten Jenkins-Slave-Clients (Docker-Images)
- Installierte Komponenten
  - Jenkins-Master

##### Backend

- Bereitstellung aller RESTful-Services
- Installierte Komponenten
  - Jenkins-Slave
  - Git
  - Java
  - Maven

##### Frontend

- Bereitstellung eines Docker-Containers für jede GUI
  - ui-client
  - ui-kundengruppen-dashboard
  - ui-parameter-dashboard
  - ui-daten-dashboard
- Installierte Komponenten
  - Jenkins-Slave
  - Git
  - Apache Webserver
  - NVM (Node Version Manager) 
    - NPM
    - NODE
  - NPM-Module, u.a.
    - Grunt

##### Infobright DB

- Zeitreihen-Datenbank
- Installierte Komponenten
  - Infobright

<div style="page-break-after: always;"></div>

##### GAMS
- Wirtschaftssimulation
- Installierte Komponenten
  - GAMS

#### LDAP
- Verzeichnisdienst
- Installierte Komponenten
  - sldap
  - phpLdapAdmin

#### Ansible
- Konfigurationsmanagement (Software-Configuration-Management - SCM)
- ermöglicht die Umsetzung einer definierten Zustandbeschreibung für einen oder mehrerer Host
- Zustandsbeschreibungen werden in YAML-Dateien definiert (Ansible Playbook)
- Beispiel für ein Ansible Playbook
  - Installation von Software (Docker, GIT)
  - Installation von Services
  - Setzen von Konfigurationen
  - Erstellung eines IRPsim-Releases (Parameter)
  - Einspielen eines bestimmten IRPsim-Release (Parameter)
- Zugriff auf Host-Systeme erfolgt über SSH, vorzugsweise über SSH-Keys
- Ausführung von Jobs zu bestimmten Zeiten oder manuell per "Knopfdruck"
  - Erstellung oder Einstellen von Releases
- Tower-Dashboard (Web-Anwendung)

#### Vagrant
- Software zum Erstellen und Verwalten von virtuellen Maschinen
  - VMWare/VirtualBox
- Konfiguration der virtuellen Maschine liegt in dem "VAGRANTFILE"
  - Start über "vagrant up"
- Provisioning über Ansible möglich
  - definierte Ansible Playblocks werden nach Start über Vagrant ausgeführt
- OS unabhängig

<div style="page-break-after: always;"></div>

## Vorteile der neuen Archtiektur
- Modularer
  - Auspaltung der Komponenten in diversen Docker-Images
  - Einfacherer Austausch von Software-Komponenten (Einsatz anderer Software)
- besssere Workload-Verteilung beim automatisiertem Build-Prozess
- LDAP-Verzeichnisdienst zur zentralen Benutzer-/Rechteverwaltung
- Auf "Knopfdruck" (Ausführung von Ansible Playbooks)
  - Aufziehen neuer IRPsim-Host-Rechner
  - Erstellung definierter Releases
  - Einspielen/Installation spezifischer Releases