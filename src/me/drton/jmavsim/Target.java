package me.drton.jmavsim;

import com.sun.j3d.utils.geometry.Sphere;

import javax.media.j3d.Material;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Color3f;
import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector4d;

import sun.reflect.generics.tree.BaseType;

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
    // center of mass in body frame
    private Vector3d c_M = new Vector3d();
    private Vector3d leg = new Vector3d();
    private Transform3D baseT3D = new Transform3D();

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
            wrx.mul(e.w);
            wrx.mul(wrx);
            I_c.add(wrx);
        }
        I_c.negate();
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
        int N = 8;
        double mass = 1.0 / N;
        for (int n = 0; n < N; n++) {
            double theta = n * 2 * Math.PI / N;
            double ctheta = Math.cos(theta);
            double stheta = Math.sin(theta);
            masses.add(new Vector4d(size * ctheta, size * stheta, 0, mass));
        }
        // masses.add(new Vector4d(0, 0, -5 * size, 1 * mass));
        double legLen = 3 * size;
        masses.add(new Vector4d(0, 0, legLen, 1 * mass));

        momentOfInertia.set(calcI_c(masses));
        momentOfInertiaInv.invert(momentOfInertia);

        // z axis points downward
        leg.set(0, 0, legLen);
        leg.sub(c_M);

        // display origin is at center of mass
        TransformGroup baseTG = new TransformGroup(baseT3D);

        // create a Sphere for each point mass
        for (Vector4d e : masses) {
            Transform3D pointMassT3D = new Transform3D();
            Vector3d massC = new Vector3d(e.x, e.y, e.z);
            massC.sub(c_M);
            pointMassT3D.setTranslation(massC);
            TransformGroup pointMassTG = new TransformGroup(pointMassT3D);
            float dSize = (float) (2 * e.w * size / getMass());
            if (e.w == 0)
                dSize = (float) (size / 4);
            Sphere massShape = new Sphere(dSize);
            massShape.getAppearance().setMaterial(
                    new Material(red, black, dimred, red, 64.0f));
            pointMassTG.addChild(massShape);
            baseTG.addChild(pointMassTG);
        }

        // draw center of mass at origin
        Transform3D cMassT3D = new Transform3D();
        TransformGroup pointMassTG = new TransformGroup(cMassT3D);
        Sphere massShape = new Sphere((float) size / 10);
        massShape.getAppearance().setMaterial(
                new Material(black, black, black, black, 64.0f));
        pointMassTG.addChild(massShape);
        baseTG.addChild(pointMassTG);

        transformGroup.addChild(baseTG);
    }

    public void initGPS(double lat, double lon) {
        gpsProjector.init(lat, lon);
    }

    Report gfReport = new Report(10);

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
            // this.velocity.set(0, 0, 0);
        }
        // Vector3d legForce = new Vector3d(gyroAcc);
        // legForce.scale(-1 / leg.length());
        // rotation.transform(legForce);
        // f.add(new Vector3d(legForce.x, legForce.y, 0));
        // gfReport.report_now(legForce, "legForce");
        return f;
    }

    static int tState = 0;
    static long lastImpulse;
    Report gtReport = new Report(1000);
    double damping = 0.002, zdamping = 0;
    Vector3d impT = new Vector3d(1, 0, 0);

    @Override
    protected Vector3d getTorque() {
        long clockTime = System.currentTimeMillis();

        // damp body frame x and y rotation
        Vector3d wDamping = new Vector3d(rotationRate);
        wDamping.scale(-damping);
        wDamping.setZ(-zdamping * rotationRate.z);
        torque.set(wDamping);
        switch (tState) {
        case 0: // spin up
            damping = 0;
            zdamping = 0;
            Vector3d delT = new Vector3d(0.0, 0.0, 0.02);
            torque.add(delT);
            if (rotationRate.length() >= 4) {
                gtReport.report_now(gyroAcc, "gyroAcc");
                out.println("^^^ finished spin-up");
                tState = 2;
            }
            break;
        case 1:
            damping = .01;
            zdamping = .01;
            if (rotationRate.length() < 1e-2) {
                gtReport.report_now(gyroAcc, "gyroAcc");
                out.println("^^^ finished spin-down");
                tState = 4;
                // damping = 0.002;
                zdamping = 0;
                // rotation.setIdentity();
            }
            break;
        case 2:
            // and apply an impulse
            Vector3d impulse = new Vector3d(impT);
            Matrix3d earth2body = new Matrix3d(rotation);
            earth2body.transpose();
            earth2body.transform(impulse);
//            impulse.scale(.2);
            torque.add(impulse);
//             impT.scale(-1);
            // damping = .002;
            out.println("Y torque impulse\n");
            tState = 3;
            break;
        case 3:
            delT = new Vector3d(0.0, 0.0, 0.0);
            torque.add(delT);
            // delay then back to state 2
            if (clockTime - lastImpulse >= 5000) {
                lastImpulse = clockTime;
                tState = 2;
            }
            break;
        case 4:
            delT = new Vector3d(0.0, 0.0, 0.0);
            torque.add(delT);
            break;
        }
        gtReport.report_periodically(gyroAcc, "gyroAcc");
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

    class Report {
        long lastReport = 0;
        long rptInterval = 100;
        long baseTime;

        public Report(long rptInterval) {
            super();
            this.rptInterval = rptInterval;
            this.baseTime = System.currentTimeMillis();
        }

        private void report_periodically(Vector3d v, String label) {
            if ((lastTime - lastReport) >= rptInterval) {
                lastReport = lastTime;
                report_now(v, label);
            }
        }

        protected void report_now(Vector3d v, String label) {
            out.format("%f: wMag: %5.3f, omega: (%5.3f, %5.3f, %5.3f),  ",
                    (lastTime - baseTime) / 1000.0, rotationRate.length(),
                    rotationRate.x, rotationRate.y, rotationRate.z);
            out.format(
                    "torque: (%5.3f, %5.3f, %5.3f), %s: (%8.7f, %8.7f, %8.7f), "
                            + "pos: (%5.3f, %5.3f, %5.3f)\n", torque.x,
                    torque.y, torque.z, label, v.x, v.y, v.z, position.x,
                    position.y, position.z);
        }
    }
}
