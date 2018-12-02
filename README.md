# Real-Time Evaluator for SLAmeter

----------
> - Verzia: 1.0
> - Stav verzie: vyvíjaná
> - Autor: Szilárd Jáger
> - Licencia: GNU GPLv3
> - Implemetačné prostredie: java version "1.7.0_75"
> - GIT: https://git.cnl.sk/monica/slameter_realtime_evaluator

----------
Reálno-časový vyhodnocovač slúži na vyhodnotenie sieťovej prevádzky na základe aktuálnych údajov. [Vyhodnocovač ](https://git.cnl.sk/monica/slameter_evaluator/wikis/home) (jeden z komponentov nástroja SLAmeter) je schopný vyhodnocovať sieťové charakteristiky len na základe historických údajov.

Po spustení aplikácia sa automaticky pripojí ku kanálu ACP a čaká, ak nejaký komponent publikuje na tento kanál, tak sa pripojí ku [Kolektoru](https://git.cnl.sk/monica/slameter_collector/wikis/home) a začne zverejňovať vyhodnotené dáta pre každý jeden komponent zvlášť. Ak niektorý komponent pošle na kanál ACP reťazec vo forme: “menokomponentu.stop“ tak zastaví publikanie ku kanálu pre daný komponent. 

----------

## Implementované komponenty ##
> - **Stratovosť v paketoch** --  Slúži na vypočítanie aktuálnej hodnoty stratovosti vyjadrenej v paketoch. Pracuje na základe informačného elementu droppedPacketDeltaCount definovaný protokolom IPFIX. 
> - **Stratovosť v bajtoch** -- Slúži na vypočítanie aktuálnej hodnoty stratovosti vyjadrenej v bajtoch. Pracuje na základe informačného elementu droppedOctetDeltaCount definovaný protokolom IPFIX. 
> - **Priepustnosť v paketoch** -- Slúži na vypočítanie aktuálnej hodnoty prenesených dát vyjadrených v paketoch. Pracuje na základe informačného elementu packetDeltaCount definovaný protokolom IPFIX. 
> - **Priepustnosť v bajtoch** -- Slúži na vypočítanie aktuálnej hodnoty prenesených dát vyjadrených v bajtoch. Pracujena základe informačného elementu octetDeltaCount definovaný protokolom IPFIX. 

----------

## Preklad zdrojových textov ##

> - pre preklad programu sú potrebné nasledujúce knižnice
>  - ACPAPI1.2.jar
>  - jedis-2.4.2.jar
>  - json-1.0.jar
>  - log4j-1.2.17.jar
>  - org.apache.commons.collections.jar
> - je potrebné stiahnúť zdrojové súbory a potrebné knižnice : [slameter_real_time_evaluator](https://git.cnl.sk/monica/slameter_realtime_evaluator.git)
> - ktorý je možný nainštalovať:

    export CLASSPATH=.:./lib/ACPAPI1.2.jar:./lib/jedis-2.4.2.jar:./lib/json-1.0.jar:./lib/log4j-1.2.17.jar:./lib/org.apache.commons.collections.jar

    rm -rf bin
    mkdir bin

    javac -d bin -sourcepath src @filelist.txt
    cd bin

    jar cf evaluator_rt.jar *


> - alebo pomocou skriptu buildInLinux.sh - je sucastou archive.zip
> - pred spustenim treba zapnut redis servera
> - spustenie programu:
    cd bin

    java -cp .:../lib/ACPAPI1.2.jar:../lib/jedis-2.4.2.jar:../lib/json-1.0.jar:../lib/log4j-1.2.17.jar:../lib/org.apache.commons.collections.jar evaluator_rt.Evaluator_rt
> - alebo pomocou skriptu runInLinux.sh - je sucastou archive.zip
