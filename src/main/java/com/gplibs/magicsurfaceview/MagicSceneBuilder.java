package com.gplibs.magicsurfaceview;

import java.util.Arrays;
import java.util.Locale;

public class MagicSceneBuilder {

    private MagicScene mScene = new MagicScene();

    public MagicSceneBuilder(MagicSurfaceView surfaceView) {
        mScene.mSurfaceView = surfaceView;
    }

    public MagicSceneBuilder addLights(Light... lights) {
        if (lights == null) {
            return this;
        }
        mScene.mLights.addAll(Arrays.asList(lights));
        return this;
    }

    public MagicSceneBuilder addSurfaces(MagicBaseSurface... objects) {
        if (objects == null) {
            return this;
        }
        for (MagicBaseSurface s : objects) {
            s.mScene = mScene;
            mScene.mSurfaces.add(s);
        }
        return this;
    }

    public MagicSceneBuilder ambientColor(int color) {
        mScene.setAmbientColor(color);
        return this;
    }

    public MagicSceneBuilder setUpdater(MagicSceneUpdater updater) {
        mScene.setUpdater(updater);
        return this;
    }

    public MagicScene build() {
        final String v = buildVertexShaderSource();
        final String f = buildFragmentShaderSource();
        mScene.init(v, f);
        return mScene;
    }

    private String buildVertexShaderSource() {
        StringBuilder sb = new StringBuilder();
        sb.append("attribute vec3 a_position;");
        sb.append("attribute vec4 a_color;");
        sb.append("attribute vec3 a_normal;");
        sb.append("attribute vec2 a_tex_coord;");
        sb.append("uniform vec3 u_camera;");
        sb.append("uniform mat4 u_m_matrix;");
        sb.append("uniform mat4 u_mvp_matrix;");
        sb.append("uniform vec4 u_ambient_color;");
        sb.append("varying vec2 v_tex_coord;");
        sb.append("varying vec4 v_model_color;");
        sb.append(createVertexShaderParams());

        sb.append("void diffuse_color(");
        sb.append("in vec3 vecNormal,");
        sb.append("in vec3 vecLight,");
        sb.append("in vec4 lightColor,");
        sb.append("inout vec4 diffuse) {");
        sb.append("float nDotViewPosition = max(0.0, dot(vecNormal, vecLight));");
        sb.append("diffuse = lightColor * nDotViewPosition;");
        sb.append("}");

        sb.append("void specular_color(");
        sb.append("in vec3 normal,");
        sb.append("in vec3 vecLight,");
        sb.append("in vec3 vecEye,");
        sb.append("in float shininess,");
        sb.append("in vec4 lightColor,");
        sb.append("inout vec4 specular) {");
        sb.append("vec3 vecHalf = normalize(vecLight + vecEye);");
        sb.append("float nDotViewHalfVector = dot(normal, vecHalf);");
        sb.append("float powerFactor = max(0.0, pow(nDotViewHalfVector, shininess));");
        sb.append("specular = lightColor * powerFactor;");
        sb.append("}");

        sb.append("void light(");
        sb.append("in vec3 normal,");
        sb.append("in bool isPointLight,");
        sb.append("in vec3 lightLocOrDir,");
        sb.append("in vec4 lightColor,");
        sb.append("in float shininess,");
        sb.append("inout vec4 diffuse,");
        sb.append("inout vec4 specular) {");
        sb.append("vec3 newPos = (u_m_matrix * vec4(a_position, 1)).xyz;");
        sb.append("vec3 normalTarget = a_position + normal;");
        sb.append("vec3 vecNormal = normalize((u_m_matrix * vec4(normalTarget, 1)).xyz - newPos);");
        sb.append("vec3 vecEye = normalize(u_camera - newPos);");
        sb.append("vec3 vecLight = normalize(isPointLight ? (lightLocOrDir - newPos) : lightLocOrDir * vec3(-1, -1, -1));");
        sb.append("diffuse_color(vecNormal, vecLight, lightColor, diffuse);");
        sb.append("specular_color(vecNormal, vecLight, vecEye, shininess, lightColor, specular);");
        sb.append("}");

        sb.append("void main() {");
        sb.append("gl_Position = u_mvp_matrix * vec4(a_position, 1);");
        sb.append("v_tex_coord = a_tex_coord;");
        sb.append("v_model_color = a_color;");
        sb.append("vec4 d = vec4(0, 0, 0, 0);");
        sb.append("vec4 s = vec4(0, 0, 0, 0);");
        sb.append(createVertexShaderMain());
        sb.append("}");

        return sb.toString();
    }

    private String createVertexShaderParams() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mScene.mSurfaces.size(); ++i) {
            append(sb, "uniform bool u_surface%d_;", i);
            append(sb, "uniform float u_s%d_shininess;", i);
            sb.append("");
        }
        if (mScene.mLights != null) {
            for (int i = 0; i < mScene.mLights.size(); ++i) {
                append(sb, "uniform bool u_light%d_;", i);
                append(sb, "uniform bool u_l%d_is_point_light;", i);
                append(sb, "uniform vec4 u_l%d_color;", i);
                append(sb, "uniform vec3 u_l%d_pos_or_dir;", i);
                append(sb, "varying vec4 v_l%d_diffuse;", i);
                append(sb, "varying vec4 v_l%d_specular;", i);
                sb.append("");
            }
        }
        return sb.toString();
    }

    private String createVertexShaderMain() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mScene.mSurfaces.size(); ++i) {
            if (i > 0) {
                sb.append(" else ");
            }
            append(sb, "if (u_surface%d_) {", i);
            if (mScene.mLights != null) {
                for (int n = 0; n < mScene.mLights.size(); ++n) {
                    sb.append(createVertexShaderMain(i, n));
                }
            }
            sb.append("}");
        }
        return sb.toString();
    }

    private String createVertexShaderMain(int surfaceIndex, int lightIndex) {
        StringBuilder sb = new StringBuilder();
        append(sb, "if (u_light%d_) {", lightIndex);
        sb.append("light(normalize(a_normal),");
        append(sb, "u_l%d_is_point_light,", lightIndex);
        append(sb, "u_l%d_pos_or_dir,", lightIndex);
        append(sb, "u_l%d_color,", lightIndex);
        append(sb, "u_s%d_shininess,d,s);", surfaceIndex);
        append(sb, "v_l%d_diffuse = d;", lightIndex);
        append(sb, "v_l%d_specular = s;", lightIndex);
        sb.append("}");
        return sb.toString();
    }

    private String buildFragmentShaderSource() {
        StringBuilder sb = new StringBuilder();
        sb.append("precision mediump float;");
        sb.append("uniform vec4 u_ambient_color;");
        sb.append(createFragmentShaderParams());
        sb.append("varying vec2 v_tex_coord;");
        sb.append("varying vec4 v_model_color;");
        sb.append(createFragmentShaderFunctions());
        sb.append("vec4 frag_color() {");
        sb.append("vec4 c = t_color() * v_model_color;");
        sb.append("return c * u_ambient_color + l_color(c);");
        sb.append("}");
        sb.append("void main() {");
        sb.append("vec4 c = frag_color();");
        sb.append("gl_FragColor = c;");
        sb.append("}");
        return sb.toString();
    };

    private String createFragmentShaderParams() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mScene.mSurfaces.size(); ++i) {
            append(sb, "uniform bool u_surface%d_;", i);
            append(sb, "uniform sampler2D u_s%d_t_body;", i);
            for (int n = 0; n < mScene.mSurfaces.get(i).mTextures.size(); ++n) {
                append(sb, "uniform sampler2D u_s%d_t%d;", i, n);
            }
        }
        sb.append("");
        if (mScene.mLights != null) {
            for (int i = 0; i < mScene.mLights.size(); ++i) {
                append(sb, "uniform bool u_light%d_;", i);
                append(sb, "varying vec4 v_l%d_diffuse;", i);
                append(sb, "varying vec4 v_l%d_specular;", i);
            }
        }
        return sb.toString();
    }

    private String createFragmentShaderFunctions() {
        StringBuilder sb = new StringBuilder();
        StringBuilder sbSnTColor = new StringBuilder();
        sb.append("vec4 t_color() {");
        sb.append("vec4 color = vec4(1.0);");
        for (int i = 0; i < mScene.mSurfaces.size(); ++i) {
            append(sbSnTColor, "vec4 s%d_t_color() {", i);
            append(sbSnTColor, "return texture2D(u_s%d_t_body, v_tex_coord.st);", i);
            sbSnTColor.append("}");

            if (i > 0) {
                sb.append(" else ");
            }
            append(sb, "if (u_surface%d_) {", i);
            append(sb, "color = s%d_t_color();", i);
            sb.append("}");
        }
        sb.append("return color;");
        sb.append("}");
        sb.insert(0, sbSnTColor);

        sb.append("vec4 l_color(vec4 color) {");
        sb.append("vec4 r = vec4(0.0);");
        if (mScene.mLights != null) {
            for (int i = 0; i < mScene.mLights.size(); ++i) {
                append(sb, "if (u_light%d_) {", i);
                append(sb, "r = r + (color * v_l%d_diffuse + color * v_l%1$d_specular);", i);
                sb.append("}");
            }
        }
        sb.append("return r;");
        sb.append("}");
        return sb.toString();
    }

    private void append(StringBuilder sb, String format, Object... params) {
        sb.append(String.format(Locale.US, format, params));
    }
}
