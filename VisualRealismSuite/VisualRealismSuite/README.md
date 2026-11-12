# 3D Visual Realism Suite (Grafika Komputer - Minggu 11)

Java 17+, LWJGL 3.3.4, JOML, GLSL 330 core.

## Menjalankan
    mvn compile exec:exec

(macOS: flag -XstartOnFirstThread sudah diatur otomatis oleh pom.xml.)

Tekstur opsional: taruh gambar di `textures/texture.png` (jalankan dari folder project).
Jika tidak ada, dipakai checkerboard prosedural.

## Kontrol
| Key | Fungsi |
|-----|--------|
| B | Phong <-> Blinn-Phong (Indikator 1) |
| F | Flat <-> Smooth Shading (Indikator 2) |
| Z | Z-Buffer Depth Test ON/OFF (Indikator 3) |
| C | Back-Face Culling ON/OFF (Indikator 3) |
| T / N | Texture ON/OFF, Nearest <-> Bilinear+Mipmap (Indikator 4) |
| H | Shadow Mapping ON/OFF (Indikator 5) |
| M | Ganti mesh: Sphere / Torus / House |
| L / SPACE | Jeda animasi cahaya / rotasi objek |
| V | VSync ON/OFF (untuk mengukur FPS di laporan) |
| Mouse drag / scroll | Orbit / zoom kamera |

Status semua toggle + FPS tampil di title bar window.
