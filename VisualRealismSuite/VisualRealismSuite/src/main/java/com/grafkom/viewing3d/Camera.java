package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Kamera orbit sederhana (drag mouse = putar, scroll = zoom). */
public class Camera {
    private float yaw = 35f, pitch = 25f, distance = 9f;
    private final Vector3f target = new Vector3f(0f, 0.3f, 0f);

    public void rotate(float dYaw, float dPitch) {
        yaw += dYaw;
        pitch = Math.max(-5f, Math.min(85f, pitch + dPitch));
    }

    public void zoom(float amount) {
        distance = Math.max(3f, Math.min(25f, distance - amount));
    }

    public Vector3f getPosition() {
        float y = (float) Math.toRadians(yaw), p = (float) Math.toRadians(pitch);
        return new Vector3f(
                (float) (Math.cos(p) * Math.sin(y)),
                (float) Math.sin(p),
                (float) (Math.cos(p) * Math.cos(y))).mul(distance).add(target);
    }

    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(getPosition(), target, new Vector3f(0f, 1f, 0f));
    }
}
