## Struktur Aplikasi
### Infrastructure
- `config/`: Berisi file konfigurasi untuk aplikasi.
- `jpa/`: Berisi file konfigurasi untuk JPA (Java Persistence API).
- `adapter/`: Berisi file adapter untuk menghubungkan antara domain dan infrastruktur.
  - `jpa/`: Berisi implementasi dari adapter untuk JPA.
  - `security/`: Berisi implementasi dari adapter untuk keamanan.

### Domain
- `model/`: Berisi model domain yang digunakan dalam aplikasi.
- `port/`: Berisi interface untuk port yang digunakan dalam aplikasi.
    - `in/`: Berisi interface untuk input port - berisi interface untuk service.
    - `out/`: Berisi interface untuk output port - berisi interface untuk repository.

### Application
- `service/`: Berisi implementasi dari service yang digunakan dalam aplikasi.
- `dto/`: Berisi Data Transfer Object (DTO) yang digunakan untuk mentransfer data antara layer.
- `cron/`: Berisi implementasi dari cron job yang digunakan dalam aplikasi.
- `controller/`: Berisi implementasi dari controller yang digunakan untuk menangani request dari client.