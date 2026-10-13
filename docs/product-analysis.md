# Analisi prodotti per famiglia e colore

La pagina Prodotti classifica automaticamente i codici già importati. Non richiede
nuovi campi, modifiche allo schema, interventi sui CSV o dipendenze esterne aggiuntive.

## Classificazione

Struttura riconosciuta: materia prima `ZR`/`ZS`, diametro `95`/`98`, altezza
`10/12/14/16/18/20/22/25/30`, famiglia e colore quando previsto.

- Famiglie senza colore: H, T senza colore, T4, G1200.
- Famiglie con colore: MS, MST, UMS, T con colore.
- Colori: A0, A1, A2, A3, A3.5, A4, B1, B2, B3, C1, C2, C3, D2, D3.
- `A35` viene visualizzato come `A3.5`; il codice originale non viene riscritto.
- I prefissi sovrapposti (MS/MST, T/T4) vengono riconosciuti senza ambiguità.

Raggruppamenti: materia prima + famiglia; oppure materia prima + famiglia + colore.
Altezza e diametro sono sempre aggregati nella classifica, ma rimangono consultabili
nelle varianti. T con colore e T senza colore restano famiglie distinte.

I codici non riconosciuti, compresi ZRPC e ZRYTZ…, sono elencati in una sezione
separata con i loro totali. Non vengono eliminati, non sono nel denominatore delle
quote della classifica e continuano a contribuire alle altre pagine dell'applicazione.

## Misure e periodi

Quantità e fatturato sono netti: vendite meno valori assoluti di resi e note di credito.
Il dettaglio mostra gli importi e le quantità delle rettifiche separatamente.
Clienti acquirenti e ordini sono contati distintamente sulle vendite: aggregare più
codici non somma i conteggi dei singoli articoli. Un ordine è identificato dalla coppia
cliente/documento, anche quando contiene più codici o varianti.

La data di riferimento è il minimo tra fine dell'anno selezionato, ultima data del
movimento nell'archivio e oggi. Il confronto annuale usa 1 gennaio–riferimento contro
le date equivalenti dell'anno precedente, con adattamento del 29 febbraio al 28.
Niente percentuali se manca copertura dell'anno precedente o la base è nulla/negativa.

Il grafico mostra tutto l'anno precedente e l'anno selezionato fino alla data di
riferimento. Riempie con zero i mesi senza movimenti; non prolunga la linea attuale
nei mesi futuri. I tooltip sono quelli del componente comune `ChartPointTooltip`.

Le quote usano i totali netti dei prodotti classificati e non cambiano durante la
ricerca. La misura delle quote è indicata nella pagina e segue l'ordinamento scelto:
quantità per Quantità/Clienti, fatturato per Fatturato. I valori delle celle restano
numerici, quindi gli ordinamenti non dipendono dalla formattazione italiana.

Clienti e Varianti permettono di scegliere quantità o fatturato per quote e
ripartizioni. La quota dei primi cinque clienti usa la misura selezionata. Con valori
netti negativi dovuti a rettifiche, le quote possono essere negative o superare il 100%.
Il valore medio per unità non è un prezzo di listino: risente del mix delle varianti.

## Interazioni

Anno e raggruppamento aggiornano la pagina automaticamente. Il raggruppamento e i
dettagli riutilizzano una fotografia immutabile di due anni di dati, senza nuove query.
“Ricarica dati” acquisisce eventuali nuove importazioni. Ricerca, raggruppamento e
ordinamento rimangono selezionati; viene mantenuto il gruppo quando disponibile.

“Andamento”, “Clienti” e “Varianti” sono componenti distinti. Il pulsante nella lista
clienti apre l'analisi del cliente con lo stesso anno. Il colore non è una dimensione
di ripartizione quando il gruppo selezionato contiene già un singolo colore.

Risposte asincrone superate, ricerche vuote e navigazione fuori pagina non possono
ripristinare un dettaglio vecchio. I tooltip vengono rimossi quando cambia il grafico
o si chiude la pagina. Gli errori permettono di riprovare senza usare dati vecchi come
se appartenessero all'anno appena richiesto.

## Responsabilità

- `ProductCodeParser`: riconoscimento e normalizzazione degli attributi.
- Modelli `products/model`: gruppi, snapshot e risultati immutabili.
- `ProductAnalysisRepository`: lettura SQL, aggregazione delle righe per documento,
  segni delle rettifiche, nomi cliente e copertura dell'archivio.
- `ProductTotalsCalculator`: totali, conteggi distinti, percentuali e valore unitario.
- `ProductAnalysisCalculator`: classifica, serie mensili, clienti e varianti.
- `ProductAnalysisService`: connessioni indipendenti e lavoro sull'executor.
- `ProductsController`: caricamenti, selezione e validità delle risposte asincrone.
- `ProductsView` e componenti di dettaglio: presentazione e interazioni JavaFX.
- `products-analysis.css`: stile della pagina, senza modificare il CSS clienti.
- Navigazione dell'applicazione: apertura dell'analisi del cliente richiesto.
