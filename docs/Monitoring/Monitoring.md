Um den Zustand eines in Betrieb befindlichen Systems zu überwachen, gibt es verschiedene Möglichkeiten. Dieses Dokument definiert die einzelnen Möglichkeiten, um diese schneller wieder nutzbar zu machen.

1. Logging

-> TODO dokumentieren syslog-ng + logspout 

2. JVisualvm

Um mittels JVisualVM eine Verbindung aufzubauen

3. JConsole

Mittels JConsole kann über eine JMX-Verbindung der grundlegende Status eines Backend-Prozesses festgestellt werden. Darüber hinaus können mittels Management-Beans einzelne Einstellungen eines laufenden Backends verändert werden. Dies ist insbesondere relevant, um an laufenden Prozessen Log-Level zu verstellen und damit ohne einen Neustart detailiertere Informationen über laufende Prozesse zu gewinnen.

Um eine Verbindung aufgebaut zu werden, muss mittels ssh eine Verbindung für dynamisches Port-Forwarding mittels des SOCKS-Protokolls augebaut werden:

ssh -D 9696 irpsim.uni-leipzig.de

und die Verbindung für den Port, über den die eigentliche JMX-Verbindung erfolgen soll, muss aufgebaut werden, für das develop-System mit dem Port 1898 also bspw:

ssh -L 1898:irpsim:1898 irpsim

Anschließend kann die Verbindung zu dem jeweiligen System über den JMX-Port des Systems aufgebaut werden:

jconsole -J-DsocksProxyHost=localhost -J-DsocksProxyPort=9696 service:jmx:rmi:///jndi/rmi://localhost:1898/jmxrmi

Danach kann über MBeans -> org.apache.logging.log4j2 -> Hashwert -> Loggers -> Loggerauswahl -> Attributes -> Level das Loglevel verstellt werden.

-> Verbindung zum Produktivserver?!
