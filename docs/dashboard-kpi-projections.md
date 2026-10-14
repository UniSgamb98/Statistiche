# Confronti KPI su proiezione annuale

Quando sono selezionati l'anno corrente e l'intero anno, le percentuali sotto
Fatturato, Quantità e Documenti confrontano la proiezione al 31 dicembre con il
totale registrato dell'anno precedente. La dicitura “proiezione al 31/12” distingue
questi confronti dai dati effettivi. Il tooltip mostra stima, valore registrato,
base precedente, metodo e data di aggiornamento.

I valori registrati restano visibili; le schede Fatturato, Quantità e Documenti
mostrano accanto anche la proiezione al 31/12, sotto il valore registrato nelle
finestre strette. Il valore medio documento resta consuntivo e ha un proprio
confronto con il valore medio registrato nel periodo equivalente dell’anno precedente. Il confronto
Clienti resta basato sui clienti distinti registrati; non viene estrapolato.
Per anni conclusi, periodi personalizzati o stime non disponibili si usano i
confronti ordinari. Il passaggio fra modalità rimuove anche i tooltip precedenti.

## Calcoli

- Fatturato: la stessa proiezione presente nel grafico storico automatico.
- Quantità: proiezione sullo storico giornaliero netto di quantità, a tre decimali.
- Documenti: proiezione sullo storico dei documenti distinti, arrotondata all'intero.
  Ogni ID viene assegnato alla prima data in cui compare nell'anno, così le righe
  dello stesso documento su più date non aumentano artificialmente la stagionalità.
  Gli ID in anni diversi sono contati separatamente, come nei riepiloghi annuali.

Le tre misure condividono l'algoritmo: mediana delle quote storiche raggiunte alla
data di riferimento, oppure proporzione dei giorni trascorsi quando mancano almeno
due anni storici utilizzabili. Ogni misura usa il proprio storico, senza applicare
il coefficiente del fatturato a quantità o documenti. Valori non positivi o date
future non producono nuove stime. La previsione non scende sotto il valore registrato.

`SeasonalProjectionCalculator` contiene l'algoritmo comune. `RevenueProjectionCalculator`
è l'adattatore che conserva l'API già usata dai grafici e dall'analisi clienti.
`DashboardProjectionService` decide quando caricare e applicare le previsioni KPI;
il repository legge gli storici. La view presenta i confronti e i tooltip, senza
calcolare la stagionalità.

La sezione KPI è separata dalla view principale: `DashboardKpiPane` coordina le
schede, `DashboardMetricCard` gestisce valori e disposizione responsive, e
`DashboardComparisonLabel` presenta variazioni e tooltip con uno stile comune.
