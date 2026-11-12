package com.grafkom.viewing3d;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;

/**
 * Praktikum 3: Texture loader (STBImage). Jika file tidak ditemukan, otomatis dibuat
 * tekstur checkerboard prosedural sehingga program tetap berjalan.
 */
public class Texture2D {
    private final int textureID;

    public Texture2D(String filePath) {
        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        boolean loaded = false;
        if (filePath != null && new File(filePath).exists()) {
            stbi_set_flip_vertically_on_load(true); // UV v=0 di bawah, gambar v=0 di atas
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer width = stack.mallocInt(1);
                IntBuffer height = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);

                ByteBuffer data = stbi_load(filePath, width, height, channels, 4);
                if (data != null) {
                    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0), 0,
                            GL_RGBA, GL_UNSIGNED_BYTE, data);
                    stbi_image_free(data);
                    loaded = true;
                    System.out.println("Texture loaded: " + filePath);
                } else {
                    System.err.println("Failed to load texture: " + filePath + " (" + stbi_failure_reason() + ")");
                }
            }
        }
        if (!loaded) {
            System.out.println("Texture file not found, memakai checkerboard prosedural. "
                    + "Taruh gambar di: " + filePath);
            uploadProceduralChecker();
        }

        glGenerateMipmap(GL_TEXTURE_2D);
        setFilter(true);
    }

    private void uploadProceduralChecker() {
        final int size = 512, tile = 16;
        ByteBuffer buf = BufferUtils.createByteBuffer(size * size * 4);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                boolean a = ((x / tile) + (y / tile)) % 2 == 0;
                int r = a ? 235 : 200, g = a ? 235 : 90, b = a ? 235 : 40;
                buf.put((byte) r).put((byte) g).put((byte) b).put((byte) 255);
            }
        }
        buf.flip();
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, size, size, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
    }

    /** true = Bilinear + Mipmapped (trilinear), false = Nearest Neighbor. */
    public void setFilter(boolean mipmapped) {
        glBindTexture(GL_TEXTURE_2D, textureID);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, mipmapped ? GL_LINEAR_MIPMAP_LINEAR : GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, mipmapped ? GL_LINEAR : GL_NEAREST);
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureID);
    }
}
