# DuckCore

Jeden plugin: /szaman (perki, kamienie, losowanie z animacjami), ranking + napis ZABOJSTWO,
klany z bonusem do perkow, scoreboard DuckMC. Paper 1.21.4, Java 21.

Build: `mvn package` -> target/DuckCore.jar (albo push na GitHub, workflow .github/workflows/build.yml).

## Komendy
- /szaman - menu Szamana
- /odlamki - stan Smoczych Odlamkow i kamieni
- /klan stworz|zapros|dolacz|opusc|usun|info|top
- /duck odlamki <gracz> <+/-ile> | kamien <gracz> <typ> <ile> | ranga | patron | kasa | reload  (duckcore.admin)

## API (dla generatorow / bossow)
- DuckCore.getGeneratorMultiplier(player)  -> mnoznik zarobku (Perk Dropu)
- DuckCore.isEventBlocked(player)          -> czy eventowka zablokowana
- DuckCore.addShards(uuid, ilosc)          -> dodaj Smocze Odlamki (np. za bossa)
