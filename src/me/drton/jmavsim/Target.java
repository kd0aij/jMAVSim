package me.drton.jmavsim;

import com.sun.j3d.utils.geometry.Sphere;

import javax.media.j3d.Appearance;
import javax.media.j3d.Material;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Color3f;
import javax.vecmath.Vector3d;
import java.io.FileNotFoundException;

/**
 * User: ton Date: 01.02.14 Time: 22:12
 */
public class Target extends VisualObject {
    protected long startTime = -1;
    public double dragMove = 0.2;
    private GlobalPositionProjector gpsProjector = new GlobalPositionProjector();
    protected boolean move = false;

    public boolean isMove() {
        return move;
    }

    public void setMove(boolean move) {
        this.move = move;
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
        
        TransformGroup baseTG = new TransformGroup(new Transform3D());
        
        Sphere sphere = new Sphere((float) size);
        Appearance bodyApp = new Appearance();
        bodyApp.setMaterial(new Material(green, black, dimgreen, green, 64.0f));
        sphere.setAppearance(bodyApp);

        Transform3D head = new Transform3D();
        head.setTranslation(new Vector3d(0, 0, size));
        TransformGroup headTG = new TransformGroup(head);
        Sphere headShape = new Sphere((float) size/4);
        Appearance headApp = new Appearance();
        headApp.setMaterial(new Material(red, black, dimred, red, 64.0f));
        headShape.setAppearance(headApp);
        headTG.addChild(headShape);
        
        baseTG.addChild(sphere);
        baseTG.addChild(headTG);
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
        if (move) //(lastTime - startTime > 30000)
            f.add(new Vector3d(0.0, Math.exp(-position.length() / 700.0) * mass
                    * 9.81 * 0.025, 0.0));
        return f;
    }

    @Override
    protected Vector3d getTorque() {
        if (lastTime - startTime < 10000)
            return new Vector3d(0.5, 0.0, 0.0);
        else
            return new Vector3d(0.0, 0.0, 0.0);
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
