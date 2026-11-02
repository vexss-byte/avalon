# AVALON: The Forgotten Kingdom (Fabric 1.21.1)

Mod game sosial-deduksi (gaya Avalon): 1 map, 1 Discussion Hall, 3 Pilar, 4 baterai per pilar.
**Semua logika dijalankan di server** (peran, voting, sabotase, hasil, pemenang). Klien hanya menampilkan GUI.

> STATUS: kode ditulis tanpa bisa di-compile di lingkungan pembuatnya (tidak ada akses internet/Gradle).
> Build pertama di GitHub Actions bisa saja menampilkan beberapa error nama method Yarn; kirim lognya dan akan diperbaiki.

## Build
1. Upload folder ini ke repo GitHub, tab **Actions** akan menjalankan `build` otomatis
   (workflow membuat `gradle-wrapper.jar` sendiri bila belum ada).
2. Ambil jar dari artifact `avalon-mod` (`build/libs/avalon-0.1.0.jar`, bukan `-sources`).
3. Pasang di **server dan semua client**: Fabric Loader 0.17.3+, Fabric API 0.116.x+1.21.1, AzureLib 3.2.4, Player Animation Library 1.1.6, Java 21.

## Cara main
1. Op menjalankan `/avalon build` sekali (membangun Hall + 3 pilar di dimensi `avalon:arena`).
2. Berdiri di lobi lalu `/avalon setlobby`.
3. `/avalon start` (semua pemain online ikut, min `minPlayers` di `config/avalon.json`, default 5).
4. Alur: hitung mundur -> portal 3D menarik pemain -> perkenalan peran -> Raja dipilih -> Raja klik kanan
   **Buku Pemilihan Tim** -> GUI pilih tim -> GUI voting rahasia (hasil dibuka setelah selesai) -> misi:
   ambil baterai di **gudang**, pasang di 4 **soket** pilar -> aktivasi -> hasil -> diskusi 3 menit
   (`[Skip]` / `/avalon skip`, Lady of the Lake) -> voting tebak kubu jahat -> ronde berikut.
5. Perintah lain: `/avalon stop`, `/avalon status`, `/avalon reload`, `/avalon menu` (buka ulang layar yang tertutup).

## Tes sendirian
- `/avalon testportal` : memutar gerbang portal + animasi tarik (PAL) untuk kamu sendiri, lalu menampilkan Hall 5 detik. Tanpa game.
- Tes game penuh solo: di `config/avalon.json` set `"soloTest": true` dan `"debugRole": "ASSASSIN"` (atau MERLIN, MORGANA, dst), `/avalon reload`, lalu `/avalon start`.
  Dengan 1 pemain, tebak-kubu-jahat dilewati dan fase Assassin tidak punya kandidat, jadi dipakai terutama untuk menguji misi, baterai, dan sabotase.

## Aturan yang diimplementasi (sesuai jawabanmu)
- Tim 3, 4, 4. Voting ditolak 5x berturut-turut = kubu jahat menang.
- Kubu jahat mengambil baterai yang SAMA dari gudang. Hanya mereka melihat hint hotbar "Klik kanan untuk sabotase".
  Klik kanan = tandai baterai (tanpa animasi/suara/partikel). Saat dipasang, efeknya IDENTIK dengan baterai biasa.
- Pilar tetap butuh 4 baterai. Aktivasi diputar sama persis; baru setelah itu server cek sabotase:
  stabil = berhasil, tidak stabil = percikan, retakan energi, korupsi merah/ungu, lalu ledakan terkontrol (tanpa merusak blok).
- Tebak kubu jahat: setelah diskusi tiap misi (`ejectAfterEveryMission`), kesempatan total = jumlah kubu jahat.
  Semua kubu jahat keluar = kubu baik menang langsung.
- Tidak ada sabotase di 3 pilar = kubu baik menang.
- Assassin hanya di ronde akhir bila ada sabotase dan kubu jahat belum ketahuan; tebakan Merlin salah = kubu baik menang.
- Waktu diskusi dan voting 3 menit (ubah di config).

## Hal yang perlu kamu putuskan / cek nanti
- **AzureLib 3.2.4 + Player Animation Library 1.1.6 + format Blockbench** sudah dipakai (jar ada di `libs/`, API dibaca langsung dari jar-nya):
  - Gerbang portal = entity `avalon:portal_gate`, model `geo/entity/portal_gate.geo.json`, animasi cincin berputar
    `animations/entity/portal_gate.animation.json` (render lewat AzureLib).
  - Baterai = item AzureLib: `geo/item/battery.geo.json` + `animations/item/battery.animation.json` (glow idle, identik untuk semua pemain).
  - Animasi pemain ditarik portal = PAL: `player_animations/pull_gate.json`, dikirim server lewat paket `PlayAnimPayload`.
  - Semua file model/animasi memakai format Bedrock, jadi bisa dibuka dan diedit di **Blockbench** (plugin AzureLib), lalu ekspor menimpa file dengan nama yang sama.
  - Client WAJIB memasang AzureLib 3.2.4 dan Player Animation Library (mod ini mendeklarasikannya sebagai dependency). Fabric Loader >= 0.17.3.
- Peta dibangun dari blok vanilla; tekstur baterai/soket/gudang adalah pixel art buatan sendiri.
- Teks "Sabotase siap pada baterai ini" hanya terlihat oleh pemain jahat itu sendiri (action bar privat).
