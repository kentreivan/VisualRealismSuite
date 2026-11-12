package com.grafkom.viewing3d;

import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Mesh 3D (triangle list) dengan Vektor Normal Vertex + Koordinat UV.
 * Menyimpan DUA versi normal di GPU:
 *  - Flat   : satu normal per face (face normal)
 *  - Smooth : normal vertex hasil rata-rata normal semua face yang berbagi titik (vertex normal averaging)
 */
public class Mesh {
    private static final int STRIDE = 8 * Float.BYTES; // pos(3) + normal(3) + uv(2)

    private final int vertexCount;
    private final int vaoFlat, vaoSmooth;
    private final float minY;

    public Mesh(float[] positions, float[] uvs) {
        vertexCount = positions.length / 3;

        float[] flatNormals = computeFlatNormals(positions);
        float[] smoothNormals = computeSmoothNormals(positions);

        float min = Float.MAX_VALUE;
        for (int i = 1; i < positions.length; i += 3) min = Math.min(min, positions[i]);
        minY = min;

        vaoFlat = upload(positions, flatNormals, uvs);
        vaoSmooth = upload(positions, smoothNormals, uvs);
    }

    /** Normal face (unit). Return false jika segitiga degenerate (luas = 0). */
    private static boolean faceNormal(float[] p, int tri, Vector3f out) {
        int i = tri * 9;
        Vector3f a = new Vector3f(p[i], p[i + 1], p[i + 2]);
        Vector3f b = new Vector3f(p[i + 3], p[i + 4], p[i + 5]);
        Vector3f c = new Vector3f(p[i + 6], p[i + 7], p[i + 8]);
        b.sub(a);
        c.sub(a);
        b.cross(c, out);
        if (out.length() < 1e-9f) return false;
        out.normalize();
        return true;
    }

    private float[] computeFlatNormals(float[] p) {
        float[] n = new float[p.length];
        Vector3f fn = new Vector3f();
        for (int t = 0; t < vertexCount / 3; t++) {
            if (!faceNormal(p, t, fn)) fn.set(0f, 1f, 0f);
            for (int v = 0; v < 3; v++) {
                int k = t * 9 + v * 3;
                n[k] = fn.x; n[k + 1] = fn.y; n[k + 2] = fn.z;
            }
        }
        return n;
    }

    private record Key(long x, long y, long z) {}

    private static Key keyOf(float[] p, int idx) {
        return new Key(Math.round(p[idx] * 10000.0), Math.round(p[idx + 1] * 10000.0), Math.round(p[idx + 2] * 10000.0));
    }

    /** N_vertex = (sum N_face_i) / ||sum N_face_i||  (vertex dengan posisi sama dianggap titik yang sama). */
    private float[] computeSmoothNormals(float[] p) {
        Map<Key, Vector3f> sums = new HashMap<>();
        Vector3f fn = new Vector3f();
        for (int t = 0; t < vertexCount / 3; t++) {
            if (!faceNormal(p, t, fn)) continue;
            for (int v = 0; v < 3; v++) {
                sums.computeIfAbsent(keyOf(p, t * 9 + v * 3), k -> new Vector3f()).add(fn);
            }
        }
        float[] n = new float[p.length];
        for (int i = 0; i < vertexCount; i++) {
            Vector3f s = sums.get(keyOf(p, i * 3));
            Vector3f r = (s == null || s.length() < 1e-9f) ? new Vector3f(0f, 1f, 0f) : new Vector3f(s).normalize();
            n[i * 3] = r.x; n[i * 3 + 1] = r.y; n[i * 3 + 2] = r.z;
        }
        return n;
    }

    private int upload(float[] pos, float[] nrm, float[] uv) {
        float[] data = new float[vertexCount * 8];
        for (int i = 0; i < vertexCount; i++) {
            data[i * 8]     = pos[i * 3];
            data[i * 8 + 1] = pos[i * 3 + 1];
            data[i * 8 + 2] = pos[i * 3 + 2];
            data[i * 8 + 3] = nrm[i * 3];
            data[i * 8 + 4] = nrm[i * 3 + 1];
            data[i * 8 + 5] = nrm[i * 3 + 2];
            data[i * 8 + 6] = uv[i * 2];
            data[i * 8 + 7] = uv[i * 2 + 1];
        }
        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, STRIDE, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, STRIDE, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, STRIDE, 6L * Float.BYTES);
        glEnableVertexAttribArray(2);

        glBindVertexArray(0);
        return vao;
    }

    public void draw(boolean smooth) {
        glBindVertexArray(smooth ? vaoSmooth : vaoFlat);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }

    public float getMinY() { return minY; }
}
