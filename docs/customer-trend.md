# Trend del cliente: due confronti distinti

Il trend mostra due confronti di fatturato e quantità senza mediare le percentuali.
I valori sono totali di vendita (`tipo_operazione='VENDITA'`), come nelle altre schede
del dettaglio cliente. Ogni periodo conta gli ordini con `COUNT(DISTINCT documento_id)`.

## Periodi

La data di riferimento è la più piccola fra fine dell'anno selezionato, ultima data di
vendita dell'intero archivio e data odierna. Non dipende dall'ultimo ordine del cliente.
Per gli anni storici completi il riferimento è il 31 dicembre.

- **Ultimi 12 mesi completi**: termina a fine del mese precedente al riferimento,
  oppure nel mese del riferimento se coincide con l'ultimo giorno del mese.
  Il confronto precedente comprende i 12 mesi immediatamente antecedenti.
- **Anno a pari data**: dal 1° gennaio alla data di riferimento, contro lo stesso
  intervallo nell'anno precedente. Il 29 febbraio si confronta con il 28 febbraio
  quando l'anno precedente non è bisestile.

Esempio con dati aggiornati al 7 ottobre 2026: ottobre 2025–settembre 2026 contro
ottobre 2024–settembre 2025; 1 gennaio–7 ottobre 2026 contro 1 gennaio–7 ottobre 2025.
Le date effettive sono sempre visibili nell'interfaccia.

## Disponibilità e interpretazione

Le percentuali sono `(attuale - precedente) / precedente * 100`, arrotondate a un
decimale. Non si calcolano se il valore precedente è zero o se la copertura
dell'archivio non comprende l'intero confronto. I totali restano visibili e il
tooltip specifica il motivo dell'indisponibilità.

Il confronto annuale è preliminare nel primo trimestre oppure quando almeno uno
dei due periodi contiene meno di tre ordini. La descrizione dà allora priorità al
confronto su 12 mesi. Negli altri casi descrive separatamente le direzioni di
fatturato e quantità nei due confronti, anche quando sono opposte.

L'etichetta “Stabile” conserva il proprio significato: riguarda la variazione degli
ordini nei due periodi di 12 mesi, con le soglie già utilizzate dal progetto.
Il grafico rappresenta gli ordini mensili, inclusi i mesi senza acquisti, e la media
mobile su tre mesi. L'ultimo mese parziale viene indicato nel titolo.

## Responsabilità

- Repository: legge totali nei quattro intervalli esatti, con date inclusive.
- `CustomerTrendPeriodResolver`: decide riferimento e intervalli.
- `CustomerTrendCalculator`: variazioni, disponibilità e stato degli ordini.
- `CustomerTrendNarrative`: descrizione combinata.
- `CustomerTrendComparisonPane`: periodi, valori, tooltip e disposizione responsive.
- `ContentSizedTabPane`: altezza del contenuto selezionato nel pannello scorrevole.
