package me.drton.jmavsim;

import java.awt.Font;
import java.util.ArrayList;
import java.util.logging.Handler;
import java.util.logging.Logger;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Font3D;
import javax.media.j3d.FontExtrusion;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Light;
import javax.media.j3d.Material;
import javax.media.j3d.PointArray;
import javax.media.j3d.PointAttributes;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Text3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.swing.JFrame;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.behaviors.mouse.MouseTranslate;
import com.sun.j3d.utils.behaviors.mouse.MouseZoom;
import com.sun.j3d.utils.geometry.Cone;
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.geometry.Stripifier;

public class G3f {
	static private Logger logger;
	static {
		logger = Logger.getLogger("graphicsUtils");
		Handler[] handler = logger.getParent().getHandlers();
		handler[0].setFormatter(new BriefFormatter());
	}

    final public static Color3f black = new Color3f(0.0f, 0.0f, 0.0f);
    final public static Color3f grey = new Color3f(0.5f, 0.5f, 0.5f);
    final public static Color3f grey1 =  new Color3f(0.8f, 0.8f, 0.8f);
    final public static Color3f grey2 =  new Color3f(0.16f, 0.16f, 0.16f);
    final public static Color3f white = new Color3f(1.0f, 1.0f, 1.0f);

    final public static Color3f red =   new Color3f(1.0f, 0.0f, 0.0f);
    final public static Color3f dimred = new Color3f(0.2f, 0.0f, 0.0f);
    final public static Color3f orange =  new Color3f(0.67f, 0.33f, 0.0f);
    final public static Color3f yellow =  new Color3f(1f, 1f, 0.0f);
    final public static Color3f dimyellow = new Color3f(0.4f, 0.4f, 0.0f);
    final public static Color3f green = new Color3f(0.0f, 1.0f, 0.0f);
    final public static Color3f dimgreen = new Color3f(0.0f, 0.2f, 0.0f);
    final public static Color3f blue =  new Color3f(0.0f, 0.0f, 1.0f);
    final public static Color3f dimblue = new Color3f(0.0f, 0.0f, 0.2f);

    //  final static StaticColors sc = new StaticColors();
	static TransparencyAttributes transAttrs =
		new TransparencyAttributes(TransparencyAttributes.NICEST, 0.4f);
	static FontExtrusion fext = new FontExtrusion();
	static Font3D font = new Font3D(new Font("Arial", Font.PLAIN, 1), fext);

	// given a transform relative to the universe, draw a set of axes
	// make the axis arrows have length +/- length[i]/2
	public static TransformGroup axesCartesian (
		Transform3D t3d,
		double[] length,
		float scale,
		String name) {

		Appearance xAppearance = new Appearance();
		Appearance yAppearance = new Appearance();
		Appearance zAppearance = new Appearance();
		Appearance mxAppearance = new Appearance();
		Appearance myAppearance = new Appearance();
		Appearance mzAppearance = new Appearance();
		Appearance sAppearance = new Appearance();

		// ambient, emissive, diffuse, specular, shininess
		xAppearance.setMaterial(
			new Material(red, black, red, red, 64.0f));
		mxAppearance.setMaterial(
			new Material(dimred, black, dimred, dimred, 64.0f));

		yAppearance.setMaterial(
			new Material(green, black, green, green, 64.0f));
		myAppearance.setMaterial(
			new Material(
				dimgreen,
				black,
				dimgreen,
				dimgreen,
				64.0f));

		zAppearance.setMaterial(
			new Material(blue, black, blue, blue, 64.0f));
		mzAppearance.setMaterial(
			new Material(
				dimblue,
				black,
				dimblue,
				dimblue,
				64.0f));

		sAppearance.setMaterial(
			new Material(white, black, black, white, 1.0f));

		TransformGroup objMatrixGroup = new TransformGroup();
		objMatrixGroup.setTransform(t3d);

		String lTxt = String.format("%5.3f", length[1]) + ":" + name;
		Text3D yLabel = new Text3D(font, lTxt, new Point3f(0.0f, -1.5f, 0.0f));
		lTxt = String.format("%5.3f", length[0]);
		Text3D xLabel = new Text3D(font, lTxt, new Point3f(0.0f, -1.5f, 0.0f));
		lTxt = String.format("%5.3f", length[2]);
		Text3D zLabel = new Text3D(font, lTxt, new Point3f(0.0f, 0.5f, 0.0f));

		Shape3D tlabelY = new Shape3D();
		Transform3D ylTransform = new Transform3D();
		ylTransform.setScale(new Vector3d(-scale, scale, scale));
		TransformGroup labelGy = new TransformGroup(ylTransform);
		tlabelY.setAppearance(yAppearance);
		tlabelY.setGeometry(yLabel);
		labelGy.addChild(tlabelY);

		Shape3D tlabelX = new Shape3D();
		Transform3D xlTransform = new Transform3D();
		xlTransform.setScale(new Vector3d(scale, -scale, scale));
		TransformGroup labelGx = new TransformGroup(xlTransform);
		tlabelX.setAppearance(xAppearance);
		tlabelX.setGeometry(xLabel);
		labelGx.addChild(tlabelX);

		Shape3D tlabelZ = new Shape3D();
		Transform3D labelTransform = new Transform3D();
		labelTransform.setScale(scale);
		TransformGroup labelGz = new TransformGroup(labelTransform);
		tlabelZ.setAppearance(zAppearance);
		tlabelZ.setGeometry(zLabel);
		labelGz.addChild(tlabelZ);

		float cRadius = 0.2f * scale;

		// Create an arrow for the x axis.
		float cLen = (float)length[0];
		Transform3D xAxis = new Transform3D();
		xAxis.rotZ(-Math.PI / 2.0);
		TransformGroup xAxisG = new TransformGroup(xAxis);
		TransformGroup xArrow = axisArrow(cRadius, cLen, xAppearance);
		xAxisG.addChild(labelGx);
		xAxisG.addChild(xArrow);
		objMatrixGroup.addChild(xAxisG);

		// Create an arrow for the minus x axis.
		Transform3D mxAxis = new Transform3D();
		mxAxis.rotZ(Math.PI / 2.0);
		TransformGroup mxAxisG = new TransformGroup(mxAxis);
		TransformGroup mxArrow = axisArrow(cRadius, cLen, mxAppearance);
		mxAxisG.addChild(mxArrow);
		objMatrixGroup.addChild(mxAxisG);

		// Create an arrow for the y axis.
		cLen = (float)length[1];
		Transform3D yAxis = new Transform3D();
		TransformGroup yAxisG = new TransformGroup(yAxis);
		TransformGroup yArrow = axisArrow(cRadius, cLen, yAppearance);
		yAxisG.addChild(yArrow);
		objMatrixGroup.addChild(yAxisG);

		// Create an arrow for the minus y axis.
		Transform3D myAxis = new Transform3D();
		myAxis.rotZ(Math.PI);
		TransformGroup myAxisG = new TransformGroup(myAxis);
		TransformGroup myArrow = axisArrow(cRadius, cLen, myAppearance);
		myAxisG.addChild(labelGy);
		myAxisG.addChild(myArrow);
		objMatrixGroup.addChild(myAxisG);

		// Create an arrow for the z axis.
		cLen = (float)length[2];
		Transform3D zAxis = new Transform3D();
		zAxis.rotX(Math.PI / 2.0);
		TransformGroup zAxisG = new TransformGroup(zAxis);
		TransformGroup zArrow = axisArrow(cRadius, cLen, zAppearance);
		zAxisG.addChild(labelGz);
		zAxisG.addChild(zArrow);
		objMatrixGroup.addChild(zAxisG);

		// Create an arrow for the minus z axis.
		Transform3D mzAxis = new Transform3D();
		mzAxis.rotX(-Math.PI / 2.0);
		TransformGroup mzAxisG = new TransformGroup(mzAxis);
		TransformGroup mzArrow = axisArrow(cRadius, cLen, mzAppearance);
		mzAxisG.addChild(mzArrow);
		objMatrixGroup.addChild(mzAxisG);

		Sphere sphere = new Sphere(1.5f * cRadius, sAppearance);
		objMatrixGroup.addChild(sphere);

		return objMatrixGroup;
	}

	// given a transform relative to the universe, draw a set of axes
	// make the axis arrows have length +/- length[i]/2
	public static TransformGroup axesCartesian2(
		Transform3D t3d,
		double[] length,
		float scale,
		String name) {

		Appearance xAppearance = new Appearance();
		Appearance yAppearance = new Appearance();
		Appearance zAppearance = new Appearance();
		Appearance mxAppearance = new Appearance();
		Appearance myAppearance = new Appearance();
		Appearance mzAppearance = new Appearance();
		Appearance sAppearance = new Appearance();

		// ambient, emissive, diffuse, specular, shininess
		xAppearance.setMaterial(
			new Material(red, black, red, red, 64.0f));
		mxAppearance.setMaterial(
			new Material(dimred, black, dimred, dimred, 64.0f));

		yAppearance.setMaterial(
			new Material(green, black, green, green, 64.0f));
		myAppearance.setMaterial(
			new Material(
				dimgreen,
				black,
				dimgreen,
				dimgreen,
				64.0f));

		zAppearance.setMaterial(
			new Material(blue, black, blue, blue, 64.0f));
		mzAppearance.setMaterial(
			new Material(
				dimblue,
				black,
				dimblue,
				dimblue,
				64.0f));

		sAppearance.setMaterial(
			new Material(white, black, black, white, 1.0f));

		TransformGroup objMatrixGroup = new TransformGroup();
		objMatrixGroup.setTransform(t3d);

		Text3D yLabel = new Text3D(font, name, new Point3f(0.0f, -1.5f, 0.0f));

		Shape3D tlabelY = new Shape3D();
		Transform3D ylTransform = new Transform3D();
		ylTransform.setScale(new Vector3d(-scale, scale, scale));
		TransformGroup labelGy = new TransformGroup(ylTransform);
		tlabelY.setAppearance(yAppearance);
		tlabelY.setGeometry(yLabel);
		labelGy.addChild(tlabelY);

		float cRadius = 0.2f * scale;

		// Create an arrow for the x axis.
		float cLen = (float)length[0];
		Transform3D xAxis = new Transform3D();
		xAxis.rotZ(-Math.PI / 2.0);
		TransformGroup xAxisG = new TransformGroup(xAxis);
		TransformGroup xArrow = axisArrow(cRadius, cLen, xAppearance);
		xAxisG.addChild(xArrow);
		objMatrixGroup.addChild(xAxisG);

		// Create an arrow for the y axis.
		cLen = (float)length[1];
		Transform3D yAxis = new Transform3D();
		TransformGroup yAxisG = new TransformGroup(yAxis);
		TransformGroup yArrow = axisArrow(cRadius, cLen, yAppearance);
		yAxisG.addChild(yArrow);
		objMatrixGroup.addChild(yAxisG);

		// Create an arrow for the z axis.
		cLen = (float)length[2];
		Transform3D zAxis = new Transform3D();
		zAxis.rotX(Math.PI / 2.0);
		TransformGroup zAxisG = new TransformGroup(zAxis);
		TransformGroup zArrow = axisArrow(cRadius, cLen, zAppearance);
		zAxisG.addChild(zArrow);
		objMatrixGroup.addChild(zAxisG);

		Sphere sphere = new Sphere(1.5f * cRadius, sAppearance);
		objMatrixGroup.addChild(sphere);

		return objMatrixGroup;
	}

	// given a set of vectors and lengths, draw coordinate frames
	// make the axis arrows have length +/- length[i]/2
	public static TransformGroup axesGeneral(
		Transform3D t3d,
		double[][] axis,
		double[] length,
		float scale,
		String name) {

		Appearance xAppearance = new Appearance();
		Appearance yAppearance = new Appearance();
		Appearance zAppearance = new Appearance();
		Appearance mxAppearance = new Appearance();
		Appearance myAppearance = new Appearance();
		Appearance mzAppearance = new Appearance();
		Appearance sAppearance = new Appearance();

		// ambient, emissive, diffuse, specular, shininess
		xAppearance.setMaterial(
			new Material(red, black, dimred, red, 64.0f));
		mxAppearance.setMaterial(
			new Material(dimred, black, dimred, yellow, 64.0f));

		yAppearance.setMaterial(
			new Material(green, black, dimgreen, green, 64.0f));
		myAppearance.setMaterial(
			new Material(
				dimgreen,
				black,
				dimgreen,
				yellow,
				64.0f));

		zAppearance.setMaterial(
			new Material(blue, black, dimblue, blue, 64.0f));
		mzAppearance.setMaterial(
			new Material(
				dimblue,
				black,
				dimblue,
				yellow,
				64.0f));

		sAppearance.setMaterial(
			new Material(white, black, black, white, 1.0f));

		TransformGroup objMatrixGroup = new TransformGroup();
		objMatrixGroup.setTransform(t3d);

		String lTxt = String.format("%5.3f", length[1]) + ":" + name;
		Text3D yLabel = new Text3D(font, lTxt, new Point3f(0.0f, -1.5f, 0.0f));
		lTxt = String.format("%5.3f", length[0]);
		Text3D xLabel = new Text3D(font, lTxt, new Point3f(0.0f, -1.5f, 0.0f));
		lTxt = String.format("%5.3f", length[2]);
		Text3D zLabel = new Text3D(font, lTxt, new Point3f(0.0f, 0.5f, 0.0f));

		Shape3D tlabelY = new Shape3D();
		Transform3D ylTransform = new Transform3D();
		ylTransform.setScale(new Vector3d(-scale, scale, scale));
		TransformGroup labelGy = new TransformGroup(ylTransform);
		tlabelY.setAppearance(yAppearance);
		tlabelY.setGeometry(yLabel);
		labelGy.addChild(tlabelY);

		Shape3D tlabelX = new Shape3D();
		Transform3D xlTransform = new Transform3D();
		xlTransform.setScale(new Vector3d(scale, -scale, scale));
		TransformGroup labelGx = new TransformGroup(xlTransform);
		tlabelX.setAppearance(xAppearance);
		tlabelX.setGeometry(xLabel);
		labelGx.addChild(tlabelX);

		Shape3D tlabelZ = new Shape3D();
		Transform3D labelTransform = new Transform3D();
		labelTransform.setScale(scale);
		TransformGroup labelGz = new TransformGroup(labelTransform);
		tlabelZ.setAppearance(zAppearance);
		tlabelZ.setGeometry(zLabel);
		labelGz.addChild(tlabelZ);

		float cRadius = 0.2f * scale;

		// Create an arrow for the x axis with direction axis[0]
		// by rotating y into axis[0]
		Vector3d yCrossX = new Vector3d(0, 1, 0); // init to yHat
		Vector3d vX = new Vector3d(axis[0]);
		double phi = yCrossX.angle(vX); // amount of rotation
		yCrossX.cross(yCrossX, vX); // about yCrossX
		AxisAngle4d xAA = new AxisAngle4d(yCrossX, phi);
		Transform3D xAxis = new Transform3D();
		xAxis.set(xAA);
		TransformGroup xAxisG = new TransformGroup(xAxis);

		float cLen = (float)length[0] / 2;
		TransformGroup xArrow = axisArrow(cRadius, cLen, xAppearance);
		xAxisG.addChild(labelGx);
		xAxisG.addChild(xArrow);
		objMatrixGroup.addChild(xAxisG);

		// Create an arrow for the minus x axis.
		Transform3D mxAxis = new Transform3D();
		xAA.angle += Math.PI;
		mxAxis.set(xAA);
		TransformGroup mxAxisG = new TransformGroup(mxAxis);
		TransformGroup mxArrow = axisArrowMinus(cRadius, cLen, mxAppearance);
		mxAxisG.addChild(mxArrow);
		objMatrixGroup.addChild(mxAxisG);

		// Create an arrow for the y axis with direction axis[1].
		// by rotating y into axis[1]
		Vector3d yCrossY = new Vector3d(0, 1, 0); // init to yHat
		Vector3d vY = new Vector3d(axis[1]);
		phi = yCrossY.angle(vY); // amount of rotation
		yCrossY.cross(yCrossY, vY); // about yCrossY
		AxisAngle4d yAA = new AxisAngle4d(yCrossY, phi);
		Transform3D yAxis = new Transform3D();
		yAxis.set(yAA);
		TransformGroup yAxisG = new TransformGroup(yAxis);

		cLen = (float)length[1] / 2;
		TransformGroup yArrow = axisArrow(cRadius, cLen, yAppearance);
		yAxisG.addChild(yArrow);
		objMatrixGroup.addChild(yAxisG);

		// Create an arrow for the minus y axis.
		Transform3D myAxis = new Transform3D();
		yAA.angle += Math.PI;
		myAxis.set(yAA);
		TransformGroup myAxisG = new TransformGroup(myAxis);
		TransformGroup myArrow = axisArrow(cRadius, cLen, myAppearance);
		myAxisG.addChild(labelGy);
		myAxisG.addChild(myArrow);
		objMatrixGroup.addChild(myAxisG);

		// Create an arrow for the z axis with direction axis[2].
		// by rotating y into axis[2]
		Vector3d yCrossZ = new Vector3d(0, 1, 0); // init to yHat
		Vector3d vZ = new Vector3d(axis[2]);
		phi = yCrossZ.angle(vZ); // amount of rotation
		yCrossZ.cross(yCrossZ, vZ); // about yCrossZ
		AxisAngle4d zAA = new AxisAngle4d(yCrossZ, phi);
		Transform3D zAxis = new Transform3D();
		zAxis.set(zAA);
		TransformGroup zAxisG = new TransformGroup(zAxis);

		cLen = (float)length[2] / 2;
		TransformGroup zArrow = axisArrow(cRadius, cLen, zAppearance);
		zAxisG.addChild(labelGz);
		zAxisG.addChild(zArrow);
		objMatrixGroup.addChild(zAxisG);

		// Create an arrow for the minus z axis.
		Transform3D mzAxis = new Transform3D();
		zAA.angle += Math.PI;
		mzAxis.set(zAA);
		TransformGroup mzAxisG = new TransformGroup(mzAxis);
		TransformGroup mzArrow = axisArrow(cRadius, cLen, mzAppearance);
		mzAxisG.addChild(mzArrow);
		objMatrixGroup.addChild(mzAxisG);

		Sphere sphere = new Sphere(1.5f * cRadius, sAppearance);
		objMatrixGroup.addChild(sphere);

		return objMatrixGroup;
	}

	public static TransformGroup axisArrow(
		float cRadius,
		float cLen,
		Appearance app) {

		// Create a cone for the arrow shaft
		Cone shaft = new Cone(cRadius, cLen, Cone.GENERATE_NORMALS, app);

		Transform3D yAxis = new Transform3D();
		yAxis.setTranslation(new Vector3d(0.0, cLen / 2, 0.0));
		TransformGroup arrowTG = new TransformGroup(yAxis);
		arrowTG.addChild(shaft);

		float headLen = cLen / 4;
		Cone zconeX =
			new Cone(2 * cRadius, headLen, Cone.GENERATE_NORMALS, app);

		Transform3D headX = new Transform3D();
		headX.setTranslation(new Vector3d(0.0, cLen - 2.5 * headLen, 0.0));
		headX.setScale(new Vector3d(1.0, 1.0, 0.03));
		TransformGroup headGX = new TransformGroup(headX);
		headGX.addChild(zconeX);
		arrowTG.addChild(headGX);

		Cone zconeZ =
			new Cone(2 * cRadius, headLen, Cone.GENERATE_NORMALS, app);

		Transform3D headZ = new Transform3D();
		headZ.setTranslation(new Vector3d(0.0, cLen - 2.5 * headLen, 0.0));
		headZ.setScale(new Vector3d(0.03, 1.0, 1.0));
		TransformGroup headGZ = new TransformGroup(headZ);
		headGZ.addChild(zconeZ);
		arrowTG.addChild(headGZ);

		return arrowTG;
	}

	public static TransformGroup mouseBehavior(
		Transform3D t3d,
		double extent) {

		// This TransformGroup will be used by the MouseTranslate
		// utility to move the objects around the canvas.
		TransformGroup moveTGroup = new TransformGroup(t3d);
		moveTGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		moveTGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

		BoundingSphere bounds =
			new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 1000000.0);

		// when extents are from 100mm to 10,000mm we used mouseZ.setFactor(-12)
		double scale = extent / 10000;

		MouseTranslate mouseT = new MouseTranslate(moveTGroup);
		mouseT.setFactor(scale * .75, -scale * .75);
		moveTGroup.addChild(mouseT);
		mouseT.setSchedulingBounds(bounds);

		MouseRotate mouseR = new MouseRotate(moveTGroup);
		mouseR.setFactor(-.0125, .0125);
		moveTGroup.addChild(mouseR);
		mouseR.setSchedulingBounds(bounds);

		MouseZoom mouseZ = new MouseZoom(moveTGroup);
		// *** fix this ***
		mouseZ.setFactor(-scale * 12);
		moveTGroup.addChild(mouseZ);
		mouseZ.setSchedulingBounds(bounds);
		return moveTGroup;
	}

	static AmbientLight ambientLight;
	public static TransformGroup createLights(Vector3d position) {
		BoundingSphere amBound =
			new BoundingSphere(
				new Point3d(0.0, 0.0, 0.0),
				10 * position.length());

		Transform3D lightingT3d = new Transform3D();
		//		position.x += 5000;
		lightingT3d.setTranslation(position);
		TransformGroup lightingTg = new TransformGroup(lightingT3d);

		// add ambient light
		Color3f lightColor = new Color3f(1f, 1f, 1f);

		ambientLight = new AmbientLight(lightColor);
		ambientLight.setCapability(Light.ALLOW_STATE_WRITE);
		ambientLight.setInfluencingBounds(amBound);

		lightingTg.addChild(ambientLight);

		// add directional lights
		//		DirectionalLight dl =
		//			new DirectionalLight(true, lightColor, new Vector3f(-1f, 1f, 1f));
		//		dl.setCapability(Light.ALLOW_STATE_WRITE);
		//		lightingTg.addChild(dl);
		//
		//		DirectionalLight dl2 =
		//			new DirectionalLight(true, lightColor, new Vector3f(1f, 1f, 1f));
		//		dl2.setCapability(Light.ALLOW_STATE_WRITE);
		//		dl2.setInfluencingBounds(amBound);
		//		dl.setInfluencingBounds(amBound);
		//		lightingTg.addChild(dl2);

		return lightingTg;
	}

	public static TransformGroup axisArrowMinus(
		float cRadius,
		float cLen,
		Appearance app) {

		// Create a cone for the arrow shaft
		Cone shaft = new Cone(cRadius, cLen, Cone.GENERATE_NORMALS, app);

		Transform3D yAxis = new Transform3D();
		yAxis.setTranslation(new Vector3d(0.0, cLen / 2, 0.0));
		TransformGroup arrowTG = new TransformGroup(yAxis);
		arrowTG.addChild(shaft);

		float headLen = cLen / 4;
		Cone zconeX =
			new Cone(2 * cRadius, headLen, Cone.GENERATE_NORMALS, app);

		Transform3D headX = new Transform3D();
		headX.set(new Vector3d(0.0, cLen - 2.5 * headLen, 0.0));
		headX.setScale(new Vector3d(1.0, 1.0, 0.03));
		// flip arrow in Y
		Transform3D rot = new Transform3D();
		rot.rotX(Math.PI);
		headX.mul(rot);
		TransformGroup headGX = new TransformGroup(headX);
		headGX.addChild(zconeX);
		arrowTG.addChild(headGX);

		Cone zconeZ =
			new Cone(2 * cRadius, headLen, Cone.GENERATE_NORMALS, app);

		Transform3D headZ = new Transform3D();
		headZ.set(new Vector3d(0.0, cLen - 2.5 * headLen, 0.0));
		headZ.setScale(new Vector3d(0.03, 1.0, 1.0));
		headZ.mul(rot);
		TransformGroup headGZ = new TransformGroup(headZ);
		headGZ.addChild(zconeZ);
		arrowTG.addChild(headGZ);

		return arrowTG;
	}

	public static void makeTransparent(Shape3D shape, float level) {
		TransparencyAttributes transAttrs =
			new TransparencyAttributes(TransparencyAttributes.NICEST, level);
		shape.getAppearance().setTransparencyAttributes(transAttrs);
	}

	public static BranchGroup drawPoints(
		float[] coords,
		int size,
		Color3f color,
		float transparency,
		Point3d center) {
		PointArray points =
			new PointArray(coords.length, PointArray.COORDINATES);
		points.setCoordinates(0, coords);

		// Create an Appearance for the points
		Appearance look = new Appearance();
		ColoringAttributes ca = new ColoringAttributes();
		ca.setColor(color);
		look.setColoringAttributes(ca);

		PointAttributes pa = new PointAttributes(size, true);
		look.setPointAttributes(pa);

		Shape3D pointArray = new Shape3D(points, look);
		if (transparency > 0)
			makeTransparent(pointArray, transparency);

		Transform3D t3d = new Transform3D();
		t3d.setTranslation(new Vector3f(center));
		TransformGroup tg1 = new TransformGroup(t3d);
		tg1.addChild(pointArray);

		BranchGroup bg1 = new BranchGroup();
		bg1.setCapability(BranchGroup.ALLOW_DETACH);
		bg1.addChild(tg1);
		return bg1;
	}

	final static int ULC = 0;
	final static int ULCm = 1;
	final static int ULCb = 2;
	final static int URC = 3;
	final static int URCm = 4;
	final static int URCb = 5;
	final static int LLC = 6;
	final static int LLCm = 7;
	final static int LLCb = 8;
	final static int LRC = 9;
	final static int LRCm = 10;
	final static int LRCb = 11;
	final static int UCC = 12;
	final static int UCCb = 13;
	final static int LCC = 14;
	final static int LCCb = 15;
	final static int CRC = 16;
	final static int CRCb = 17;
	final static int CLC = 18;
	final static int CLCb = 19;
	final static float[][] offset = new float[][] { { -1, -1, -1 }, {
			-1, -1, 0 }, {
			-1, -1, 1 }, {
			1, -1, -1 }, {
			1, -1, 0 }, {
			1, -1, 1 }, {
			-1, 1, -1 }, {
			-1, 1, 0 }, {
			-1, 1, 1 }, {
			1, 1, -1 }, {
			1, 1, 0 }, {
			1, 1, 1 }, {
			0, -1, -1 }, {
			0, -1, 1 }, {
			0, 1, -1 }, {
			0, 1, 1 }, {
			-1, 0, -1 }, {
			-1, 0, 1 }, {
			1, 0, -1 }, {
			1, 0, 1 }
	};

	static void addVertex(
		Point3f p,
		int type,
		float xSize,
		float ySize,
		float zSize,
		ArrayList bQuads) {

		// init to offset for appropriate corner
		Point3f v = new Point3f(offset[type]);
		// Each corner is set by adding or subtracting Size; this makes the
		// face span 2*Size.  Divide each Size by 2.
		// scale offsets to desired size
		v.x *= xSize / 2;
		v.y *= ySize / 2;
		v.z *= zSize / 2;
		// shift to point p
		v.add(p);
		// add to vertex list
		bQuads.add(v);
	}
	// construct crosses at each location in points
	public static BranchGroup drawCrosses(
		Point3f[] points,
		float size,
		Color3f color,
		Point3d center) {

		ArrayList bQuads = new ArrayList();
		float xSize = size;
		float ySize = size;
		float small = size / 10;
		for (int i = 0; i < points.length; i++) {
			// z center faces
			addVertex(points[i], ULCm, xSize, small, 0, bQuads);
			addVertex(points[i], LLCm, xSize, small, 0, bQuads);
			addVertex(points[i], LRCm, xSize, small, 0, bQuads);
			addVertex(points[i], URCm, xSize, small, 0, bQuads);
			addVertex(points[i], ULCm, small, ySize, 0, bQuads);
			addVertex(points[i], LLCm, small, ySize, 0, bQuads);
			addVertex(points[i], LRCm, small, ySize, 0, bQuads);
			addVertex(points[i], URCm, small, ySize, 0, bQuads);
		}
		int nVertices = bQuads.size();
		Point3f[] qVertices = new Point3f[nVertices];
		bQuads.toArray(qVertices);

		GeometryInfo cloud = new GeometryInfo(GeometryInfo.QUAD_ARRAY);
		cloud.setCoordinates(qVertices);

		// Create an Appearance for the cloud
		Appearance look = new Appearance();
		ColoringAttributes ca = new ColoringAttributes();
		ca.setColor(color);
		look.setColoringAttributes(ca);

		// setting only diffuse coefficient non-zero results in constant hue
		// for all orientations
		Point3f spec = new Point3f(color);
		spec.scale(0.1f);
		Color3f spec3f = new Color3f(spec);
		Material mat = new Material(color, black, color, spec3f, 32.0f);
		look.setMaterial(mat);

		PolygonAttributes pa = new PolygonAttributes();
		pa.setCullFace(PolygonAttributes.CULL_NONE);
		pa.setBackFaceNormalFlip(true);
		look.setPolygonAttributes(pa);

		NormalGenerator ng = new NormalGenerator();
		ng.generateNormals(cloud);

		Stripifier st = new Stripifier();
		st.stripify(cloud);

		GeometryArray cArray = cloud.getIndexedGeometryArray();
		cArray.setCapability(GeometryArray.ALLOW_COORDINATE_READ);
		cArray.setCapability(GeometryArray.ALLOW_COUNT_READ);
		cArray.setCapability(GeometryArray.ALLOW_FORMAT_READ);
		cArray.setCapability(GeometryArray.ALLOW_INTERSECT);
		cArray.setCapability(GeometryArray.ALLOW_REF_DATA_READ);

		Shape3D crossShape = new Shape3D(cArray, look);

		Transform3D t3d = new Transform3D();
		t3d.setTranslation(new Vector3f(center));
		TransformGroup tg1 = new TransformGroup(t3d);
		tg1.addChild(crossShape);

		BranchGroup bg1 = new BranchGroup();
		bg1.setCapability(BranchGroup.ALLOW_DETACH);
		bg1.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		bg1.addChild(tg1);
		bg1.setPickable(true);
		return bg1;
	}
}