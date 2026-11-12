package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.opengl.GL20.*;

/** Membungkus kompilasi, linking, dan uniform GLSL. */
public class ShaderProgram {
    private final int programId;

    public ShaderProgram(String vertexResource, String fragmentResource) {
        int vs = compile(GL_VERTEX_SHADER, readResource(vertexResource), vertexResource);
        int fs = compile(GL_FRAGMENT_SHADER, readResource(fragmentResource), fragmentResource);

        programId = glCreateProgram();
        glAttachShader(programId, vs);
        glAttachShader(programId, fs);
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            throw new RuntimeException("Link error: " + glGetProgramInfoLog(programId));
        }
        glDeleteShader(vs);
        glDeleteShader(fs);
    }

    private static int compile(int type, String source, String name) {
        int id = glCreateShader(type);
        glShaderSource(id, source);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == 0) {
            throw new RuntimeException("Compile error in " + name + ":\n" + glGetShaderInfoLog(id));
        }
        return id;
    }

    private static String readResource(String path) {
        try (InputStream in = ShaderProgram.class.getResourceAsStream(path)) {
            if (in == null) throw new RuntimeException("Resource tidak ditemukan: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void use() { glUseProgram(programId); }

    public void setInt(String name, int v)        { glUniform1i(glGetUniformLocation(programId, name), v); }
    public void setBool(String name, boolean v)   { glUniform1i(glGetUniformLocation(programId, name), v ? 1 : 0); }
    public void setVec3(String name, Vector3f v)  { glUniform3f(glGetUniformLocation(programId, name), v.x, v.y, v.z); }
    public void setVec3(String name, float x, float y, float z) { glUniform3f(glGetUniformLocation(programId, name), x, y, z); }

    public void setMat4(String name, Matrix4f m) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            m.get(fb);
            glUniformMatrix4fv(glGetUniformLocation(programId, name), false, fb);
        }
    }
}
