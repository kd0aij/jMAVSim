package me.drton.jmavsim;

import com.sun.j3d.utils.geometry.Cylinder;
import com.sun.j3d.utils.geometry.Sphere;

import javax.media.j3d.Appearance;
import javax.media.j3d.Material;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Color3f;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector4d;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.*;

/**
 * User: ton Date: 01.02.14 Time: 22:12
 */
public class Target extends VisualObject {
    protected long startTime = -1;
    public double dragMove = 0.2;
    private GlobalPositionProjector gpsProjector = new GlobalPositionProjector();
    protected boolean applyAccel = false;
    static Vector3d torque = new Vector3d(0.0, 0.0, 2 * Math.PI / 5.0);
    private Vector3d c_M = new Vector3d();

    public boolean isApplyAccel() {
        return applyAccel;
    }

    public void setApplyAccel(boolean move) {
        this.applyAccel = move;
    }

    private Matrix3d skewSymm(Vector3d b) {
        Matrix3d result = new Matrix3d();
        result.setM01(-b.z);
        result.setM02(b.y);
        result.setM10(b.z);
        result.setM12(-b.x);
        result.setM20(-b.y);
        result.setM21(b.x);
        return result;
    }

    private Matrix3d calcI_c(List<Vector4d> masses) {
        double M = 0;
        for (Vector4d e : masses) {
            M += e.w;
            c_M.x += e.w * e.x;
            c_M.y += e.w * e.y;
            c_M.z += e.w * e.z;
        }
        c_M.scale(1.0 / M);
        setMass(M);
        Matrix3d I_c = new Matrix3d();
        Vector3d r = new Vector3d();
        for (Vector4d e : masses) {
            r.set(e.x, e.y, e.z);
            r.sub(c_M);
            Matrix3d wrx = skewSymm(r);
            // there really should be a minus sign here, but it seems to be
            // applied in MechanicalObject:update in calculating rotation rate
            wrx.mul(e.w);
            wrx.mul(wrx);
            I_c.add(wrx);
        }
        return I_c;
    }

    static Color3f black = new Color3f(0.0f, 0.0f, 0.0f);
    static Color3f white = new Color3f(0.5f, 0.5f, 0.5f);
    static Color3f red = new Color3f(1.0f, 0.0f, 0.0f);
    static Color3f dimred = new Color3f(0.2f, 0.0f, 0.0f);
    static Color3f green = new Color3f(0.0f, 1.0f, 0.0f);
    static Color3f dimgreen = new Color3f(0.0f, 0.2f, 0.0f);
    static Color3f blue = new Color3f(0.0f, 0.0f, 1.0f);
    static Color3f dimblue = new Color3f(0.0f, 0.0f, 0.2f);
    static Color3f grey1 = new Color3f(0.8f, 0.8f, 0.8f);
    static Color3f grey2 = new Color3f(0.16f, 0.16f, 0.16f);

    static Color3f orange = new Color3f(0.67f, 0.33f, 0.0f);
    static Color3f yellow = new Color3f(1f, 1f, 0.0f);

    public Target(World world, double size) throws FileNotFoundException {
        super(world);

        ArrayList<Vector4d> masses = new ArrayList<Vector4d>();
        double mass = 1.0 / 7;
        masses.add(new Vector4d(-size, 0, 0, mass));
        masses.add(new Vector4d(size, 0, 0, mass));
        masses.add(new Vector4d(0, -size, 0, mass));
        masses.add(new Vector4d(0, size, 0, mass));
        masses.add(new Vector4d(0, 0, -size, 2 * mass));
        masses.add(new Vector4d(0, 0, size, mass));
        momentOfInertia.set(calcI_c(masses));

        Transform3D baseT3D = new Transform3D();
        baseT3D.rotX(-Math.PI/32);
        baseT3D.transform(torque);
        TransformGroup baseTG = new TransformGroup(baseT3D);
        
        // create a Sphere for each point mass
        for (Vector4d e : masses) {
            Transform3D pointMassT3D = new Transform3D();
            pointMassT3D.setTranslation(new Vector3d(e.x, e.y, e.z));
            TransformGroup pointMassTG = new TransformGroup(pointMassT3D);
            Sphere massShape = new Sphere((float) (7 * e.w * size / 4));
            massShape.getAppearance().setMaterial(
                    new Material(red, black, dimred, red, 64.0f));
            pointMassTG.addChild(massShape);
            baseTG.addChild(pointMassTG);
        }
        
        // connect center of mass to each sphere
        for (Vector4d e : masses) {
            Vector3d line = new Vector3d(e.x, e.y, e.z);
            double height = line.length();
            Transform3D cylT3D = new Transform3D();
            if (e.x != 0) {
                cylT3D.rotZ(Math.PI/2);
            } else if (e.z != 0) {
                cylT3D.rotX(Math.PI/2);
            }
            line.scale(0.5);
            cylT3D.setTranslation(line);
            TransformGroup pointMassTG = new TransformGroup(cylT3D);
            Cylinder connector = new Cylinder((float) (size / 16), (float) height);
            connector.getAppearance().setMaterial(
                    new Material(black, black, black, black, 64.0f));
            pointMassTG.addChild(connector);
            baseTG.addChild(pointMassTG);
        }
        
        transformGroup.addChild(baseTG);
    }

    public void initGPS(double lat, double lon) {
        gpsProjector.init(lat, lon);
    }

    @Override
    protected Vector3d getForce() {
        if (startTime < 0)
            startTime = lastTime;
        Vector3d f = new Vector3d(velocity);
        f.scale(-f.length() * dragMove);
        Vector3d mg = new Vector3d(getWorld().getEnvironment().getG());
        mg.scale(-mass);
        f.add(mg);
        if (applyAccel) { // (lastTime - startTime > 30000)
            f.add(new Vector3d(0.0, Math.exp(-position.length() / 700.0) * mass
                    * 9.81 * 0.025, 0.0));
        } else {
            this.velocity.set(0, 0, 0);
        }

        return f;
    }

    static int tState = 0;
    static long lastReport = 0;

    @Override
    protected Vector3d getTorque() {
        switch (tState) {
        case 0:
            if ((lastTime - lastReport) > 1000) {
                lastReport = lastTime;
                out.println("target angular velocity: " + this.getRotationRate());
            }
            if (lastTime - startTime > 5000) {
                tState = 2;
                torque.set(0.0, 0.0, 0.0);
                System.out.println("Target torque: " + torque);
            }
            break;
        case 1:
            if (lastTime - startTime > 6000) {
                tState = 2;
                torque.set(0, 0, 0);
                System.out.println("Target torque: " + torque);
            }
            break;
        case 2:
            break;
        }
        return torque;
    }

    public GPSPosition getGPS() {
        double[] latlon = gpsProjector.reproject(getPosition().x,
                getPosition().y);
        GPSPosition gps = new GPSPosition();
        gps.lat = latlon[0];
        gps.lon = latlon[1];
        gps.alt = -getPosition().z;
        gps.eph = 1.0;
        gps.epv = 1.0;
        gps.vn = getVelocity().x;
        gps.ve = getVelocity().y;
        gps.vd = getVelocity().z;
        return gps;
    }
}
