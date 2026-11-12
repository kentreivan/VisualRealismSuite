package com.grafkom.viewing3d;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Pembuat mesh prosedural: Sphere, Torus, Rumah 3D, dan Ground Plane. Semua CCW (tampak luar). */
public class MeshFactory {

    private static class Builder {
        final List<Float> pos = new ArrayList<>();
        final List<Float> uv = new ArrayList<>();

        void vertex(Vector3f p, float u, float v) {
            pos.add(p.x); pos.add(p.y); pos.add(p.z);
            uv.add(u); uv.add(v);
        }

        void tri(Vector3f a, Vector3f b, Vector3f c,
                 float ua, float va, float ub, float vb, float uc, float vc) {
            vertex(a, ua, va);
            vertex(b, ub, vb);
            vertex(c, uc, vc);
        }

        /** Quad CCW a-b-c-d (dilihat dari luar) dengan UV (0,0),(u,0),(u,v),(0,v). */
        void quad(Vector3f a, Vector3f b, Vector3f c, Vector3f d, float u, float v) {
            tri(a, b, c, 0, 0, u, 0, u, v);
            tri(a, c, d, 0, 0, u, v, 0, v);
        }

        Mesh build() {
            return new Mesh(toArray(pos), toArray(uv));
        }

        private static float[] toArray(List<Float> l) {
            float[] r = new float[l.size()];
            for (int i = 0; i < r.length; i++) r[i] = l.get(i);
            return r;
        }
    }

    private static Vector3f v(float x, float y, float z) { return new Vector3f(x, y, z); }

    // ---------------------------------------------------------------- Sphere
    public static Mesh sphere(float radius, int stacks, int slices) {
        Builder b = new Builder();
        for (int i = 0; i < stacks; i++) {
            for (int j = 0; j < slices; j++) {
                Vector3f p00 = sph(radius, i, j, stacks, slices);
                Vector3f p01 = sph(radius, i, j + 1, stacks, slices);
                Vector3f p10 = sph(radius, i + 1, j, stacks, slices);
                Vector3f p11 = sph(radius, i + 1, j + 1, stacks, slices);

                float u0 = (float) j / slices, u1 = (float) (j + 1) / slices;
                float v0 = 1f - (float) i / stacks, v1 = 1f - (float) (i + 1) / stacks;

                b.tri(p00, p01, p10, u0, v0, u1, v0, u0, v1);
                b.tri(p01, p11, p10, u1, v0, u1, v1, u0, v1);
            }
        }
        return b.build();
    }

    private static Vector3f sph(float r, int i, int j, int stacks, int slices) {
        double theta = Math.PI * i / stacks;
        double phi = 2.0 * Math.PI * j / slices;
        return v((float) (r * Math.sin(theta) * Math.cos(phi)),
                 (float) (r * Math.cos(theta)),
                 (float) (r * Math.sin(theta) * Math.sin(phi)));
    }

    // ----------------------------------------------------------------- Torus
    public static Mesh torus(float majorR, float minorR, int rings, int sides) {
        Builder b = new Builder();
        for (int i = 0; i < rings; i++) {
            for (int j = 0; j < sides; j++) {
                Vector3f p00 = tor(majorR, minorR, i, j, rings, sides);
                Vector3f p10 = tor(majorR, minorR, i + 1, j, rings, sides);
                Vector3f p01 = tor(majorR, minorR, i, j + 1, rings, sides);
                Vector3f p11 = tor(majorR, minorR, i + 1, j + 1, rings, sides);

                float u0 = 3f * i / rings, u1 = 3f * (i + 1) / rings;
                float v0 = (float) j / sides, v1 = (float) (j + 1) / sides;

                b.tri(p00, p01, p10, u0, v0, u0, v1, u1, v0);
                b.tri(p10, p01, p11, u1, v0, u0, v1, u1, v1);
            }
        }
        return b.build();
    }

    private static Vector3f tor(float R, float r, int i, int j, int rings, int sides) {
        double phi = 2.0 * Math.PI * i / rings;
        double theta = 2.0 * Math.PI * j / sides;
        double ring = R + r * Math.cos(theta);
        return v((float) (ring * Math.cos(phi)), (float) (r * Math.sin(theta)), (float) (ring * Math.sin(phi)));
    }

    // ----------------------------------------------------------------- House
    public static Mesh house() {
        Builder b = new Builder();
        float yb = -1f, ye = 0.2f, yr = 1.2f; // alas, atap (eave), puncak (ridge)

        // Dinding (+z, -z, +x, -x) & alas
        b.quad(v(-1, yb, 1), v(1, yb, 1), v(1, ye, 1), v(-1, ye, 1), 1, 1);
        b.quad(v(1, yb, -1), v(-1, yb, -1), v(-1, ye, -1), v(1, ye, -1), 1, 1);
        b.quad(v(1, yb, 1), v(1, yb, -1), v(1, ye, -1), v(1, ye, 1), 1, 1);
        b.quad(v(-1, yb, -1), v(-1, yb, 1), v(-1, ye, 1), v(-1, ye, -1), 1, 1);
        b.quad(v(-1, yb, -1), v(1, yb, -1), v(1, yb, 1), v(-1, yb, 1), 1, 1);

        // Atap: dua sisi miring, dua segitiga gable, dan sisi bawah
        float o = 1.1f;
        b.quad(v(o, ye, o), v(o, ye, -o), v(0, yr, -o), v(0, yr, o), 1, 1);       // kanan
        b.quad(v(-o, ye, -o), v(-o, ye, o), v(0, yr, o), v(0, yr, -o), 1, 1);     // kiri
        b.tri(v(-o, ye, o), v(o, ye, o), v(0, yr, o), 0, 0, 1, 0, 0.5f, 1);       // gable depan
        b.tri(v(o, ye, -o), v(-o, ye, -o), v(0, yr, -o), 0, 0, 1, 0, 0.5f, 1);    // gable belakang
        b.quad(v(-o, ye, -o), v(o, ye, -o), v(o, ye, o), v(-o, ye, o), 1, 1);     // bawah atap
        return b.build();
    }

    // ---------------------------------------------------------------- Ground
    /** Bidang lantai horizontal di ketinggian y, normal menghadap +Y. */
    public static Mesh plane(float half, float y, float uvRepeat) {
        Builder b = new Builder();
        b.quad(v(-half, y, -half), v(-half, y, half), v(half, y, half), v(half, y, -half), uvRepeat, uvRepeat);
        return b.build();
    }
}
